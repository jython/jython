package org.python.compiler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import org.python.core.PyObject;

public class JavaMaker extends ProxyMaker {
	public String pythonClass, pythonModule;
    public String[] properties;
    public String[] packages;
    //Hashtable methods;
    PyObject methods;
    public boolean frozen, main;
    public String[] interfaces;
    public Class superclass;
    
	public JavaMaker(Class superclass, String[] interfaces,
	                    String pythonClass, String pythonModule, String myClass,
	                    PyObject methods) {
	    this(superclass, interfaces, pythonClass, pythonModule, myClass, null, null,
	            methods, false, false);
	                    }
	                    
	public JavaMaker(Class superclass, String[] interfaces,
	                    String pythonClass, String pythonModule, String myClass,
	                    String[] packages, String[] properties,
	                    PyObject methods, //String[] methods,
	                    boolean frozen, boolean main) {
		super("foo");
		if (superclass == null) superclass = PyObject.class;
		this.classname = superclass.getName();
		//methods != null ? superclass.getName() : "org.python.proxies."+superclass.getName());
		this.interfaces = interfaces;
		//System.out.println("props: "+properties+", "+properties.length);
		this.pythonClass = pythonClass;
		this.pythonModule = pythonModule;
		this.myClass = myClass;
		this.packages = packages;
		this.properties = properties;
		this.frozen = frozen;
		this.main = main;
		this.methods = methods;
		this.superclass = superclass;
		/*if (methods != null) {
    		this.methods = new Hashtable();
    		for (int i=0; i<methods.length; i++) {
    		    this.methods.put(methods[i], methods[i]);
    		}
    	}*/
	}

    private void makeStrings(Code code, String[] list) throws Exception {
		if (list != null) {
		    int n = list.length;
    		code.iconst(n);
    		code.anewarray(code.pool.Class("java/lang/String"));
    		int strings = code.getLocal();
    		code.astore(strings);
    		for(int i=0; i<n; i++) {
    			code.aload(strings);
    			code.iconst(i);
    			code.ldc(list[i]);
    			code.aastore();
    		}
    		code.aload(strings);
    		code.freeLocal(strings);
    	} else {
    	    code.aconst_null();
    	}
    }

	public void addConstructor(String name, Class[] parameters, Class ret,
					String sig, int access) throws Exception {
	    /* Need a fancy constructor for the Java side of things */
		Code code = classfile.addMethod("<init>", sig, access);
		callSuper(code, "<init>", name, parameters, null, sig);
		code.aload(0);
		code.ldc(pythonModule);
		code.ldc(pythonClass);
		getArgs(code, parameters);

        makeStrings(code, packages);
        makeStrings(code, properties);

        code.iconst(frozen ? 1 : 0);

		int initProxy = code.pool.Methodref("org/python/core/Py", "initProxy",
			"(Lorg/python/core/PyProxy;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;[Ljava/lang/String;[Ljava/lang/String;Z)V");
		code.invokestatic(initProxy);
		code.return_();
	}

	public void addProxy() throws Exception {
	    if (methods != null) super.addProxy();
	    if (main) addMain();
	}

	public void addMethods(Class c) throws Exception {
	    if (methods != null) {
	        super.addMethods(c);
	    }
	}

	public void addMethod(Method method, int access) throws Exception {
	    //System.out.println("add: "+method.getName()+", "+methods.containsKey(method.getName()));
	    // Check to see if it's an abstract method
	    if (Modifier.isAbstract(access)) {
	        // Maybe throw an exception here???
	        super.addMethod(method, access);
	    } else if (methods.__finditem__(method.getName().intern()) != null) {
	        super.addMethod(method, access);
	    } /*else if (Modifier.isProtected(access)) {
	        super.addMethod(method, access | Modifier.ABSTRACT);
	    }*/
	}

	public void build() throws Exception {
		//Class superclass = Class.forName(classname);
		Class[] ints = new Class[interfaces.length];
		for(int i=0; i<interfaces.length; i++) {
		    ints[i] = Class.forName(interfaces[i]);
		}

		build(superclass, ints);
	}

	public void addMain() throws Exception {
		Code code = classfile.addMethod("main", "([Ljava/lang/String;)V",
										ClassFile.PUBLIC | ClassFile.STATIC);

        // Load the name of the Python module to run
        code.ldc(pythonModule);
        // Load in any command line arguments
        code.aload(0);
        makeStrings(code, packages);
        makeStrings(code, properties);
        code.iconst(frozen ? 1 : 0);

        int runMain = code.pool.Methodref("org/python/core/Py", "runMain",
			"(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;Z)V");
        code.invokestatic(runMain);
        code.return_();
	}

}
