// Copyright © Corporation for National Research Initiatives
package org.python.core;
import java.util.Vector;

public class PyCompoundCallable extends PyObject {
    private Vector callables;
    private PySystemState systemState;

    public PyCompoundCallable () {
        callables = new Vector();
        systemState = Py.getSystemState();
    }

    public void append(PyObject callable) {
        callables.addElement(callable);
    }

    public void clear() {
        callables.removeAllElements();
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
	// Set the system state to handle callbacks from java threads
	Py.setSystemState(systemState);
	int n = callables.size();
	//System.out.println("callable: "+n);
        for (int i=0; i<n; i++) {
            ((PyObject)callables.elementAt(i)).__call__(args, keywords);
        }
        return Py.None;
    }

    public String toString() {
        return "<CompoundCallable with "+callables.size()+" callables>";
    }
}
