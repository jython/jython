// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.*;
import java.util.*;
import org.python.parser.*;
import org.python.parser.ast.*;
import org.python.core.Py;
import org.python.core.PyException;

class PyIntegerConstant extends Constant implements ClassConstants
{
    int value;

    public PyIntegerConstant(int value) {
        this.value = value;
    }

    public void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, $pyInteger);
    }

    public void put(Code c) throws IOException {
        module.classfile.addField(name, $pyInteger, access);
        c.iconst(value);
        int mref_newInteger = c.pool.Methodref(
            "org/python/core/Py",
            "newInteger",
            "(I)" + $pyInteger);
        c.invokestatic(mref_newInteger);
        c.putstatic(module.classfile.name, name, $pyInteger);
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

class PyFloatConstant extends Constant implements ClassConstants
{
    double value;

    public PyFloatConstant(double value) {
        this.value = value;
    }

    public void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, $pyFloat);
    }

    public void put(Code c) throws IOException {
        module.classfile.addField(name, $pyFloat, access);
        c.ldc(c.pool.Double(value));
        int mref_newFloat = c.pool.Methodref("org/python/core/Py",
                                             "newFloat",
                                             "(D)" + $pyFloat);
        c.invokestatic(mref_newFloat);
        c.putstatic(module.classfile.name, name, $pyFloat);
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

class PyComplexConstant extends Constant implements ClassConstants
{
    double value;

    public PyComplexConstant(double value) {
        this.value = value;
    }

    public void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, $pyComplex);
    }

    public void put(Code c) throws IOException {
        module.classfile.addField(name, $pyComplex, access);
        c.ldc(c.pool.Double(value));
        int mref_newImaginary = c.pool.Methodref(
            "org/python/core/Py",
            "newImaginary",
            "(D)" + $pyComplex);
        c.invokestatic(mref_newImaginary);
        c.putstatic(module.classfile.name, name, $pyComplex);
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

class PyStringConstant extends Constant implements ClassConstants
{
    String value;

    public PyStringConstant(String value) {
        this.value = value;
    }

    public void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, $pyStr);
    }

    public void put(Code c) throws IOException {
        module.classfile.addField(name, $pyStr, access);
        c.ldc(value);
        int mref_newString = c.pool.Methodref(
            "org/python/core/Py",
            "newString",
            "(" + $str + ")" + $pyStr);
        c.invokestatic(mref_newString);
        c.putstatic(module.classfile.name, name, $pyStr);
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

class PyLongConstant extends Constant implements ClassConstants
{
    String value;

    public PyLongConstant(String value) {
        this.value = value;
    }

    public void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, $pyLong);
    }

    public void put(Code c) throws IOException {
        module.classfile.addField(name, $pyLong, access);
        c.ldc(value);
        int mref_newLong = c.pool.Methodref(
            "org/python/core/Py",
            "newLong",
            "(" + $str + ")" + $pyLong);
        c.invokestatic(mref_newLong);
        c.putstatic(module.classfile.name, name, $pyLong);
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

class PyCodeConstant extends Constant implements ClassConstants
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

    public void get(Code c) throws IOException {
        c.getstatic(module.classfile.name, name, $pyCode);
    }

    public void put(Code c) throws IOException {
        module.classfile.addField(name, $pyCode, access);
        c.iconst(argcount);

        //Make all names
        if (names != null) {
            CodeCompiler.makeStrings(c, names, names.length);
        } else { // classdef
             CodeCompiler.makeStrings(c, null, 0);
        }

        c.ldc(((PyStringConstant)module.filename).value);
        c.ldc(co_name);
        c.iconst(co_firstlineno);

        c.iconst(arglist ? 1 : 0);
        c.iconst(keywordlist ? 1 : 0);

        int mref_self = c.pool.Fieldref(module.classfile.name,
                                        "self",
                                        "L"+module.classfile.name+";");
        c.getstatic(mref_self);
        //c.aconst_null();

        c.iconst(id);

        if (cellvars != null)
            CodeCompiler.makeStrings(c, cellvars, cellvars.length);
        else
            c.aconst_null();
        if (freevars != null)
            CodeCompiler.makeStrings(c, freevars, freevars.length);
        else
            c.aconst_null();

        c.iconst(jy_npurecell);

        c.iconst(moreflags);

        int mref_newCode = c.pool.Methodref(
            "org/python/core/Py",
            "newCode",
            "(I" + $strArr + $str + $str + "IZZ" + $pyFuncTbl + "I" +
                $strArr + $strArr + "II)" + $pyCode);

        c.invokestatic(mref_newCode);
        //c.aconst_null();
        c.putstatic(module.classfile.name, name, $pyCode);
    }
}

