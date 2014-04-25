/* Copyright (c) Jython Developers */
package org.python.modules.posix;

import jnr.constants.Constant;
import jnr.constants.platform.Errno;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.Platform;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinFunctionNarrow;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.imp;
import org.python.core.io.IOBase;
import org.python.core.io.FileDescriptors;
import org.python.core.io.FileIO;
import org.python.core.io.RawIOBase;
import org.python.core.util.StringUtil;

/**
 * The posix/nt module, depending on the platform.
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

    /** Lazily initialzed singleton source for urandom. */
    private static class UrandomSource {
        static final SecureRandom INSTANCE = new SecureRandom();
    }

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

        // os.access flags
        dict.__setitem__("F_OK", Py.newInteger(F_OK));
        dict.__setitem__("X_OK", Py.newInteger(X_OK));
        dict.__setitem__("W_OK", Py.newInteger(W_OK));
        dict.__setitem__("R_OK", Py.newInteger(R_OK));
        // Successful termination
        dict.__setitem__("EX_OK", Py.Zero);

        boolean nativePosix = posix.isNative();
        dict.__setitem__("_native_posix", Py.newBoolean(nativePosix));
        dict.__setitem__("_posix_impl", Py.java2py(posix));
        dict.__setitem__("environ", getEnviron());
        dict.__setitem__("error", Py.OSError);
        dict.__setitem__("stat_result", PyStatResult.TYPE);

        // Faster call paths
        dict.__setitem__("lstat", new LstatFunction());
        dict.__setitem__("stat", new StatFunction());

        // Hide from Python
        Hider.hideFunctions(PosixModule.class, dict, os, nativePosix);
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("__init__", null);
        dict.__setitem__("getPOSIX", null);
        dict.__setitem__("getOSName", null);
        dict.__setitem__("badFD", null);

        // Hide __doc__s
        PyList keys = (PyList)dict.invoke("keys");
        for (Iterator<?> it = keys.listIterator(); it.hasNext();) {
            String key = (String)it.next();
            if (key.startsWith("__doc__")) {
                it.remove();
                dict.__setitem__(key, null);
            }
        }
        dict.__setitem__("__all__", keys);

        dict.__setitem__("__name__", new PyString(os.getModuleName()));
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
    public static boolean access(PyObject path, int mode) {
        String absolutePath = absolutePath(path);
        File file = new File(absolutePath);
        boolean result = true;

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
                result = posix.stat(absolutePath).isExecutable();
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
    public static void chdir(PyObject path) {
        // stat raises ENOENT for us if path doesn't exist
        if (!posix.stat(absolutePath(path)).isDirectory()) {
            throw Py.OSError(Errno.ENOTDIR, path);
        }

        if (realpath == null) {
            realpath = imp.load("os.path").__getattr__("realpath");
        }
        Py.getSystemState().setCurrentWorkingDir(realpath.__call__(path).asString());
    }

    public static PyString __doc__chmod = new PyString(
        "chmod(path, mode)\n\n" +
        "Change the access permissions of a file.");
    public static void chmod(PyObject path, int mode) {
        if (posix.chmod(absolutePath(path), mode) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__chown = new PyString(
        "chown(path, uid, gid)\n\n" +
        "Change the owner and group id of path to the numeric uid and gid.");
    @Hide(OS.NT)
    public static void chown(PyObject path, int uid, int gid) {
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

    public static PyString __doc__fdatasync = new PyString(
        "fdatasync(fildes)\n\n" +
        "force write of file with filedescriptor to disk.\n" +
        "does not force update of metadata.");
    @Hide(OS.NT)
    public static void fdatasync(PyObject fd) {
        fsync(fd, false);
    }

    public static PyString __doc__fsync = new PyString(
        "fsync(fildes)\n\n" +
        "force write of file with filedescriptor to disk.");
    public static void fsync(PyObject fd) {
        fsync(fd, true);
    }

    /**
     * Internal fsync implementation.
     */
    private static void fsync(PyObject fd, boolean metadata) {
        RawIOBase rawIO = FileDescriptors.get(fd);
        rawIO.checkClosed();
        Channel channel = rawIO.getChannel();
        if (!(channel instanceof FileChannel)) {
            throw Py.OSError(Errno.EINVAL);
        }

        try {
            ((FileChannel)channel).force(metadata);
        } catch (ClosedChannelException cce) {
            // In the rare case it's closed but the rawIO wasn't
            throw Py.ValueError("I/O operation on closed file");
        } catch (IOException ioe) {
            throw Py.OSError(ioe);
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

    public static PyString __doc__getcwd = new PyString(
        "getcwd() -> path\n\n" +
        "Return a string representing the current working directory.");
    public static PyObject getcwd() {
        return Py.newString(Py.getSystemState().getCurrentWorkingDir());
    }

    public static PyString __doc__getcwdu = new PyString(
        "getcwd() -> path\n\n" +
        "Return a unicode string representing the current working directory.");
    public static PyObject getcwdu() {
        return Py.newUnicode(Py.getSystemState().getCurrentWorkingDir());
    }

    public static PyString __doc__getegid = new PyString(
        "getegid() -> egid\n\n" +
        "Return the current process's effective group id.");
    @Hide(OS.NT)
    public static int getegid() {
        return posix.getegid();
    }

    public static PyString __doc__geteuid = new PyString(
        "geteuid() -> euid\n\n" +
        "Return the current process's effective user id.");
    @Hide(OS.NT)
    public static int geteuid() {
        return posix.geteuid();
    }

    public static PyString __doc__getgid = new PyString(
        "getgid() -> gid\n\n" +
        "Return the current process's group id.");
    @Hide(OS.NT)
    public static int getgid() {
        return posix.getgid();
    }

    public static PyString __doc__getlogin = new PyString(
        "getlogin() -> string\n\n" +
        "Return the actual login name.");
    @Hide(OS.NT)
    public static PyObject getlogin() {
        return new PyString(posix.getlogin());
    }

    public static PyString __doc__getppid = new PyString(
        "getppid() -> ppid\n\n" +
        "Return the parent's process id.");
    @Hide(OS.NT)
    public static int getppid() {
        return posix.getppid();
    }

    public static PyString __doc__getuid = new PyString(
        "getuid() -> uid\n\n" +
        "Return the current process's user id.");
    @Hide(OS.NT)
    public static int getuid() {
        return posix.getuid();
    }

    public static PyString __doc__getpid = new PyString(
        "getpid() -> pid\n\n" +
        "Return the current process id");
    @Hide(posixImpl = PosixImpl.JAVA)
    public static int getpid() {
        return posix.getpid();
    }

    public static PyString __doc__getpgrp = new PyString(
        "getpgrp() -> pgrp\n\n" +
        "Return the current process group id.");
    @Hide(OS.NT)
    public static int getpgrp() {
        return posix.getpgrp();
    }

    public static PyString __doc__isatty = new PyString(
        "isatty(fd) -> bool\n\n" +
        "Return True if the file descriptor 'fd' is an open file descriptor\n" +
        "connected to the slave end of a terminal.");
    public static boolean isatty(PyObject fdObj) {
        if (fdObj instanceof PyInteger) {
            FileDescriptor fd;
            switch (fdObj.asInt()) {
            case 0:
                fd = FileDescriptor.in;
                break;
            case 1:
                fd = FileDescriptor.out;
                break;
            case 2:
                fd = FileDescriptor.err;
                break;
            default:
                throw Py.NotImplementedError("Integer file descriptor compatibility only "
                                             + "available for stdin, stdout and stderr (0-2)");
            }
            return posix.isatty(fd);
        }

        Object tojava = fdObj.__tojava__(FileDescriptor.class);
        if (tojava != Py.NoConversion) {
            return posix.isatty((FileDescriptor)tojava);
        }

        tojava = fdObj.__tojava__(IOBase.class);
        if (tojava == Py.NoConversion) {
            throw Py.TypeError("a file descriptor is required");
        }
        return ((IOBase)tojava).isatty();
    }

    public static PyString __doc__kill = new PyString(
        "kill(pid, sig)\n\n" +
        "Kill a process with a signal.");
    @Hide(OS.NT)
    public static void kill(int pid, int sig) {
        if (posix.kill(pid, sig) < 0) {
            throw errorFromErrno();
        }
    }

    public static PyString __doc__lchmod = new PyString(
        "lchmod(path, mode)\n\n" +
        "Change the access permissions of a file. If path is a symlink, this\n" +
        "affects the link itself rather than the target.");
    @Hide(OS.NT)
    public static void lchmod(PyObject path, int mode) {
        if (posix.lchmod(absolutePath(path), mode) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__lchown = new PyString(
        "lchown(path, uid, gid)\n\n" +
        "Change the owner and group id of path to the numeric uid and gid.\n" +
        "This function will not follow symbolic links.");
    @Hide(OS.NT)
    public static void lchown(PyObject path, int uid, int gid) {
        if (posix.lchown(absolutePath(path), uid, gid) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__link = new PyString(
        "link(src, dst)\n\n" +
        "Create a hard link to a file.");
    @Hide(OS.NT)
    public static void link(PyObject src, PyObject dst) {
        if (posix.link(absolutePath(src), absolutePath(dst)) < 0) {
            throw errorFromErrno();
        }
    }

    public static PyString __doc__listdir = new PyString(
        "listdir(path) -> list_of_strings\n\n" +
        "Return a list containing the names of the entries in the directory.\n\n" +
        "path: path of directory to list\n\n" +
        "The list is in arbitrary order.  It does not include the special\n" +
        "entries '.' and '..' even if they are present in the directory.");
    public static PyList listdir(PyObject path) {
        String absolutePath = absolutePath(path);
        File file = new File(absolutePath);
        String[] names = file.list();

        if (names == null) {
            // Can't read the path for some reason. stat will throw an error if it can't
            // read it either
            FileStat stat = posix.stat(absolutePath);
            // It exists, maybe not a dir, or we don't have permission?
            if (!stat.isDirectory()) {
                throw Py.OSError(Errno.ENOTDIR, path);
            }
            if (!file.canRead()) {
                throw Py.OSError(Errno.EACCES, path);
            }
            throw Py.OSError("listdir(): an unknown error occurred: " + path);
        }

        PyList list = new PyList();
        PyString string = (PyString) path;
        for (String name : names) {
            list.append(string.createInstance(name));
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

    public static PyString __doc__mkdir = new PyString(
        "mkdir(path [, mode=0777])\n\n" +
        "Create a directory.");
    public static void mkdir(PyObject path) {
        mkdir(path, 0777);
    }

    public static void mkdir(PyObject path, int mode) {
        if (posix.mkdir(absolutePath(path), mode) < 0) {
            throw errorFromErrno(path);
        }
    }

    public static PyString __doc__open = new PyString(
        "open(filename, flag [, mode=0777]) -> fd\n\n" +
        "Open a file (for low level IO).\n\n" +
        "Note that the mode argument is not currently supported on Jython.");
    public static FileIO open(PyObject path, int flag) {
        return open(path, flag, 0777);
    }

    public static FileIO open(PyObject path, int flag, int mode) {
        String absolutePath = absolutePath(path);
        File file = new File(absolutePath);
        boolean reading = (flag & O_RDONLY) != 0;
        boolean writing = (flag & O_WRONLY) != 0;
        boolean updating = (flag & O_RDWR) != 0;
        boolean creating = (flag & O_CREAT) != 0;
        boolean appending = (flag & O_APPEND) != 0;
        boolean truncating = (flag & O_TRUNC) != 0;
        boolean exclusive = (flag & O_EXCL) != 0;
        boolean sync = (flag & O_SYNC) != 0;

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
            new FileIO((PyString) path, "w").close();
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
        return new FileIO((PyString) path, fileIOMode);
    }

    public static PyString __doc__popen = new PyString(
        "popen(command [, mode='r' [, bufsize]]) -> pipe\n\n" +
        "Open a pipe to/from a command returning a file object.");
    public static PyObject popen(PyObject[] args, String[] kwds) {
        // XXX: popen lives in posix in 2.x, but moves to os in 3.x. It's easier for us to
        // keep it in os
        // import os; return os.popen(*args, **kwargs)
        return imp.load("os").__getattr__("popen").__call__(args, kwds);
    }

    public static PyString __doc__putenv = new PyString(
        "putenv(key, value)\n\n" +
        "Change or add an environment variable.");
    public static void putenv(String key, String value) {
        // XXX: Consider deprecating putenv/unsetenv
        // import os; os.environ[key] = value
        PyObject environ = imp.load("os").__getattr__("environ");
        environ.__setitem__(key, new PyString(value));
    }

    public static PyString __doc__read = new PyString(
        "read(fd, buffersize) -> string\n\n" +
        "Read a file descriptor.");
    public static PyObject read(PyObject fd, int buffersize) {
        try {
            return new PyString(StringUtil.fromBytes(FileDescriptors.get(fd).read(buffersize)));
        } catch (PyException pye) {
            throw badFD();
        }
    }

    public static PyString __doc__readlink = new PyString(
        "readlink(path) -> path\n\n" +
        "Return a string representing the path to which the symbolic link points.");
    @Hide(OS.NT)
    public static String readlink(PyObject path) {
        try {
            return posix.readlink(absolutePath(path));
        } catch (IOException ioe) {
            throw Py.OSError(ioe);
        }
    }

    public static PyString __doc__remove = new PyString(
        "remove(path)\n\n" +
        "Remove a file (same as unlink(path)).");
    public static void remove(PyObject path) {
        unlink(path);
    }

    public static PyString __doc__rename = new PyString(
        "rename(old, new)\n\n" +
        "Rename a file or directory.");
    public static void rename(PyObject oldpath, PyObject newpath) {
        if (!new File(absolutePath(oldpath)).renameTo(new File(absolutePath(newpath)))) {
            PyObject args = new PyTuple(Py.Zero, new PyString("Couldn't rename file"));
            throw new PyException(Py.OSError, args);
        }
    }

    public static PyString __doc__rmdir = new PyString(
        "rmdir(path)\n\n" +
        "Remove a directory.");
    public static void rmdir(PyObject path) {
        File file = new File(absolutePath(path));
        if (!file.exists()) {
            throw Py.OSError(Errno.ENOENT, path);
        } else if (!file.isDirectory()) {
            throw Py.OSError(Errno.ENOTDIR, path);
        } else if (!file.delete()) {
            PyObject args = new PyTuple(Py.Zero, new PyString("Couldn't delete directory"),
                                        path);
            throw new PyException(Py.OSError, args);
        }
    }

    public static PyString __doc__setpgrp = new PyString(
        "setpgrp()\n\n" +
        "Make this process a session leader.");
    @Hide(OS.NT)
    public static void setpgrp() {
        if (posix.setpgrp(0, 0) < 0) {
            throw errorFromErrno();
        }
    }

    public static PyString __doc__setsid = new PyString(
        "setsid()\n\n" +
        "Call the system call setsid().");
    @Hide(OS.NT)
    public static void setsid() {
        if (posix.setsid() < 0) {
            throw errorFromErrno();
        }
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
            // XXX: have jnr-constants handle this fallback
            errno = Enum.valueOf(jnr.constants.platform.linux.Errno.class,
                                 errno.name());
        }
        return new PyString(errno.toString());
    }

    public static PyString __doc__symlink = new PyString(
        "symlink(src, dst)\n\n" +
        "Create a symbolic link pointing to src named dst.");
    @Hide(OS.NT)
    public static void symlink(PyObject src, PyObject dst) {
        if (posix.symlink(asPath(src), absolutePath(dst)) < 0) {
            throw errorFromErrno();
        }
    }

    public static PyString __doc__system = new PyString(
        "system(command) -> exit_status\n\n" +
        "Execute the command (a string) in a subshell.");
    public static PyObject system(PyObject command) {
        // import subprocess; return subprocess.call(command, shell=True)
        return imp.load("subprocess").invoke("call", command, new PyObject[] {Py.True},
                                             new String[] {"shell"});
    }

    public static PyString __doc__umask = new PyString(
        "umask(new_mask) -> old_mask\n\n" +
        "Set the current numeric umask and return the previous umask.");
    @Hide(posixImpl = PosixImpl.JAVA)
    public static int umask(int mask) {
        return posix.umask(mask);
    }

    public static PyString __doc__unlink = new PyString("unlink(path)\n\n"
            + "Remove a file (same as remove(path)).");

    public static void unlink(PyObject path) {
        String absolutePath = absolutePath(path);
        File file = new File(absolutePath);
        if (file.isDirectory()) {
            throw Py.OSError(Errno.EISDIR, path);
        } else if (!file.delete()) {
            // Something went wrong, does stat raise an error?
            posix.stat(absolutePath);
            // It exists, do we not have permissions?
            if (!file.canWrite()) {
                throw Py.OSError(Errno.EPERM, path);
            }
            throw Py.OSError("unlink(): an unknown error occurred: " + absolutePath);
        }
    }

    public static PyString __doc__utime = new PyString(
        "utime(path, (atime, mtime))\n" +
        "utime(path, None)\n\n" +
        "Set the access and modified time of the file to the given values.  If the\n" +
        "second form is used, set the access and modified times to the current time.");
    public static void utime(PyObject path, PyObject times) {
        long[] atimeval;
        long[] mtimeval;

        if (times == Py.None) {
            atimeval = mtimeval = null;
        } else if (times instanceof PyTuple && times.__len__() == 2) {
            atimeval = extractTimeval(times.__getitem__(0));
            mtimeval = extractTimeval(times.__getitem__(1));
        } else {
            throw Py.TypeError("utime() arg 2 must be a tuple (atime, mtime)");
        }
        if (posix.utimes(absolutePath(path), atimeval, mtimeval) < 0) {
            throw errorFromErrno(path);
        }
    }

    /**
     * Convert seconds (with a possible fraction) from epoch to a 2 item array of seconds,
     * microseconds from epoch as longs.
     *
     * @param seconds a PyObject number
     * @return a 2 item long[]
     */
    private static long[] extractTimeval(PyObject seconds) {
        long[] timeval = new long[] {Platform.IS_32_BIT ? seconds.asInt() : seconds.asLong(), 0L};
        if (seconds instanceof PyFloat) {
            // can't exceed 1000000
            long usec = (long)((seconds.asDouble() % 1.0) * 1e6);
            if (usec < 0) {
                // If rounding gave us a negative number, truncate
                usec = 0;
            }
            timeval[1] = usec;
        }
        return timeval;
    }

    public static PyString __doc__wait = new PyString(
        "wait() -> (pid, status)\n\n" +
        "Wait for completion of a child process.");
    @Hide(OS.NT)
    public static PyObject wait$() {
        int[] status = new int[1];
        int pid = posix.wait(status);
        if (pid < 0) {
            throw errorFromErrno();
        }
        return new PyTuple(Py.newInteger(pid), Py.newInteger(status[0]));
    }

    public static PyString __doc__waitpid = new PyString(
        "wait() -> (pid, status)\n\n" +
        "Wait for completion of a child process.");
    public static PyObject waitpid(int pid, int options) {
        int[] status = new int[1];
        pid = posix.waitpid(pid, status, options);
        if (pid < 0) {
            throw errorFromErrno();
        }
        return new PyTuple(Py.newInteger(pid), Py.newInteger(status[0]));
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

    public static PyString __doc__unsetenv = new PyString(
        "unsetenv(key)\n\n" +
        "Delete an environment variable.");
    public static void unsetenv(String key) {
        // import os; try: del os.environ[key]; except KeyError: pass
        PyObject environ = imp.load("os").__getattr__("environ");
        try {
            environ.__delitem__(key);
        } catch (PyException pye) {
            if (!pye.match(Py.KeyError)) {
                throw pye;
            }
        }
    }

    public static PyString __doc__urandom = new PyString(
        "urandom(n) -> str\n\n" +
        "Return a string of n random bytes suitable for cryptographic use.");
    public static PyObject urandom(int n) {
        byte[] buf = new byte[n];
        UrandomSource.INSTANCE.nextBytes(buf);
        return new PyString(StringUtil.fromBytes(buf));
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
     * Return a path as a String from a PyObject 
     *
     * @param path a PyObject, raising a TypeError if an invalid path type
     * @return a String path
     */
    private static String asPath(PyObject path) {
        if (path instanceof PyString) {
            return path.toString();
        }
        throw Py.TypeError(String.format("coercing to Unicode: need string, %s type found",
                                         path.getType().fastGetName()));
    }

    /**
     * Return the absolute form of path.
     *
     * @param path a PyObject, raising a TypeError if an invalid path type
     * @return an absolute path String
     */
    private static String absolutePath(PyObject path) {
        return PySystemState.getPathLazy(asPath(path));
    }

    private static PyException badFD() {
        return Py.OSError(Errno.EBADF);
    }

    private static PyException errorFromErrno() {
        return Py.OSError(Errno.valueOf(posix.errno()));
    }

    private static PyException errorFromErrno(PyObject path) {
        return Py.OSError(Errno.valueOf(posix.errno()), path);
    }

    public static POSIX getPOSIX() {
        return posix;
    }

    public static String getOSName() {
        return os.getModuleName();
    }

    static class LstatFunction extends PyBuiltinFunctionNarrow {
        LstatFunction() {
            super("lstat", 1, 1,
                  "lstat(path) -> stat result\n\n" +
                  "Like stat(path), but do not follow symbolic links.");
        }

        @Override
        public PyObject __call__(PyObject path) {
            return PyStatResult.fromFileStat(posix.lstat(absolutePath(path)));
        }
    }

    static class StatFunction extends PyBuiltinFunctionNarrow {
        StatFunction() {
            super("stat", 1, 1,
                  "stat(path) -> stat result\n\n" +
                  "Perform a stat system call on the given path.\n\n" +
                  "Note that some platforms may return only a small subset of the\n" +
                  "standard fields");
        }

        @Override
        public PyObject __call__(PyObject path) {
            String absolutePath = absolutePath(path);

            // round tripping from a string to a file to a string loses
            // trailing slashes so add them back back in to get correct posix.stat
            // behaviour if path is not a directory.
            if (asPath(path).endsWith(File.separator)) {
                absolutePath = absolutePath + File.separator;
            }
            return PyStatResult.fromFileStat(posix.stat(absolutePath));
        }
    }
}
