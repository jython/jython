package org.python.antlr.ast;
import org.python.antlr.PythonTree;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import java.io.DataOutputStream;
import java.io.IOException;

public class ErrorStmt extends stmtType {

    public static final String[] _fields = new String[] {};

    public ErrorStmt(PythonTree tree) {
        super(tree);
    }

    public String toString() {
        return "ErrorStmt";
    }

    public String toStringTree() {
        return "ErrorStmt";
    }

    public int getLineno() {
        return getLine();
    }

    public int getCol_offset() {
        return getCharPositionInLine();
    }

    public <R> R accept(VisitorIF<R> visitor) {
        return null;
    }

    public void traverse(VisitorIF visitor) throws Exception {
        //no op.
    }

}
