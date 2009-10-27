/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import com.kenai.constantine.Constant;
import com.kenai.constantine.platform.Errno;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Map;

import org.jruby.ext.posix.FileStat;
import org.jruby.ext.posix.JavaPOSIX;
import org.jruby.ext.posix.POSIX;
import org.jruby.ext.posix.POSIXFactory;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.imp;
import org.python.core.io.FileDescriptors;
import org.python.core.io.FileIO;
import org.python.core.io.RawIOBase;
import org.python.core.util.RelativeFile;
import org.python.core.util.StringUtil;

/**
 * The underlying _posix or _nt module, named depending on the platform.
 *
 * This currently contains only some of the basics of the posix/nt modules (which are
 * implemented in Python), most importantly things like PythonPOSIXHandler that are slower
 * to instantiate and thus would affect startup time.
 *
 * Eventually more if not all of the pure Python module should end up here.
 */
public class PosixModule implements ClassDictInit {

    public static final PyString __doc__ = new PyString(
        "This module provides access to operating system functionality that is\n" +
        "standardized by the C Standard and the POSIX standard (a thinly\n" +
        "disguised Unix interface).  Refer to the library manual and\n" +
        "corresponding Unix manual entries for more information on calls.");

    /** Current OS information. */
    private static OS os = OS.getOS();

    /** Platform specific POSIX services. */
    private static POSIX posix = POSIXFactory.getPOSIX(new PythonPOSIXHandler(), true);

    /** os.open flags. */
    private static int O_RDONLY = 0x0;
    private static int O_WRONLY = 0x1;
    private static int O_RDWR = 0x2;
    private static int O_APPEND = 0x8;
    private static int O_SYNC = 0x80;
    private static int O_CREAT = 0x200;
    private static int O_TRUNC = 0x400;
    private static int O_EXCL = 0x800;

    /** os.access constants. */
    private static int F_OK = 0;
    private static int X_OK = 1 << 0;
    private static int W_OK = 1 << 1;
    private static int R_OK = 1 << 2;

    /** os.path.realpath function for use by chdir. Lazily loaded. */
    private static PyObject realpath;

    public static void classDictInit(PyObject dict) {
        // only expose the open flags we support
        dict.__setitem__("O_RDONLY", Py.newInteger(O_RDONLY));
        dict.__setitem__("O_WRONLY", Py.newInteger(O_WRONLY));
        dict.__setitem__("O_RDWR", Py.newInteger(O_RDWR));
        dict.__setitem__("O_APPEND", Py.newInteger(O_APPEND));
        dict.__setitem__("O_SYNC", Py.newInteger(O_SYNC));
        dict.__setitem__("O_CREAT", Py.newInteger(O_CREAT));
        dict.__setitem__("O_TRUNC", Py.newInteger(O_TRUNC));
        dict.__setitem__("O_EXCL", Py.newInteger(O_EXCL));

        dict.__setitem__("F_OK", Py.newInteger(F_OK));
        dict.__setitem__("X_OK", Py.newInteger(X_OK));
        dict.__setitem__("W_OK", Py.newInteger(W_OK));
        dict.__setitem__("R_OK", Py.newInteger(R_OK));
        // Successful termination
        dict.__setitem__("EX_OK", Py.Zero);

        dict.__setitem__("environ", getEnviron());
        dict.__setitem__("error", Py.OSError);
        dict.__setitem__("stat_result", PyStatResult.TYPE);
        dict.__setitem__("_posix_impl", Py.java2py(posix));
        dict.__setitem__("_native_posix", Py.newBoolean(!(posix instanceof JavaPOSIX)));

        // Hide from Python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("getPOSIX", null);
        dict.__setitem__("getOSName", null);
        dict.__setitem__("badFD", null);

        dict.__setitem__("__all__", dict.invoke("keys"));

        dict.__setitem__("__name__", new PyString("_" + os.getModuleName()));
        dict.__setitem__("__doc__", __doc__);
    }

    public static PyString __doc___exit = new PyString(
        "_exit(status)\n\n" +
        "Exit to the system with specified status, without normal exit processing.");
    public static void _exit() {
        _exit(0);
    }

