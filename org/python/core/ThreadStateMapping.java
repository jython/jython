
package org.python.core;

public class ThreadStateMapping {
    private static boolean checkedJava2 = false;

    public static ThreadStateMapping makeMapping() {
        if (!checkedJava2) {
            checkedJava2 = true;
            String version = System.getProperty("java.version");
            if (version.compareTo("1.2") >= 0) {
                try {
                    Class c = Class.forName("org.python.core.ThreadStateMapping2");
                    return (ThreadStateMapping) c.newInstance();
                } catch (Throwable t) { }
            }
        }
        return new ThreadStateMapping();
    }


    // There are hacks that could improve performance slightly in the
    // interim, but I'd rather wait for the right solution.
    private static java.util.Hashtable threads;
    private static ThreadState cachedThreadState;

    // Mechanism provided by Drew Morrissey and approved by JimH which
    // occasionally cleans up dead threads, preventing the hashtable from
    // leaking memory when many threads are used (e.g. in an embedded
    // application).
    //
    // counter of additions to the threads table
    private static int additionCounter = 0;
    // maximum number of thread additions before cleanup is triggered
    private static final int MAX_ADDITIONS = 25;


    public ThreadState getThreadState(PySystemState newSystemState) {
        Thread t = Thread.currentThread();
        ThreadState ts = cachedThreadState;
        if (ts != null && ts.thread == t) {
            return ts;
        }

        if (threads == null)
            threads = new java.util.Hashtable();

        ts = (ThreadState)threads.get(t);
        if (ts == null) {
            if (newSystemState == null) {
                Py.writeDebug("threadstate", "no current system state");
                //t.dumpStack();
                newSystemState = Py.defaultSystemState;
            }
            ts = new ThreadState(t, newSystemState);
            //System.err.println("new ts: "+ts+", "+ts.systemState);
            threads.put(t, ts);
            // increase the counter each time a thread reference is added
            // to the table
            additionCounter++;
            if (additionCounter > MAX_ADDITIONS) {
                cleanupThreadTable();
                additionCounter = 0;
            }
        }
        cachedThreadState = ts;
        //System.err.println("returning ts: "+ts+", "+ts.systemState);
        return ts;
    }


    
    /**
     * Enumerates through the thread table looking for dead thread
     * references and removes them.  Called internally by
     * getThreadState(PySystemState).
     */
    private void cleanupThreadTable() {
        // loop through thread table removing dead thread references
        for (java.util.Enumeration e = threads.keys(); e.hasMoreElements();) {
            try {
                Object key = e.nextElement();
                ThreadState tempThreadState = (ThreadState)threads.get(key);
                if ((tempThreadState != null) &&
                    (tempThreadState.thread != null) &&
                    !tempThreadState.thread.isAlive())
                {
                    threads.remove(key);
                }
            }
            catch (ClassCastException exc) {
                // TBD: we should throw some type of exception here
            }
        }
    }
}
