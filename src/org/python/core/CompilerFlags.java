
package org.python.core;

public class CompilerFlags {

    private int co_flags;

    public boolean nested_scopes = true;
    public boolean division;
    public boolean generator_allowed = true;
    public boolean with_statement;
    public boolean absolute_import;

    public boolean only_ast;
    public boolean dont_imply_dedent;
    public boolean source_is_utf8;

    public String encoding;

    public CompilerFlags() {}

    public CompilerFlags(int co_flags) {
        this.co_flags = co_flags;
        nested_scopes = isEnabled(PyTableCode.CO_NESTED);
        division = isEnabled(PyTableCode.CO_FUTUREDIVISION);
        generator_allowed = isEnabled(PyTableCode.CO_GENERATOR_ALLOWED);
        absolute_import = isEnabled(PyTableCode.CO_FUTURE_ABSOLUTE_IMPORT);
        with_statement = isEnabled(PyTableCode.CO_WITH_STATEMENT);
        only_ast = isEnabled(PyTableCode.PyCF_ONLY_AST);
        dont_imply_dedent = isEnabled(PyTableCode.PyCF_DONT_IMPLY_DEDENT);
        source_is_utf8 = isEnabled(PyTableCode.PyCF_SOURCE_IS_UTF8);
    }

    private boolean isEnabled(int codeConstant) {
        return (co_flags & codeConstant) != 0;
    }

    public String toString() {
        return String.format("CompilerFlags[division=%s nested_scopes=%s generators=%s "
                             + "with_statement=%s absolute_import=%s only_ast=%s "
                             + "dont_imply_dedent=%s  source_is_utf8=%s]", division, nested_scopes,
                             generator_allowed, with_statement, absolute_import, only_ast,
                             dont_imply_dedent, source_is_utf8);
    }

}
