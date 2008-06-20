
package org.python.core;

public class CompilerFlags {

    public boolean nested_scopes = true;
    public boolean division;
    public boolean generator_allowed = true;
    public boolean only_ast = false;
    public boolean with_statement = false;

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
        if ((co_flags & org.python.core.PyTableCode.CO_WITH_STATEMENT) != 0) {
            this.with_statement = true;
        }
        if ((co_flags & org.python.core.PyTableCode.PyCF_ONLY_AST) != 0) {
            this.only_ast = true;
        }
    }
    
    public String toString() {
        return String.format("CompilerFlags[division=%s nested_scopes=%s generators=%s "
                             + "with_statement=%s]", division, nested_scopes, generator_allowed,
                             with_statement);
    }
    
}
