package org.python.antlr.ast;
import org.python.antlr.PythonTree;

public class ErrorMod extends modType {

    public static final String[] _fields = new String[] {};

    public ErrorMod(PythonTree tree) {
        super(tree);
    }

    public String toString() {
        return "ErrorMod";
    }

    public String toStringTree() {
        return "ErrorMod";
    }

    public int getLineno() {
        return getLine();
    }

    public int getCol_offset() {
        return getCharPositionInLine();
    }

}
