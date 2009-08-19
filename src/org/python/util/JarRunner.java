package org.python.util;

import org.python.core.imp;
import org.python.core.PySystemState;

/**
 * JarRunner initializes sys (PySystemState), passing args in (including the
 * name "__run__" as arg 0 for consistancy with Python expectations), and
 * import __run__.  It is intended to be used to allow an application jarred up
 * with Jython's runtime to run like "java -jar foo.jar".  It requires a
 * __run__.py in the jar that will execute the program, and the following entry
 * in the manifest:
 *
 * Main-Class: org.python.util.JarRunner
 *
 * XXX: For the moment it should be considered experimental, but it is simple
 * enough that I expect to be able to remove this warning pretty quickly.
 */
public class JarRunner {

    public static void run(String[] args) {
        final String runner = "__run__";
        String[] argv = new String[args.length + 1];
        argv[0] = runner;
        System.arraycopy(args, 0, argv, 1, args.length);
        PySystemState.initialize(PySystemState.getBaseProperties(), null, argv);
        imp.load(runner);
    }

    public static void main(String[] args) {
        run(args);
    }
}
