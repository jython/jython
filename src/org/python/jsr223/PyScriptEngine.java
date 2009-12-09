package org.python.jsr223;

import java.lang.reflect.Method;
import org.python.core.*;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.python.util.PythonInterpreter;

public class PyScriptEngine extends AbstractScriptEngine implements Compilable, Invocable {

    private final PythonInterpreter interp;
    private final ScriptEngineFactory factory;
    private final PyModule module;

    PyScriptEngine(ScriptEngineFactory factory) {
        this.factory = factory;
        interp = new PythonInterpreter(new PyScriptEngineScope(this, context));
        module = (PyModule)Py.getSystemState().modules.__finditem__("__main__");
    }

    public Object eval(String script, ScriptContext context) throws ScriptException {
        return eval(compileScript(script, context), context);
    }

    private Object eval(PyCode code, ScriptContext context) throws ScriptException {
        try {
            interp.setIn(context.getReader());
            interp.setOut(context.getWriter());
            interp.setErr(context.getErrorWriter());
            return interp.eval(code).__tojava__(Object.class);
        } catch (PyException pye) {
            throw scriptException(pye);
        }
    }

    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return eval(compileScript(reader, context), context);
    }

    public Bindings createBindings() {
        return new SimpleBindings();
    }

    public ScriptEngineFactory getFactory() {
        return factory;
    }

    public CompiledScript compile(String script) throws ScriptException {
        return new PyCompiledScript(compileScript(script, context));
    }

    public CompiledScript compile(Reader reader) throws ScriptException {
        return new PyCompiledScript(compileScript(reader, context));
    }

    private PyCode compileScript(String script, ScriptContext context) throws ScriptException {
        try {
            String filename = (String) context.getAttribute(ScriptEngine.FILENAME);
            if (filename == null) {
                return interp.compile(script);
            } else {
                return interp.compile(script, filename);
            }
        } catch (PyException pye) {
            throw scriptException(pye);
        }
    }

    private PyCode compileScript(Reader reader, ScriptContext context) throws ScriptException {
        try {
            String filename = (String) context.getAttribute(ScriptEngine.FILENAME);
            if (filename == null) {
                return interp.compile(reader);
            } else {
                return interp.compile(reader, filename);
            }
        } catch (PyException pye) {
            throw scriptException(pye);
        }
    }

    public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException,
            NoSuchMethodException {
        try {
            if (!(thiz instanceof PyObject)) {
                thiz = Py.java2py(thiz);
            }
            PyObject method = ((PyObject) thiz).__findattr__(name);
            if (method == null) {
                throw new NoSuchMethodException(name);
            }
            return method.__call__(Py.javas2pys(args)).__tojava__(Object.class);
        } catch (PyException pye) {
            throw scriptException(pye);
        }
    }

    public Object invokeFunction(String name, Object... args) throws ScriptException,
            NoSuchMethodException {
        try {
            PyObject function = interp.get(name);
            if (function == null) {
                throw new NoSuchMethodException(name);
            }
            return function.__call__(Py.javas2pys(args)).__tojava__(Object.class);
        } catch (PyException pye) {
            throw scriptException(pye);
        }
    }

    public <T> T getInterface(Class<T> clazz) {
        return getInterface(module, clazz);
    }

    public <T> T getInterface(Object obj, Class<T> clazz) {
        if (obj == null) {
            throw new IllegalArgumentException("object expected");
        }
        if (clazz == null || !clazz.isInterface()) {
            throw new IllegalArgumentException("interface expected");
        }
        final PyObject thiz = Py.java2py(obj);
        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class[] { clazz },
            new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        PyObject pyMethod = thiz.__findattr__(method.getName());
                        if (pyMethod == null)
                            throw new NoSuchMethodException(method.getName());
                        PyObject result = pyMethod.__call__(Py.javas2pys(args));
                        return result.__tojava__(Object.class);
                    } catch (PyException pye) {
                        throw scriptException(pye);
                    }
                }
            });
        return proxy;
    }

    private static ScriptException scriptException(PyException pye) {
        ScriptException se = null;
        try {
            pye.normalize();

            PyObject type = pye.type;
            PyObject value = pye.value;
            PyTraceback tb = pye.traceback;

            if (__builtin__.isinstance(value, Py.SyntaxError)) {
                PyObject filename = value.__findattr__("filename");
                PyObject lineno = value.__findattr__("lineno");
                PyObject offset = value.__findattr__("offset");
                value = value.__findattr__("msg");

                se = new ScriptException(
                        Py.formatException(type, value),
                        filename == null ? "<script>" : filename.toString(),
                        lineno == null ? 0 : lineno.asInt(),
                        offset == null ? 0 : offset.asInt());
            } else if (tb != null) {
                String filename;
                if (tb.tb_frame == null || tb.tb_frame.f_code == null) {
                    filename = null;
                } else {
                    filename = tb.tb_frame.f_code.co_filename;
                }
                se = new ScriptException(
                        Py.formatException(type, value),
                        filename,
                        tb.tb_lineno);
            } else {
                se = new ScriptException(Py.formatException(type, value));
            }
            se.initCause(pye);
            return se;
        } catch (Exception ee) {
            se = new ScriptException(pye);
        }
        return se;
    }

    private class PyCompiledScript extends CompiledScript {
        private PyCode code;

        PyCompiledScript(PyCode code) {
            this.code = code;
        }

        @Override
        public ScriptEngine getEngine() {
            return PyScriptEngine.this;
        }

        @Override
        public Object eval(ScriptContext ctx) throws ScriptException {
            return PyScriptEngine.this.eval(code, ctx);
        }
    }
}
