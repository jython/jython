package org.python.compiler;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

/**
 * 
 * @author shashank
 */
public class InvokedynamicCodeCompiler extends CodeCompiler {

	public InvokedynamicCodeCompiler(Module module, boolean print_results) {
		super(module, print_results);
	}
	
	// just use indy functions to make sure the build process is proper.
	public void foo(){
		Class mhClazz = MethodHandles.class;
		MethodType target = null;
		new MutableCallSite(target);
	}

}
