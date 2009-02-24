"""Given a module, generates a new module where all functions that are
   not builtin (including within classes) have their func_code pointed
   to a PyBytecode constructor. This enables CPython to generate for
   Jython the desired PBC.
"""

from __future__ import with_statement
import inspect
import networkx # add dependency to a setup script? probably overkill
import sys
from collections import defaultdict

attrs = ["co_" + x for x in """
    argcount nlocals stacksize flags
    code consts names varnames
    filename name firstlineno lnotab
    freevars cellvars
""".split()]  # add 

ROOT = object()

class Extract(object):

    def __init__(self, mod, writer):
        self.mod = mod
        self.writer = writer
        self.codeobjs = {}
        self.codeobjs_names = {}
        self.counters = defaultdict(int)
        self.depend = networkx.DiGraph()

    def extract(self):
        mod = self.mod
        writer = self.writer
        functionobjs = self.candidate_functions()
        for name, f in functionobjs:
            self.extract_code_obj(f)

        print >> writer, "from %s import *" % mod.__name__
        print >> writer, "from org.python.core import PyBytecode"
        print >> writer
        print >> writer, "_codeobjs = {}"
        print >> writer

        objs = networkx.topological_sort(self.depend)
        for obj in objs:
            if not inspect.iscode(obj):
                continue
            name = self.codeobjs_names[obj]
            print >> writer, "_codeobjs[%r] = %s" % (name, self.codeobjs[obj])
            print >> writer
        for name, f in functionobjs:
            # this may be a Jython diff, need to determine further; need to check if im_func or not on the object
            print >> writer, "try: %s.func_code = _codeobjs[%r]" % (name, self.codeobjs_names[f.func_code])
            print >> writer, "except (AttributeError, ValueError): pass" # ignore setting cells, im_func, etc... %s.im_func.func_code = _codeobjs[%r]" % (name, self.codeobjs_names[f.func_code])
    
        print >> writer
        print >> writer, 'if __name__ == "__main__":'
        print >> writer, '    test_main()'

    def candidate_functions(self):
        """functions and methods we will retrieve code objects"""
        
        mod = self.mod
        functions =  inspect.getmembers(mod, inspect.isfunction)
        functions = [(name, f) for (name, f) in functions if not inspect.isbuiltin(f) and name != "test_main"]

        classes = inspect.getmembers(mod, inspect.isclass)
        for classname, cls in classes:
            for methodname, method in inspect.getmembers(cls, inspect.ismethod):
                if inspect.getmodule(method) == mod:
                    functions.append(("%s.%s" % (classname, methodname), method))
        return functions

    def extract_code_obj(self, f_or_code):
        if inspect.iscode(f_or_code):
            code = f_or_code
        else:
            code = f_or_code.func_code
        self.extract_def(code)

    def extract_def(self, code):
        if code in self.codeobjs_names:
            #print >> sys.stderr, "Already seen", code
            return "_codeobjs[%r]" % (self.codeobjs_names[code],)

        co_name = code.co_name
        #print >> sys.stderr, "Processing", code
        name = co_name + "." + str(self.counters[co_name])
        self.counters[co_name] += 1
        self.codeobjs_names[code] = name
        values = []
        self.depend.add_edge(code, ROOT)
        for attr in attrs:
            # treat co_consts specially - maybe also need to use pickling in case repr is not suitable
            if attr == 'co_consts':
                co_consts = []
                for const in getattr(code, attr):
                    if inspect.iscode(const):
                        #print >> sys.stderr, "Extracting code const " + str(const)
                        co_consts.append(self.extract_def(const))
                        self.depend.add_edge(const, code)
                    else:
                        co_consts.append(repr(const))
                values.append((attr, "["+', '.join(co_consts)+"]"))
            else:
                values.append((attr, repr(getattr(code, attr))))
        self.codeobjs[code] = "PyBytecode(\n" + '\n'.join([' '* 4 + v + ', # ' + attr for (attr, v) in values])+"\n    )"
        return "_codeobjs[%r]" % (name,)


# for now, just use the cwd
def import_modules(path):
    import glob
    import os.path

    sys.path.insert(0, path)
    for name in sorted(glob.iglob("test/test_*.py")):
        modname = os.path.splitext(os.path.basename(name))[0]
        qualified_name = "test." + modname
        print "Trying", qualified_name
        try:
            topmod = __import__(qualified_name)
        except Exception, e:
            print "Could not import", qualified_name, ":", repr(e)
            continue
        mod = getattr(topmod, modname)
        try:
            mod.test_main
        except AttributeError, e:
            print "No test_main in", qualified_name
            continue

        output_name = "test/" + modname + "_pbc.py"
        with open(output_name, "w") as f:
            print "Extracting", name, "to", output_name
            Extract(mod, f).extract()

if __name__ == '__main__':
    path = modname = sys.argv[1]
    import_modules(path)
