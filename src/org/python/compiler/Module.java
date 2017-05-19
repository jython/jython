// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import static org.python.util.CodegenUtils.ci;
import static org.python.util.CodegenUtils.p;
import static org.python.util.CodegenUtils.sig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.List;
import javax.xml.bind.DatatypeConverter;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.antlr.ParseException;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.Suite;
import org.python.antlr.base.mod;
import org.python.core.ClasspathPyImporter;
import org.python.core.CodeBootstrap;
import org.python.core.CodeFlag;
import org.python.core.CodeLoader;
import org.python.core.CompilerFlags;
import org.python.core.imp;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyBytecode;
import org.python.core.PyComplex;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyFloat;
import org.python.core.PyFrame;
import org.python.core.PyFunctionTable;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyRunnable;
import org.python.core.PyRunnableBootstrap;
import org.python.core.PyString;
import org.python.core.PyUnicode;
import org.python.core.ThreadState;
import org.python.modules._marshal;

class PyIntegerConstant extends Constant implements ClassConstants, Opcodes {

    final int value;

    PyIntegerConstant(int value) {
        this.value = value;
    }

    @Override
    void get(Code c) throws IOException {
        c.iconst(value);  // it would be nice if we knew we didn't have to box next
        c.invokestatic(p(Py.class), "newInteger", sig(PyInteger.class, Integer.TYPE));
    }

    @Override
    void put(Code c) throws IOException {}

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyIntegerConstant) {
            return ((PyIntegerConstant)o).value == value;
        } else {
            return false;
        }
    }
}


class PyFloatConstant extends Constant implements ClassConstants, Opcodes {

    final double value;

    PyFloatConstant(double value) {
        this.value = value;
    }

    @Override
    void get(Code c) throws IOException {
        c.ldc(new Double(value));
        c.invokestatic(p(Py.class), "newFloat", sig(PyFloat.class, Double.TYPE));
    }

    @Override
    void put(Code c) throws IOException {}

    @Override
    public int hashCode() {
        return (int)value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyFloatConstant) {
            // Ensure hashtable works things like for -0.0 and NaN (see java.lang.Double.equals).
            PyFloatConstant pyco = (PyFloatConstant)o;
            return Double.doubleToLongBits(pyco.value) == Double.doubleToLongBits(value);
        } else {
            return false;
        }
    }
}


class PyComplexConstant extends Constant implements ClassConstants, Opcodes {

    final double value;

    PyComplexConstant(double value) {
        this.value = value;
    }

    @Override
    void get(Code c) throws IOException {
        c.ldc(new Double(value));
        c.invokestatic(p(Py.class), "newImaginary", sig(PyComplex.class, Double.TYPE));
    }

    @Override
    void put(Code c) throws IOException {}

    @Override
    public int hashCode() {
        return (int)value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyComplexConstant) {
            // Ensure hashtable works things like for -0.0 and NaN (see java.lang.Double.equals).
            PyComplexConstant pyco = (PyComplexConstant)o;
            return Double.doubleToLongBits(pyco.value) == Double.doubleToLongBits(value);
        } else {
            return false;
        }
    }
}


class PyStringConstant extends Constant implements ClassConstants, Opcodes {

    final String value;

    PyStringConstant(String value) {
        this.value = value;
    }

    @Override
    void get(Code c) throws IOException {
        c.ldc(value);
        c.invokestatic(p(PyString.class), "fromInterned", sig(PyString.class, String.class));
    }

    @Override
    void put(Code c) throws IOException {}

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyStringConstant) {
            return ((PyStringConstant)o).value.equals(value);
        } else {
            return false;
        }
    }
}


class PyUnicodeConstant extends Constant implements ClassConstants, Opcodes {

    final String value;

    PyUnicodeConstant(String value) {
        this.value = value;
    }

    @Override
    void get(Code c) throws IOException {
        c.ldc(value);
        c.invokestatic(p(PyUnicode.class), "fromInterned", sig(PyUnicode.class, String.class));
    }

    @Override
    void put(Code c) throws IOException {}

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyUnicodeConstant) {
            return ((PyUnicodeConstant)o).value.equals(value);
        } else {
            return false;
        }
    }
}


class PyLongConstant extends Constant implements ClassConstants, Opcodes {

