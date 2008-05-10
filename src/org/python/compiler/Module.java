// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.parser.ParseException;
import org.python.parser.SimpleNode;
import org.python.parser.ast.Suite;
import org.python.parser.ast.modType;

class PyIntegerConstant extends Constant implements ClassConstants, Opcodes
{
    int value;

    public PyIntegerConstant(int value) {
        this.value = value;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyInteger);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyInteger, access);
        mv.iconst(value);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newInteger", "(I)" + $pyInteger);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyInteger);
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(Object o) {
        if (o instanceof PyIntegerConstant)
            return ((PyIntegerConstant)o).value == value;
        else
            return false;
    }
}

class PyFloatConstant extends Constant implements ClassConstants, Opcodes
{
    double value;

    public PyFloatConstant(double value) {
        this.value = value;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyFloat);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyFloat, access);
        mv.visitLdcInsn(new Double(value));
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newFloat", "(D)" + $pyFloat);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyFloat);
    }

    public int hashCode() {
        return (int)value;
    }

    public boolean equals(Object o) {
        if (o instanceof PyFloatConstant)
            return ((PyFloatConstant)o).value == value;
        else
            return false;
    }
}

class PyComplexConstant extends Constant implements ClassConstants, Opcodes
{
    double value;

    public PyComplexConstant(double value) {
        this.value = value;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyComplex);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyComplex, access);
        mv.visitLdcInsn(new Double(value));
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newImaginary", "(D)" + $pyComplex);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyComplex);
    }

    public int hashCode() {
        return (int)value;
    }

    public boolean equals(Object o) {
        if (o instanceof PyComplexConstant)
            return ((PyComplexConstant)o).value == value;
        else
            return false;
    }
}

class PyStringConstant extends Constant implements ClassConstants, Opcodes
{
    String value;

    public PyStringConstant(String value) {
        this.value = value;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyStr);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyStr, access);
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/PyString", "fromInterned", "(" + $str + ")" + $pyStr);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyStr);
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof PyStringConstant)
            return ((PyStringConstant)o).value.equals(value);
        else
            return false;
    }
}

class PyUnicodeConstant extends Constant implements ClassConstants, Opcodes
{
    String value;

    public PyUnicodeConstant(String value) {
        this.value = value;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyUnicode);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyUnicode, access);
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/PyUnicode", "fromInterned", "(" + $str + ")" + $pyUnicode);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyUnicode);
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof PyUnicodeConstant)
            return ((PyUnicodeConstant)o).value.equals(value);
        else
            return false;
    }
}

class PyLongConstant extends Constant implements ClassConstants, Opcodes
{
    String value;

    public PyLongConstant(String value) {
        this.value = value;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyLong);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyLong, access);
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newLong", "(" + $str + ")" + $pyLong);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyLong);
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof PyLongConstant)
            return ((PyLongConstant)o).value.equals(value);
        else return false;
    }
}

class PyCodeConstant extends Constant implements ClassConstants, Opcodes
{
    public String co_name;
    public int argcount;
    public String[] names;
    public int id;
    public int co_firstlineno;
    public boolean arglist, keywordlist;
    String fname;

    // for nested scopes
    public String[] cellvars;
    public String[] freevars;
    public int jy_npurecell;

    public int moreflags;

    public PyCodeConstant() { ;
    }

    public void get(Code mv) throws IOException {
        mv.visitFieldInsn(GETSTATIC, module.classfile.name, name, $pyCode);
    }

