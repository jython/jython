package org.python.core;

import com.google.common.collect.MapMaker;
import java.util.Map;

/**
 * A ThreadState augments a standard java.lang.Thread to support Python semantics. The ThreadStateMapping utility class
 * ensures that the runtime can look up a ThreadState at any time for a given Thread, while also ensuring that it is
 * properly cleaned up.
 *
 * A ThreadState to Thread relation must be maintained over the entirety of the Python call stack, including any
 * interleaving with Java code. This relationship is maintained by inCallThreadState; the use of ThreadLocal<Object[]>
 * ensures that the Thread has no static class loader dependency on the Jython runtime or any classes that are loaded.
 * Because ThreadState itself has a weak reference to PySystemState, we also need to ensure a hard reference to it is
 * separately maintained in the call stack.
 *
 * The ThreadState to Thread relationship also need to be maintained so long as the PySystemState for a ThreadState (shared by
 * N ThreadState objects) is referenced. This relationship is maintained by the bijective mapping provided by
 * globalThreadStates and inverseGlobalThreadStates.
 *
 * See discussion here: http://bugs.jython.org/issue2321
 * and: http://bugs.jython.org/issue1327
 *
 * NOTE and possible FIXME:
 * inCallThreadState does not currently track changes rebinding a ThreadState to a new PySystemState in the Python
 * call stack. But not certain if this actually would actually work - changing sys in this way for some thread of
 * execution likely would cause lots of issues in the Python code!!!
 */

class ThreadStateMapping {
    private static final Map<Thread, ThreadState> globalThreadStates =
            new MapMaker().weakKeys().makeMap();
    private static final Map<ThreadState, Thread> inverseGlobalThreadStates =
            new MapMaker().weakValues().makeMap();
    private static final ThreadLocal<Object[]> inCallThreadState = new ThreadLocal<Object[]>() {
        @Override
        protected Object[] initialValue() {
            return new Object[2]; // ThreadState, hard ref to the ThreadState's PySystemState
        }
    };

    public ThreadState getThreadState(PySystemState newSystemState) {
        Object[] scoped = inCallThreadState.get();
        if (scoped[0] != null) {
            return (ThreadState)scoped[0];
        }
        Thread currentThread = Thread.currentThread();
        ThreadState ts = globalThreadStates.get(currentThread);
        if (ts != null) {
            return ts;
        }

        if (newSystemState == null) {
            Py.writeDebug("threadstate", "no current system state");
            if (Py.defaultSystemState == null) {
                PySystemState.initialize();
            }
            newSystemState = Py.defaultSystemState;
        }

        PySystemStateRef freedRef = (PySystemStateRef)PySystemStateRef.referenceQueue.poll();
        while (freedRef != null ) {
            globalThreadStates.remove(inverseGlobalThreadStates.remove(freedRef.getThreadState()));
            freedRef = (PySystemStateRef)PySystemStateRef.referenceQueue.poll();
        }
        ts = new ThreadState(newSystemState);
        globalThreadStates.put(currentThread, ts);
        inverseGlobalThreadStates.put(ts, currentThread);
        return ts;
    }

    public static void enterCall(ThreadState ts) {
        if (ts.call_depth == 0) {
            Object[] scoped = inCallThreadState.get();
            scoped[0] = ts;
            scoped[1] = ts.getSystemState();
        } else if (ts.call_depth > ts.getSystemState().getrecursionlimit()) {
            throw Py.RuntimeError("maximum recursion depth exceeded");
        }
        ts.call_depth++;
    }

    public static void exitCall(ThreadState ts) {
        ts.call_depth--;
        if (ts.call_depth == 0) {
            Object[] scoped = inCallThreadState.get();
            scoped[0] = null; // allow ThreadState to be GCed
            scoped[1] = null; // allow corresponding PySystemState to be GCed
        }
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<Thread, ThreadState>[] entriesPrototype = new Map.Entry[0];
    public static PyDictionary _current_frames() {
        Map.Entry<Thread, ThreadState>[] entries = globalThreadStates.entrySet().toArray(entriesPrototype);
        int i = 0;
        for (Map.Entry<Thread, ThreadState> entry: entries) {
            if (entry.getValue().frame != null) {
                ++i;
            }
        }
        PyObject elements[] = new PyObject[i*2];
        i = 0;
        for (Map.Entry<Thread, ThreadState> entry: entries) {
            if (entry.getValue().frame != null) {
                elements[i++] = Py.newInteger(entry.getKey().getId());
                elements[i++] = entry.getValue().frame;
            }
        }
        return new PyDictionary(elements);
    }
}
