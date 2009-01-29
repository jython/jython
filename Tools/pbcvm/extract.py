"""Given a module, generates a new module where all functions that are
   not builtin (including within classes) have their func_code pointed
   to a PyBytecode constructor. This enables CPython to generate for
   Jython the desired PBC.
"""

import inspect
import networkx # add dependency to a setup script? probably overkill
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
depend = networkx.DiGraph()
root = "root"

def extract(mod):
    functionobjs = candidate_functions(mod)
    for name, f in functionobjs:
        #print >> sys.stderr, "Extracting", f
        extract_code_obj(f)
    codes = {}
    for code, definition in _codeobjs.iteritems():
        codes[_codeobjs_names[code]] = definition
    print "from %s import *" % mod.__name__
    print "from org.python.core import PyBytecode"
    print
    print "_codeobjs = {}"
    print

    objs = networkx.topological_sort(depend)
    for obj in objs:
        if not inspect.iscode(obj):
            continue
        name = _codeobjs_names[obj]
        print "_codeobjs[%r] = %s" % (name, _codeobjs[obj])
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
    depend.add_edge(code, root)
    for attr in attrs:
        if attr == 'co_consts':
            co_consts = []
            for const in getattr(code, attr):
                if inspect.iscode(const):
                    print >> sys.stderr, "Extracting code const " + str(const)
                    co_consts.append(extract_def(const))
                    depend.add_edge(const, code)
                else:
                    co_consts.append(repr(const))
            values.append((attr, "["+', '.join(co_consts)+"]"))
        else:
            values.append((attr, repr(getattr(code, attr))))
    _codeobjs[code] = "PyBytecode(\n" + '\n'.join([' '* 4 + v + ', # ' + attr for (attr, v) in values])+"\n    )"
    return "_codeobjs[%r]" % (name,)


if __name__ == '__main__':
    modname = sys.argv[1]
    mod = __import__(modname)
    extract(mod)
