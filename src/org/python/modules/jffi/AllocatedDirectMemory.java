
package org.python.modules.jffi;

public interface AllocatedDirectMemory extends DirectMemory {
    public void free();
    public void setAutoRelease(boolean autorelease);
}
