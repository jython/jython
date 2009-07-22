package org.python.jsr223;

import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;

@ExposedType(name = "scope", isBaseType = false)
public final class PyScriptEngineScope extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyScriptEngineScope.class);

    private final ScriptContext context;
    private final ScriptEngine engine;

    PyScriptEngineScope(ScriptEngine engine, ScriptContext context) {
        this.context = context;
        this.engine = engine;
    }

    @ExposedGet(name = "context")
    public PyObject pyGetContext() {
        return Py.java2py(context);
    }

    @ExposedGet(name = "engine")
    public PyObject pyGetEngine() {
        return Py.java2py(engine);
    }

    @ExposedMethod
    public PyObject scope_keys() {
        PyList members = new PyList();
        synchronized (context) {
            List<Integer> scopes = context.getScopes();
            for (int scope : scopes) {
                Bindings bindings = context.getBindings(scope);
                if (bindings == null)
                    continue;
                for (String key : bindings.keySet())
                    members.append(new PyString(key));
            }
        }
        members.sort();
        return members;
    }

    // Not necessary for functionality; present to satisfy __builtin__.PyMapping_check
    @ExposedMethod
    public PyObject __getitem__(PyObject key) {
        return super.__getitem__(key);
    }

    public PyObject __finditem__(PyObject key) {
        return __finditem__(key.asString());
    }

    public PyObject __finditem__(String key) {
        synchronized (context) {
            int scope = context.getAttributesScope(key);
            if (scope == -1)
                return null;
            return Py.java2py(context.getAttribute(key, scope));
        }
    }

    @ExposedMethod
    public void __setitem__(PyObject key, PyObject value) {
        __setitem__(key.asString(), value);
    }

    public void __setitem__(String key, PyObject value) {
        synchronized (context) {
            int scope = context.getAttributesScope(key);
            if (scope == -1)
                scope = ScriptContext.ENGINE_SCOPE;
            context.setAttribute(key, value.__tojava__(Object.class), scope);
        }
    }

    @ExposedMethod
    public void __delitem__(PyObject key) {
        __delitem__(key.asString());
    }

    public void __delitem__(String key) {
        synchronized (context) {
            int scope = context.getAttributesScope(key);
            if (scope == -1)
                throw Py.KeyError(key);
            context.removeAttribute(key, scope);
        }
    }
}
