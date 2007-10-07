package org.python.expose;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.security.SecureClassLoader;
import java.util.List;
import java.util.Vector;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.python.core.BytecodeLoader;
import org.python.expose.MethodExposer;
import org.python.expose.TypeBuilder;
import org.python.expose.TypeExposer;

import junit.framework.TestCase;

public class TypeExposerTest extends TestCase {

    public void testFindMethods() {
        TypeExposer ecp = new TypeExposer(SimpleExposed.class);
        List<Method> methods = ecp.findMethods();
        assertEquals(1, methods.size());
    }

    public void testGetName() {
        assertEquals("SimpleExposed", new TypeExposer(SimpleExposed.class).getName());
        assertEquals("somethingcompletelydifferent",
                     new TypeExposer(Rename.class).getName());
    }

    public void testNoExposed() {
        try {
            new TypeExposer(Unexposed.class);
            fail("Passing a class without @Exposed to ExposedClassProcessor should throw an IllegalArgumentException");
        } catch(IllegalArgumentException iae) {}
    }

    public void testGenerate() throws InstantiationException, IllegalAccessException {
        ByteLoader l = new ByteLoader();
        TypeExposer ecp = new TypeExposer(SimpleExposed.class);
        for(Method m : ecp.findMethods()) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            MethodExposer mp = new MethodExposer(m);
            mp.generate(cw);
            l.loadClassFromBytes(mp.getClassName(), cw.toByteArray());
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        TraceClassVisitor cv = new TraceClassVisitor(new CheckClassAdapter(cw),
                                                     new PrintWriter(System.out));
        ecp.generate(cv);
        Class descriptor = l.loadClassFromBytes(ecp.getClassName(), cw.toByteArray());
        TypeBuilder t = (TypeBuilder)descriptor.newInstance();
        assertEquals("SimpleExposed", t.getName());
        assertEquals(SimpleExposed.class, t.getTypeClass());
        assertNotNull(t.getDict().__finditem__("simple_method"));
    }

    public class Unexposed {}

    @Exposed(name = "somethingcompletelydifferent")
    public class Rename {}
}
