package org.python.core;

import org.python.antlr.base.mod;
import org.python.compiler.LegacyCompiler;

/**
 * Facade for different compiler implementations.
 * 
 * The static methods of this class act as a Facade for the compiler subsystem.
 * This is so that the rest of Jython (even generated code) can statically link
 * to the static interface of this class, while allowing for different
 * implementations of the various components of the compiler subsystem.
 * 
 * @author Tobias Ivarsson
 */
public class CompilerFacade {
    
    private static volatile PythonCompiler compiler = loadDefaultCompiler();

    public static void setCompiler(PythonCompiler compiler) {
        CompilerFacade.compiler = compiler;
    }

    private static PythonCompiler loadDefaultCompiler() {
        return new LegacyCompiler();
    }

    public static PyCode compile(mod node, String name, String filename,
            boolean linenumbers, boolean printResults, CompilerFlags cflags) {
        try {
            PythonCodeBundle bundle = compiler.compile(node, name, filename,
                    linenumbers, printResults, cflags);
            bundle.saveCode(Options.proxyDebugDirectory);
            return bundle.loadCode();
        } catch (Throwable t) {
            throw ParserFacade.fixParseError(null, t, filename);
        }
    }
}
