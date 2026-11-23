// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a Contributor Agreement.
package org.python.tests;

import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySet;
import org.python.core.PyType;

/** Java-defined native Python types registered reflectively for collection proxy tests. */
public class CollectionProxyFixtures {

    public static class DictSubclass extends PyDictionary {
        public DictSubclass() {
            super(PyType.fromClass(DictSubclass.class));
        }
    }

    public static class OverridingDict extends PyDictionary {
        private int callCount;

        public OverridingDict() {
            super(PyType.fromClass(OverridingDict.class));
        }

        @Override
        public PyObject get(PyObject key, PyObject defaultValue) {
            callCount++;
            return super.get(key, defaultValue);
        }

        public int getCallCount() {
            return callCount;
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
