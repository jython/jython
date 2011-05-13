// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.OutputStream;

import org.python.core.util.StringUtil;

public class StdoutWrapper extends OutputStream {
    protected String name;

    public StdoutWrapper() {
        this.name = "stdout";
    }

    protected PyObject getObject(PySystemState ss) {
        return ss.stdout;
    }

    protected void setObject(PySystemState ss, PyObject out) {
        ss.stdout = out;
    }

    protected PyObject myFile() {
        PySystemState ss = Py.getSystemState();
        PyObject out = getObject(ss);
        if (out == null) {
            throw Py.AttributeError("missing sys." + this.name);
        }
        if (out.getJavaProxy() != null) {
            PyFile f = null;

            Object tojava = out.__tojava__(OutputStream.class);
            if (tojava != null && tojava != Py.NoConversion) {
                f = new PyFile((OutputStream)tojava);
            }
            if (f != null) {
                setObject(ss, f);
                return f;
            }
        }
        return out;
    }

    @Override
    public void flush() {
        PyObject out = myFile();
        if (out instanceof PyFile) {
            ((PyFile) out).flush();
        } else {
            try {
                out.invoke("flush");
            } catch (PyException pye) {
                // ok
            }
        }
    }

    public void write(String s) {
        PyObject out = myFile();

        if (out instanceof PyFile) {
            ((PyFile) out).write(s);
        } else {
            out.invoke("write", new PyString(s));
        }
    }

    @Override
    public void write(int i) {
        write(new String(new char[] { (char) i }));
    }

    @Override
    public void write(byte[] data, int off, int len) {
        write(StringUtil.fromBytes(data, off, len));
    }

    public void flushLine() {
        PyObject out = myFile();

        if (out instanceof PyFile) {
            PyFile file = (PyFile) out;
            if (file.softspace) {
                file.write("\n");
                file.flush();
            }
            file.softspace = false;
        } else {
            PyObject ss = out.__findattr__("softspace");
            if (ss != null && ss.__nonzero__()) {
                out.invoke("write", Py.Newline);
            }
            try {
                out.invoke("flush");
            } catch (PyException pye) {
                // ok
            }
            out.__setattr__("softspace", Py.Zero);
        }
    }

    public void print(PyObject obj, boolean space, boolean newline) {
        print(new PyObject[] {obj}, space, newline, null);
    }

    public void print(PyObject[] objs, boolean space, boolean newline, PyObject sep) {
        PyObject out = myFile();

        if (out instanceof PyFile) {
            PyFile file = (PyFile)out;
            if (file.softspace) {
                file.write(" ");
                file.softspace = false;
            }

            for (PyObject obj: objs) {
                String s;
                if (obj instanceof PyUnicode && file.encoding != null) {
                    s = ((PyUnicode)obj).encode(file.encoding, "strict");
                } else {
                    s = obj.__str__().toString();
                }
                file.write(s);

                if (obj instanceof PyString) {
                    int len = s.length();
                    if (len == 0 || !Character.isWhitespace(s.charAt(len - 1))
                        || s.charAt(len - 1) == ' ') {
                        file.softspace = space;
                    }
                } else {
                    file.softspace = space;
                }
            }

            if (newline) {
                file.write("\n");
                file.softspace = false;
            }
            file.flush();
        } else if (out instanceof PyFileWriter) {
            PyFileWriter file = (PyFileWriter)out;
            if (file.softspace) {
                file.write(" ");
                file.softspace = false;
            }

            for (PyObject obj: objs) {
                // since we are outputting directly to a character stream,
                // avoid doing an encoding
                String s;
                if (obj instanceof PyString) {
                    s = ((PyString) obj).getString();
                } else {
                    s = obj.toString();
                }
                file.write(s);

                if (obj instanceof PyString) {
                    int len = s.length();
                    if (len == 0 || !Character.isWhitespace(s.charAt(len - 1))
                        || s.charAt(len - 1) == ' ') {
                        file.softspace = space;
                    }
                } else {
                    file.softspace = space;
                }
            }

            if (newline) {
                file.write("\n");
                file.softspace = false;
            }
            file.flush();
        } else {
            PyObject ss = out.__findattr__("softspace");
            if (ss != null && ss.__nonzero__()) {
                out.invoke("write", Py.Space);
                out.__setattr__("softspace", Py.Zero);
            }

            for (PyObject obj: objs) {
                if (!(obj instanceof PyUnicode)) {
                    obj = obj.__str__();
                }
                out.invoke("write", obj);

                if (obj instanceof PyString) {
                    String s = obj.toString();
                    int len = s.length();
                    if (len == 0 || !Character.isWhitespace(s.charAt(len - 1))
                        || s.charAt(len - 1) == ' ') {
                        out.__setattr__("softspace", space ? Py.One : Py.Zero);
                    }
                } else {
                    out.__setattr__("softspace", space ? Py.One : Py.Zero);
                }
            }

            if (newline) {
                out.invoke("write", Py.Newline);
                out.__setattr__("softspace", Py.Zero);
            }
        }
    }

    public void print(String s) {
        print(new PyString(s), false, false);
    }

    public void println(String s) {
        print(new PyString(s), false, true);
    }

    public void print(PyObject obj) {
        print(obj, false, false);
    }

    public void printComma(PyObject obj) {
        print(obj, true, false);
    }

    public void println(PyObject obj) {
        print(obj, false, true);
    }

    public void println() {
        PyObject out = myFile();

        if (out instanceof PyFile) {
            PyFile file = (PyFile) out;
            file.write("\n");
            file.flush();
            file.softspace = false;
        } else {
            out.invoke("write", Py.Newline);
            out.__setattr__("softspace", Py.Zero);
        }
    }
}