public class Module implements ClassConstants, CompilationContext
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
                                  ClassFile.SYNCHRONIZED | ClassFile.PUBLIC);
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
    public Constant PyLong(String value) {
        return findConstant(new PyLongConstant(value));
    }

    /*public PyCodeConstant PyCode(SimpleNode tree, String name,
      ArgListCompiler ac,
      boolean fast_locals, boolean class_body)
      throws Exception {
      return PyCode(tree, name, ac, fast_locals, class_body, false, 0);
      }
      public PyCodeConstant PyCode(SimpleNode tree, String name,
      ArgListCompiler ac,
      boolean fast_locals, boolean class_body,
      int firstlineno)
      throws Exception {
      return PyCode(tree, name, ac, fast_locals, class_body, false,
                    firstlineno);
      }
      public PyCodeConstant PyCode(SimpleNode tree, String name,
      ArgListCompiler ac,
      boolean fast_locals, boolean class_body,
      boolean printResults)
      throws Exception {
      return PyCode(tree, name, ac, fast_locals, class_body, printResults, 0);
      }*/

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

    private static final String[] emptyStringAr = new String[0];

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

        Code c = classfile.addMethod(
            code.fname,
            "(" + $pyFrame + ")" + $pyObj,
            ClassFile.PUBLIC);

        //Do something to add init_code to tree
        CodeCompiler compiler = new CodeCompiler(this, printResults);

        if (ac != null && ac.init_code.size() > 0) {
            ac.appendInitCode((Suite) tree);
        }

        if (scope != null) {
          int nparamcell = scope.jy_paramcells.size();
          if (nparamcell > 0) {
            if (to_cell == 0) {
                to_cell = classfile.pool.Methodref("org/python/core/PyFrame",
                    "to_cell","(II)V");
            }
            Hashtable tbl = scope.tbl;
            Vector paramcells = scope.jy_paramcells;
            for (int i = 0; i < nparamcell; i++) {
                c.aload(1);
                SymInfo syminf = (SymInfo)tbl.get(paramcells.elementAt(i));
                c.iconst(syminf.locals_index);
                c.iconst(syminf.env_index);
                c.invokevirtual(to_cell);
            }
          }
        }

        compiler.parse(tree, c, fast_locals, className, classBody,
                       scope, cflags);

        // !classdef only
        if (!classBody) code.names = toNameAr(compiler.names,false);

        if (scope != null) {
            code.cellvars = toNameAr(scope.cellvars,true);
            code.freevars = toNameAr(scope.freevars,true);
            code.jy_npurecell = scope.jy_npurecell;
        }

        if (compiler.optimizeGlobals) {
            code.moreflags |= org.python.core.PyTableCode.CO_OPTIMIZED;
        }

        code.module = this;
        code.name = code.fname;
        return code;
    }

    //This block of code writes out the various standard methods
    public void addInit() throws IOException {
        Code c = classfile.addMethod("<init>", "()V", ClassFile.PUBLIC);
        c.aload(0);
        c.invokespecial(c.pool.Methodref("org/python/core/PyFunctionTable",
                                         "<init>",
                                         "()V"));
        c.return_();
    }

    public void addRunnable() throws IOException {
        Code c = classfile.addMethod("getMain",
                                     "()" + $pyCode,
                                     ClassFile.PUBLIC);
        mainCode.get(c);
        c.areturn();
    }

    public void addMain() throws IOException {
        Code c = classfile.addMethod("main", "(" + $str + ")V",
                                     ClassFile.PUBLIC | ClassFile.STATIC);


        int mref_self = c.pool.Fieldref(classfile.name,
                                        "self",
                                        "L"+classfile.name+";");
        c.getstatic(mref_self);
        c.aload(0);
        c.invokestatic(c.pool.Methodref(
            "org/python/core/Py",
            "do_main",
            "(" + $pyRunnable + $strArr + ")V"));
        c.return_();
    }

    public void addConstants() throws IOException {
        Code c = classfile.addMethod("<clinit>", "()V", ClassFile.STATIC);

        classfile.addField("self", "L"+classfile.name+";",
                           ClassFile.STATIC|ClassFile.FINAL);
        c.new_(c.pool.Class(classfile.name));
        c.dup();
        c.invokespecial(c.pool.Methodref(classfile.name, "<init>", "()V"));
        c.putstatic(c.pool.Fieldref(classfile.name,
                                    "self",
                                    "L"+classfile.name+";"));

        Enumeration e = constants.elements();

        while (e.hasMoreElements()) {
            Constant constant = (Constant)e.nextElement();
            constant.put(c);
        }

        for(int i=0; i<codes.size(); i++) {
            PyCodeConstant pyc = (PyCodeConstant)codes.elementAt(i);
            pyc.put(c);
        }

        c.return_();
    }

    public void addFunctions() throws IOException {
        Code code = classfile.addMethod(
            "call_function",
            "(I" + $pyFrame + ")" + $pyObj,
            ClassFile.PUBLIC);

        Label def = code.getLabel();
        Label[] labels = new Label[codes.size()];
        int i;
        for(i=0; i<labels.length; i++)
            labels[i] = code.getLabel();

        //Get index for function to call
        code.iload(1);

        code.tableswitch(def, 0, labels);
        for(i=0; i<labels.length; i++) {
            labels[i].setPosition();
            code.aload(0);
            code.aload(2);
            code.invokevirtual(
                classfile.name,
                ((PyCodeConstant)codes.elementAt(i)).fname,
                "(" + $pyFrame + ")" + $pyObj);
            code.areturn();
        }
        def.setPosition();

        //Should probably throw internal exception here
        code.aconst_null();
        code.areturn();

    }

    public void write(OutputStream stream) throws IOException {
        addInit();
        addRunnable();
        //addMain();

        addConstants();
        addFunctions();

        classfile.addInterface("org/python/core/PyRunnable");
        if (sfilename != null) {
            classfile.addAttribute(new SourceFile(sfilename, classfile.pool));
        }
        classfile.addAttribute(new APIVersion(org.python.core.imp.APIVersion,
                                              classfile.pool));
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
