// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import org.python.base.InterpreterError;

/**
 * An interpreter is responsible for certain variable aspects of the
 * "context" within which Python code executes. Chief among these is
 * the search path (in fact the whole mechanism) along which modules
 * are found, and the dictionary of imported modules itself.
 * <p>
 * The interpreter also holds the wrappers around the standard input
 * and output streams, and the registry of codecs. Many of these are
 * exposed through the {@code sys} module, rather than any class
 * with "interpreter" in the name.
 */
class Interpreter {

    /**
     * The list of modules created by this interpreter, exposed as
     * {@code sys.modules} when we have a {@code sys} module
     */
    final PyDict modules = new PyDict();

    /**
     * The builtins module. An instance is created with each
     * {@code Interpreter}. Not {@code null}.
     */
    final PyModule builtinsModule;

    /** Create a new {@code Interpreter}. */
    Interpreter() {
        builtinsModule = new BuiltinsModule();
        builtinsModule.exec();
        // addModule(builtinsModule);
    }

    /**
     * Add the given module to the interpreter's list of modules
     * (effectively the source of {@code sys.modules}).
     *
     * @param m to add
     */
    void addModule(PyModule m) {
        if (modules.putIfAbsent(m.name, m) != null)
            throw new InterpreterError("Interpreter.addModule: Module already added %s", m.name);
    }

    /**
     * Execute the code object and return the result. This is quite like
     * {@link BuiltinsModule#exec(Object, Object, Object, Object)
     * builtins.exec()}, except that it works without a surrounding
     * {@link PyFrame}, from which it could infer {@code globals} and
     * {@code locals}. It will create a frame, but it may be on an empty
     * stack.
     *
     * @param code compiled code object
     * @param globals global context dictionary
     * @param locals local variables (a Python mapping), may be the same
     *     as {@code globals} or {@code null}
     * @return result of evaluation
     */
    // Compare CPython PyEval_EvalCode in ceval.c
    Object eval(PyCode code, PyDict globals, Object locals) {
        if (locals == null) { locals = globals; }
        globals.putIfAbsent("__builtins__", builtinsModule);
        PyFunction<?> func = code.createFunction(this, globals);
        PyFrame<?> f = func.createFrame(locals);
        return f.eval();
    }

    /**
     * Execute the code object and return the result. This is the
     * equivalent of {@link #eval(PyCode, PyDict, Object) eval(code,
     * globals, globals)}
     *
     * @param code compiled code object
     * @param globals global context dictionary
     * @return result of evaluation
     */
    Object eval(PyCode code, PyDict globals) { return eval(code, globals, globals); }

    /**
     * Get the value of an attribute of the built-in module, equivalent
     * to {@code builtinsModule.dict.get(name)}.
     *
     * @param name of the attribute ({@code String} or {@code str})
     * @return value of the attribute
     */
    Object getBuiltin(String name) { return builtinsModule.dict.get(name); }
}