    public void put(Code mv) throws IOException {
        module.classfile.addField(name, $pyCode, access);
        mv.iconst(argcount);

        //Make all names
        if (names != null) {
            CodeCompiler.makeStrings(mv, names, names.length);
        } else { // classdef
             CodeCompiler.makeStrings(mv, null, 0);
        }
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(co_name);
        mv.iconst(co_firstlineno);

        mv.iconst(arglist ? 1 : 0);
        mv.iconst(keywordlist ? 1 : 0);

        mv.visitFieldInsn(GETSTATIC, module.classfile.name, "self", "L"+module.classfile.name+";");

        mv.iconst(id);

        if (cellvars != null)
            CodeCompiler.makeStrings(mv, cellvars, cellvars.length);
        else
            mv.visitInsn(ACONST_NULL);
        if (freevars != null)
            CodeCompiler.makeStrings(mv, freevars, freevars.length);
        else
            mv.visitInsn(ACONST_NULL);

        mv.iconst(jy_npurecell);

        mv.iconst(moreflags);

        mv.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "newCode", "(I" + $strArr + $str + $str + "IZZ" + $pyFuncTbl + "I" + $strArr + $strArr + "II)" + $pyCode);
        mv.visitFieldInsn(PUTSTATIC, module.classfile.name, name, $pyCode);
    }
}

public class Module implements Opcodes, ClassConstants, CompilationContext
{
    ClassFile classfile;
    Constant filename;
    String sfilename;
    public Constant mainCode;
    public boolean linenumbers;
    public boolean setFile=true;
    Future futures;
    Hashtable scopes;

    public Module(String name, String filename, boolean linenumbers) {
        this.linenumbers = linenumbers;
        classfile = new ClassFile(name, "org/python/core/PyFunctionTable",
                                  ACC_SYNCHRONIZED | ACC_PUBLIC);
        constants = new Hashtable();
        sfilename = filename;
        if (filename != null)
            this.filename = PyString(filename);
        else
            this.filename = null;
        codes = new Vector();
        futures = new Future();
        scopes = new Hashtable();
    }

    public Module(String name) {
        this(name, name+".py", true);
    }

    // This block of code handles the pool of Python Constants
    Hashtable constants;

    private Constant findConstant(Constant c) {
        Constant ret = (Constant)constants.get(c);
        if (ret != null)
            return ret;
        ret = c;
        c.module = this;
        //More sophisticated name mappings might be nice
        c.name = "_"+constants.size();
        constants.put(ret, ret);
        return ret;
    }

    public Constant PyInteger(int value) {
        return findConstant(new PyIntegerConstant(value));
    }

    public Constant PyFloat(double value) {
        return findConstant(new PyFloatConstant(value));
    }

    public Constant PyComplex(double value) {
        return findConstant(new PyComplexConstant(value));
    }

    public Constant PyString(String value) {
        return findConstant(new PyStringConstant(value));
    }
    public Constant PyUnicode(String value) {
        return findConstant(new PyUnicodeConstant(value));
    }
    public Constant PyLong(String value) {
        return findConstant(new PyLongConstant(value));
    }

    Vector codes;
    private boolean isJavaIdentifier(String s) {
        char[] chars = s.toCharArray();
        if (chars.length == 0)
            return false;
        if (!Character.isJavaIdentifierStart(chars[0]))
            return false;

        for(int i=1; i<chars.length; i++) {
            if (!Character.isJavaIdentifierPart(chars[i]))
                return false;
        }
        return true;
    }

    private String[] toNameAr(Vector names,boolean nullok) {
        int sz = names.size();
        if (sz ==0 && nullok) return null;
        String[] nameArray = new String[sz];
        names.copyInto(nameArray);
        return nameArray;
    }

    private int to_cell;

    public PyCodeConstant PyCode(modType tree, String name,
                                 boolean fast_locals, String className,
                                 boolean classBody, boolean printResults,
                                 int firstlineno, ScopeInfo scope)
        throws Exception
    {
        return PyCode(tree,name,fast_locals,className,classBody,
                      printResults,firstlineno,scope,null);
    }


