/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.python.core.PyObject;
import org.python.core.PySystemState;

/**
 * A class to take responsibility for closing a stream on JVM shutdown. On creation a Closer adds
 * itself to a list that will be run by PySystemState on shutdown and it maintains a reference to
 * its client PyIOBase. The client (assuming it kept a reference to the Closer) may dismiss the
 * closer at any time: normally this will be when the client stream is closed directly, or
 * implicitly through {@link PyIOBase#__exit__(PyObject, PyObject, PyObject)}. A possibility is that
 * the JVM exits before an <code>close()</code> is called. In that case, {@link Closer#call()} will
 * be called, and the <code>Closer</code> will call the client's close operation.
 * <p>
 * In case the close operation is overridden so that it does not dismiss the closer, it is useful to
 * have the client's finalize dismiss the closer. If not, the Closer will remain registered and this
 * will keep the <code>PyIOBase</code> object alive in the JVM, which is mostly harmless.
 */
/*
 * Adapted from PyFile, but significantly different to call the overridden close operation of the
 * client itself, rather than just the underlying file.
 */
class Closer<C extends PyIOBase> implements Callable<Void> {

    /** The client in need of closing. */
    private final WeakReference<C> client;

    /** Interpreter state that will call {@link #call()} on shutdown. */
    protected PySystemState sys;
    private AtomicBoolean dismissed = new AtomicBoolean(); //Defaults to false

    public Closer(C toClose, PySystemState sys) {
        this.client = new WeakReference<C>(toClose);
        this.sys = sys;
        sys.registerCloser(this);
    }

    /**
     * Tell the Closer that its services are no longer required. This unhooks it from the shutdown
     * list. Repeated calls are allowed but only the first has any effect.
     */
    public void dismiss() {
        if (dismissed.compareAndSet(false, true)) {
            sys.unregisterCloser(this);
        }
    }

    /**
     * Called as part of a shutdown process. If the client is still (weakly) reachable, invoke its
     * "close" method.
     */
    @Override
    public Void call() {
    	// This will prevent repeated work and dismiss() manipulating the list of closers
    	if (dismissed.compareAndSet(false, true)) {
            // Call close on the client (if it still exists)
            C toClose = client.get();
            if (toClose != null) {
                toClose.invoke("close");
            }
        }
        return null;
    }
}
