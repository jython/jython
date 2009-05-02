package org.python.jsr223;

import java.io.Reader;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class PyScriptEngine extends AbstractScriptEngine implements Compilable, Invocable {

    private final PythonInterpreter interp;
    private final ScriptEngineFactory factory;

    PyScriptEngine(ScriptEngineFactory factory) {
        this.factory = factory;
        interp = new PythonInterpreter();
    }

    public Object eval(String script, ScriptContext context) throws ScriptException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // it would be nice if we supported a Reader interface in Py.compileFlags, instead of having
    // to create a string here
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Bindings createBindings() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ScriptEngineFactory getFactory() {
        return factory;
    }

    // i assume this should simply return a PyModule object or something
    public CompiledScript compile(String script) throws ScriptException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CompiledScript compile(Reader script) throws ScriptException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
        try {
            if (thiz instanceof PyObject) {
                return ((PyObject) thiz).invoke(name, java2py(args)).__tojava__(Object.class);
            }
            throw new NoSuchMethodException(name);
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.AttributeError)) {
                throw new NoSuchMethodException(name);
            }
            throw new ScriptException(pye);
        }
    }

    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        try {
            return interp.get(name).__call__(java2py(args)).__tojava__(Object.class);
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.AttributeError)) {
                throw new NoSuchMethodException(name);
            }
            throw new ScriptException(pye);
        }
    }

    public <T> T getInterface(Class<T> clasz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T getInterface(Object thiz, Class<T> clasz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static PyObject[] java2py(Object[] args) {
        PyObject wrapped[] = new PyObject[args.length];
        for (int i = 0; i < args.length; i++) {
            wrapped[i] = Py.java2py(args[i]);
        }
        return wrapped;
    }

    // wraps a PyCode object
    private static class PyCompiledScript extends CompiledScript {

        @Override
        public Object eval(ScriptContext arg0) throws ScriptException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ScriptEngine getEngine() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
