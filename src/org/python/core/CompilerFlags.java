// At some future point this will also be extended - in conjunction with Py#compileFlags - to add
// support for a compiler factory that user code can choose in place of the normal compiler.
// (Perhaps a better name might have been "CompilerOptions".)

package org.python.core;

public class CompilerFlags {

    private int co_flags;

    public boolean optimized;
    public boolean newlocals;
    public boolean varargs;
    public boolean varkeywords;
    public boolean generator;

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
        optimized = isEnabled(PyBaseCode.CO_OPTIMIZED);
        newlocals = isEnabled(PyBaseCode.CO_NEWLOCALS);
        varargs = isEnabled(PyBaseCode.CO_VARARGS);
        varkeywords = isEnabled(PyBaseCode.CO_VARKEYWORDS);
        generator = isEnabled(PyBaseCode.CO_GENERATOR);
        nested_scopes = isEnabled(PyBaseCode.CO_NESTED);
        division = isEnabled(PyBaseCode.CO_FUTUREDIVISION);
        generator_allowed = isEnabled(PyBaseCode.CO_GENERATOR_ALLOWED);
        absolute_import = isEnabled(PyBaseCode.CO_FUTURE_ABSOLUTE_IMPORT);
        with_statement = isEnabled(PyBaseCode.CO_WITH_STATEMENT);
        only_ast = isEnabled(PyBaseCode.PyCF_ONLY_AST);
        dont_imply_dedent = isEnabled(PyBaseCode.PyCF_DONT_IMPLY_DEDENT);
        source_is_utf8 = isEnabled(PyBaseCode.PyCF_SOURCE_IS_UTF8);
    }

    private boolean isEnabled(int codeConstant) {
        return (co_flags & codeConstant) != 0;
    }

    @Override
    public String toString() {
        return String.format("CompilerFlags[division=%s nested_scopes=%s generators=%s "
                             + "with_statement=%s absolute_import=%s only_ast=%s "
                             + "dont_imply_dedent=%s  source_is_utf8=%s]", division, nested_scopes,
                             generator_allowed, with_statement, absolute_import, only_ast,
                             dont_imply_dedent, source_is_utf8);
    }

    public int toBits() {
        return  (optimized ? PyBaseCode.CO_OPTIMIZED : 0) |
                (newlocals ? PyBaseCode.CO_NEWLOCALS : 0) |
                (varargs ? PyBaseCode.CO_VARARGS : 0) |
                (varkeywords ? PyBaseCode.CO_VARKEYWORDS : 0) |
                (generator ? PyBaseCode.CO_GENERATOR : 0) |
                (nested_scopes ? PyBaseCode.CO_NESTED : 0) |
                (division ? PyBaseCode.CO_FUTUREDIVISION : 0) |
                (generator_allowed ? PyBaseCode.CO_GENERATOR_ALLOWED : 0) |
                (absolute_import ? PyBaseCode.CO_FUTURE_ABSOLUTE_IMPORT : 0) |
                (with_statement ? PyBaseCode.CO_WITH_STATEMENT : 0) |
                (only_ast ? PyBaseCode.PyCF_ONLY_AST : 0) |
                (dont_imply_dedent ? PyBaseCode.PyCF_DONT_IMPLY_DEDENT : 0) |
                (source_is_utf8 ? PyBaseCode.PyCF_SOURCE_IS_UTF8 : 0);
    }

    // this will not strictly be an OR once we have other options, like a compiler factory
    // in that case, we would assume
    public CompilerFlags combine(CompilerFlags flags) {
        return new CompilerFlags(this.toBits() | flags.toBits());
    }

    public CompilerFlags combine(int flags) {
        return new CompilerFlags(this.toBits() | flags);
    }
}
