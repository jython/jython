
package org.python.core;

public class CompilerFlags extends Object {
    
    public CompilerFlags() {}
    
    public CompilerFlags(int co_flags) {
        if ((co_flags&org.python.core.PyTableCode.CO_NESTED) != 0) nested_scopes = true;
    }

    public boolean nested_scopes;

}
