package org.python.jsr223;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;

/**
 * JSR 223 does not map well to Jython's concept of "locals" and "globals".
 * Instead, SimpleScriptContext provides ENGINE_SCOPE and GLOBAL_SCOPE, each
 * with its own bindings.  We adapt this multi-scope object for use as both
 * a local and global dictionary.
 */
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
        List<Integer> scopes = context.getScopes();
        for (int scope : scopes) {
            Bindings bindings = context.getBindings(scope);
            if (bindings == null)
                continue;
            for (String key : bindings.keySet())
                members.append(new PyString(key));
        }
        members.sort();
        return members;
    }

    // satisfy mapping and lookup
    @ExposedMethod
    @Override
    public PyObject __getitem__(PyObject key) {
        return __finditem__(key);
    }

    // satisfy iterable
    @ExposedMethod
    @Override
    public PyObject __iter__() {
        return new ScopeIterator(this);
    }

    @ExposedMethod(defaults = "Py.None")
    final PyObject scope_get(PyObject keyObj, PyObject defaultObj) {
        String key = keyObj.asString();
        int scope = context.getAttributesScope(key);
        return scope == -1 ? defaultObj : Py.java2py(context.getAttribute(key, scope));
    }

    @ExposedMethod
    final boolean scope_has_key(PyObject key) {
        return context.getAttributesScope(key.asString()) != -1;
    }

    @Override
    public boolean __contains__(PyObject obj) {
        return scope___contains__(obj);
    }

    @ExposedMethod
    final boolean scope___contains__(PyObject obj) {
        return scope_has_key(obj);
    }

    @ExposedMethod(defaults = "Py.None")
    final PyObject scope_setdefault(PyObject keyObj, PyObject failObj) {
        PyObject result;
        String key = keyObj.asString();
        int scope = context.getAttributesScope(key);
        if (scope == -1) {
            scope = ScriptContext.ENGINE_SCOPE;
            context.setAttribute(key,
                                 failObj instanceof PyType
                                 ? failObj : failObj.__tojava__(Object.class),
                                 scope);
            result = failObj;
        } else {
            result = Py.java2py(context.getAttribute(key, scope));
        }
        return result;
    }

    @Override
    public String toString() {
        return getDictionary().toString();
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        return __finditem__(key.asString());
    }

    @Override
    public PyObject __finditem__(String key) {
        int scope = context.getAttributesScope(key);
        if (scope == -1)
            return null;
        return Py.java2py(context.getAttribute(key, scope));
    }

    @ExposedMethod
    @Override
    public void __setitem__(PyObject key, PyObject value) {
        __setitem__(key.asString(), value);
    }

    @Override
    public void __setitem__(String key, PyObject value) {
        int scope = context.getAttributesScope(key);
        if (scope == -1)
            scope = ScriptContext.ENGINE_SCOPE;
        context.setAttribute(key,
                             value instanceof PyType ? value : value.__tojava__(Object.class),
                             scope);
    }

    @ExposedMethod
    @Override
    public void __delitem__(PyObject key) {
        __delitem__(key.asString());
    }

    @Override
    public void __delitem__(String key) {
        int scope = context.getAttributesScope(key);
        if (scope == -1)
            throw Py.KeyError(key);
        context.removeAttribute(key, scope);
    }

    private Map<PyObject, PyObject> getMap() {
        ScopeIterator iterator = new ScopeIterator(this);
        Map<PyObject, PyObject> map = new HashMap<PyObject, PyObject>(iterator.size());
        PyObject key = iterator.__iternext__();
        while (key != null) {
            map.put(key, __finditem__(key));
            key = iterator.__iternext__();
        }
        return map;
    }

    private PyDictionary getDictionary() {
        return new PyDictionary(getMap());
    }

    public class ScopeIterator extends PyIterator {
        private int _index;
        private int _size;
        private PyObject _keys;

        ScopeIterator(PyScriptEngineScope scope) {
            _keys = scope.scope_keys();
            _size = _keys.__len__();
            _index = -1;
        }

        public int size() {
            return _size;
        }

        @Override
        public PyObject __iternext__() {
            PyObject result = null;
            _index++;
            if (_index < size()) {
                result = _keys.__getitem__(_index);
            }
            return result;
        }
    }
}
