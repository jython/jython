// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the interactive {@link Repl}, which compiles with a CPython
 * subprocess ({@link ReplCompiler}). The Gradle build tells us which
 * CPython to use through the system property
 * {@value ReplCompiler#CPYTHON_PROPERTY}.
 */
@DisplayName("The REPL ...")
class ReplTest extends UnitTestSupport {

    /** One compiler subprocess shared by all the tests. */
    private static ReplCompiler compiler;

    @BeforeAll
    static void startCompiler() throws IOException { compiler = new ReplCompiler(); }

    @AfterAll
    static void stopCompiler() { if (compiler != null) { compiler.close(); } }

    /** What a session wrote to its output and error streams. */
    private record Session(String out, String err) {}

    /**
     * Run a REPL session on the given input lines, capturing
     * {@code System.out} (where expression values go) and the error
     * stream, which is also {@code System.err} for the session (where
     * the interpreter reports Python exceptions).
     *
     * @param lines input to the session
     * @return captured output and error text
     * @throws IOException from the REPL
     */
    private static Session session(String... lines) throws IOException {
        BufferedReader in = new BufferedReader(new StringReader(String.join("\n", lines) + "\n"));
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        PrintStream savedOut = System.out, savedErr = System.err;
        try (PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
                PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8)) {
            System.setOut(out);
            System.setErr(err);
            new Repl(in, out, err, compiler).run();
        } finally {
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
        return new Session(outBytes.toString(StandardCharsets.UTF_8),
                errBytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("echoes the value of an expression")
    void echoesExpression() throws IOException {
        Session s = session("x = 6 * 7", "x");
        assertEquals(">>> >>> 42\n>>> \n", s.out());
        assertEquals("", s.err());
    }

    @Test
    @DisplayName("does not echo None")
    void doesNotEchoNone() throws IOException {
        Session s = session("None", "x = None", "x", "'abc'.upper()");
        assertEquals(">>> >>> >>> >>> 'ABC'\n>>> \n", s.out());
        assertEquals("", s.err());
    }

    @Test
    @DisplayName("prompts for continuation lines")
    void continuesCompoundStatement() throws IOException {
        Session s = session("x = 1", "if x:", "    y = 2", "", "y");
        assertEquals(">>> >>> ... ... >>> 2\n>>> \n", s.out());
        assertEquals("", s.err());
    }

    @Test
    @DisplayName("finishes a compound statement at end of input")
    void finishesAtEof() throws IOException {
        Session s = session("if True:", "    z = 3");
        assertEquals("", s.err());
        assertTrue(s.out().startsWith(">>> ... ... "), s.out());
    }

    @Test
    @DisplayName("reports a syntax error and carries on")
    void reportsSyntaxError() throws IOException {
        Session s = session("1 +", "2");
        assertTrue(s.err().contains("SyntaxError"), s.err());
        assertTrue(s.out().contains("2\n"), s.out());
    }

    @Test
    @DisplayName("reports a Python exception and carries on")
    void reportsNameError() throws IOException {
        Session s = session("undefined_name", "5");
        assertEquals("NameError: name 'undefined_name' is not defined\n", s.err());
        assertTrue(s.out().contains("5\n"), s.out());
    }

    @Test
    @DisplayName("reports an unimplemented feature and carries on")
    void reportsMissingFeature() throws IOException {
        // True division is not yet implemented in BINARY_OP. (Use a
        // variable, or CPython will fold 1 / 2 to a constant.)
        Session s = session("n = 1", "n / 2", "6");
        assertTrue(s.err().contains("internal error"), s.err());
        assertTrue(s.out().contains("6\n"), s.out());
    }

    @Test
    @DisplayName("keeps globals between statements")
    void keepsGlobals() throws IOException {
        // (list has no __repr__ yet, so we look at the elements)
        Session s = session("a = [1, 2, 3]", "b = a[1:]", "len(b)", "b[0]", "__name__");
        assertEquals(">>> >>> >>> 2\n>>> 2\n>>> '__main__'\n>>> \n", s.out());
        assertEquals("", s.err());
    }

    @Test
    @DisplayName("provides print()")
    void printBuiltin() throws IOException {
        Session s = session("print('a', 1, sep='-')", "print()", "print(2, end='!')");
        assertEquals(">>> a-1\n>>> \n>>> 2!>>> \n", s.out());
        assertEquals("", s.err());
    }

    @Test
    @DisplayName("compiler distinguishes complete, incomplete and invalid input")
    void compilerResults() throws IOException {
        assertInstanceOf(ReplCompiler.Incomplete.class, compiler.compile("if x:"));
        assertInstanceOf(ReplCompiler.Code.class, compiler.compile("1"));
        assertInstanceOf(ReplCompiler.Error.class, compiler.compile("1 +"));
    }
}
