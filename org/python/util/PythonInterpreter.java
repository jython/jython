package org.python.util;
import org.python.core.*;

/**
 *
 * The PythonInterpreter class is a standard wrapper for a JPython interpreter
 * for use embedding in a Java application.
 *
 * @author  Jim Hugunin
 * @version 1.0, 02/23/97
 */

public class PythonInterpreter {
    PyModule module;
    PySystemState systemState;
    PyObject locals;

    // Initialize from a possibly precompiled Python module
   /**
     * Create a new Interpreter with an empty dictionary
     */
    public PythonInterpreter() {
        this(null, null);
    }

    /**
     * Create a new interpreter with the given dictionary to use as its namespace
     *
     * @param dict	the dictionary to use
     */
    // Optional dictionary willl be used for locals namespace
    public PythonInterpreter(PyObject dict) {
        this(dict, null);
    }
    
    public PythonInterpreter(PyObject dict, PySystemState systemState) {
        Py.initPython();
        if (dict == null) dict = new PyStringMap();
        if (systemState == null) systemState = new PySystemState();
        module = new PyModule("main", dict);
        this.systemState = systemState;
        locals = module.__dict__;
        setState();
    }
    
    private void setState() {
        Py.setSystemState(systemState);
    }
        
    
    /**
     * Set the Python object to use for the standard output stream
     *
     * @param outStream Python file-like object to use as output stream
     */
    public void setOut(PyObject outStream) {
        systemState.stdout = outStream;
    }
    
    /**
     * Set a java.io.Writer to use for the standard output stream
     *
     * @param outStream Writer to use as output stream
     */    
    public void setOut(java.io.Writer outStream) {
        setOut(new PyFile(outStream));
    }

    /**
     * Set a java.io.OutputStream to use for the standard output stream
     *
     * @param outStream OutputStream to use as output stream
     */        
    public void setOut(java.io.OutputStream outStream) {
        setOut(new PyFile(outStream));
    }
    
    public void setErr(PyObject outStream) {
        systemState.stderr = outStream;
    }
    
    public void setErr(java.io.Writer outStream) {
        setErr(new PyFile(outStream));
    }
    
    public void setErr(java.io.OutputStream outStream) {
        setErr(new PyFile(outStream));
    }
    
    public boolean runsource(String source) {
        return runsource(source, "<input>", "single");
    }
    
