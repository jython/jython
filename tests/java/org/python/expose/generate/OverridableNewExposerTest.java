package org.python.expose.generate;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

public class OverridableNewExposerTest extends InterpTestCase implements PyTypes, Opcodes {

    public void setUp() throws Exception {
        super.setUp();
        ne = new OverridableNewExposer(Type.getType(Instantiable.class),
                                Type.getType(Instantiable.class),
                                ACC_PUBLIC,
                                "creator",
                                Type.getMethodDescriptor(VOID, new Type[] {APYOBJ, ASTRING}),
                                new String[] {});
        Class descriptor = ne.load(new BytecodeLoader.Loader());
        instance = (PyNewWrapper)descriptor.newInstance();
        type = PyType.fromClass(Instantiable.class);
        instance.setWrappedType(type);
    }

    public void testSimple() throws Exception {
        assertEquals("org/python/expose/generate/OverridableNewExposerTest$Instantiable$exposed___new__",
                     ne.getInternalName());
        assertEquals("org.python.expose.generate.OverridableNewExposerTest$Instantiable$exposed___new__",
                     ne.getClassName());
        assertSame("__new__", instance.__getattr__("__name__").toString());
        Instantiable created = (Instantiable)instance.__call__(type);
        assertEquals("Just calling the actual new doesn't call its init", 0, created.timesCalled);
        created = (Instantiable)instance.new_impl(true, type, Py.EmptyObjects, Py.NoKeywords);
        assertEquals("Passing true to new_init should get the init method called",
                     1,
                     created.timesCalled);
        assertEquals("the regular type is passed in for normal instantiation",
                     type,
                     created.forType);
    }

    public void testSubtype() throws Exception {
        PyType sub = (PyType)PyType.newType(new PyNewWrapper() {

            public PyObject new_impl(boolean init,
                                     PyType subtype,
                                     PyObject[] args,
                                     String[] keywords) {
                return new Instantiable(subtype);
            }
        }, PyType.TYPE, "subinst", new PyTuple(new PyObject[] {type}), new PyDictionary());
        Instantiable created = (Instantiable)instance.new_impl(true,
                                                               sub,
                                                               Py.EmptyObjects,
                                                               Py.NoKeywords);
        assertEquals("new's init isn't called when a subtype comes in", 0, created.timesCalled);
        assertSame("the subtype is created when a subtype is passed in", sub, created.forType);
    }

    @ExposedType()
    public static class Instantiable extends PyObject {

        public Instantiable(PyType forType) {
            this.forType = forType;
        }

        @ExposedNew
        public void creator(PyObject[] args, String[] keywords) {
            timesCalled++;
        }

        private PyType forType;

        private int timesCalled;
    }

    private OverridableNewExposer ne;

    private PyNewWrapper instance;

    private PyType type;
}
