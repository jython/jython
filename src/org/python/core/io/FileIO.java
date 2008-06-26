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

import org.python.core.imp;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.util.RelativeFile;
import org.python.modules.errno;

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

    /** true if the file is readable */
    private boolean readable = false;

    /** true if the file is writable */
    private boolean writable = false;

    /** true if the file is in appending mode */
    private boolean appending = false;

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
        String rafMode = "r" + (writable ? "w" : "");
        try {
            file = new RandomAccessFile(fullPath, rafMode);
            fileChannel = file.getChannel();
        } catch (FileNotFoundException fnfe) {
            if (fullPath.isDirectory()) {
                throw Py.IOError(errno.EISDIR, "Is a directory");
            }
            if ( (rafMode.equals("rw") && !fullPath.canWrite()) ||
        	    fnfe.getMessage().endsWith("(Permission denied)")) {
                throw Py.IOError(errno.EACCES, "Permission denied: '" + name + "'");
            }
            throw Py.IOError(errno.ENOENT, "No such file or directory: '" + name + "'");
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
        boolean plus = false;

        for (int i = 0; i < mode.length(); i++) {
            switch (mode.charAt(i)) {
            case 'r':
                if (rwa) {
                    badMode();
                }
                readable = rwa = true;
                break;
            case 'w':
                if (rwa) {
                    badMode();
                }
                writable = rwa = true;
                break;
            case 'a':
                if (rwa) {
                    badMode();
                }
                appending = writable = rwa = true;
                break;
            case '+':
                if (plus) {
                    badMode();
                }
                readable = writable = plus = true;
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
     * Set the appropriate file position for writable/appending modes.
     *
     */
    private void initPosition() {
        if (appending) {
            seek(0, 2);
        } else if (writable && !readable) {
            truncate(0);
        }
    }

    /** {@inheritDoc} */
    public boolean isatty() {
        checkClosed();
        if (file == null) return false;
        
        try {
            return imp.load("os").__getattr__("isatty").__call__(Py.java2py(file.getFD())).__nonzero__();
        } catch (IOException e) {
            return false;
        }
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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
                throw Py.IOError(errno.EINVAL, "invalid whence value");
            }
            fileChannel.position(pos);
            return pos;
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /** {@inheritDoc} */
    public long tell() {
        checkClosed();
        try {
            return fileChannel.position();
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /** {@inheritDoc} */
    public long truncate(long size) {
        checkClosed();
        checkWritable();
        try {
            fileChannel.truncate(size);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        return size;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public Object __tojava__(Class cls) {
        if (OutputStream.class.isAssignableFrom(cls) && writable) {
            return Channels.newOutputStream(fileChannel);
        } else if (InputStream.class.isAssignableFrom(cls) && readable) {
            return Channels.newInputStream(fileChannel);
        }
        return super.__tojava__(cls);
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return readable;
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return writable;
    }

    /** {@inheritDoc} */
    public Channel getChannel() {
        return fileChannel;
    }
}