    final String value;

    PyLongConstant(String value) {
        this.value = value;
    }

    @Override
    void get(Code c) throws IOException {
        c.ldc(value);
        c.invokestatic(p(Py.class), "newLong", sig(PyLong.class, String.class));
    }

    @Override
    void put(Code c) throws IOException {}

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyLongConstant) {
            return ((PyLongConstant)o).value.equals(value);
        } else {
            return false;
        }
    }
}


class PyCodeConstant extends Constant implements ClassConstants, Opcodes {

    final String co_name;
    final int argcount;
    final List<String> names;
    final int id;
    final int co_firstlineno;
    final boolean arglist, keywordlist;
    final String fname;
    // for nested scopes
    final List<String> cellvars;
    final List<String> freevars;
    final int jy_npurecell;
    final int moreflags;

    PyCodeConstant(mod tree, String name, boolean fast_locals, String className, boolean classBody,
            boolean printResults, int firstlineno, ScopeInfo scope, CompilerFlags cflags,
            Module module) throws Exception {

        this.co_name = name;
        this.co_firstlineno = firstlineno;
        this.module = module;

        // Needed so that moreflags can be final.
        int _moreflags = 0;

        if (scope.ac != null) {
            arglist = scope.ac.arglist;
            keywordlist = scope.ac.keywordlist;
            argcount = scope.ac.names.size();

            // Do something to add init_code to tree
            // XXX: not sure we should be modifying scope.ac in a PyCodeConstant
            // constructor.
            if (scope.ac.init_code.size() > 0) {
                scope.ac.appendInitCode((Suite)tree);
            }
        } else {
            arglist = false;
            keywordlist = false;
            argcount = 0;
        }

        id = module.codes.size();

        // Better names in the future?
        if (isJavaIdentifier(name)) {
            fname = name + "$" + id;
        } else {
            fname = "f$" + id;
        }
        // XXX: is fname needed at all, or should we just use "name"?
        // It is needed to disambiguate functions and methods with
        // same name, but in different classes. The function-fields
        // and PyCode-fields that Jython generates don't use fully
        // qualified names. So fname is used.
        this.name = fname;

        // !classdef only
        if (!classBody) {
            names = toNameAr(scope.names, false);
        } else {
            names = null;
        }

        cellvars = toNameAr(scope.cellvars, true);
        freevars = toNameAr(scope.freevars, true);
        jy_npurecell = scope.jy_npurecell;

        if (CodeCompiler.checkOptimizeGlobals(fast_locals, scope)) {
            _moreflags |= org.python.core.CodeFlag.CO_OPTIMIZED.flag;
        }
        if (scope.generator) {
            _moreflags |= org.python.core.CodeFlag.CO_GENERATOR.flag;
        }
        if (cflags != null) {
            if (cflags.isFlagSet(CodeFlag.CO_GENERATOR_ALLOWED)) {
                _moreflags |= org.python.core.CodeFlag.CO_GENERATOR_ALLOWED.flag;
            }
            if (cflags.isFlagSet(CodeFlag.CO_FUTURE_DIVISION)) {
                _moreflags |= org.python.core.CodeFlag.CO_FUTURE_DIVISION.flag;
            }
        }
        moreflags = _moreflags;
    }

    // XXX: this can probably go away now that we can probably just copy the list.
    private List<String> toNameAr(List<String> names, boolean nullok) {
        int sz = names.size();
        if (sz == 0 && nullok) {
            return null;
        }
        List<String> nameArray = new ArrayList<String>();
        nameArray.addAll(names);
        return nameArray;
    }

    public static boolean isJavaIdentifier(String s) {
        char[] chars = s.toCharArray();
        if (chars.length == 0) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(chars[0])) {
            return false;
        }