    public static void _exit(int status) {
        System.exit(status);
    }

    public static PyString __doc__access = new PyString(
        "access(path, mode) -> True if granted, False otherwise\n\n" +
        "Use the real uid/gid to test for access to a path.  Note that most\n" +
        "operations will use the effective uid/gid, therefore this routine can\n" +
        "be used in a suid/sgid environment to test if the invoking user has the\n" +
        "specified access to the path.  The mode argument can be F_OK to test\n" +
        "existence, or the inclusive-OR of R_OK, W_OK, and X_OK.");
    public static boolean access(String path, int mode) {
        ensurePath(path);
        boolean result = true;
        File file = new RelativeFile(path);

        if (!file.exists()) {
            result = false;
        }
        if ((mode & R_OK) != 0 && !file.canRead()) {
            result = false;
        }
        if ((mode & W_OK) != 0 && !file.canWrite()) {
            result = false;
        }
        if ((mode & X_OK) != 0) {
            // NOTE: always true without native jna-posix stat
            try {
                result = posix.stat(file.getPath()).isExecutable();
            } catch (PyException pye) {
                if (!pye.match(Py.OSError)) {
                    throw pye;
                }
                // ENOENT
                result = false;
            }
        }
        return result;
    }

    public static PyString __doc__chdir = new PyString(
        "chdir(path)\n\n" +
        "Change the current working directory to the specified path.");
    public static void chdir(PyObject pathObj) {
        if (!(pathObj instanceof PyString)) {
            throw Py.TypeError(String.format("coercing to Unicode: need string, '%s' type found",
                                             pathObj.getType().fastGetName()));
        }

        String path = pathObj.toString();
        // stat raises ENOENT for us if path doesn't exist
        if (!posix.stat(absolutePath(path)).isDirectory()) {
            throw Py.OSError(Errno.ENOTDIR, path);
        }

        if (realpath == null) {
            realpath = imp.load("os.path").__getattr__("realpath");
        }
        Py.getSystemState().setCurrentWorkingDir(realpath.__call__(pathObj).toString());
    }

