
package org.python.core;

public class CompilerFlags {
    
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
    }
    
    public String toString() {
        return "CompilerFlags[division=" + division + " nested_scopes=" + nested_scopes + " generators="
                + generator_allowed + "]";
    }
    
    

    public boolean nested_scopes = true;
    public boolean division;
    public boolean generator_allowed;

    public String encoding;
}