    public boolean runsource(String source, String filename) {
        return runsource(source, filename, "single");
    }
    /**Compile and run some source in the interpreter.

        Arguments are as for compile_command().

        One several things can happen:

        1) The input is incorrect; compile_command() raised an
        exception (SyntaxError or OverflowError).  A syntax traceback
        will be printed by calling the showsyntaxerror() method.

        2) The input is incomplete, and more input is required;
        compile_command() returned None.  Nothing happens.

        3) The input is complete; compile_command() returned a code
        object.  The code is executed by calling self.runcode() (which
        also handles run-time exceptions, except for SystemExit).

        The return value is 1 in case 2, 0 in the other cases (unless
        an exception is raised).  The return value can be used to
        decide whether to use sys.ps1 or sys.ps2 to prompt the next
        line.**/
    public boolean runsource(String source, String filename, String symbol) {
        PyObject code;
        try {
            code = org.python.modules.codeop.compile_command(source, filename, symbol);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SyntaxError)) {
                // Case 1
                showexception(exc);
                return false;
            } else {
                throw exc;
            }
        }
        
        // Case 2
        if (code == Py.None) return true;
        
        // Case 3
        runcode(code);
        return false;
    }

    /**
    execute a code object.

        When an exception occurs, self.showtraceback() is called to
        display a traceback.  All exceptions are caught except
        SystemExit, which is reraised.

        A note about KeyboardInterrupt: this exception may occur
        elsewhere in this code, and may not always be caught.  The
        caller should be prepared to deal with it.
    **/
    public void runcode(PyObject code) {
        try {
            exec(code);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.SystemExit)) throw exc;
            showexception(exc);
        }
    }

    public void showexception(PyException exc) {
        // Should probably add code to handle skipping top stack frames somehow...
        write(exc.toString());
    }

    public void write(String data) {
        Py.stderr.write(data);
    }
    
    public StringBuffer buffer = new StringBuffer();
    public String filename="<stdin>";
    
    public void resetbuffer() {
        buffer.setLength(0);
    }
    
    /**Closely emulate the interactive Python console.

        The optional banner argument specify the banner to print
        before the first interaction; by default it prints a banner
        similar to the one printed by the real Python interpreter,
        followed by the current class name in parentheses (so as not
        to confuse this with the real interpreter -- since it's so
        close!).**/
    public void interact() {
        interact(null);
    }

    public static String getDefaultBanner() {
	return "JPython "+PySystemState.version+" on "+
	    PySystemState.platform+"\n"+PySystemState.copyright;
    }
    
    public void interact(String banner) {
        if (banner == null) {
            banner = getDefaultBanner();
        }
        write(banner);
        write("\n");
	
	// Dummy exec in order to speed up response on first command
	exec("2");
                
        boolean more = false;
        while (true) {
            PyObject prompt = more ? systemState.ps2 : systemState.ps1;
            String line;
            try {
                line = raw_input(prompt);
            } catch (PyException exc) {
                if (!Py.matchException(exc, Py.EOFError)) throw exc;
                write("\n");
                break;
            }
            more = push(line);
        }
    }

    /**Push a line to the interpreter.

        The line should not have a trailing newline; it may have
        internal newlines.  The line is appended to a buffer and the
        interpreter's runsource() method is called with the
        concatenated contents of the buffer as source.  If this
        indicates that the command was executed or invalid, the buffer
        is reset; otherwise, the command is incomplete, and the buffer
        is left as it was after the line was appended.  The return
        value is 1 if more input is required, 0 if the line was dealt
        with in some way (this is the same as runsource()).
    **/

    public boolean push(String line) {
        if (buffer.length() > 0) buffer.append("\n");
        buffer.append(line);
        boolean more = runsource(buffer.toString(), filename);
        if (!more) resetbuffer();
        return more;
    }
        
    /**Write a prompt and read a line.

        The returned line does not include the trailing newline.
        When the user enters the EOF key sequence, EOFError is raised.

        The base implementation uses the built-in function
        raw_input(); a subclass may replace this with a different
        implementation.
    **/ 
    public String raw_input(PyObject prompt) {
        return __builtin__.raw_input(prompt);
    }
    
    /** Pause the current code, sneak an exception raiser into sys.trace_func,
    and then continue the code hoping that JPython will get control to do the break;
    **/
    public void interrupt(ThreadState ts) throws InterruptedException {
        TraceFunction breaker = new BreakTraceFunction();
        TraceFunction oldTrace = ts.systemState.tracefunc;
        ts.systemState.tracefunc = breaker;
        if (ts.frame != null) 
            ts.frame.tracefunc = breaker;
        ts.systemState.tracefunc = oldTrace;
        //ts.thread.join();
    }
    
    /**
     * Evaluate a string as Python source and return the result
     *
     * @param s	the string to evaluate
     */
    public PyObject eval(String s) {
        setState();
        return __builtin__.eval(new PyString(s), locals);
    }

    /**
     * Execute a string of Python source in the local namespace
     *
     * @param s	the string to execute
     */
    public void exec(String s) {
        setState();
        Py.exec(new PyString(s), locals, locals);
    }
    
    /**
     * Execute a Python code object in the local namespace
     *
     * @param code the code object to execute
     */
    public void exec(PyObject code) {
        setState();
        Py.exec(code, locals, locals);
    }

    /**
     * Execute a file of Python source in the local namespace
     *
     * @param s	the name of the file to execute
     */
    public void execfile(String s) {
        setState();
        __builtin__.execfile(s, locals);
    }

    //public void execfile(java.io.InputStream s) throws PyException {
    //}

    // Getting and setting the locals dictionary
    public PyObject getLocals() {
        return locals;
    }

    public void setLocals(PyObject d) {
        locals = d;
    }

    /**
     * Set a variable in the local namespace
     *
     * @param name	the name of the variable
     * @param value the value to set the variable to.  
        Will be automatically converted to an appropriate Python object.
     */
    public void set(String name, Object value) {
        locals.__setitem__(name.intern(), Py.java2py(value));
    }

    /**
     * Set a variable in the local namespace
     *
     * @param name	the name of the variable
     * @param value the value to set the variable to
     */
    public void set(String name, PyObject value) {
        locals.__setitem__(name.intern(), value);
    }


    /**
     * Get the value of a variable in the local namespace
     *
     * @param name	the name of the variable
     */
    public PyObject get(String name) {
        return locals.__finditem__(name.intern());
    }
    
    /**
     * Get the value of a variable in the local namespace
     * Value will be returned as an instance of the given Java class.
     * <code>interp.get("foo", Object.class)</code> will return the most appropriate
     * generic Java object.
     *
     * @param name	the name of the variable
     * @param javaclass the class of object to return
     */ 
    public Object get(String name, Class javaclass) {
        return Py.tojava(locals.__finditem__(name.intern()), javaclass);
    }
}

class BreakTraceFunction extends TraceFunction {
    private void doBreak() {
        throw new Error("Python interrupt");
        //Thread.currentThread().interrupt();
    }
    
    public TraceFunction traceCall(PyFrame frame) { doBreak(); return null; }
    public TraceFunction traceReturn(PyFrame frame, PyObject ret) { doBreak(); return null;}
    public TraceFunction traceLine(PyFrame frame, int line) { doBreak(); return null;}
    public TraceFunction traceException(PyFrame frame, PyException exc) { doBreak(); return null;}
}
