package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name="builtin_function_or_method")
public abstract class PyBuiltinFunction extends PyObject {

    public interface Info {

        String getName();

        int getMaxargs();

        int getMinargs();

        PyException unexpectedCall(int nargs, boolean keywords);
    }

    public static class DefaultInfo implements Info {

        public DefaultInfo(String name, int minargs, int maxargs) {
            this.name = name;
            this.minargs = minargs;
            this.maxargs = maxargs;
        }
        
        public DefaultInfo(String name) {
            this(name, -1, -1);
        }

        private String name;

        private int maxargs, minargs;

        public String getName() {
            return name;
        }

        public int getMaxargs() {
            return maxargs;
        }

        public int getMinargs() {
            return minargs;
        }

        public static boolean check(int nargs, int minargs, int maxargs) {
            if(nargs < minargs)
                return false;
            if(maxargs != -1 && nargs > maxargs)
                return false;
            return true;
        }

        public static PyException unexpectedCall(int nargs,
                                                 boolean keywords,
                                                 String name,
                                                 int minargs,
                                                 int maxargs) {
            if(keywords)
                return Py.TypeError(name + "() takes no keyword arguments");
            String argsblurb;
            if(minargs == maxargs) {
                if(minargs == 0)
                    argsblurb = "no arguments";
                else if(minargs == 1)
                    argsblurb = "exactly one argument";
                else
                    argsblurb = minargs + " arguments";
            } else if(maxargs == -1) {
                return Py.TypeError(name + "() requires at least " + minargs
                        + " (" + nargs + " given)");
            } else {
                if(minargs <= 0)
                    argsblurb = "at most " + maxargs + " arguments";
                else
                    argsblurb = minargs + "-" + maxargs + " arguments";
            }
            return Py.TypeError(name + "() takes " + argsblurb + " (" + nargs
                    + " given)");
        }

        public PyException unexpectedCall(int nargs, boolean keywords) {
            return unexpectedCall(nargs, keywords, name, minargs, maxargs);
        }
    }

    protected PyBuiltinFunction(Info info) {
        this.info = info;
    }

    protected Info info;

    public void setInfo(Info info) {
        this.info = info;
    }

    /**
     * @return a new instance of this type of PyBuiltinFunction bound to self
     */
    abstract public PyBuiltinFunction bind(PyObject self);

    @ExposedGet(name="__self__")
    public PyObject getSelf() {
        return Py.None;
    }

    public String toString() {
        PyObject self = getSelf();
        if(self == null)
            return "<built-in function " + info.getName() + ">";
        else {
            String typename = self.getType().fastGetName();
            return "<built-in method " + info.getName() + " of " + typename
                    + " object>";
        }
    }

    @ExposedGet(name="__name__")
    public PyObject fastGetName() {
        return Py.newString(this.info.getName());
    }

    @ExposedGet(name="__doc__")
    public PyObject fastGetDoc() {
        return Py.None;
    }

    @ExposedGet(name="__call__")
    public PyObject makeCall() {
        return this;
    }
}
