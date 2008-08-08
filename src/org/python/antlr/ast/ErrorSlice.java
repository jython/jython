package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class ErrorSlice extends sliceType {

    public static final String[] _fields = new String[] {};

    public ErrorSlice(PythonTree tree) {
        super(tree);
    }

    public String toString() {
        return "ErrorSlice";
    }

    public String toStringTree() {
        return "ErrorSlice";
    }

    public int getLineno() {
        return getLine();
    }

    public int getCol_offset() {
        return getCharPositionInLine();
    }

}
