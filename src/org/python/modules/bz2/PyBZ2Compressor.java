package org.python.modules.bz2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.core.Untraversable;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@Untraversable
@ExposedType(name = "bz2.BZ2Compressor")
public class PyBZ2Compressor extends PyObject {

    private CaptureStream captureStream = null;
    private BZip2CompressorOutputStream compressStream = null;

    public static final PyType TYPE = PyType.fromClass(PyBZ2Compressor.class);

    public PyBZ2Compressor() {
        super(TYPE);
    }

    public PyBZ2Compressor(PyType subType) {
        super(subType);
    }

    @ExposedNew
    final void BZ2Compressor___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("bz2compressor", args, kwds,
                new String[] { "compresslevel" }, 0);

        int compresslevel = ap.getInt(0, 9);

        try {
            captureStream = new CaptureStream();
            compressStream = new BZip2CompressorOutputStream(captureStream,
                    compresslevel);
        } catch (IOException e) {
            throw Py.IOError(e.getMessage());
        }
    }

    @ExposedMethod
    public PyString BZ2Compressor_compress(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("compress", args, kwds,
                new String[] { "data" }, 1);

        PyString data = (PyString) ap.getPyObject(0);

        PyString returnData = null;
        try {
            compressStream.write(data.toBytes());

            returnData = readData();
        } catch (IOException e) {
            throw Py.IOError(e.getMessage());
        }

        return returnData;
    }

    private PyString readData() {
        if (!captureStream.hasData()) {
            return Py.EmptyString;
        }
        
        byte[] buf = captureStream.readData();
        captureStream.resetByteArray();
        return new PyString(StringUtil.fromBytes(buf));
    }

    @ExposedMethod
    public PyString BZ2Compressor_flush(PyObject[] args, String[] kwds) {
        PyString finalData = Py.EmptyString;
        try {
            compressStream.finish();
            compressStream.close();

            finalData = readData();

            captureStream.close();
        } catch (IOException e) {
            throw Py.IOError(e.getMessage());
        }

        return finalData;
    }

    private class CaptureStream extends OutputStream {

        private final ByteArrayOutputStream capturedData = new ByteArrayOutputStream();

        @Override
        public void write(int byteData) throws IOException {
            capturedData.write(byteData);
        }

        public byte[] readData() {
            return capturedData.toByteArray();
        }

        public void resetByteArray() {
            capturedData.reset();
        }

        public boolean hasData() {
            return capturedData.size() > 0;
        }

    }
}
