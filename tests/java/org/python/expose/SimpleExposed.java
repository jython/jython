package org.python.expose;

import org.python.core.PyObject;

@Exposed(name = "simpleexposed")
public class SimpleExposed extends PyObject {

    public int timesCalled;

    @Exposed
    public void simple_method() {
        timesCalled++;
    }

    @Exposed
    public void simpleexposed_prefixed() {}

    public void method() {}
}