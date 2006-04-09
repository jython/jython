
package org.python.compiler;

public class SymInfo extends Object {

    public SymInfo(int flags) {
        this.flags = flags;
    }

    public SymInfo(int flags,int locals_index) {
        this.flags = flags;
        this.locals_index = locals_index;
    }

    public int flags;
    public int locals_index;
    public int env_index;


    public String toString() {
        return "SymInfo[" + flags + " " + locals_index + " " +
                        env_index + "]";
    }
}
