package org.python.antlr;

public interface AST {
    public static String[] emptyStringArray = new String[0];

    public String[] get_attributes();
    public String[] get_fields();
}
