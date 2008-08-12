
package org.python.core;

public class CompilerFlags {

    public boolean nested_scopes = true;
    public boolean division;
    public boolean generator_allowed = true;
    public boolean with_statement = false;
    public boolean absolute_import = false;

    public boolean only_ast = false;
    public boolean dont_imply_dedent = false;
    public boolean source_is_utf8 = false;

    public String encoding;
    
    public CompilerFlags(){}

    public CompilerFlags(int co_flags) {
        if ((co_flags & org.python.core.PyTableCode.CO_NESTED) != 0) {
            this.nested_scopes = true;
        }
        if ((co_flags & org.python.core.PyTableCode.CO_FUTUREDIVISION) != 0) {
            this.division = true;
        }
        if ((co_flags & org.python.core.PyTableCode.CO_GENERATOR_ALLOWED) != 0) {
            this.generator_allowed = true;
        }
        if ((co_flags & org.python.core.PyTableCode.CO_FUTURE_ABSOLUTE_IMPORT) != 0) {
            this.absolute_import = true;
        }       
        if ((co_flags & org.python.core.PyTableCode.CO_WITH_STATEMENT) != 0) {
            this.with_statement = true;
        }
        if ((co_flags & org.python.core.PyTableCode.PyCF_ONLY_AST) != 0) {
            this.only_ast = true;
        }
        if ((co_flags & org.python.core.PyTableCode.PyCF_DONT_IMPLY_DEDENT) != 0) {
            this.dont_imply_dedent = true;
        }
        if ((co_flags & org.python.core.PyTableCode.PyCF_SOURCE_IS_UTF8) != 0) {
            this.source_is_utf8 = true;
        }
    
    }

    public String toString() {
        return String.format("CompilerFlags[division=%s nested_scopes=%s generators=%s "
                             + "with_statement=%s absolute_import=%s only_ast=%s "
                             + "dont_imply_dedent=%s  source_is_utf8=%s]", division, nested_scopes,
                             generator_allowed, with_statement, absolute_import, only_ast, 
                             dont_imply_dedent, source_is_utf8);
    }
    
}
