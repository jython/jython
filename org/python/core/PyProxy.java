package org.python.core;

// This interface should be applicable to ANY class
// Choose names that are extremely unlikely to have conflicts
public interface PyProxy {
    abstract public void _setPyInstance(PyInstance proxy);
    abstract public PyInstance _getPyInstance();
	
    abstract public void _setPySystemState(PySystemState ss);
    abstract public PySystemState _getPySystemState();
}