        for (int i = 1; i < chars.length; i++) {
            if (!Character.isJavaIdentifierPart(chars[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyCode.class));
    }

    @Override
    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyCode.class), access);
        c.iconst(argcount);

        // Make all names
        int nameArray;
        if (names != null) {
            nameArray = CodeCompiler.makeStrings(c, names);
        } else { // classdef
            nameArray = CodeCompiler.makeStrings(c, null);
        }
        c.aload(nameArray);
        c.freeLocal(nameArray);
        c.aload(1);
        c.ldc(co_name);
        c.iconst(co_firstlineno);

        c.iconst(arglist ? 1 : 0);
        c.iconst(keywordlist ? 1 : 0);

        c.getstatic(module.classfile.name, "self", "L" + module.classfile.name + ";");

        c.iconst(id);

        if (cellvars != null) {
            int strArray = CodeCompiler.makeStrings(c, cellvars);
            c.aload(strArray);
            c.freeLocal(strArray);
        } else {
            c.aconst_null();
        }
        if (freevars != null) {
            int strArray = CodeCompiler.makeStrings(c, freevars);
            c.aload(strArray);
            c.freeLocal(strArray);
        } else {
            c.aconst_null();
        }

        c.iconst(jy_npurecell);

        c.iconst(moreflags);

        c.invokestatic(
                p(Py.class),
                "newCode",
                sig(PyCode.class, Integer.TYPE, String[].class, String.class, String.class,
                        Integer.TYPE, Boolean.TYPE, Boolean.TYPE, PyFunctionTable.class,
                        Integer.TYPE, String[].class, String[].class, Integer.TYPE, Integer.TYPE));
        c.putstatic(module.classfile.name, name, ci(PyCode.class));
    }
}

class PyBytecodeConstant extends Constant implements ClassConstants, Opcodes {
    PyBytecodeConstant(String name, String className, CompilerFlags cflags,
            Module module) throws Exception {
        super();
        this.module = module;
        this.name = name;
    }

    @Override
    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyCode.class));
    }

    @Override
    void put(Code c) throws IOException {
    }
}

public class Module implements Opcodes, ClassConstants, CompilationContext {

    ClassFile classfile;
    Constant filename;
    String sfilename;
    Constant mainCode;
    boolean linenumbers;
    Future futures;
    Hashtable<PythonTree, ScopeInfo> scopes;
    List<PyCodeConstant> codes;
    long mtime;
    private int setter_count = 0;
    private final static int USE_SETTERS_LIMIT = 100;
    private final static int MAX_SETTINGS_PER_SETTER = 4096;

    /** The pool of Python Constants */
    Hashtable<Constant, Constant> constants;

    /** Table of oversized methods represented as CPython bytecode. */
    protected Hashtable<String, String> oversized_methods = null;

    public Module(String name, String filename, boolean linenumbers) {
        this(name, filename, linenumbers, org.python.core.imp.NO_MTIME);
    }

    public Module(String name, String filename, boolean linenumbers, long mtime) {
        this.linenumbers = linenumbers;
        this.mtime = mtime;
        classfile =
                new ClassFile(name, p(PyFunctionTable.class), ACC_SYNCHRONIZED | ACC_PUBLIC, mtime);
        constants = new Hashtable<Constant, Constant>();
        sfilename = filename;
        if (filename != null) {
            this.filename = stringConstant(filename);
        } else {
            this.filename = null;
        }
        codes = new ArrayList<PyCodeConstant>();
        futures = new Future();
        scopes = new Hashtable<PythonTree, ScopeInfo>();
    }

    public Module(String name) {
        this(name, name + ".py", true, org.python.core.imp.NO_MTIME);
    }

    private Constant findConstant(Constant c) {
        Constant ret = constants.get(c);
        if (ret != null) {
            return ret;
        }
        ret = c;
        c.module = this;
        // More sophisticated name mappings might be nice
        c.name = "_" + constants.size();
        constants.put(ret, ret);
        return ret;
    }

    Constant integerConstant(int value) {
        return findConstant(new PyIntegerConstant(value));
    }

    Constant floatConstant(double value) {
        return findConstant(new PyFloatConstant(value));
    }

    Constant complexConstant(double value) {
        return findConstant(new PyComplexConstant(value));
    }

    Constant stringConstant(String value) {
        return findConstant(new PyStringConstant(value));
    }

    Constant unicodeConstant(String value) {
        return findConstant(new PyUnicodeConstant(value));
    }

    Constant longConstant(String value) {
        return findConstant(new PyLongConstant(value));
    }

    Constant codeConstant(mod tree, String name, boolean fast_locals, String className,
            boolean classBody, boolean printResults, int firstlineno, ScopeInfo scope,
            CompilerFlags cflags) throws Exception {
        return codeConstant(tree, name, fast_locals, className, null, classBody, printResults,
                firstlineno, scope, cflags);
    }

