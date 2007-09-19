package org.python.core;

import java.io.IOException;
import java.io.InputStream;

public class FilelikeInputStream extends InputStream {

    private PyObject filelike;

    public FilelikeInputStream(PyObject filelike) {
        this.filelike = filelike;
    }

    public int read() throws IOException {
        byte[] oneB = new byte[1];
        int numread = read(oneB, 0, 1);
        if(numread == -1) {
            return -1;
        }
        return oneB[0];
    }

    public int read(byte b[], int off, int len) throws IOException {
        if(b == null) {
            throw new NullPointerException();
        } else if((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if(len == 0) {
            return 0;
        }
        String result = ((PyString)filelike.__getattr__("read")
                .__call__(new PyInteger(len))).string;
        if(result.length() == 0) {
            return -1;
        }
        System.arraycopy(PyString.to_bytes(result), 0, b, off, result.length());
        return result.length();
    }

    public void close() throws IOException {
        filelike.__getattr__("close").__call__();
    }
}
