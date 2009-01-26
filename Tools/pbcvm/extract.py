"""Given a module, codegens a new module where all functions imported
from it (using __all__ ?) are replaced with functions tied to
PyBytecode, including references to code objs in co_consts. Other
objects are simply imported from the original object. Hopefully this
provides an opportunity to test something two different ways, which
seems nice."""

import inspect
import sys
from collections import defaultdict

attrs = ["co_" + x for x in """
    argcount nlocals stacksize flags
    code consts names varnames
    filename name firstlineno lnotab
""".split()]  # add freevars cellvars?

# this has to be instantiated per module! so simply move into a class
_codeobjs = {}
_codeobjs_names = {}
counters = defaultdict(int)

# XXX - need to capture with a toposort the dependencies of
# recursive code objects and emit in that order

def extract(mod):
    functionobjs = candidate_functions(mod)
    for name, f in functionobjs:
        #print >> sys.stderr, "Extracting", f
        extract_code_obj(f)
    codes = {}
    for code, definition in _codeobjs.iteritems():
        codes[_codeobjs_names[code]] = definition
    print "from %s import *" % mod.__name__
    print "from org.python.core import PyBytecode, PyFunction"
    print
    print "_codeobjs = {}"
    print
    for name, obj in sorted(codes.iteritems()):
        print "_codeobjs[%r] = %s" % (name, obj)
        print
    for name, f in functionobjs:
        print "%s.func_code = _codeobjs[%r]" % (name, _codeobjs_names[f.func_code])
    
    print
    print 'if __name__ == "__main__":'
    print '    test_main()'


def candidate_functions(mod):
    """functions and methods we will retrieve code objects"""

    functions =  inspect.getmembers(mod, inspect.isfunction)
    functions = [(name, f) for (name, f) in functions if not inspect.isbuiltin(f)]

    classes = inspect.getmembers(mod, inspect.isclass)
    for classname, cls in classes:
        #print >> sys.stderr, "Extracting from", cls
        for methodname, method in inspect.getmembers(cls, inspect.ismethod):
            #print >> sys.stderr, "Extracting method", method
            if inspect.getmodule(method) == mod:
                functions.append(("%s.%s" % (classname, methodname), method))
    return functions

# what if we are passed a builtin? need to identify that signature
def extract_code_obj(f_or_code):
    if inspect.iscode(f_or_code):
        code = f_or_code
    else:
        code = f_or_code.func_code
    extract_def(code)

def extract_def(code):
    if code in _codeobjs_names:
        print >> sys.stderr, "Already seen", code
        return "_codeobjs[%r]" % (_codeobjs_names[code],)

    co_name = code.co_name
    print >> sys.stderr, "Processing", code
    name = co_name + "." + str(counters[co_name])
    counters[co_name] += 1
    _codeobjs_names[code] = name
    # need to treat co_consts specially - maybe use pickling if repr is not suitable?
    values = []
    for attr in attrs:
        if attr == 'co_consts':
            co_consts = []
            for const in getattr(code, attr):
                if inspect.iscode(const):
                    print >> sys.stderr, "Extracting code const " + str(const)
                    co_consts.append(extract_def(const))
                else:
                    co_consts.append(repr(const))
            values.append("["+', '.join(co_consts)+"]")
        elif attr == 'co_lnotab':
            values.append(repr(None))
        else:
            values.append(repr(getattr(code, attr)))
    _codeobjs[code] = "PyBytecode(\n" + ',\n'.join([' '* 4 + v for v in values])+")"
    return "_codeobjs[%r]" % (name,)


if __name__ == '__main__':
    modname = sys.argv[1]
    mod = __import__(modname)
    extract(mod)
