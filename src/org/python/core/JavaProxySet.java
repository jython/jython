package org.python.core;

import java.util.Set;

/** Proxy objects implementing java.util.Set */

class JavaProxySet {

    private static class SetMethod extends PyBuiltinMethodNarrow {
        protected SetMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        protected SetMethod(String name, int minArgs, int maxArgs) {
            super(name, minArgs, maxArgs);
        }

        protected Set asSet(){
            return (Set)self.getJavaProxy();
        }

        protected Set newSet() {
            try {
                return (Set) asSet().getClass().newInstance();
            } catch (IllegalAccessException e) {
                throw Py.JavaError(e);
            } catch (InstantiationException e) {
                throw Py.JavaError(e);
            }
        }
    }

    private static final PyBuiltinMethodNarrow setIsDisjointProxy = new SetMethod("isdisjoint", 1) {
        @Override
        public PyObject __call__(PyObject other) {
            return Py.None;
        }
    };

    static PyBuiltinMethod[] getProxyMethods() {
        return new PyBuiltinMethod[]{};
    }

    static PyBuiltinMethod[] getPostProxyMethods() {
        return new PyBuiltinMethod[]{};
    }

}