    Constant codeConstant(mod tree, String name, boolean fast_locals, String className,
            Str classDoc, boolean classBody, boolean printResults, int firstlineno,
            ScopeInfo scope, CompilerFlags cflags) throws Exception {
        if (oversized_methods != null && oversized_methods.containsKey(name+firstlineno)) {
            // For now this only declares the field.
            // PyBytecodeConstant is just a dummy to allow the caller to work properly.
            // It is intentionally not added to codes, because it doesn't participate in
            // FunctionTable and doesn't mess up addFunctions and addConstants this way.
            PyBytecodeConstant bcode = new PyBytecodeConstant(
                    oversized_methods.get(name+firstlineno), className, cflags, this);
            classfile.addField(bcode.name, ci(PyCode.class), ACC_PUBLIC | ACC_STATIC);
            return bcode;
        }
        PyCodeConstant code = new PyCodeConstant(tree, name, fast_locals, className, classBody,
                printResults, firstlineno, scope, cflags, this);
        codes.add(code);

        CodeCompiler compiler = new CodeCompiler(this, printResults);
        Code c = classfile.addMethod(code.fname,
                sig(PyObject.class, PyFrame.class, ThreadState.class), ACC_PUBLIC);
        compiler.parse(tree, c, fast_locals, className, classDoc, classBody, scope, cflags);
        return code;
    }

    /** This block of code writes out the various standard methods */
    public void addInit() throws IOException {
        Code c = classfile.addMethod("<init>", sig(Void.TYPE, String.class), ACC_PUBLIC);
        c.aload(0);
        c.invokespecial(p(PyFunctionTable.class), "<init>", sig(Void.TYPE));
        addConstants(c);
    }

    public void addRunnable() throws IOException {
        Code c = classfile.addMethod("getMain", sig(PyCode.class), ACC_PUBLIC);
        mainCode.get(c);
        c.areturn();
    }

    public void addMain() throws IOException {
        Code c = classfile.addMethod("main",
                sig(Void.TYPE, String[].class), ACC_PUBLIC | ACC_STATIC);
        c.new_(classfile.name);
        c.dup();
        c.ldc(classfile.name);
        c.invokespecial(classfile.name, "<init>", sig(Void.TYPE, String.class));
        c.invokevirtual(classfile.name, "getMain", sig(PyCode.class));
        c.invokestatic(p(CodeLoader.class), CodeLoader.SIMPLE_FACTORY_METHOD_NAME,
                sig(CodeBootstrap.class, PyCode.class));
        c.aload(0);
        c.invokestatic(p(Py.class), "runMain", sig(Void.TYPE, CodeBootstrap.class, String[].class));
        c.return_();
    }

    public void addBootstrap() throws IOException {
        Code c = classfile.addMethod(CodeLoader.GET_BOOTSTRAP_METHOD_NAME,
                sig(CodeBootstrap.class), ACC_PUBLIC | ACC_STATIC);
        c.ldc(Type.getType("L" + classfile.name + ";"));
        c.invokestatic(p(PyRunnableBootstrap.class), PyRunnableBootstrap.REFLECTION_METHOD_NAME,
                sig(CodeBootstrap.class, Class.class));
        c.areturn();
    }

    void addConstants(Code c) throws IOException {
        classfile.addField("self", "L" + classfile.name + ";", ACC_STATIC);
        c.aload(0);
        c.putstatic(classfile.name, "self", "L" + classfile.name + ";");
        Enumeration<Constant> e = constants.elements();

        while (e.hasMoreElements()) {
            Constant constant = e.nextElement();
            constant.put(c);
        }

        for (PyCodeConstant pyc: codes) {
            pyc.put(c);
        }

        c.return_();
    }

