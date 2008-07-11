import com.sun.jna as jna

def get_libc():
    return CDLL("c")

class Array(object):
    def __init__(self):
        pass

class FuncPtr(object):
    def __init__(self, fn, arg_types, ret_type):
        self.fn = fn
        # decode
        self.arg_types = arg_types
        self.ret_type = ret_type

    def __call__(self, *args):
        pass

class CDLL(object):
    def __init__(self, libname):
        self.lib = jna.NativeLibrary.getInstance(libname)
        self.cache = dict()

    def ptr(self, name, argtypes, restype):
        fn = self.lib.getFunction(name)
        key = (name, tuple(argtypes), restype)
        try:
            return self.cache[key]
        except KeyError:
            fn = FuncPtr(name, argtypes, restype)
            self.cache[key] = fn
            return fn



