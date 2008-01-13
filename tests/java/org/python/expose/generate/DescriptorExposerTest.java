package org.python.expose.generate;

import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyDataDescr;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

public class DescriptorExposerTest extends InterpTestCase implements PyTypes {

    private static final String RETURN_STRING_DESCRIPTOR = Type.getMethodDescriptor(STRING,
                                                                                    new Type[0]);

    private static final PyType PY_TYPE = PyType.fromClass(DescExposed.class);

    private static final Type ASM_TYPE = Type.getType(DescExposed.class);

    private DescExposed se = new DescExposed();

    private static final String NEW_VAL = "This is not what was there before";

    interface DescSetup {

        void setup(DescriptorExposer de);
    }

    public PyDataDescr makeDescriptor(DescSetup setup, String name) throws Exception {
        DescriptorExposer de = new DescriptorExposer(ASM_TYPE, name);
        setup.setup(de);
        Class descriptor = de.load(new BytecodeLoader.Loader());
        return (PyDataDescr)descriptor.newInstance();
    }

    public PyDataDescr makeDescriptor(DescSetup setup) throws Exception {
        return makeDescriptor(setup, "desc");
    }

    public void testNoDescriptors() throws Exception {
        DescriptorExposer de = new DescriptorExposer(ASM_TYPE, "desc");
        try {
            de.load(new BytecodeLoader.Loader());
            fail("Should not be able to generate a descriptor with no getter");
        } catch(InvalidExposingException ise) {
            // Should be thrown when a getter isn't added.
        }
    }

    public void testMethodGetter() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addMethodGetter("toString", RETURN_STRING_DESCRIPTOR);
            }
        });
        assertEquals(SimpleExposed.TO_STRING_RETURN, instance.__get__(se, PY_TYPE).toString());
        assertFalse(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testFieldGetter() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addFieldGetter("toStringVal", STRING);
            }
        });
        assertEquals(SimpleExposed.TO_STRING_RETURN, instance.__get__(new DescExposed(), PY_TYPE)
                .toString());
        assertFalse(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testMethodSetter() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addMethodGetter("toString", RETURN_STRING_DESCRIPTOR);
                de.addMethodSetter("setToString", "(Ljava/lang/String;)V");
            }
        });
        instance.__set__(se, Py.newString(NEW_VAL));
        assertEquals(NEW_VAL, se.toString());
        assertTrue(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testFieldSetter() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addFieldGetter("toStringVal", STRING);
                de.addFieldSetter("toStringVal", STRING);
            }
        });
        instance.__set__(se, Py.newString(NEW_VAL));
        assertEquals(NEW_VAL, se.toString());
        assertTrue(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testMethodDel() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addMethodGetter("toString", RETURN_STRING_DESCRIPTOR);
                de.addMethodDeleter("deleteToString", "()V");
            }
        });
        instance.__delete__(se);
        assertNull(se.toString());
        assertTrue(instance.implementsDescrDelete());
    }

    public void testInt() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addFieldGetter("i", INT);
                de.addFieldSetter("i", INT);
            }
        }, "i");
        assertEquals(7, instance.__get__(se, PY_TYPE).asInt());
        instance.__set__(se, Py.newInteger(12));
        assertEquals(12, instance.__get__(se, PY_TYPE).asInt());
    }

    public void testByte() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addFieldGetter("b", BYTE);
                de.addMethodSetter("setB", "(B)");
            }
        }, "b");
        assertEquals(0, instance.__get__(se, PY_TYPE).asInt());
        instance.__set__(se, Py.newInteger(-1));
        assertEquals(-1, instance.__get__(se, PY_TYPE).asInt());
    }

    public void testLong() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addMethodGetter("l", "()J");
                de.addFieldSetter("l", Type.LONG_TYPE);
            }
        }, "l");
        assertEquals(0, instance.__get__(se, PY_TYPE).asInt());
        instance.__set__(se, Py.newInteger(12));
        assertEquals(12, instance.__get__(se, PY_TYPE).asInt());
    }

    public void testDouble() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addMethodGetter("getD", "()D");
                de.addMethodSetter("setD", "(D)");
            }
        }, "d");
        assertEquals(98.7, Py.py2double(instance.__get__(se, PY_TYPE)));
        instance.__set__(se, Py.newInteger(12));
        assertEquals(12.0, Py.py2double(instance.__get__(se, PY_TYPE)));
    }

    public void testBool() throws Exception {
        PyDataDescr instance = makeDescriptor(new DescSetup() {

            public void setup(DescriptorExposer de) {
                de.addFieldGetter("bool", BOOLEAN);
                de.addFieldSetter("bool", BOOLEAN);
            }
        }, "bool");
        assertEquals(false, Py.py2boolean(instance.__get__(se, PY_TYPE)));
        instance.__set__(se, Py.True);
        assertEquals(true, Py.py2boolean(instance.__get__(se, PY_TYPE)));
    }

    public class DescExposed extends PyObject {

        public void setToString(String newVal) {
            toStringVal = newVal;
        }

        public void deleteToString() {
            toStringVal = null;
        }

        public String toString() {
            return toStringVal;
        }

        public long l() {
            return l;
        }

        public void setB(byte newB) {
            b = newB;
        }

        public void setD(double d) {
            this.d = d;
        }

        public double getD() {
            return d;
        }

        public double d = 98.7;

        public int i = 7;

        public long l;

        public byte b;
        
        public boolean bool;

        public String toStringVal = SimpleExposed.TO_STRING_RETURN;
    }
}