    public PyCodeConstant PyCode(modType tree, String name,
                                 boolean fast_locals, String className,
                                 boolean classBody, boolean printResults,
                                 int firstlineno,
                                 ScopeInfo scope,
                                 org.python.core.CompilerFlags cflags)
        throws Exception
    {
        PyCodeConstant code = new PyCodeConstant();
        ArgListCompiler ac = (scope != null)?scope.ac:null;

        if (ac != null) {
            code.arglist = ac.arglist;
            code.keywordlist = ac.keywordlist;
            code.argcount = ac.names.size();
        }

        code.co_name = name;
        code.co_firstlineno = firstlineno;
        code.id = codes.size();

        //Better names in the future?
        if (isJavaIdentifier(name))
            code.fname = name+"$"+code.id;
        else
            code.fname = "f$"+code.id;

        codes.addElement(code);

        Code mv = classfile.addMethod(
            code.fname,
            "(" + $pyFrame + ")" + $pyObj,
            ACC_PUBLIC);

        CodeCompiler compiler = new CodeCompiler(this, printResults);

        Label genswitch = new Label();
        if (scope.generator) {
            mv.visitJumpInsn(GOTO, genswitch);
        }
        Label start = new Label();
        mv.visitLabel(start);

        //Do something to add init_code to tree
        if (ac != null && ac.init_code.size() > 0) {
            ac.appendInitCode((Suite) tree);
        }
        int nparamcell = scope.jy_paramcells.size();
        if(nparamcell > 0) {
            Hashtable tbl = scope.tbl;
            Vector paramcells = scope.jy_paramcells;
            for(int i = 0; i < nparamcell; i++) {
                mv.visitVarInsn(ALOAD, 1);
                SymInfo syminf = (SymInfo)tbl.get(paramcells.elementAt(i));
                mv.iconst(syminf.locals_index);
                mv.iconst(syminf.env_index);
                mv.visitMethodInsn(INVOKEVIRTUAL,"org/python/core/PyFrame", "to_cell", "(II)V");
            }
        }

        compiler.parse(tree, mv, fast_locals, className, classBody,
                       scope, cflags);

        if (scope.generator) {
            mv.visitLabel(genswitch);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, "org/python/core/PyFrame", "f_lasti", "I"); 
            Label[] yields = new Label[compiler.yields.size()+1];

            yields[0] = start;
            for (int i = 1; i < yields.length; i++) {
                yields[i] = (Label) compiler.yields.elementAt(i-1);
            }
            mv.visitTableSwitchInsn(0, yields.length - 1, start, yields);
            // XXX: Generate an error
        }

        // !classdef only
        if (!classBody) code.names = toNameAr(compiler.names,false);

        code.cellvars = toNameAr(scope.cellvars, true);
        code.freevars = toNameAr(scope.freevars, true);
        code.jy_npurecell = scope.jy_npurecell;

        if (compiler.optimizeGlobals) {
            code.moreflags |= org.python.core.PyTableCode.CO_OPTIMIZED;
        }
        if (compiler.my_scope.generator) {
            code.moreflags |= org.python.core.PyTableCode.CO_GENERATOR;
        }
        if (cflags != null) {
            if (cflags.generator_allowed) {
                code.moreflags |= org.python.core.PyTableCode.CO_GENERATOR_ALLOWED;
            }
            if (cflags.division) {
                code.moreflags |= org.python.core.PyTableCode.CO_FUTUREDIVISION;
            }
        }

