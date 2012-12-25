package org.python.compiler.custom_proxymaker;

/* 
 * Tests constructor signatures
 */

import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.*;
import org.python.util.ProxyCompiler;

import java.awt.Container;
import javax.swing.BoxLayout;

public class ConstructorSignatureTest {
    Class<?> proxy;
    
    @Before
    public void setUp() throws Exception {
        ProxyCompiler.compile("tests/python/custom_proxymaker/constructor_signatures.py", "build/classes");
        proxy = Class.forName("custom_proxymaker.tests.ConstructorSignatures");
    }
    
    @Ignore // Constructor signatures are not working yet
    @Test
    @SuppressWarnings("unused")
    public void returnsVoid() throws Exception {
        Constructor<?> constructor = proxy.getConstructor(new Class<?>[] {Container.class, Integer.TYPE});
        constructor.newInstance(new Container(), BoxLayout.X_AXIS);
    }
}
