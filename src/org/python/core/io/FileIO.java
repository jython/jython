/* Copyright (c) 2007-2012 Jython Developers */
package org.python.core.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import jnr.constants.platform.Errno;
import jnr.posix.util.FieldAccess;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.util.RelativeFile;
import org.python.modules.posix.PosixModule;

/**
 * Raw I/O implementation for OS files.
 *
 * @author Philip Jenvey
 */
public class FileIO extends RawIOBase {

    // would be nicer if we directly imported from os, but crazy to do so
    // since in python code itself
    private static class os {
        public static final int SEEK_SET = 0;
        public static final int SEEK_CUR = 1;
        public static final int SEEK_END = 2;
    }

    /** The underlying file channel */
    private FileChannel fileChannel;

    /** The underlying RandomAccessFile, if known. May be null */
    private RandomAccessFile file;

    /** The underlying FileOutputStream, if known. May be null */
    private FileOutputStream fileOutputStream;

    /** true if the file is opened for reading ('r') */
    private boolean reading;

    /** true if the file is opened for writing ('w', 'a', or '+') */
    private boolean writing;

    /** true if the file is in appending mode ('a') */
    private boolean appending;

    /** true if the file is opened for reading and writing ('+') */
    private boolean plus;

    /** true if write will emulate O_APPEND mode */
    private boolean emulateAppend;

    /**
     * @see #FileIO(PyString name, String mode)
     */
    public FileIO(String name, String mode) {
        this(Py.newUnicode(name), mode);
    }

    /**
     * Construct a FileIO instance for the specified file name, which will be decoded using the
     * nominal Jython file system encoding if it is a <code>str/bytes</code> rather than a
     * <code>unicode</code>.
     *
     * The mode can be 'r', 'w' or 'a' for reading (default), writing or appending. Add a '+' to the
     * mode to allow simultaneous reading and writing.
     *
     * @param name the name of the file
     * @param mode a raw io file mode String
     */
    public FileIO(PyString name, String mode) {
        parseMode(mode);
        File absPath = new RelativeFile(Py.fileSystemDecode(name));

        try {
            if ((appending && !(reading || plus)) || (writing && !reading && !plus)) {
                // Take advantage of FileOutputStream's append mode
                fromFileOutputStream(absPath);
            } else {
                fromRandomAccessFile(absPath);
                emulateAppend = appending;
            }
        } catch (FileNotFoundException fnfe) {
            if (absPath.isDirectory()) {
                throw Py.IOError(Errno.EISDIR, name);
            }
            if ((writing && !absPath.canWrite())
                || fnfe.getMessage().endsWith("(Permission denied)")) {
                throw Py.IOError(Errno.EACCES, name);
            }
            throw Py.IOError(Errno.ENOENT, name);
        }

        initPosition();
    }

    /**
     * Construct a FileIO instance with the given FileChannel.
     *
     * The mode can be 'r', 'w' or 'a' for reading (default), writing
     * or appending. Add a '+' to the mode to allow simultaneous
     * reading and writing.
     *
     * @param fileChannel a FileChannel object
     * @param mode a raw io file mode String
     */
    public FileIO(FileChannel fileChannel, String mode) {
        parseMode(mode);
        this.fileChannel = fileChannel;
        initPosition();
    }

    /**
     * Parse the Python mode string.
     *
     * The mode can be 'r', 'w' or 'a' for reading (default), writing
     * or appending. Add a '+' to the mode to allow simultaneous
     * reading and writing.
     *
     * @param mode a raw io file mode String
     */
    private void parseMode(String mode) {
        boolean rwa = false;

        for (int i = 0; i < mode.length(); i++) {
            switch (mode.charAt(i)) {
            case 'r':
                if (plus || rwa) {
                    badMode();
                }
                reading = rwa = true;
                break;
            case 'w':
                if (plus || rwa) {
                    badMode();
                }
                writing = rwa = true;
                break;
            case 'a':
                if (plus || rwa) {
                    badMode();
                }
                appending = writing = rwa = true;
                break;
            case '+':
                if (plus || !rwa) {
                    badMode();
                }
                writing = plus = true;
                break;
            default:
                throw Py.ValueError("invalid mode: '" + mode + "'");
            }
        }

        if (!rwa) {
            badMode();
        }
    }

