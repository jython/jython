package org.python.compiler.custom_proxymaker;

/*
 * Tests support for various combinations of method signatures
 */

import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.Before;
import org.junit.Test;
import org.python.util.ProxyCompiler;

public class MethodSignatureTest {
	Class<?> proxy;
	
	@Before
	public void setUp() throws Exception {
		ProxyCompiler.compile("tests/python/custom_proxymaker/method_signatures.py", "build/classes");
        proxy = Class.forName("custom_proxymaker.tests.MethodSignatures");	
	}

    @Test
    public void methodThrows() throws Exception {
        Method method = proxy.getMethod("throwsException");
        assertArrayEquals(new Class<?>[] {RuntimeException.class}, method.getExceptionTypes());
    }
    
    @Test
    public void returnsVoid() throws Exception {
        Method method = proxy.getMethod("throwsException");
        assertEquals(Void.TYPE, method.getReturnType());
    }
 
    @Test
    public void returnsLong() throws Exception {
        Method method = proxy.getMethod("returnsLong");
        assertEquals(Long.TYPE, method.getReturnType());
    }
    
    @Test
    public void returnsObject() throws Exception {
        Method method = proxy.getMethod("returnsObject");
        assertEquals(Object.class, method.getReturnType());
    }
    
    @Test
    public void returnsArray() throws Exception {
        Method method = proxy.getMethod("returnsArray");
        Object compareType = Array.newInstance(Long.TYPE, 0);
        assertEquals(compareType.getClass(), method.getReturnType());
    }
    
    @Test
    public void returnsArrayObj() throws Exception {
        Method method = proxy.getMethod("returnsArrayObj");
        Object compareType = Array.newInstance(Object.class, 0);
        assertEquals(compareType.getClass(), method.getReturnType());
    }
    
    @Test
    @SuppressWarnings("unused")
    public void acceptsString() throws Exception {
        Class<?>[] partypes = new Class[] {String.class};
        Method method = proxy.getMethod("acceptsString", partypes);
    }
    
    @Test
    @SuppressWarnings("unused")
    public void acceptsArray() throws Exception {
        Object compareType = Array.newInstance(Long.TYPE, 0);
        Class<?>[] partypes = new Class[] {compareType.getClass()};
        Method method = proxy.getMethod("acceptsArray", partypes);
    }

}
