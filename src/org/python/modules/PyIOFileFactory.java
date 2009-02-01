package org.python.modules;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.core.__builtin__;

// XXX - add support for StringIO, not just cStringIO

public class PyIOFileFactory {

    private PyIOFileFactory() {
    }

    public static PyIOFile createIOFile(PyObject file) {
        Object f = file.__tojava__(cStringIO.StringIO.class);
        if (f != Py.NoConversion) {
            return new cStringIOFile(file);
        } else if (__builtin__.isinstance(file, FileType)) {
            return new FileIOFile(file);
        } else {
            return new ObjectIOFile(file);
        }
    }

    private static PyType FileType = PyType.fromClass(PyFile.class);

    // Use a cStringIO as a file.
    static class cStringIOFile implements PyIOFile {

        cStringIO.StringIO file;

        cStringIOFile(PyObject file) {
            this.file = (cStringIO.StringIO) file.__tojava__(Object.class);
        }

        public void write(String str) {
            file.write(str);
        }

        public void write(char ch) {
            file.writeChar(ch);
        }

        public void flush() {
        }

        public String read(int len) {
            return file.read(len).asString();
        }

        public String readlineNoNl() {
            return file.readlineNoNl().asString();
        }
    }


    // Use a PyFile as a file.
    static class FileIOFile implements PyIOFile {

        PyFile file;

        FileIOFile(PyObject file) {
            this.file = (PyFile) file.__tojava__(PyFile.class);
            if (this.file.getClosed()) {
                throw Py.ValueError("I/O operation on closed file");
            }
        }

        public void write(String str) {
            file.write(str);
        }

        public void write(char ch) {
            file.write(cStringIO.getString(ch));
        }

        public void flush() {
        }

        public String read(int len) {
            return file.read(len).toString();
        }

        public String readlineNoNl() {
            String line = file.readline().toString();
            return line.substring(0, line.length() - 1);
        }
    }


    // Use any python object as a file.
    static class ObjectIOFile implements PyIOFile {

        char[] charr = new char[1];
        StringBuilder buff = new StringBuilder();
        PyObject write;
        PyObject read;
        PyObject readline;
        final int BUF_SIZE = 256;

        ObjectIOFile(PyObject file) {
//          this.file = file;
            write = file.__findattr__("write");
            read = file.__findattr__("read");
            readline = file.__findattr__("readline");
        }

        public void write(String str) {
            buff.append(str);
            if (buff.length() > BUF_SIZE) {
                flush();
            }
        }

        public void write(char ch) {
            buff.append(ch);
            if (buff.length() > BUF_SIZE) {
                flush();
            }
        }

        public void flush() {
            write.__call__(new PyString(buff.toString()));
            buff.setLength(0);
        }

        public String read(int len) {
            return read.__call__(new PyInteger(len)).toString();
        }

        public String readlineNoNl() {
            String line = readline.__call__().toString();
            return line.substring(0, line.length() - 1);
        }
    }
}