    /**
     * Open the underlying FileChannel from a RandomAccessFile.
     *
     * @param absPath The absolute path File to open
     */
    private void fromRandomAccessFile(File absPath) throws FileNotFoundException {
        String rafMode = "r" + (writing ? "w" : "");
        if (plus && reading && !absPath.isFile()) {
            // suppress "permission denied"
            writing = false;
            throw new FileNotFoundException("");
        }
        file = new RandomAccessFile(absPath, rafMode);
        fileChannel = file.getChannel();
    }

    /**
     * Open the underlying FileChannel from a FileOutputStream in append mode, as opposed
     * to a RandomAccessFile, for the use of the OS's underlying O_APPEND mode. This can
     * only be used by 'a' (not 'a+') mode.
     *
     * @param absPath The absolute path File to open
     */
    private void fromFileOutputStream(File absPath) throws FileNotFoundException {
        fileOutputStream = new FileOutputStream(absPath, appending);
        fileChannel = fileOutputStream.getChannel();
    }

    /**
     * Raise a value error due to a mode string not containing exactly
     * one r/w/a/+ character.
     *
     */
    private void badMode() {
        throw Py.ValueError("Must have exactly one of read/write/append mode");
    }

    /**
     * Set the appropriate file position for writing/appending modes.
     *
     */
    private void initPosition() {
        if (appending) {
            seek(0, os.SEEK_END);
        } else if (writing && !reading) {
            try {
                fileChannel.truncate(0);
            } catch (IOException ioe) {
                // On Solaris and Linux, ftruncate(3C) returns EINVAL if not a regular
                // file whereas, e.g., open(os.devnull, "w") works. Because we have to
                // simulate the "w" mode in Java, we suppress the exception.
                // Likewise Windows returns ERROR_INVALID_FUNCTION in that case and
                // ERROR_INVALID_HANDLE on ttys. Identifying those by the IOException
                // message is tedious as their messages are localized, so we suppress them
                // all =[
                //
                // Unfortunately attempting to distinguish by localized messages is too hard.
                // Give up and swallow the exception.
                // See http://bugs.jython.org/issue1944
            }
        }
    }

