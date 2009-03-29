package org.python.core;

import java.io.OutputStream;

public interface PythonCodeBundle {

    PyCode loadCode() throws Exception;

    void writeTo(OutputStream stream) throws Exception;

    void saveCode(String directory) throws Exception;

}
