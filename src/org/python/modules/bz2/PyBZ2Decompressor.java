package org.python.modules.bz2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "bz2.BZ2Decompressor")
public class PyBZ2Decompressor extends PyObject {

    @ExposedGet
    public PyString unused_data = Py.EmptyString;

    private boolean eofReached = false;
    private BZip2CompressorInputStream decompressStream = null;

    private byte[] accumulator = new byte[0];

    public static final PyType TYPE = PyType.fromClass(PyBZ2Decompressor.class);

    public PyBZ2Decompressor() {
        super(TYPE);
    }

    public PyBZ2Decompressor(PyType objtype) {
        super(objtype);
    }

    @ExposedNew
    @ExposedMethod
    final void BZ2Decompressor___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("bz2decompressor", args, kwds,
                new String[0], 0);
    }

    @ExposedMethod
    final PyString BZ2Decompressor_decompress(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("compress", args, kwds,
                new String[] { "data" }, 1);

        PyString data = (PyString) ap.getPyObject(0);

        PyString returnData = Py.EmptyString;

        if (eofReached) {
            throw Py.EOFError("Data stream EOF reached");
        }

        byte[] indata = data.toBytes();
        if (indata.length > 0) {
            ByteBuffer bytebuf = ByteBuffer.allocate(accumulator.length
                    + indata.length);
            bytebuf.put(accumulator);
            bytebuf.put(indata);
            accumulator = bytebuf.array();
        }

        ByteArrayOutputStream decodedStream = new ByteArrayOutputStream();
        final byte[] buf = accumulator;
        for (int i = 0; i < buf.length; i++) {
            if (((i + 3) < buf.length) &&
                (((char) buf[i] == '\\') && ((char) buf[i + 1] == 'x'))) {
                int decodedByte = ((Character.digit((char) buf[i + 2], 16) << 4) + Character
                        .digit((char) buf[i + 3], 16));
                decodedStream.write(decodedByte);
                i += 3;
            } else {
                decodedStream.write(buf[i]);
            }
        }

        ByteArrayInputStream compressedData = new ByteArrayInputStream(
                decodedStream.toByteArray());

        try {
            decompressStream = new BZip2CompressorInputStream(compressedData);
        } catch (IOException e) {
            return Py.EmptyString;
        }

        ByteArrayOutputStream databuf = new ByteArrayOutputStream();
        int currentByte = -1;
        try {
            while ((currentByte = decompressStream.read()) != -1) {
                databuf.write(currentByte);
            }
            returnData = new PyString(new String(databuf.toByteArray()));
            if (compressedData.available() > 0) {
                byte[] unusedbuf = new byte[compressedData.available()];
                compressedData.read(unusedbuf);

                unused_data = (PyString) unused_data.__add__(new PyString(
                        new String(unusedbuf)));
            }
            eofReached = true;
        } catch (IOException e) {
            return Py.EmptyString;
        }

        return returnData;
    }

}
