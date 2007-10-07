package org.python.expose;

import org.python.core.PyObject;

@Exposed
public class SimpleExposed extends PyObject {

    public int timesCalled;

    @Exposed
    public void simple_method() {
        timesCalled++;
    }

    public void method() {}
}