// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.OutputStream;
import java.io.Writer;

public class StdoutWrapper extends OutputStream {
    protected String name;

    public StdoutWrapper() {
        this.name = "stdout";
    }

    protected PyObject getObject(PySystemState ss) {
        return ss.stdout;
    }

    protected void setObject(PySystemState ss, PyObject obj) {
        ss.stdout = obj;
    }

    protected PyObject myFile() {
        PySystemState ss = Py.getSystemState();
        PyObject obj = getObject(ss);
        if (obj == null) {
            throw Py.AttributeError("missing sys." + this.name);
        }
        if (obj instanceof PyJavaInstance) {
            PyFile f = null;

            Object tmp = obj.__tojava__(OutputStream.class);
            if ((tmp != Py.NoConversion) && (tmp != null)) {
                OutputStream os = (OutputStream) tmp;
                f = new PyFile(os, "<java OutputStream>");
            } else {
                tmp = obj.__tojava__(Writer.class);
                if ((tmp != Py.NoConversion) && (tmp != null)) {
                    Writer w = (Writer) tmp;
                    f = new PyFile(w, "<java Writer>");
                }
            }
            if (f != null) {
                setObject(ss, f);
                return f;
            }
        }
        return obj;
    }

    public void flush() {
        PyObject obj = myFile();
        if (obj instanceof PyFile) {
            ((PyFile) obj).flush();
        } else {
            obj.invoke("flush");
        }
    }

    public void write(String s) {
        PyObject obj = myFile();

        if (obj instanceof PyFile) {
            ((PyFile) obj).write(s);
        } else {
            obj.invoke("write", new PyString(s));
        }
    }

    public void write(int i) {
        write(new String(new char[] { (char) i }));
    }

    public void write(byte[] data, int off, int len) {
        write(new String(data, off, len));
    }

    public void clearSoftspace() {
        PyObject obj = myFile();

        if (obj instanceof PyFile) {
            PyFile file = (PyFile) obj;
            if (file.softspace) {
                file.write("\n");
                file.flush();
            }
            file.softspace = false;
        } else {
            PyObject ss = obj.__findattr__("softspace");
            if (ss != null && ss.__nonzero__()) {
                obj.invoke("write", Py.Newline);
            }
            obj.invoke("flush");
            obj.__setattr__("softspace", Py.Zero);
        }
    }

    public void print(PyObject o, boolean space, boolean newline) {
        PyString string = o.__str__();
        PyObject obj = myFile();

        if (obj instanceof PyFile) {
            PyFile file = (PyFile) obj;
            String s = string.toString();
            if (newline) {
                s = s + "\n";
            }
            if (file.softspace) {
                s = " " + s;
            }
            file.write(s);
            file.flush();
            if (space && s.endsWith("\n")) {
                space = false;
            }
            file.softspace = space;
        } else {
            PyObject ss = obj.__findattr__("softspace");
            if (ss != null && ss.__nonzero__()) {
                obj.invoke("write", Py.Space);
            }
            obj.invoke("write", string);
            if (newline) {
                obj.invoke("write", Py.Newline);
            }
            // obj.invoke("flush");

            if (space && string.toString().endsWith("\n")) {
                space = false;
            }
            obj.__setattr__("softspace", space ? Py.One : Py.Zero);
        }
    }

    public void print(String s) {
        print(new PyString(s), false, false);
    }

    public void println(String s) {
        print(new PyString(s), false, true);
    }

    public void print(PyObject o) {
        print(o, false, false);
    }

    public void printComma(PyObject o) {
        print(o, true, false);
    }

    public void println(PyObject o) {
        print(o, false, true);
    }

    public void println() {
        PyObject obj = myFile();

        if (obj instanceof PyFile) {
            PyFile file = (PyFile) obj;
            file.write("\n");
            file.flush();
            file.softspace = false;
        } else {
            obj.invoke("write", Py.Newline);
            obj.__setattr__("softspace", Py.Zero);
        }
    }
}
