# Copyright © Corporation for National Research Initiatives

import operator
import org
from BaseEvaluator import BaseEvaluator



class FlowControlException(Exception):
    pass

class DoBreak(FlowControlException):
    pass

class DoContinue(FlowControlException):
    pass

class DoReturn(FlowControlException):
    pass



class PythonInterpreter(BaseEvaluator):
    def __init__(self):
        BaseEvaluator.__init__(self)
        self.locals = {}
        self.globals = {}
        self.builtins = makeBuiltins()

    #primitive values
    def int_const(self, value):
        return PyInteger(value)

    def float_const(self, value):
        return PyFloat(value)

    def string_const(self, value):
        return PyString(value)

    # builtin types
    def list_op(self, values):
        ret = []
        for value in values:
            ret.append(self.visit(value).value)
        return PyObject(ret)

    def tuple_op(self, values):
        lst = self.list_op(values)
        return PyObject(tuple(lst.value))

    def dictionary_op(self, items):
        dict = {}
        for key, value in items:
            dict[self.visit(key).value] = self.visit(value).value

    #namespaces
    def set_name(self, name, value):
        if self.globalnames.has_key(name):
            self.globals[name] = value
        else:
            #print 'setlocal', name, value
            self.locals[name] = value

    def name_const(self, name):
        #print 'get', self.locals
        if self.locals.has_key(name):
            return self.locals[name]
        elif self.globals.has_key(name):
            return self.globals[name]
        else:
            return self.builtins[name]

    def get_module(self, name):
        top = __import__(name[0])
        for part in name[1:]:
            top = getattr(top, part)
        return PyObject(top)

    #flow control
    def pass_stmt(self):
        pass

    def continue_stmt(self):
        raise DoContinue

    def break_stmt(self):
        raise DoBreak

    def return_stmt(self, value):
        raise DoReturn, self.visit(value)

    def while_stmt(self, test, body, else_body=None):
        while self.visit(test).nonzero():
            try:
                self.visit(body)
            except DoBreak:
                break
            except DoContinue:
                continue
        else:
            if else_body is not None:
                self.visit(else_body)

    def if_stmt(self, tests, else_body=None):
        for test, body in tests:
            if self.visit(test).nonzero():
                return self.visit(body)
        if else_body != None:
            return self.visit(else_body)

    def for_stmt(self, index, sequence, body, else_body=None):
        seq = self.visit(sequence)
        i = 0
        try:
            while 1:
                self.set(index, seq.getitem(PyObject(i)))
                i = i+1
                try:
                    self.visit(body)
                except DoBreak:
                    break
                except DoContinue:
                    continue
        except IndexError:
            if else_body is not None:
                self.visit(else_body)



class PyObject:
    def print_line(self):
        print self.value

    def print_continued(self):
        print self.value,

    def __init__(self, value):
        self.value = value

    def nonzero(self):
        return not not self.value

    def unop(self, op):
        test = getattr(org.python.core.PyObject, '__'+op+'__')(self.value)
        return PyObject(test)

    def compop(self, op, y):
        #print 'comp', self.value, op, y.value
        test = getattr(org.python.core.PyObject, '_'+op)(self.value, y.value)
        return PyObject(test)

    binop = compop

    def getitem(self, index):
        return PyObject(operator.getitem(self.value, index.value))

    def setitem(self, index, value):
        operator.setitem(self.value, index.value, value.value)

    def getattr(self, name):
        return PyObject(getattr(self.value, name))

    def setattr(self, name, value):
        setattr(self.value, name, value.value)

    def call(self, args, kws):
        newargs = []
        for arg in args:
            newargs.append(arg.value)
        newkws = {}
        for name, value in kws.items():
            newkws[name] = value.value
        return PyObject(apply(self.value, tuple(newargs), newkws))

    def invoke(self, name, args, kws):
        return self.getattr(name).call(args, kws)

    def dir(self):
        return dir(self.value)



class PyInteger(PyObject):
    pass

class PyFloat(PyObject):
    pass

class PyString(PyObject):
    pass



def makeBuiltins():
    dict = {}
    import __builtin__
    for name in dir(__builtin__):
        value = getattr(__builtin__, name)
        dict[name] = PyObject(value)
    return dict



true = PyObject(1);
false = PyObject(0)



data = """
print 2, 2+2
print 1<2
print 1<2<3
print 3<2
print 1<3<2

print 2*8-9, 6/3
x = 99
print x
y = x+9
print x, y
print 'testing 1, 2, 3'

if 1:
    print 'true'
else:
    print 'false'

i = 0
while i < 5:
    print i
    i = i+1

while i < 10:
    print i
    i = i+1
    if i == 8: break

print len
print [1,2,3]
print len([1,2,3])

import string
print string.join(["a", "b"])

from string import split
print split, split("a b c d e")

"""

pi = PythonInterpreter()
pi.execstring(data)
