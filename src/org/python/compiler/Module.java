// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.python.core.CodeBootstrap;
import org.python.core.CodeFlag;
import org.python.core.CodeLoader;
import org.python.core.CompilerFlags;
import org.python.core.ThreadState;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyComplex;
import org.python.core.PyException;
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
import org.objectweb.asm.Type;
import org.python.antlr.ParseException;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.Suite;
import org.python.antlr.base.mod;
import static org.python.util.CodegenUtils.*;

class PyIntegerConstant extends Constant implements ClassConstants, Opcodes {

    final int value;

    PyIntegerConstant(int value) {
        this.value = value;
    }

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyInteger.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyInteger.class), access);
        c.iconst(value);
        c.invokestatic(p(Py.class), "newInteger", sig(PyInteger.class, Integer.TYPE));
        c.putstatic(module.classfile.name, name, ci(PyInteger.class));
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyIntegerConstant) {
            return ((PyIntegerConstant) o).value == value;
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

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyFloat.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyFloat.class), access);
        c.ldc(new Double(value));
        c.invokestatic(p(Py.class), "newFloat", sig(PyFloat.class, Double.TYPE));
        c.putstatic(module.classfile.name, name, ci(PyFloat.class));
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyFloatConstant) {
            return ((PyFloatConstant) o).value == value;
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

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyComplex.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyComplex.class), access);
        c.ldc(new Double(value));
        c.invokestatic(p(Py.class), "newImaginary", sig(PyComplex.class, Double.TYPE));
        c.putstatic(module.classfile.name, name, ci(PyComplex.class));
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyComplexConstant) {
            return ((PyComplexConstant) o).value == value;
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

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyString.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyString.class), access);
        c.ldc(value);
        c.invokestatic(p(PyString.class), "fromInterned", sig(PyString.class, String.class));
        c.putstatic(module.classfile.name, name, ci(PyString.class));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyStringConstant) {
            return ((PyStringConstant) o).value.equals(value);
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

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyUnicode.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyUnicode.class), access);
        c.ldc(value);
        c.invokestatic(p(PyUnicode.class), "fromInterned", sig(PyUnicode.class, String.class));
        c.putstatic(module.classfile.name, name, ci(PyUnicode.class));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyUnicodeConstant) {
            return ((PyUnicodeConstant) o).value.equals(value);
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

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyLong.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyLong.class), access);
        c.ldc(value);
        c.invokestatic(p(Py.class), "newLong", sig(PyLong.class, String.class));
        c.putstatic(module.classfile.name, name, ci(PyLong.class));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PyLongConstant) {
            return ((PyLongConstant) o).value.equals(value);
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
            Module module)
            throws Exception {

        this.co_name = name;
        this.co_firstlineno = firstlineno;
        this.module = module;

        //Needed so that moreflags can be final.
        int _moreflags = 0;

        if (scope.ac != null) {
            arglist = scope.ac.arglist;
            keywordlist = scope.ac.keywordlist;
            argcount = scope.ac.names.size();

            //Do something to add init_code to tree
            //XXX: not sure we should be modifying scope.ac in a PyCodeConstant
            //constructor.
            if (scope.ac.init_code.size() > 0) {
                scope.ac.appendInitCode((Suite) tree);
            }
        } else {
            arglist = false;
            keywordlist = false;
            argcount = 0;
        }

        id = module.codes.size();

        //Better names in the future?
        if (isJavaIdentifier(name)) {
            fname = name + "$" + id;
        } else {
            fname = "f$" + id;
        }
        //XXX: is fname needed at all, or should we just use "name"?
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

    //XXX: this can probably go away now that we can probably just copy the list.
    private List<String> toNameAr(List<String> names, boolean nullok) {
        int sz = names.size();
        if (sz == 0 && nullok) {
            return null;
        }
        List<String> nameArray = new ArrayList<String>();
        nameArray.addAll(names);
        return nameArray;
    }

    private boolean isJavaIdentifier(String s) {
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

    void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, ci(PyCode.class));
    }

    void put(Code c) throws IOException {
        module.classfile.addField(name, ci(PyCode.class), access);
        c.iconst(argcount);

        //Make all names
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

        c.invokestatic(p(Py.class), "newCode", sig(PyCode.class, Integer.TYPE,
                String[].class, String.class, String.class, Integer.TYPE, Boolean.TYPE,
                Boolean.TYPE, PyFunctionTable.class, Integer.TYPE, String[].class,
                String[].class, Integer.TYPE, Integer.TYPE));
        c.putstatic(module.classfile.name, name, ci(PyCode.class));
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

    /** The pool of Python Constants */
    Hashtable<Constant, Constant> constants;

    public Module(String name, String filename, boolean linenumbers) {
        this(name, filename, linenumbers, org.python.core.imp.NO_MTIME);
    }

    public Module(String name, String filename, boolean linenumbers, long mtime) {
        this.linenumbers = linenumbers;
        this.mtime = mtime;
        classfile = new ClassFile(name, p(PyFunctionTable.class),
                ACC_SYNCHRONIZED | ACC_PUBLIC, mtime);
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
        //More sophisticated name mappings might be nice
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

    PyCodeConstant codeConstant(mod tree, String name, boolean fast_locals, String className,
            boolean classBody, boolean printResults, int firstlineno, ScopeInfo scope)
            throws Exception {
        return codeConstant(tree, name, fast_locals, className, classBody,
                printResults, firstlineno, scope, null);
    }

    PyCodeConstant codeConstant(mod tree, String name, boolean fast_locals, String className,
            boolean classBody, boolean printResults, int firstlineno, ScopeInfo scope,
            CompilerFlags cflags)
            throws Exception {
        PyCodeConstant code = new PyCodeConstant(tree, name, fast_locals,
                className, classBody, printResults, firstlineno, scope, cflags,
                this);
        codes.add(code);

        CodeCompiler compiler = new CodeCompiler(this, printResults);

        Code c = classfile.addMethod(
                code.fname,
                sig(PyObject.class, PyFrame.class, ThreadState.class),
                ACC_PUBLIC);

        compiler.parse(tree, c, fast_locals, className, classBody, scope, cflags);
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
        Code c = classfile.addMethod("main", sig(Void.TYPE, String[].class),
                ACC_PUBLIC | ACC_STATIC);
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
        Code c = classfile.addMethod(CodeLoader.GET_BOOTSTRAP_METHOD_NAME, sig(CodeBootstrap.class),
                ACC_PUBLIC | ACC_STATIC);
        c.ldc(Type.getType("L" + classfile.name + ";"));
        c.invokestatic(p(PyRunnableBootstrap.class), PyRunnableBootstrap.REFLECTION_METHOD_NAME,
                sig(CodeBootstrap.class, Class.class));
        c.areturn();
    }

    void addConstants(Code c) throws IOException {
        classfile.addField("self", "L" + classfile.name + ";", ACC_STATIC | ACC_FINAL);
        c.aload(0);
        c.putstatic(classfile.name, "self", "L" + classfile.name + ";");
        Enumeration e = constants.elements();

        while (e.hasMoreElements()) {
            Constant constant = (Constant) e.nextElement();
            constant.put(c);
        }

        for (int i = 0; i < codes.size(); i++) {
            PyCodeConstant pyc = codes.get(i);
            pyc.put(c);
        }

        c.return_();
    }

    public void addFunctions() throws IOException {
        Code code = classfile.addMethod("call_function",
                sig(PyObject.class, Integer.TYPE, PyFrame.class, ThreadState.class), ACC_PUBLIC);

        code.aload(0); // this
        code.aload(2); // frame
        code.aload(3); // thread state
        Label def = new Label();
        Label[] labels = new Label[codes.size()];
        int i;
        for (i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }

        //Get index for function to call
        code.iload(1);
        code.tableswitch(0, labels.length - 1, def, labels);
        for (i = 0; i < labels.length; i++) {
            code.label(labels[i]);
            code.invokevirtual(classfile.name, (codes.get(i)).fname, sig(PyObject.class,
                    PyFrame.class, ThreadState.class));
            code.areturn();
        }
        code.label(def);

        //Should probably throw internal exception here
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
    public Future getFutures() {
        return futures;
    }

    public String getFilename() {
        return sfilename;
    }

    public ScopeInfo getScopeInfo(PythonTree node) {
        return scopes.get(node);
    }

    public void error(String msg, boolean err, PythonTree node)
            throws Exception {
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
            boolean linenumbers, boolean printResults, CompilerFlags cflags)
            throws Exception {
        compile(node, ostream, name, filename, linenumbers, printResults, cflags,
                org.python.core.imp.NO_MTIME);
    }

    public static void compile(mod node, OutputStream ostream, String name, String filename,
            boolean linenumbers, boolean printResults, CompilerFlags cflags, long mtime)
            throws Exception {
        Module module = new Module(name, filename, linenumbers, mtime);
        if (cflags == null) {
            cflags = new CompilerFlags();
        }
        module.futures.preprocessFutures(node, cflags);
        new ScopesCompiler(module, module.scopes).parse(node);

        //Add __doc__ if it exists

        Constant main = module.codeConstant(node, "<module>", false, null, false,
                printResults, 0,
                module.getScopeInfo(node),
                cflags);
        module.mainCode = main;
        module.write(ostream);
    }
}
