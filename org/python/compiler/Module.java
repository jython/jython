// Copyright © Corporation for National Research Initiatives

package org.python.compiler;

import java.io.*;
import java.util.*;
import org.python.parser.*;

class PyIntegerConstant extends Constant 
{
    int value;

    public PyIntegerConstant(int value) {
	this.value = value;
    }

    public void get(Code c) throws IOException {
	c.getstatic(module.classfile.name, name,
		    "Lorg/python/core/PyInteger;");
    }

    public void put(Code c) throws IOException {
	module.classfile.addField(name, "Lorg/python/core/PyInteger;", access);
	c.iconst(value);
	int mref_newInteger = c.pool.Methodref(
	    "org/python/core/Py",
	    "newInteger",
	    "(I)Lorg/python/core/PyInteger;");
	c.invokestatic(mref_newInteger);
	c.putstatic(module.classfile.name, name,
		    "Lorg/python/core/PyInteger;");
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

class PyFloatConstant extends Constant
{
    double value;

    public PyFloatConstant(double value) {
	this.value = value;
    }

    public void get(Code c) throws IOException {
	c.getstatic(module.classfile.name, name, "Lorg/python/core/PyFloat;");
    }

    public void put(Code c) throws IOException {
	module.classfile.addField(name, "Lorg/python/core/PyFloat;", access);
	c.ldc(c.pool.Double(value));
	int mref_newFloat = c.pool.Methodref("org/python/core/Py",
					     "newFloat",
					     "(D)Lorg/python/core/PyFloat;");
	c.invokestatic(mref_newFloat);
	c.putstatic(module.classfile.name, name, "Lorg/python/core/PyFloat;");
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

class PyComplexConstant extends Constant
{
    double value;

    public PyComplexConstant(double value) {
	this.value = value;
    }

    public void get(Code c) throws IOException {
	c.getstatic(module.classfile.name, name,
		    "Lorg/python/core/PyComplex;");
    }

    public void put(Code c) throws IOException {
	module.classfile.addField(name, "Lorg/python/core/PyComplex;", access);
	c.ldc(c.pool.Double(value));
	int mref_newImaginary = c.pool.Methodref(
	    "org/python/core/Py",
	    "newImaginary",
	    "(D)Lorg/python/core/PyComplex;");
	c.invokestatic(mref_newImaginary);
	c.putstatic(module.classfile.name, name,
		    "Lorg/python/core/PyComplex;");
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

class PyStringConstant extends Constant
{
    String value;

    public PyStringConstant(String value) {
	this.value = value;
    }

    public void get(Code c) throws IOException {
	c.getstatic(module.classfile.name, name, "Lorg/python/core/PyString;");
    }

    public void put(Code c) throws IOException {
	module.classfile.addField(name, "Lorg/python/core/PyString;", access);
	c.ldc(value);
	int mref_newString = c.pool.Methodref(
	    "org/python/core/Py",
	    "newString",
	    "(Ljava/lang/String;)Lorg/python/core/PyString;");
	c.invokestatic(mref_newString);
	c.putstatic(module.classfile.name, name, "Lorg/python/core/PyString;");
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

class PyLongConstant extends Constant
{
    String value;

    public PyLongConstant(String value) {
	this.value = value;
    }

    public void get(Code c) throws IOException {
	c.getstatic(module.classfile.name, name, "Lorg/python/core/PyLong;");
    }

    public void put(Code c) throws IOException {
	module.classfile.addField(name, "Lorg/python/core/PyLong;", access);
	c.ldc(value);
	int mref_newLong = c.pool.Methodref(
	    "org/python/core/Py",
	    "newLong",
	    "(Ljava/lang/String;)Lorg/python/core/PyLong;");
	c.invokestatic(mref_newLong);
	c.putstatic(module.classfile.name, name, "Lorg/python/core/PyLong;");
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

class PyCodeConstant extends Constant
{
    public String co_name;
    public int argcount;
    public String[] names;
    public int id;
    public int co_firstlineno;
    public boolean arglist, keywordlist;
    String fname;

    public PyCodeConstant() { ;
    }

    public void get(Code c) throws IOException {
	c.getstatic(module.classfile.name, name, "Lorg/python/core/PyCode;");
    }

    public void put(Code c) throws IOException {
	module.classfile.addField(name, "Lorg/python/core/PyCode;", access);
	c.iconst(argcount);

	//Make all names
	CodeCompiler.makeStrings(c, names, names.length);

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

	int mref_newCode = c.pool.Methodref(
	    "org/python/core/Py",
	    "newCode",
	    "(I[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZZLorg/python/core/PyFunctionTable;I)Lorg/python/core/PyCode;");

	c.invokestatic(mref_newCode);
	//c.aconst_null();
	c.putstatic(module.classfile.name, name, "Lorg/python/core/PyCode;");
    }
}

public class Module
{
    ClassFile classfile;
    Constant filename;
    String sfilename;
    public Constant mainCode;
    public boolean linenumbers;
    public boolean setFile=true;

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
      return PyCode(tree, name, ac, fast_locals, class_body, false, firstlineno);
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
	
    public PyCodeConstant PyCode(SimpleNode tree, String name,
				 ArgListCompiler ac,
				 boolean fast_locals, String className,
				 boolean classBody, boolean printResults, 
				 int firstlineno)
	throws Exception
    {
	PyCodeConstant code = new PyCodeConstant();
	int i;
	code.arglist = ac.arglist;
	code.keywordlist = ac.keywordlist;
	code.argcount = ac.names.size();

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
	    "(Lorg/python/core/PyFrame;)Lorg/python/core/PyObject;",
	    ClassFile.PUBLIC);

	//Do something to add init_code to tree
	CodeCompiler compiler = new CodeCompiler(this, printResults);

        if (ac.init_code.getNumChildren() > 0) {
            ac.init_code.jjtAddChild(tree, ac.init_code.getNumChildren());
            tree = ac.init_code;
        }

	//System.out.println("l: "+c.getLocal()+", "+c.getLocal()+", "+c.getLocal());
	//c.print("in code: "+name);
	compiler.parse(tree, c, fast_locals, className, classBody, ac);

	code.names = new String[compiler.names.size()];
	for(i=0; i<compiler.names.size(); i++) {
	    code.names[i] = (String)compiler.names.elementAt(i);
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
				     "()Lorg/python/core/PyCode;",
				     ClassFile.PUBLIC);
	mainCode.get(c);
	c.areturn();
    }

    public void addMain() throws IOException {
	Code c = classfile.addMethod("main", "([Ljava/lang/String;)V",
				     ClassFile.PUBLIC | ClassFile.STATIC);


	int mref_self = c.pool.Fieldref(classfile.name,
					"self",
					"L"+classfile.name+";");
	c.getstatic(mref_self);
	c.aload(0);
	c.invokestatic(c.pool.Methodref(
	    "org/python/core/Py",
	    "do_main",
	    "(Lorg/python/core/PyRunnable;[Ljava/lang/String;)V"));
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
	    "(ILorg/python/core/PyFrame;)Lorg/python/core/PyObject;",
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
		"(Lorg/python/core/PyFrame;)Lorg/python/core/PyObject;");
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

    public static void compile(SimpleNode node, OutputStream ostream,
			       String name, String filename,
			       boolean linenumbers, boolean printResults,
			       boolean setFile)
	throws Exception
    {
	Module module = new Module(name, filename, linenumbers);
	module.setFile = setFile;
	//Add __doc__ if it exists
	//Add __file__ for filename (if it exists?)

	Constant main = module.PyCode(node, "?",
				      new ArgListCompiler(),
				      false, null, false, printResults, 0);
	module.mainCode = main;
	module.write(ostream);
    }
}
