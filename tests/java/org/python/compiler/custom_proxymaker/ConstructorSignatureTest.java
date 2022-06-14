package org.python.compiler.custom_proxymaker;

import java.awt.Container;
import java.lang.reflect.Constructor;

import javax.swing.BoxLayout;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.python.util.ProxyCompiler;

public class ConstructorSignatureTest {

    Class<?> proxy;

    @Before
    public void setUp() throws Exception {
        ProxyCompiler.compile("tests/python/custom_proxymaker/constructor_signatures.py",
                "build/testclasses");
        proxy = Class.forName("custom_proxymaker.tests.ConstructorSignatures");
    }

    @Ignore // Constructor signatures are not working yet
    @Test
    public void returnsVoid() throws Exception {
        Constructor<?> constructor =
                proxy.getConstructor(new Class<?>[] {Container.class, Integer.TYPE});
        constructor.newInstance(new Container(), BoxLayout.X_AXIS);
    }
}
