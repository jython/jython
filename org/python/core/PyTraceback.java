// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyTraceback extends PyObject
{
    public PyObject tb_next;
    public PyFrame tb_frame;
    public int tb_lineno;

    public PyTraceback(PyFrame frame) {
        tb_frame = frame;
        if (tb_frame != null)
            tb_lineno = tb_frame.getline();
        tb_next = Py.None;
    }

    public PyTraceback(PyTraceback next) {
        tb_next = next;
        if (next != null) {
            tb_frame = next.tb_frame.f_back;
            tb_lineno = tb_frame.getline();
        }
    }

    // filename, lineno, function_name
    // "  File \"%.900s\", line %d, in %s\n"
    private String line() {
        if (tb_frame == null || tb_frame.f_code == null)
            return "  (no code object) at line "+tb_lineno+"\n";
        return "  File \""+tb_frame.f_code.co_filename+
            "\", line "+tb_lineno+
            ", in "+tb_frame.f_code.co_name+"\n";
    }

    public void dumpStack(StringBuffer buf) {
        buf.append(line());
        if (tb_next != Py.None && tb_next != this)
            ((PyTraceback)tb_next).dumpStack(buf);
        else if (tb_next == this) {
            buf.append("circularity detected!"+this+tb_next);
        }
    }

    public String dumpStack() {
        StringBuffer buf = new StringBuffer();

        buf.append("Traceback (innermost last):\n");
        dumpStack(buf);

        return buf.toString();
    }

    public String toString() {
        return "<traceback object at " + hashCode() + ">";
    }
}