    public void addFunctions() throws IOException {
        Code code = classfile.addMethod("call_function",
                sig(PyObject.class, Integer.TYPE, PyFrame.class, ThreadState.class), ACC_PUBLIC);

        if (!codes.isEmpty()) {
            code.aload(0); // this
            code.aload(2); // frame
            code.aload(3); // thread state
            Label def = new Label();
            Label[] labels = new Label[codes.size()];
            int i;
            for (i = 0; i < labels.length; i++) {
                labels[i] = new Label();
            }
    
            // Get index for function to call
            code.iload(1);
            code.tableswitch(0, labels.length - 1, def, labels);
            for (i = 0; i < labels.length; i++) {
                code.label(labels[i]);
                code.invokevirtual(classfile.name, (codes.get(i)).fname,
                        sig(PyObject.class, PyFrame.class, ThreadState.class));
                code.areturn();
            }
            code.label(def);
        }

        // Should probably throw internal exception here
        code.aconst_null();
        code.areturn();
    }

    public void write(OutputStream stream) throws IOException {
        addInit();
        addRunnable();
        addMain();
        addBootstrap();

        addFunctions();

        classfile.addInterface(p(PyRunnable.class));
        if (sfilename != null) {
            classfile.setSource(sfilename);
        }
        classfile.write(stream);
    }

    // Implementation of CompilationContext
    @Override
    public Future getFutures() {
        return futures;
    }

    @Override
    public String getFilename() {
        return sfilename;
    }

    @Override
    public ScopeInfo getScopeInfo(PythonTree node) {
        return scopes.get(node);
    }

    @Override
    public void error(String msg, boolean err, PythonTree node) throws Exception {
        if (!err) {
            try {
                Py.warning(Py.SyntaxWarning, msg, (sfilename != null) ? sfilename : "?",
                        node.getLine(), null, Py.None);
                return;
            } catch (PyException e) {
                if (!e.match(Py.SyntaxWarning)) {
                    throw e;
                }
            }
        }
        throw new ParseException(msg, node);
    }

    public static void compile(mod node, OutputStream ostream, String name, String filename,
            boolean linenumbers, boolean printResults, CompilerFlags cflags) throws Exception {
        compile(node, ostream, name, filename, linenumbers, printResults, cflags,
                org.python.core.imp.NO_MTIME);
    }

    protected static void _module_init(mod node, Module module, boolean printResults,
            CompilerFlags cflags) throws Exception {
        if (cflags == null) {
            cflags = new CompilerFlags();
        }
        module.futures.preprocessFutures(node, cflags);
        new ScopesCompiler(module, module.scopes).parse(node);

        // Add __doc__ if it exists

        Constant main = module.codeConstant(node, "<module>", false, null, false,
                printResults, 0, module.getScopeInfo(node), cflags);
        module.mainCode = main;
    }

