// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a Contributor Agreement.
package org.python.core;

import junit.framework.TestCase;

import org.python.util.PythonInterpreter;

/** Tests collection methods on Java-defined subclasses of native Python types. */
public class PyJavaTypeCollectionTest extends TestCase {

    private PythonInterpreter interp;

    @Override
    protected void setUp() {
        interp = new PythonInterpreter(new PyStringMap(), new PySystemState());
    }

    @Override
    protected void tearDown() {
        interp.close();
    }

    public void testDictionaryGet() {
        interp.set("mapping", new DictSubclass());
        interp.exec("mapping['key'] = 'value'\n"
                + "mapping['none'] = None\n"
                + "assert mapping.get('key') == 'value'\n"
                + "assert mapping.get('key', 'fallback') == 'value'\n"
                + "assert mapping.get('missing') is None\n"
                + "assert mapping.get('missing', 'fallback') == 'fallback'\n"
                + "assert mapping.get('none', 'fallback') is None\n");
    }

    public void testDictionaryValues() {
        interp.set("mapping", new DictSubclass());
        interp.exec("mapping['key'] = 'value'\n"
                + "assert mapping.values() == ['value']\n");
    }

    public void testDictionaryGetOverride() {
        OverridingDict mapping = new OverridingDict();
        interp.set("mapping", mapping);
        interp.exec("assert mapping.get('missing', 'fallback') == 'fallback'\n");
        assertEquals(1, mapping.getCalls);
    }

    public void testListSort() {
        interp.set("values", new ListSubclass());
        interp.exec("values.append(2)\n"
                + "values.append(1)\n"
                + "values.sort()\n"
                + "assert list(values) == [1, 2]\n");
    }

    public void testListRemove() {
        interp.set("values", new ListSubclass());
        interp.exec("values.append('value')\n"
                + "values.remove('value')\n"
                + "assert len(values) == 0\n");
    }

    public void testSetRemove() {
        interp.set("values", new SetSubclass());
        interp.exec("values.add('value')\n"
                + "values.remove('value')\n"
                + "assert len(values) == 0\n");
    }

    public void testSetDeepcopyOverride() {
        interp.set("values", new CopyableSet());
        interp.exec("import copy\n"
                + "values.add('value')\n"
                + "copied = copy.deepcopy(values)\n"
                + "assert copied == set(['value'])\n"
                + "assert copied is not values\n");
    }

    public void testWrappedJavaCollections() {
        interp.exec("from java.util import ArrayList, HashMap, HashSet\n"
                + "mapping = HashMap()\n"
                + "mapping['key'] = 'value'\n"
                + "mapping['none'] = None\n"
                + "assert mapping.get('key', 'fallback') == 'value'\n"
                + "assert mapping.get('missing', 'fallback') == 'fallback'\n"
                + "assert mapping.get('none', 'fallback') is None\n"
                + "assert sorted(mapping.values(), key=str) == sorted(['value', None], key=str)\n"
                + "values = ArrayList([2, 1])\n"
                + "values.sort()\n"
                + "assert list(values) == [1, 2]\n"
                + "values.remove(1)\n"
                + "assert list(values) == [2]\n"
                + "values = HashSet(['value'])\n"
                + "values.remove('value')\n"
                + "assert len(values) == 0\n");
    }

    public static class DictSubclass extends PyDictionary {
        public DictSubclass() {
            super(PyType.fromClass(DictSubclass.class));
        }
    }

    public static class OverridingDict extends PyDictionary {
        private int getCalls;

        public OverridingDict() {
            super(PyType.fromClass(OverridingDict.class));
        }

        @Override
        public PyObject get(PyObject key, PyObject defaultValue) {
            getCalls++;
            return super.get(key, defaultValue);
        }
    }

    public static class ListSubclass extends PyList {
        public ListSubclass() {
            super(PyType.fromClass(ListSubclass.class));
        }
    }

    public static class SetSubclass extends PySet {
        public SetSubclass() {
            super(PyType.fromClass(SetSubclass.class));
        }
    }

    public static class CopyableSet extends PySet {
        public CopyableSet() {
            super(PyType.fromClass(CopyableSet.class));
        }

        public PyObject __deepcopy__(PyObject memo) {
            return new PySet(this);
        }
    }
}
