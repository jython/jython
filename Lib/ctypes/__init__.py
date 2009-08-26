import jffi

class _CTypeMetaClass(type):

    def __new__(cls, name, bases, dict):
        return type.__new__(cls, name, bases, dict)

    def __mul__(self, len):
        dict = { '_jffi_type': jffi.Type.Array(self, len) }
        return type("%s_%d" % (self.__name__, len), (_ArrayCData,), dict)

class _ScalarCData(jffi.ScalarCData):
    __metaclass__ = _CTypeMetaClass

    def size(self):
        return self._jffi_type.size

    size = classmethod(size)

class _ArrayCData(object):
    def __init__(self, *args):
        raise NotImplementedError("instantiating arrays is not implemented yet")

    def __len__(self):
        return self._jffi_type.length

def sizeof(type):
    return type._jffi_type.size

def alignment(type):
    return type._jffi_type.alignment

def byref(cdata):
    return cdata.byref()

def pointer(cdata):
    return cdata.pointer()

def POINTER(type):
    return jffi.Type.Pointer(type._jffi_type, type)

class c_byte(_ScalarCData):
    _jffi_type = jffi.Type.BYTE

class c_ubyte(_ScalarCData):
    _jffi_type = jffi.Type.UBYTE

class c_short(_ScalarCData):
    _jffi_type = jffi.Type.SHORT

class c_ushort(_ScalarCData):
    _jffi_type = jffi.Type.USHORT

class c_int(_ScalarCData):
    _jffi_type = jffi.Type.INT

class c_uint(_ScalarCData):
    _jffi_type = jffi.Type.UINT

class c_longlong(_ScalarCData):
    _jffi_type = jffi.Type.LONGLONG

class c_ulonglong(_ScalarCData):
    _jffi_type = jffi.Type.ULONGLONG

class c_long(_ScalarCData):
    _jffi_type = jffi.Type.LONG

class c_ulong(_ScalarCData):
    _jffi_type = jffi.Type.ULONG

class c_float(_ScalarCData):
    _jffi_type = jffi.Type.FLOAT

class c_double(_ScalarCData):
    _jffi_type = jffi.Type.DOUBLE

c_int8 = c_byte
c_uint8 = c_ubyte
c_int16 = c_short
c_uint16 = c_ushort
c_int32 = c_int
c_uint32 = c_uint
c_int64 = c_longlong
c_uint64 = c_ulonglong

c_size_t = c_ulong
c_ssize_t = c_long

class c_char_p(_ScalarCData):
    _jffi_type = jffi.Type.STRING

class c_void_p(_ScalarCData):
    _jffi_type = jffi.Type.POINTER

class _Function(jffi.Function):
    _restype = c_int
    _argtypes = None

    def set_restype(self, restype):
        self._jffi_restype = restype._jffi_type
        self._restype = restype

    def get_restype(self):
        return self._restype

    def set_argtypes(self, argtypes):
        jffi_types = []
        for t in argtypes:
            jffi_types.append(t._jffi_type)
        self._jffi_argtypes = jffi_types
        self._argtypes = argtypes

    def get_argtypes(self):
        return self._argtypes

    restype = property(get_restype, set_restype)
    argtypes = property(get_argtypes, set_argtypes)


class CDLL:
    DEFAULT_MODE = jffi.RTLD_GLOBAL | jffi.RTLD_LAZY

    def __init__(self, name, mode = DEFAULT_MODE, handle = None):
        self._handle = jffi.dlopen(name, mode)

    def __getattr__(self, name):
        if name.startswith('__') and name.endswith('__'):
            raise AttributeError, name
        func = self.__getitem__(name)
        setattr(self, name, func)
        return func

    def __getitem__(self, name):
        return _Function(self._handle.find_symbol(name))

class LibraryLoader(object):
    def __init__(self, dlltype):
        self._dlltype = dlltype

    def __getattr__(self, name):
        if name[0] == '_':
            raise AttributeError(name)
        dll = self._dlltype(name)
        setattr(self, name, dll)
        return dll

    def __getitem__(self, name):
        return getattr(self, name)

    def LoadLibrary(self, name):
        return self._dlltype(name)

cdll = LibraryLoader(CDLL)

#
#class _StructMetaClass(type):
#    def __new__(cls, name, bases, dict):
#        for attr in dict:
#            if attr == '_fields_':
#                print "%s has attr %s" % (name, attr)
#        return type.__new__(cls, name, bases, dict)
#
#class Structure:
#    __metaclass__ = _StructMetaClass
