package org.python.modules._threading;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;
import com.google.common.collect.MapMaker;
import java.util.Map;

public class _threading implements ClassDictInit {

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", Py.newString("_threading"));
        dict.__setitem__("Lock", Lock.TYPE);
        dict.__setitem__("RLock", Lock.TYPE);
        dict.__setitem__("_Lock", Lock.TYPE);
        dict.__setitem__("_RLock", Lock.TYPE);
        dict.__setitem__("Condition", Condition.TYPE);
//        dict.__setitem__("JavaThread", JavaThread.TYPE);
    }

    // internals to support threading.py, test_threading.py
    public static Map<Long, PyObject> _threads = new MapMaker().weakValues().makeMap();
    public static Map<Thread, PyObject> _jthread_to_pythread = new MapMaker().weakKeys().makeMap();
    public static Map<Long, PyObject> _active = _threads;

    public static void _register_thread(Thread jthread, PyObject pythread) {
        _threads.put(jthread.getId(), pythread);
        _jthread_to_pythread.put(jthread, pythread);
    }

    public static void _unregister_thread(Thread jthread) {
         _threads.remove(jthread.getId());
    }

}
