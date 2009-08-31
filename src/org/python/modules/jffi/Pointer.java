
package org.python.modules.jffi;

public interface Pointer {
    long getAddress();
    DirectMemory getMemory();
}