    private static PyBytecode loadPyBytecode(String filename, boolean try_cpython)
            throws RuntimeException
    {
        if (filename.startsWith(ClasspathPyImporter.PYCLASSPATH_PREFIX)) {
            ClassLoader cld = Py.getSystemState().getClassLoader();
            if (cld == null) {
                cld = imp.getParentClassLoader();
            }
            URL py_url = cld.getResource(filename.replace(
                    ClasspathPyImporter.PYCLASSPATH_PREFIX, ""));
            if (py_url != null) {
                filename = py_url.getPath();
            } else {
                // Should never happen, but let's play it safe and treat this case.
                throw new RuntimeException(
                        "\nEncountered too large method code in \n"+filename+",\n"+
                        "but couldn't resolve that filename within classpath.\n"+
                        "Make sure the source file is at a proper location.");
            }
        }
        String cpython_cmd_msg =
                "\n\nAlternatively provide proper CPython 2.7 execute command via"+
                "\ncpython_cmd property, e.g. call "+
                "\n    jython -J-Dcpython_cmd=python"+
                "\nor if running pip on Jython:"+
                "\n    pip install --global-option=\"-J-Dcpython_cmd=python\" <package>";
        String large_method_msg = "\nEncountered too large method code in \n"+filename+"\n";
        String please_provide_msg =
                "\nPlease provide a CPython 2.7 bytecode file (.pyc) to proceed, e.g. run"+
                "\npython -m py_compile "+filename+"\nand try again.";

        String pyc_filename = filename+"c";
        File pyc_file = new File(pyc_filename);
        if (pyc_file.exists()) {
            PyFile f = new PyFile(pyc_filename, "rb", 4096);
            byte[] bts = f.read(8).toBytes();
            int magic = (bts[1]<< 8) & 0x0000FF00 |
                        (bts[0]<< 0) & 0x000000FF;
//            int mtime_pyc = (bts[7]<<24) & 0xFF000000 |
//                            (bts[6]<<16) & 0x00FF0000 |
//                            (bts[5]<< 8) & 0x0000FF00 |
//                            (bts[4]<< 0) & 0x000000FF;
            if (magic != 62211) { // check Python 2.7 bytecode
                throw new RuntimeException(large_method_msg+
                        "\n"+pyc_filename+
                        "\ncontains wrong bytecode version, not CPython 2.7 bytecode."+
                        please_provide_msg);
            }
            _marshal.Unmarshaller un = new _marshal.Unmarshaller(f);
            PyObject code = un.load();
            f.close();
            if (code instanceof PyBytecode) {
                return (PyBytecode) code;
            }
            throw new RuntimeException(large_method_msg+
                    "\n"+pyc_filename+
                    "\ncontains invalid bytecode."+
                    please_provide_msg);
        } else {
            String CPython_command = System.getProperty("cpython_cmd");
            if (try_cpython && CPython_command != null) {
                // check version...
                String command_ver = CPython_command+" --version";
                String command = CPython_command+" -m py_compile "+filename;
                String tried_create_pyc_msg = "\nTried to create pyc-file by executing\n"+
                        command+"\nThis failed because of\n";
                Exception exc = null;
                int result = 0;
                try {
                    Process p = Runtime.getRuntime().exec(command_ver);
                    // Python 2.7 writes version to error-stream for some reason:
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    String cp_version = br.readLine();
                    while (br.readLine() != null) {}
                    br.close();
                    if (cp_version == null) {
                        // Also try input-stream as fallback, just in case...
                        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        cp_version = br.readLine();
                        while (br.readLine() != null) {}
                        br.close();
                    }
                    result = p.waitFor();
                    if (!cp_version.startsWith("Python 2.7.")) {
                        throw new RuntimeException(large_method_msg+
                                tried_create_pyc_msg+
                                "wrong Python version: "+cp_version+"."+
                                "\nRequired is Python 2.7.x.\n"+
                                please_provide_msg+cpython_cmd_msg);
                    }
                }
                catch (InterruptedException ie) {
                    exc = ie;
                }
                catch (IOException ioe) {
                    exc = ioe;
                }
                if (exc == null && result == 0) {
                    try {
                        Process p = Runtime.getRuntime().exec(command);
                        result = p.waitFor();
                        if (result == 0) {
                            return loadPyBytecode(filename, false);
                        }
                    }
                    catch (InterruptedException ie) {
                        exc = ie;
                    }
                    catch (IOException ioe) {
                        exc = ioe;
                    }
                }
                String exc_msg = large_method_msg+
                        tried_create_pyc_msg+
                        (exc != null ? exc.toString() : "bad return: "+result)+".\n"+
                        please_provide_msg+cpython_cmd_msg;
                throw exc != null ? new RuntimeException(exc_msg, exc) : new RuntimeException(exc_msg);
            } else {
                throw new RuntimeException(large_method_msg+
                        please_provide_msg+cpython_cmd_msg);
            }
        }
    }
    
    private static String serializePyBytecode(PyBytecode btcode) throws java.io.IOException {
        // For some reason we cannot do this using _marshal:
        /*
        cStringIO.StringIO buf = cStringIO.StringIO();
        _marshal.Marshaller marsh = new _marshal.Marshaller(buf);
        marsh.dump(largest_m_code);
        String code_str = buf.getvalue().asString();

        _marshal.Unmarshaller un2 = new _marshal.Unmarshaller(cStringIO.StringIO(code_str));
        PyBytecode code = (PyBytecode) un2.load();

         This says 'ValueError: bad marshal data'
         Maybe the issue is actually with cStringIO, because bytecode-marshalling uses
         bytes not directly suitable as String-values. cStringIO does not use Base64 or
         something, but rather supports only string-compatible data.
        */
        // so we use Java-reflection...

        // serialize the object
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(bo);
        so.writeObject(btcode);
        so.flush();
        String code_str = DatatypeConverter.printBase64Binary(bo.toByteArray());
        so.close();
        bo.close();
        return code_str;
    }

    private static final int maxLiteral = 65535;
    
