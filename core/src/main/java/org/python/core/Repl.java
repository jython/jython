// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An interactive read-eval-print loop for the Jython 3 runtime.
 * <p>
 * Each statement is compiled by CPython (see {@link ReplCompiler})
 * and executed by {@link CPython311Frame} in a {@code globals}
 * dictionary that persists for the session. The value of an expression
 * statement is printed by the byte code itself (via
 * {@code PRINT_EXPR}) to {@code System.out}.
 * <p>
 * Run it from the build with {@code ./gradlew -q --console=plain
 * core:repl}.
 */
public class Repl {

    /** Prompt for a new statement. */
    static final String PS1 = ">>> ";
    /** Prompt for a continuation line. */
    static final String PS2 = "... ";

    private final BufferedReader in;
    private final PrintStream out;
    private final PrintStream err;
    private final ReplCompiler compiler;

    private final Interpreter interp = new Interpreter();
    private final PyDict globals = new PyDict();

    /** Lines of the statement being entered. */
    private final List<String> buffer = new ArrayList<>();

    /**
     * Create a REPL on the given streams. The results of expression
     * statements go to {@code System.out}, so {@code out} should
     * normally be the same stream.
     *
     * @param in source of input lines
     * @param out destination of prompts
     * @param err destination of error reports
     * @param compiler to compile input
     */
    Repl(BufferedReader in, PrintStream out, PrintStream err, ReplCompiler compiler) {
        this.in = in;
        this.out = out;
        this.err = err;
        this.compiler = compiler;
        globals.put("__name__", "__main__");
    }

    /**
     * Run the loop until end of input.
     *
     * @throws IOException if reading input or talking to CPython fails
     */
    void run() throws IOException {
        String prompt = PS1;
        while (true) {
            out.print(prompt);
            out.flush();
            String line = in.readLine();
            if (line == null) {
                // End of input: finish any statement in progress.
                if (!buffer.isEmpty()) { push(""); }
                out.println();
                return;
            }
            prompt = push(line) ? PS2 : PS1;
        }
    }

    /**
     * Add a line to the statement being entered and, if it is then
     * complete, execute it (or report the error).
     *
     * @param line to add
     * @return {@code true} if more input is needed
     * @throws IOException if talking to CPython fails
     */
    // Compare CPython code.InteractiveConsole.push
    boolean push(String line) throws IOException {
        buffer.add(line);
        ReplCompiler.Result result = compiler.compile(String.join("\n", buffer));
        if (result instanceof ReplCompiler.Incomplete) { return true; }
        buffer.clear();
        if (result instanceof ReplCompiler.Code c) {
            execute(c.code());
        } else if (result instanceof ReplCompiler.Error e) {
            err.println(e.message());
        }
        return false;
    }

    /**
     * Execute compiled code in the session {@code globals}, reporting
     * (but surviving) any exception.
     *
     * @param code to execute
     */
    private void execute(CPython311Code code) {
        try {
            interp.eval(code, globals);
        } catch (PyException pye) {
            /*
             * Nothing to print: CPython311Frame already reports the
             * exception to System.err before re-throwing it. When the
             * frame stops doing that (e.g. once it handles exceptions
             * within Python), put back err.println(pye) here: no
             * traceback yet, just "TypeName: message".
             */
        } catch (RuntimeException | StackOverflowError e) {
            // Includes InterpreterError (and MissingFeature)
            err.println("internal error: " + e);
        } finally {
            out.flush();
            err.flush();
        }
    }

    /**
     * Run an interactive session on the standard streams.
     *
     * @param args ignored
     * @throws IOException if reading input or talking to CPython fails
     */
    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try (ReplCompiler compiler = new ReplCompiler()) {
            System.out.printf("Jython 3 (prototype) using CPython compiler %s%n",
                    compiler.executable);
            System.out.println("Ctrl-D to exit.");
            new Repl(in, System.out, System.err, compiler).run();
        }
    }
}
