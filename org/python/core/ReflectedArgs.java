// Copyright © Corporation for National Research Initiatives
package org.python.core;

class ReflectedArgs {
    public Class[] args;
    public Object data;
    public Class declaringClass;
    public boolean isStatic;
    public int flags;

    public static final int StandardCall=0;
    public static final int PyArgsCall=1;
    public static final int PyArgsKeywordsCall=2;


    public ReflectedArgs(Object data, Class[] args,
                         Class declaringClass, boolean isStatic)
    {
        this.data = data;
        this.args = args;
        this.declaringClass = declaringClass;
        this.isStatic = isStatic;

        if (args.length == 1 && args[0] == PyObject[].class) {
            flags = PyArgsCall;
        } else if (args.length == 2 &&
                   args[0] == PyObject[].class &&
                   args[1] == String[].class)
        {
            flags = PyArgsKeywordsCall;
        } else {
            flags = StandardCall;
        }
    }

    public boolean matches(PyObject self, PyObject[] pyArgs,
                            String[] keywords, ReflectedCallData callData)
    {
        if (flags != PyArgsKeywordsCall) {
            if (keywords != null && keywords.length != 0)
                return false;
        }

        //if (isStatic ? self != null : self == null) return Py.NoConversion;
        /* Ugly code to handle mismatch in static vs. instance functions... */
        /* Will be very inefficient in cases where static and instance
           functions both exist with same names and number of args
        */
        if (isStatic) {
            if (self != null) {
                /*PyObject[] newArgs = new PyObject[pyArgs.length+1];
                  System.arraycopy(pyArgs, 0, newArgs, 1, pyArgs.length);
                  newArgs[0] = self;
                  pyArgs = newArgs;*/
                self = null;
            }
        } else {
            if (self == null) {
                if (pyArgs.length == 0)
                    return false;
                self = pyArgs[0];
                PyObject[] newArgs = new PyObject[pyArgs.length-1];
                System.arraycopy(pyArgs, 1, newArgs, 0, newArgs.length);
                pyArgs = newArgs;
            }
        }

        if (flags == PyArgsKeywordsCall) { // foo(PyObject[], String[])
            callData.setLength(2);
            callData.args[0] = pyArgs;
            callData.args[1] = keywords;
            callData.self = self;
            if (self != null) {
                Object tmp = self.__tojava__(declaringClass);
                if (tmp != Py.NoConversion)
                    callData.self = tmp;
            }
            return true;
        } else if (flags == PyArgsCall) { // foo(PyObject[])
            callData.setLength(1);
            callData.args[0] = pyArgs;
            callData.self = self;
            if (self != null) {
                Object tmp = self.__tojava__(declaringClass);
                if (tmp != Py.NoConversion)
                    callData.self = tmp;
            }
            return true;
        }

        int n = args.length;
        if (pyArgs.length != n)
            return false;

        // Make error messages clearer
        callData.errArg = -1;

        if (self != null) {
            Object tmp = self.__tojava__(declaringClass);
            if (tmp == Py.NoConversion)
                return false;
            callData.self = tmp;
        } else {
            callData.self = null;
        }

        callData.setLength(n);
        Object[] javaArgs = callData.args;

        for(int i=0; i<n; i++) {
            if ((javaArgs[i] = pyArgs[i].__tojava__(args[i])) ==
                Py.NoConversion)
            {
                // Make error messages clearer
                if (i > callData.errArg)
                    callData.errArg = i;
                return false;
            }
        }
        return true;
    }

    public static int precedence(Class arg) {
        if (arg == Object.class)
            return 3000;
        if (arg.isPrimitive()) {
            if (arg == Long.TYPE) return 10;
            if (arg == Integer.TYPE) return 11;
            if (arg == Short.TYPE) return 12;
            if (arg == Character.TYPE) return 13;
            if (arg == Byte.TYPE) return 14;
            if (arg == Double.TYPE) return 20;
            if (arg == Float.TYPE) return 21;
            if (arg == Boolean.TYPE) return 30;
        }
        // Consider Strings a primitive type
        // This makes them higher priority than byte[]
        if (arg == String.class)
            return 40;

        if (arg.isArray()) {
            Class componentType = arg.getComponentType();
            if (componentType == Object.class)
                return 2500;
            return 100+precedence(componentType);
        }
        return 2000;
    }

    /* Returns 0 iff arg1 == arg2
       Returns +/-1 iff arg1 and arg2 are unimportantly different
       Returns +/-2 iff arg1 and arg2 are significantly different
    */
    public static int compare(Class arg1, Class arg2) {
        int p1 = precedence(arg1);
        int p2 = precedence(arg2);
        // Special code if they are both nonprimitives
        // Superclasses/superinterfaces are considered greater than sub's
        if (p1 >= 2000 && p2 >= 2000) {
            if (arg1.isAssignableFrom(arg2)) {
                if (arg2.isAssignableFrom(arg1)) return 0;
                else return +2;
            } else {
                if (arg2.isAssignableFrom(arg1)) return -2;
                else {
                    int cmp = arg1.getName().compareTo(arg2.getName());
                    return cmp > 0 ? +1 : -1;
                }
            }
        } else {
            return p1 > p2 ? +2 : (p1 == p2 ? 0 : -2);
        }
    }

    public static final int REPLACE = 1998;

    public int compareTo(ReflectedArgs other) {
        Class[] oargs = other.args;

        // First decision based on flags
        if (other.flags != flags) {
            return other.flags < flags ? -1 : +1;
        }

        // Decision based on number of args
        int n = args.length;
        if (n < oargs.length) return -1;
        if (n > oargs.length) return +1;

        // Decide based on static/non-static
        if (isStatic && !other.isStatic) return +1;
        if (!isStatic && other.isStatic) return -1;

        // Compare the arg lists
        int cmp = 0;
        for (int i=0; i<n; i++) {
            int tmp = compare(args[i], oargs[i]);
            if (tmp == +2 || tmp == -2) cmp = tmp;
            if (cmp == 0) cmp = tmp;
        }

        if (cmp != 0) return cmp > 0 ? +1 : -1;

        // If arg lists are equivalent, look at declaring classes
        boolean replace =
            other.declaringClass.isAssignableFrom(declaringClass);

        // For static methods, use the child's version
        // For instance methods, use the parent's version
        if (!isStatic) replace = !replace;

        return replace ? REPLACE : 0;
    }

    public String toString() {
        String s = ""+declaringClass+", "+isStatic+", "+flags+", "+data+"\n";
        s = s+"\t(";
        for(int j=0; j<args.length; j++) {
            s += args[j].getName()+", ";
        }
        s += ")";
        return s;
    }
}
