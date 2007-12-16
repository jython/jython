package org.python.expose.generate;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedType;

public class NewExposerTest extends InterpTestCase implements Opcodes {

    public void testSimple() throws Exception {
        NewExposer ne = new NewExposer(Type.getType(Instantiable.class),
                                       ACC_STATIC | ACC_PUBLIC,
                                       "creator",
                                       NewExposer.NEW_DESCRIPTOR,
                                       new String[] {}); 
        assertEquals("org/python/expose/generate/NewExposerTest$Instantiable$exposed___new__",
                     ne.getInternalName());
        assertEquals("org.python.expose.generate.NewExposerTest$Instantiable$exposed___new__",
                     ne.getClassName());
        Class descriptor = ne.load(new BytecodeLoader.Loader());
        PyNewWrapper instance = (PyNewWrapper)descriptor.newInstance();
        instance.setWrappedType(PyType.fromClass(Instantiable.class));
        assertSame("__new__", instance.__getattr__("__name__").toString());
        assertEquals(Py.One, instance.__call__(PyType.fromClass(Instantiable.class)));
    }

    @ExposedType()
    public static class Instantiable extends PyObject {

        public static PyObject creator(PyNewWrapper new_,
                                       boolean init,
                                       PyType subtype,
                                       PyObject[] args,
                                       String[] keywords) {
            return Py.One;
        }
    }
}
