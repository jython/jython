package org.python.compiler;

/**
 * Some JavaMaker smoke tests 
 */

import java.lang.reflect.Array;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;


public class JavaMakerSmokeTest {
	
    public PythonInterpreter interp;
    public Class<?> proxyClass;
    
    @Before
    public void setUp() throws Exception {
        Properties props = new Properties(System.getProperties());
        props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize(props, null);
        interp = new PythonInterpreter();
        
        String input = new String();
        input += "import java.io.ByteArrayInputStream\n";
        input += "import java.lang.String\n";
        input += "import org.python.core.Options\n";
        input += "org.python.core.Options.proxyDebugDirectory = 'build/classes'\n";
        input += "class ProxyTest(java.io.ByteArrayInputStream):\n";
        input += "    def somemethod(self): pass\n";
        input += "ProxyTest(java.lang.String('teststr').getBytes())\n";
        interp.exec(input);
        
        proxyClass = Class.forName("org.python.proxies.__main__$ProxyTest$0");
    }
    
    @Test
    public void constructors() throws Exception {
        proxyClass.getConstructor(Array.newInstance(Byte.TYPE, 0).getClass());
        proxyClass.getConstructor(Array.newInstance(Byte.TYPE, 0).getClass(), Integer.TYPE, Integer.TYPE);
    }
    
    @Test

    public void methods() throws Exception {
        proxyClass.getMethod("classDictInit", org.python.core.PyObject.class);
        proxyClass.getMethod("close");
    }
    
    @Test

    public void annotations() throws Exception {
        proxyClass.getAnnotation(org.python.compiler.APIVersion.class);
        proxyClass.getAnnotation(org.python.compiler.MTime.class);
    }
    
    @Test
    public void interfaces() throws Exception {
        Class<?>[] interfaces = new Class<?>[]{org.python.core.PyProxy.class,
                org.python.core.ClassDictInit.class};
        assertArrayEquals(interfaces, proxyClass.getInterfaces());
    }
}
