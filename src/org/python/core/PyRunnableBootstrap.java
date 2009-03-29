package org.python.core;

import java.lang.reflect.Constructor;

public class PyRunnableBootstrap implements CodeBootstrap {

    public static final String REFLECTION_METHOD_NAME = "getFilenameConstructorReflectionBootstrap";
    private final PyRunnable runnable;

    PyRunnableBootstrap(PyRunnable runnable) {
        this.runnable = runnable;
    }

    public PyCode loadCode(CodeLoader loader) {
        return runnable.getMain();
    }

    public static CodeBootstrap getFilenameConstructorReflectionBootstrap(
            Class<? extends PyRunnable> cls) {
        final Constructor<? extends PyRunnable> constructor;
        try {
            constructor = cls.getConstructor(String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "PyRunnable class does not specify apropriate constructor.",
                    e);
        }
        return new CodeBootstrap() {

            public PyCode loadCode(CodeLoader loader) {
                try {
                    return constructor.newInstance(loader.filename).getMain();
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "PyRunnable class constructor does not support instantiation protocol.",
                            e);
                }
            }

        };
    }

}