    public static PyString __doc__chmod = new PyString(
        "chmod(path, mode)\n\n" +
        "Change the access permissions of a file.");
    public static void chmod(String path, int mode) {
        if (posix.chmod(absolutePath(path), mode) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__chown = new PyString(
        "chown(path, uid, gid)\n\n" +
        "Change the owner and group id of path to the numeric uid and gid.");
    public static void chown(String path, int uid, int gid) {
        if (posix.chown(absolutePath(path), uid, gid) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__close = new PyString(
        "close(fd)\n\n" +
        "Close a file descriptor (for low level IO).");
    public static void close(PyObject fd) {
        try {
            FileDescriptors.get(fd).close();
        } catch (PyException pye) {
            throw badFD();
        }
    }

    public static PyString __doc__fdopen = new PyString(
        "fdopen(fd [, mode='r' [, bufsize]]) -> file_object\n\n" +
        "Return an open file object connected to a file descriptor.");
    public static PyObject fdopen(PyObject fd) {
        return fdopen(fd, "r");

    }

    public static PyObject fdopen(PyObject fd, String mode) {
        return fdopen(fd, mode, -1);

    }
    public static PyObject fdopen(PyObject fd, String mode, int bufsize) {
        if (mode.length() == 0 || !"rwa".contains("" + mode.charAt(0))) {
            throw Py.ValueError(String.format("invalid file mode '%s'", mode));
        }
        RawIOBase rawIO = FileDescriptors.get(fd);
        if (rawIO.closed()) {
            throw badFD();
        }

        try {
            return new PyFile(rawIO, "<fdopen>", mode, bufsize);
        } catch (PyException pye) {
            if (!pye.match(Py.IOError)) {
                throw pye;
            }
            throw Py.OSError(Errno.EINVAL);
        }
    }

    public static PyString __doc__ftruncate = new PyString(
        "ftruncate(fd, length)\n\n" +
        "Truncate a file to a specified length.");
    public static void ftruncate(PyObject fd, long length) {
        try {
            FileDescriptors.get(fd).truncate(length);
        } catch (PyException pye) {
            throw Py.IOError(Errno.EBADF);
        }
    }

    public static PyString __doc___getcwd = new PyString(
        "getcwd() -> path\n\n" +
        "Return a string representing the current working directory.");
    public static PyObject getcwd() {
        return Py.newString(Py.getSystemState().getCurrentWorkingDir());
    }

    public static PyString __doc___getcwdu = new PyString(
        "getcwd() -> path\n\n" +
        "Return a unicode string representing the current working directory.");
    public static PyObject getcwdu() {
        return Py.newUnicode(Py.getSystemState().getCurrentWorkingDir());
    }

    public static PyString __doc__listdir = new PyString(
        "listdir(path) -> list_of_strings\n\n" +
        "Return a list containing the names of the entries in the directory.\n\n" +
        "path: path of directory to list\n\n" +
        "The list is in arbitrary order.  It does not include the special\n" +
        "entries '.' and '..' even if they are present in the directory.");
    public static PyList listdir(String path) {
        ensurePath(path);
        PyList list = new PyList();
        File file = new RelativeFile(path);
        String[] names = file.list();

        if (names == null) {
            // Can't read the path for some reason. stat will throw an error if it can't
            // read it either
            FileStat stat = posix.stat(file.getPath());
            // It exists, maybe not a dir, or we don't have permission?
            if (!stat.isDirectory()) {
                throw Py.OSError(Errno.ENOTDIR, path);
            }
            if (!file.canRead()) {
                throw Py.OSError(Errno.EACCES, path);
            }
            // 
            throw Py.OSError("listdir(): an unknown error occured: " + path);
        }
        for (String name : names) {
            list.append(new PyString(name));
        }
        return list;
    }

    public static PyString __doc__lseek = new PyString(
        "lseek(fd, pos, how) -> newpos\n\n" +
        "Set the current position of a file descriptor.");
    public static long lseek(PyObject fd, long pos, int how) {
        try {
            return FileDescriptors.get(fd).seek(pos, how);
        } catch (PyException pye) {
            throw badFD();
        }
    }

    public static PyString __doc__lstat = new PyString(
        "lstat(path) -> stat result\n\n" +
        "Like stat(path), but do not follow symbolic links.");
    public static PyObject lstat(String path) {
        return PyStatResult.fromFileStat(posix.lstat(absolutePath(path)));
    }

    public static PyString __doc__mkdir = new PyString(
        "mkdir(path [, mode=0777])\n\n" +
        "Create a directory.");
    public static void mkdir(String path) {
        mkdir(path, 0777);
    }

    public static void mkdir(String path, int mode) {
        if (posix.mkdir(absolutePath(path), mode) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__open = new PyString(
        "open(filename, flag [, mode=0777]) -> fd\n\n" +
        "Open a file (for low level IO).\n\n" +
        "Note that the mode argument is not currently supported on Jython.");
    public static FileIO open(String path, int flag) {
        return open(path, flag, 0777);
    }

    public static FileIO open(String path, int flag, int mode) {
        ensurePath(path);
        boolean reading = (flag & O_RDONLY) != 0;
        boolean writing = (flag & O_WRONLY) != 0;
        boolean updating = (flag & O_RDWR) != 0;
        boolean creating = (flag & O_CREAT) != 0;
        boolean appending = (flag & O_APPEND) != 0;
        boolean truncating = (flag & O_TRUNC) != 0;
        boolean exclusive = (flag & O_EXCL) != 0;
        boolean sync = (flag & O_SYNC) != 0;
        File file = new RelativeFile(path);

        if (updating && writing) {
            throw Py.OSError(Errno.EINVAL, path);
        }
        if (!creating && !file.exists()) {
            throw Py.OSError(Errno.ENOENT, path);
        }

        if (!writing) {
            if (updating) {
                writing = true;
            } else {
                reading = true;
            }
        }

        if (truncating && !writing) {
            // Explicitly truncate, writing will truncate anyway
            new FileIO(path, "w").close();
        }

        if (exclusive && creating) {
            try {
                if (!file.createNewFile()) {
                    throw Py.OSError(Errno.EEXIST, path);
                }
            } catch (IOException ioe) {
                throw Py.OSError(ioe);
            }
        }

        String fileIOMode = (reading ? "r" : "") + (!appending && writing ? "w" : "")
                + (appending && (writing || updating) ? "a" : "") + (updating ? "+" : "");
        if (sync && (writing || updating)) {
            try {
                return new FileIO(new RandomAccessFile(file, "rws").getChannel(), fileIOMode);
            } catch (FileNotFoundException fnfe) {
                throw Py.OSError(file.isDirectory() ? Errno.EISDIR : Errno.ENOENT, path);
            }
        }
        return new FileIO(path, fileIOMode);
    }

    public static PyString __doc__read = new PyString(
        "read(fd, buffersize) -> string\n\n" +
        "Read a file descriptor.");
    public static String read(PyObject fd, int buffersize) {
        try {
            return StringUtil.fromBytes(FileDescriptors.get(fd).read(buffersize));
        } catch (PyException pye) {
            throw badFD();
        }
    }

    public static PyString __doc__stat = new PyString(
        "stat(path) -> stat result\n\n" +
        "Perform a stat system call on the given path.\n\n" +
        "Note that some platforms may return only a small subset of the\n" +
        "standard fields");
    public static PyObject stat(String path) {
        return PyStatResult.fromFileStat(posix.stat(absolutePath(path)));
    }

    public static PyString __doc__strerror = new PyString(
        "strerror(code) -> string\n\n" +
        "Translate an error code to a message string.");
    public static PyObject strerror(int code) {
        Constant errno = Errno.valueOf(code);
        if (errno == Errno.__UNKNOWN_CONSTANT__) {
            return new PyString("Unknown error: " + code);
        }
        if (errno.name() == errno.toString()) {
            // Fake constant or just lacks a description, fallback to Linux's
            // XXX: have constantine handle this fallback
            errno = Enum.valueOf(com.kenai.constantine.platform.linux.Errno.class,
                                 errno.name());
        }
        return new PyString(errno.toString());
    }

    public static PyString __doc__write = new PyString(
        "write(fd, string) -> byteswritten\n\n" +
        "Write a string to a file descriptor.");
    public static int write(PyObject fd, String string) {
        try {
            return FileDescriptors.get(fd).write(ByteBuffer.wrap(StringUtil.toBytes(string)));
        } catch (PyException pye) {
            throw badFD();
        }
    }

    /**
     * Helper function for the subprocess module, returns the potential shell commands for
     * this OS.
     *
     * @return a tuple of lists of command line arguments. E.g. (['/bin/sh', '-c'])
     */
    public static PyObject _get_shell_commands() {
        String[][] commands = os.getShellCommands();
        PyObject[] commandsTup = new PyObject[commands.length];
        int i = 0;
        for (String[] command : commands) {
            PyList args = new PyList();
            for (String arg : command) {
                args.append(new PyString(arg));
            }
            commandsTup[i++] = args;
        }
        return new PyTuple(commandsTup);
    }

    /**
     * Initialize the environ dict from System.getenv. environ may be empty when the
     * security policy doesn't grant us access.
     */
    private static PyObject getEnviron() {
        PyObject environ = new PyDictionary();
        Map<String, String> env;
        try {
            env = System.getenv();
        } catch (SecurityException se) {
            return environ;
        }
        for (Map.Entry<String, String> entry : env.entrySet()) {
            environ.__setitem__(Py.newString(entry.getKey()), Py.newString(entry.getValue()));
        }
        return environ;
    }

    /**
     * Return the absolute form of path.
     *
     * @param path a path String, raising a TypeError when null
     * @return an absolute path String
     */
    private static String absolutePath(String path) {
        ensurePath(path);
        return new RelativeFile(path).getPath();
    }

    private static void ensurePath(String path) {
        if (path == null) {
            throw Py.TypeError("coercing to Unicode: need string or buffer, NoneType found");
        }
    }

    private static PyException badFD() {
        return Py.OSError(Errno.EBADF);
    }

    private static PyException errorFromErrno(String path) {
        return Py.OSError(Errno.valueOf(posix.errno()), path);
    }

    public static POSIX getPOSIX() {
        return posix;
    }

    public static String getOSName() {
        return os.getModuleName();
    }
}
