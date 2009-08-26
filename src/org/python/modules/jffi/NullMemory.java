

package org.python.modules.jffi;

/**
 * An implementation of MemoryIO that throws an exception on any access.
 */
public class NullMemory extends InvalidMemory implements DirectMemory {
    static final NullMemory INSTANCE = new NullMemory();
    public NullMemory() {
        super("NULL pointer access");
    }

    public long getAddress() {
        return 0L;
    }

    public boolean isNull() {
        return true;
    }
    public final boolean isDirect() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DirectMemory && ((DirectMemory) obj).getAddress() == 0;
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