    /**
     * This method stores Python-Bytecode in String literals.
     * While Java supports rather long strings, constrained only by
     * int-addressing of arrays, it supports only up to 65535 characters
     * in literals (not sure how escape-sequences are counted).
     * To circumvent this limitation, the code is automatically splitted
     * into several literals with the following naming-scheme.
     *
     * - The marker-interface 'ContainsPyBytecode' indicates that a class
     *   contains (static final) literals of the following scheme:
     * - a prefix of '___' indicates a bytecode-containing string literal
     * - a number indicating the number of parts follows
     * - '0_' indicates that no splitting occurred
     * - otherwise another number follows, naming the index of the literal
     * - indexing starts at 0
     *
     * Examples:
     * ___0_method1   contains bytecode for method1
     * ___2_0_method2 contains first part of method2's bytecode
     * ___2_1_method2 contains second part of method2's bytecode
     *
     * Note that this approach is provisional. In future, Jython might contain
     * the bytecode directly as bytecode-objects. The current approach was
     * feasible with far less complicated JVM bytecode-manipulation, but needs
     * special treatment after class-loading.
     */
    private static void insert_code_str_to_classfile(String name, String code_str, Module module)
            throws java.io.IOException {
        // We might need to split the code into several literals.
        if (code_str.length() > maxLiteral) {
            int splits = code_str.length()/maxLiteral;
            if (code_str.length()%maxLiteral > 0) {
                ++splits;
            }
            int pos = 0, i = 0;
            for (; pos+maxLiteral <= code_str.length(); ++i) {
                module.classfile.addFinalStringLiteral(
                        "___"+splits+"_"+i+"_"+name,
                        code_str.substring(pos, pos+maxLiteral));
                pos += maxLiteral;
            }
            if (i < splits) {
                module.classfile.addFinalStringLiteral(
                        "___"+splits+"_"+i+"_"+name,
                        code_str.substring(pos));
            }
        } else {
            module.classfile.addFinalStringLiteral("___0_"+name, code_str);
        }
    }

    public static void compile(mod node, OutputStream ostream, String name, String filename,
            boolean linenumbers, boolean printResults, CompilerFlags cflags, long mtime)
            throws Exception {
        try {
            Module module = new Module(name, filename, linenumbers, mtime);
            _module_init(node, module, printResults, cflags);
            module.write(ostream);
        } catch (RuntimeException re) {
            if (re.getMessage() != null && re.getMessage().equals("Method code too large!")) {
                PyBytecode btcode = loadPyBytecode(filename, true);
                int thresh = 22000;
                // No idea, how to determine at this point if a method is oversized, so we just try
                // a threshold regarding Python code-length, while JVM restriction is actually about
                // Java bytecode length. Anyway; given that code-lengths are strongly related, this
                // should work well enough.
                
                while (true) { // Always enjoy to write a line like this :)
                    try {
                        List<PyBytecode> largest_m_codes = new ArrayList<>();
                        Stack<PyBytecode> buffer = new Stack<>();
                        //HashSet<PyBytecode> allCodes = new HashSet<>();
                        buffer.push(btcode);
                        //allCodes.add(btcode);
                        while (!buffer.isEmpty()) {
                            // Probably this cannot yield cycles, so cycle-proof stuff
                            // is out-commented for now. (everything regarding 'allCodes')
                            PyBytecode bcode = buffer.pop();
                            if (bcode.co_code.length > thresh) {
                                largest_m_codes.add(bcode);
                            } else {
                                // If a function needs to be represented as CPython bytecode, we create
                                // all inner PyCode-items (classes, functions, methods) also as CPython
                                // bytecode implicitly, so no need to look at them individually.
                                // Maybe we can later optimize this such that inner methods can be
                                // JVM-bytecode as well (if not oversized themselves).
                                for (PyObject item: bcode.co_consts) {
                                    if (item instanceof PyBytecode /*&& !allCodes.contains(item)*/) {
                                        PyBytecode mpbc = (PyBytecode) item;
                                        buffer.push(mpbc);
                                        //allCodes.add(mpbc);
                                    }
                                }
                            }
                        }
                        Module module = new Module(name, filename, linenumbers, mtime);
                        module.oversized_methods = new Hashtable<>(largest_m_codes.size());
                        int ov_id = 0;
                        String name_id;
                        for (PyBytecode largest_m_code: largest_m_codes) {
                            if (!PyCodeConstant.isJavaIdentifier(largest_m_code.co_name)) {
                                name_id = "f$_"+ov_id++;
                            } else {
                                name_id = largest_m_code.co_name+"$_"+ov_id++;
                            }
                            if (largest_m_code.co_name.equals("<module>")) {
                                // In Jython's opinion module begins at line 0
                                // (while CPython reports line 1)
                                module.oversized_methods.put(
                                        largest_m_code.co_name+0, name_id);
                            } else {
                                module.oversized_methods.put(
                                        largest_m_code.co_name+largest_m_code.co_firstlineno, name_id);
                            }
                            String code_str = serializePyBytecode(largest_m_code);
                            insert_code_str_to_classfile(name_id, code_str, module);
                        }
                        module.classfile.addInterface(p(org.python.core.ContainsPyBytecode.class));
                        _module_init(node, module, printResults, cflags);
                        module.write(ostream);
                        break;
                    } catch (RuntimeException e) {
                        if (re.getMessage() == null || !e.getMessage().equals("Method code too large!")) {
                            throw e;
                        } else {
                            thresh -= 100;
                        }
                    }
                    if (thresh == 10000) { /* This value should be well feasible by JVM-bytecode,
                                              so something else must be wrong. */
                        throw new RuntimeException(
                                "For unknown reason, too large method code couldn't be resolved"+
                                "\nby PyBytecode-approach:\n"+filename);
                    }
                }
            } else {
                throw re;
            }
        }
    }