    @Override
    public boolean isatty() {
        checkClosed();
        if (file == null || fileOutputStream == null) {
            return false;
        }
        try {
            return PosixModule.getPOSIX().isatty(file != null
                                                 ? file.getFD() : fileOutputStream.getFD());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int readinto(ByteBuffer buf) {
        checkClosed();
        checkReadable();
        try {
            int n = fileChannel.read(buf);
            return n > 0 ? n : 0;
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Read bytes into each of the specified ByteBuffers via scatter i/o. Returns number of bytes
     * read (0 for EOF).
     *
     * @param bufs {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long readinto(ByteBuffer[] bufs) {
        checkClosed();
        checkReadable();
        try {
            long n = fileChannel.read(bufs);
            return n > 0L ? n : 0L;
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Read until EOF with one readinto() call.
     *
     * Takes advantage of the fact that the underlying file's size is
     * available. However, we have to special case if file size is 0,
     * as seen in the /proc virtual file system - in this case we cannot
     * assume the file is truly empty.
     *
     * @return {@inheritDoc}
     */
    @Override
    public ByteBuffer readall() {
        checkClosed();
        checkReadable();
        // NOTE: This could be less accurate than multiple reads if
        // the file is growing
        long toRead;
        try {
            toRead = Math.max(0, fileChannel.size() - fileChannel.position());
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }

        if (toRead > Integer.MAX_VALUE) {
            throw Py.OverflowError("requested number of bytes is more than a Python string can "
                                   + "hold");
        } else if (toRead == 0) {
            // Support files that the underlying OS has labeled with size=0, even though they have content,
            // eg, /proc files on Linux
            return readallInChunks();
        } else {
            ByteBuffer all = ByteBuffer.allocate((int) toRead);
            readinto(all);
            all.flip();
            return all;
        }
    }

    private ByteBuffer readallInChunks() {
        // assumes checks have been performed
        final List<ByteBuffer> chunks = new ArrayList<>();
        final int MAX_CHUNK_SIZE = 8192;
        int length = 0;
        while (true) {
            final ByteBuffer chunk = ByteBuffer.allocate(MAX_CHUNK_SIZE);
            readinto(chunk);
            int chunkSize = chunk.position();
            length += chunkSize;
            chunk.flip();
            chunks.add(chunk);
            if (chunkSize < MAX_CHUNK_SIZE) {
                break;
            }
        }

        // given the size of MAX_CHUNK_SIZE, perhaps most or even all files in /proc,
        // or similar virtual filesystems, if any, might fit into one chunk
        if (chunks.size() == 1) {
            return chunks.get(0);
        }

        // otherwise append together into a single ByteBuffer
        final ByteBuffer all = ByteBuffer.allocate(length);
        for (ByteBuffer chunk : chunks) {
            all.put(chunk);
        }
        all.flip();
        return all;
    }

    @Override
    public int write(ByteBuffer buf) {
        checkClosed();
        checkWritable();
        try {
            return emulateAppend ? appendFromByteBuffer(buf)    // use this helper function to advance the file channel's position post-write
                                    : fileChannel.write(buf);   // this does change the file channel's position
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    private int appendFromByteBuffer(ByteBuffer buf) throws IOException {
        int written = fileChannel.write(buf, fileChannel.position());   // this does not change the file channel's position!
        if (written > 0) {
            // we need to manually update the file channel's position post-write
            fileChannel.position(fileChannel.position() + written);
        }
        return written;
    }

    /**
     * Write bytes from each of the specified ByteBuffers via gather
     * i/o.
     *
     * @param bufs {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long write(ByteBuffer[] bufs) {
        checkClosed();
        checkWritable();
        try {
            return !emulateAppend ? fileChannel.write(bufs) : writeAppend(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Write multiple ByteBuffers while emulating O_APPEND mode.
     *
     * @param bufs an array of ByteBuffers
     * @return the number of bytes written as a long
     */
    private long writeAppend(ByteBuffer[] bufs) throws IOException {
        long count = 0;
        int bufCount;
        for (ByteBuffer buf : bufs) {
            if (!buf.hasRemaining()) {
                continue;
            }
            bufCount = appendFromByteBuffer(buf);
            if (bufCount == 0) {
                break;
            }
            count += bufCount;
        }
        return count;
    }

    @Override
    public long seek(long pos, int whence) {
        checkClosed();
        try {
            switch (whence) {
            case os.SEEK_SET:
                break;
            case os.SEEK_CUR:
                pos += fileChannel.position();
                break;
            case os.SEEK_END:
                pos += fileChannel.size();
                break;
            default:
                throw Py.IOError(Errno.EINVAL);
            }
            fileChannel.position(pos);
            return pos;
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    @Override
    public long tell() {
        checkClosed();
        try {
            return fileChannel.position();
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    @Override
    public long truncate(long size) {
        checkClosed();
        checkWritable();
        try {
            long oldPosition = fileChannel.position();
            fileChannel.truncate(size);
            // seek necessary on Windows only, see <http://bugs.python.org/issue801631>
            fileChannel.position(oldPosition);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        return size;
    }

    @Override
    public void close() {
        if (closed()) {
            return;
        }
        try {
            fileChannel.close();
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        super.close();
    }

    @Override
    public OutputStream asOutputStream() {
        return writing ? Channels.newOutputStream(fileChannel) : super.asOutputStream();
    }

    @Override
    public InputStream asInputStream() {
        return readable() ? Channels.newInputStream(fileChannel) : super.asInputStream();
    }

    @Override
    public boolean readable() {
        return reading || plus;
    }

    @Override
    public boolean writable() {
        return writing;
    }

    @Override
    public FileChannel getChannel() {
        return fileChannel;
    }

    public FileDescriptor getFD() {
        if (file != null) {
            try {
                return file.getFD();
            } catch (IOException ioe) {
                throw Py.OSError(ioe);
            }
        } else if (fileOutputStream != null) {
            try {
                return fileOutputStream.getFD();
            } catch (IOException ioe) {
                throw Py.OSError(ioe);
            }
        }
        throw Py.OSError(Errno.EBADF);
    }

    public PyObject __int__() {
        int intFD = -1;
        try {
            Field fdField = FieldAccess.getProtectedField(FileDescriptor.class, "fd");
            intFD = fdField.getInt(getFD());
        } catch (SecurityException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        return Py.newInteger(intFD);
    }

    public PyObject __add__(PyObject otherObj) {
        return __int__().__add__(otherObj);
    }
}
