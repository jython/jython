/**
 * Just a simple interface to demonstrate how subclassing
 * from Jython breaks because of the two protected
 * methods 'finalize' and 'clone'.
 */
public interface test294j extends Cloneable {
    public void doStart();
    public void doEnd();
}