    public void emitNum(Num node, Code code) throws Exception {
        if (node.getInternalN() instanceof PyInteger) {
            integerConstant(((PyInteger)node.getInternalN()).getValue()).get(code);
        } else if (node.getInternalN() instanceof PyLong) {
            longConstant(((PyObject)node.getInternalN()).__str__().toString()).get(code);
        } else if (node.getInternalN() instanceof PyFloat) {
            floatConstant(((PyFloat)node.getInternalN()).getValue()).get(code);
        } else if (node.getInternalN() instanceof PyComplex) {
            complexConstant(((PyComplex)node.getInternalN()).imag).get(code);
        }
    }

    public void emitStr(Str node, Code code) throws Exception {
        PyString s = (PyString)node.getInternalS();
        if (s instanceof PyUnicode) {
            unicodeConstant(s.asString()).get(code);
        } else {
            stringConstant(s.asString()).get(code);
        }
    }

    public boolean emitPrimitiveArraySetters(java.util.List<? extends PythonTree> nodes, Code code)
            throws Exception {
        final int n = nodes.size();
        if (n < USE_SETTERS_LIMIT) {
            return false;  // Too small to matter, so bail
        }

        // Only attempt if all nodes are either Num or Str, otherwise bail
        boolean primitive_literals = true;
        for (int i = 0; i < n; i++) {
            PythonTree node = nodes.get(i);
            if (!(node instanceof Num || node instanceof Str)) {
                primitive_literals = false;
            }
        }
        if (!primitive_literals) {
            return false;
        }

        final int num_setters = (n / MAX_SETTINGS_PER_SETTER) + 1;
        code.iconst(n);
        code.anewarray(p(PyObject.class));
        for (int i = 0; i < num_setters; i++) {
            Code setter = this.classfile.addMethod("set$$" + setter_count,
                    sig(Void.TYPE, PyObject[].class), ACC_STATIC | ACC_PRIVATE);

            for (int j = 0; (j < MAX_SETTINGS_PER_SETTER)
                    && ((i * MAX_SETTINGS_PER_SETTER + j) < n); j++) {
                setter.aload(0);
                setter.iconst(i * MAX_SETTINGS_PER_SETTER + j);
                PythonTree node = nodes.get(i * MAX_SETTINGS_PER_SETTER + j);
                if (node instanceof Num) {
                    emitNum((Num)node, setter);
                } else if (node instanceof Str) {
                    emitStr((Str)node, setter);
                }
                setter.aastore();
            }
            setter.return_();
            code.dup();
            code.invokestatic(this.classfile.name, "set$$" + setter_count,
                    sig(Void.TYPE, PyObject[].class));
            setter_count++;
        }
        return true;
    }

}