        code.module = this;
        code.name = code.fname;
        return code;
    }

    //This block of code writes out the various standard methods
    public void addInit() throws IOException {
        Code mv = classfile.addMethod("<init>", "(Ljava/lang/String;)V", ACC_PUBLIC);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "org/python/core/PyFunctionTable", "<init>", "()V");
        addConstants(mv);
    }

    public void addRunnable() throws IOException {
        Code mv = classfile.addMethod("getMain", "()" + $pyCode, ACC_PUBLIC);
        mainCode.get(mv);
        mv.visitInsn(ARETURN);
    }

    public void addMain() throws IOException {
        MethodVisitor mv = classfile.addMethod("main", "(" + $strArr + ")V",
                ACC_PUBLIC | ACC_STATIC);
        mv.visitTypeInsn(NEW,classfile.name);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(classfile.name);
        mv.visitMethodInsn(INVOKESPECIAL, classfile.name, "<init>", "(" + $str + ")V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC,"org/python/core/Py", "runMain", "(" + $pyRunnable + $strArr + ")V");
        mv.visitInsn(RETURN);
    }

    public void addConstants(Code mv) throws IOException {
        classfile.addField("self", "L"+classfile.name+";",
                           ACC_STATIC|ACC_FINAL);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(PUTSTATIC, classfile.name, "self", "L"+classfile.name+";");
        Enumeration e = constants.elements();

        while (e.hasMoreElements()) {
            Constant constant = (Constant)e.nextElement();
            constant.put(mv);
        }

        for(int i=0; i<codes.size(); i++) {
            PyCodeConstant pyc = (PyCodeConstant)codes.elementAt(i);
            pyc.put(mv);
        }

        mv.visitInsn(RETURN);
    }

    public void addFunctions() throws IOException {
        MethodVisitor mv = classfile.addMethod(
            "call_function",
            "(I" + $pyFrame + ")" + $pyObj,
            ACC_PUBLIC);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        Label def = new Label();
        Label[] labels = new Label[codes.size()];
        int i;
        for(i=0; i<labels.length; i++)
            labels[i] = new Label();

        //Get index for function to call
        mv.visitVarInsn(ILOAD, 1);
        mv.visitTableSwitchInsn(0, labels.length - 1, def, labels);
        for(i=0; i<labels.length; i++) {
            mv.visitLabel(labels[i]);
            mv.visitMethodInsn(INVOKEVIRTUAL, classfile.name, ((PyCodeConstant)codes.elementAt(i)).fname, "(" + $pyFrame + ")" + $pyObj);
            mv.visitInsn(ARETURN);
        }
        mv.visitLabel(def);

        //Should probably throw internal exception here
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
    }

    public void write(OutputStream stream) throws IOException {
        addInit();
        addRunnable();
        addMain();

        addFunctions();

        classfile.addInterface("org/python/core/PyRunnable");
        if (sfilename != null) {
            //FIXME: switch to asm style source file naming.
            //classfile.addAttribute(new SourceFile(sfilename));
        }
        //FIXME: switch to asm style.
        //classfile.addAttribute(new APIVersion(org.python.core.imp.APIVersion));
        classfile.write(stream);
    }

    // Implementation of CompilationContext
    public Future getFutures() { return futures; }

    public String getFilename() { return sfilename; }

    public ScopeInfo getScopeInfo(SimpleNode node) {
        return (ScopeInfo) scopes.get(node);
    }

    public void error(String msg,boolean err,SimpleNode node)
        throws Exception
    {
        if (!err) {
            try {
                Py.warning(Py.SyntaxWarning, msg,
                           (sfilename != null) ? sfilename : "?",
                           node.beginLine ,null, Py.None);
                return;
            } catch(PyException e) {
                if (!Py.matchException(e, Py.SyntaxWarning))
                    throw e;
            }
        }
        throw new ParseException(msg,node);
    }

    public static void compile(modType node, OutputStream ostream,
                               String name, String filename,
                               boolean linenumbers, boolean printResults,
                               boolean setFile,
                               org.python.core.CompilerFlags cflags)
        throws Exception
    {
        Module module = new Module(name, filename, linenumbers);
        module.setFile = setFile;
        module.futures.preprocessFutures(node, cflags);
        new ScopesCompiler(module, module.scopes).parse(node);

        //Add __doc__ if it exists
        //Add __file__ for filename (if it exists?)

        Constant main = module.PyCode(node, "?", false, null, false,
                                      printResults, 0,
                                      module.getScopeInfo(node),
                                      cflags);
        module.mainCode = main;
        module.write(ostream);
    }
}
