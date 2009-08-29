/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import com.kenai.constantine.platform.Errno;
import org.jruby.ext.posix.util.Platform;
import org.python.core.Py;
import org.python.core.util.FileUtil;
import org.python.core.util.RelativeFile;

/**
 * Raw I/O implementation for OS files.
 *
 * @author Philip Jenvey
 */
public class FileIO extends RawIOBase {

    /** The underlying file channel */
    private FileChannel fileChannel;

    /** The underlying file (if known) */
    private RandomAccessFile file;

    /** true if the file is opened for reading ('r') */
    private boolean reading = false;

    /** true if the file is opened for writing ('w', 'a', or '+') */
    private boolean writing = false;

    /** true if the file is in appending mode ('a') */
    private boolean appending = false;

    /** true if the file is opened for reading and writing ('+') */
    private boolean plus = false;

    /**
     * Construct a FileIO instance for the specified file name.
     *
     * The mode can be 'r', 'w' or 'a' for reading (default), writing
     * or appending. Add a '+' to the mode to allow simultaneous
     * reading and writing.
     *
     * @param name the name of the file
     * @param mode a raw io file mode String
     */
    public FileIO(String name, String mode) {
        parseMode(mode);

        File fullPath = new RelativeFile(name);
        String rafMode = "r" + (writing ? "w" : "");
        try {
            if (plus && reading && !fullPath.isFile()) {
                writing = false; // suppress "permission denied"
                throw new FileNotFoundException("");
            }
            file = new RandomAccessFile(fullPath, rafMode);
            fileChannel = file.getChannel();
        } catch (FileNotFoundException fnfe) {
            if (fullPath.isDirectory()) {
                throw Py.IOError(Errno.EISDIR, name);
            }
            if ((writing && !fullPath.canWrite())
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
            seek(0, 2);
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
                if (Platform.IS_WINDOWS ||
                    ((Platform.IS_SOLARIS || Platform.IS_LINUX)
                     && Errno.EINVAL.description().equals(ioe.getMessage()))) {
                    return;
                }
                throw Py.IOError(ioe);
            }
        }
    }

    @Override
    public boolean isatty() {
        checkClosed();
        if (file == null) {
            return false;
        }
        try {
            return FileUtil.isatty(file.getFD());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int readinto(ByteBuffer buf) {
        checkClosed();
        checkReadable();
        try {
            return fileChannel.read(buf);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Read bytes into each of the specified ByteBuffers via scatter
     * i/o.
     *
     * @param bufs {@inheritDoc}
     * @return {@inheritDoc}
     */
    public long readinto(ByteBuffer[] bufs) {
        checkClosed();
        checkReadable();
        try {
            return fileChannel.read(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Read until EOF with one readinto() call.
     *
     * Takes advantage of the fact that the underlying file's size is
     * available.
     *
     * @return {@inheritDoc}
     */
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
        }

        ByteBuffer all = ByteBuffer.allocate((int)toRead);
        readinto(all);
        all.flip();
        return all;
    }

    @Override
    public int write(ByteBuffer buf) {
        checkClosed();
        checkWritable();
        try {
            return fileChannel.write(buf);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Write bytes from each of the specified ByteBuffers via gather
     * i/o.
     *
     * @param bufs {@inheritDoc}
     * @return {@inheritDoc}
     */
    public long write(ByteBuffer[] bufs) {
        checkClosed();
        checkWritable();
        try {
            return fileChannel.write(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    @Override
    public long seek(long pos, int whence) {
        checkClosed();
        try {
            switch (whence) {
            case 0:
                break;
            case 1:
                pos += fileChannel.position();
                break;
            case 2:
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
    public Channel getChannel() {
        return fileChannel;
    }
}
