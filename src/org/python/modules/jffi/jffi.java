
package org.python.modules.jffi;

import com.kenai.jffi.Library;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;


public class jffi implements ClassDictInit  {
    public static final int FUNCFLAG_STDCALL = 0x0;
    public static final int FUNCFLAG_CDECL = 0x1;
    public static final int FUNCFLAG_HRESULT = 0x2;
    public static final int FUNCFLAG_PYTHONAPI = 0x4;
    public static final int FUNCFLAG_USE_ERRNO = 0x8;
    public static final int FUNCFLAG_USE_LASTERROR = 0x10;

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", Py.newString("jffi"));
        dict.__setitem__("DynamicLibrary", DynamicLibrary.TYPE);
        dict.__setitem__("Type", CType.TYPE);
        dict.__setitem__("Function", Function.TYPE);
        dict.__setitem__("CData", CData.TYPE);
        dict.__setitem__("PointerCData", PointerCData.TYPE);
        dict.__setitem__("ScalarCData", ScalarCData.TYPE);
        dict.__setitem__("StringCData", StringCData.TYPE);
        dict.__setitem__("Structure", Structure.TYPE);
        dict.__setitem__("StructLayout", StructLayout.TYPE);
        dict.__setitem__("FUNCFLAG_STDCALL", Py.newInteger(FUNCFLAG_STDCALL));
        dict.__setitem__("FUNCFLAG_CDECL", Py.newInteger(FUNCFLAG_CDECL));
        
        dict.__setitem__("RTLD_GLOBAL", Py.newInteger(Library.GLOBAL));
        dict.__setitem__("RTLD_LOCAL", Py.newInteger(Library.LOCAL));
        dict.__setitem__("RTLD_LAZY", Py.newInteger(Library.LAZY));
        dict.__setitem__("RTLD_NOW", Py.newInteger(Library.NOW));

        dict.__setitem__("__version__", Py.newString("0.0.1"));
    }

    public static PyObject dlopen(PyObject name, PyObject mode) {
        return new DynamicLibrary(name != Py.None ? name.asString() : null, mode.asInt());
    }

    public static PyObject get_errno() {
        return Py.newInteger(0);
    }

    public static PyObject set_errno(PyObject type) {
        return Py.newInteger(0);
    }
    public static PyObject pointer(PyObject type) {
        return Py.newInteger(0);
    }
    public static PyObject POINTER(PyObject type) {
        return type;
    }
}
