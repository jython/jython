# Copyright (c) Corporation for National Research Initiatives
import jast
import org, java



def makeAnys(args):
    ret = []
    for arg in args:
        ret.append(arg.asAny())
    return ret



def PyObjectArray(args):
    aargs = makeAnys(args)
    return Object(
        jast.FilledArray("PyObject", aargs),
        findType(java.lang.Class.forName("[Lorg.python.core.PyObject;")))

def makeStringArray(args):
    return Object(
        jast.StringArray(args),
        findType(java.lang.Class.forName("[Ljava.lang.String;")))



class DelegateMethod:
    def __init__(self, method, code):
        self.method = method
        self.code = code

    def __call__(self, *args):
        return apply(self.method, (self.code, )+args)


class Object:
    def __init__(self, code, value):
        self.code = code
        self.value = value

    def asAny(self):
        return self.asa(org.python.core.PyObject)

    def makeReference(self, code):
        return Object(code, self.value)

    def makeStatement(self):
        return self.value.getStatement(self.code)

    def mergeWith(self, other):
        return Object(jast.Comment("oops"), Generic)

    def __repr__(self):
        return "Object(%s, %s)" % (self.code, self.value.__class__.__name__)

    # delegate most methods to self.value using DelegateMethod
    def __getattr__(self, name):
        return DelegateMethod(getattr(self.value, name), self.code)


primitives = {java.lang.Integer.TYPE  : 'py2int',
              java.lang.Character.TYPE: 'py2char',
              }

import JavaCall



class JavaObject:
    def __init__(self, javaclass):
        self.javaclass = javaclass

    def isa(self, code, type):
        if type.isAssignableFrom(self.javaclass):
            return code

    def asa(self, code, type, message=None):
        ret = self.isa(code, type)
        if ret is not None:
            return ret

        if type == org.python.core.PyObject:
            return self.asAny(code)

    def asAny(self, code):
        return jast.InvokeStatic('Py', 'java2py', [code])

    def getStatement(self, code):
        return code



class JavaInteger(JavaObject):
    def __init__(self):
        JavaObject.__init__(self, java.lang.Integer.TYPE)

    def asAny(self, code):
        return jast.InvokeStatic('Py', 'newInteger', [code])



class JavaString(JavaObject):
    def __init__(self):
        JavaObject.__init__(self, java.lang.String)

    def asAny(self, code):
        return jast.InvokeStatic('Py', 'newString', [code])



class PyObject:
    #I hate static fields for dynamic info!
    attributes = {}
    def getStatement(self, code):
        return code

    def asAny(self, code):
        return code

    def isa(self, code, type):
        if type == org.python.core.PyObject:
            return code

    def asa(self, code, type, message=None):
        ret = self.isa(code, type)
        if ret is not None:
            return ret

        if primitives.has_key(type):
            return jast.InvokeStatic('Py', primitives[type], [code])
        if type == java.lang.Boolean.TYPE:
            return jast.Invoke(code, '__nonzero__', [])

        tname = type.__name__
        tojava = jast.InvokeStatic('Py', 'tojava',
                                   [code, jast.GetStaticAttribute(tname,'class')])
        return jast.Cast(tname, tojava)

    def print_line(self, code):
        return jast.InvokeStatic("Py", "println", [self.asAny(code)])

    def print_continued(self, code):
        return jast.InvokeStatic("Py", "printComma", [self.asAny(code)])

    def print_line_to(self, file, code=None):
        if code is None:
            return jast.InvokeStatic("Py", "printlnv", [self.asAny(file)])
        else:
            return jast.InvokeStatic("Py", "println", [self.asAny(file),
                                                       self.asAny(code)])

    def print_continued_to(self, file, code):
        return jast.InvokeStatic("Py", "printComma", [self.asAny(file),
                                                      self.asAny(code)])

    def domethod(self, code, name, *args):
        meth = getattr(org.python.core.PyObject, name)
        code, type = JavaCall.call(meth, Object(code, self), args)
        return Object(code, findType(type))

    def nonzero(self, code):
        return self.domethod(code, "__nonzero__").asa(java.lang.Boolean.TYPE)

    def unop(self, code, op):
        return self.domethod(code, '__'+op+'__')

    def compop(self, code, op, y):
        return self.domethod(code, "_"+op, y)

    binop = compop

    def aug_binop(self, code, op, y):
        return self.domethod(code, op, y)

    def igetitem(self, code, index):
        return self.domethod(code, "__getitem__",
                             Object(jast.IntegerConstant(index), IntType))

    def getslice(self, code, start, stop, step):
        #print start, stop, step
        return self.domethod(code, "__getslice__", start, stop, step)   

    def delslice(self, code, start, stop, step):
        return self.domethod(code, "__delslice__",
                             start, stop, step).getStatement()

    def setslice(self, code, start, stop, step, value):
        return self.domethod(code, "__setslice__",
                             start, stop, step, value).getStatement()

    def getitem(self, code, index):
        return self.domethod(code, "__getitem__", index)

    def delitem(self, code, index):
        return self.domethod(code, "__delitem__", index).getStatement()

    def setitem(self, code, index, value):
        return self.domethod(code, "__setitem__", index, value).getStatement()

    def getattr(self, code, name):
        if not PyObject.attributes.has_key(name):
            PyObject.attributes[name] = None
        name = Object(jast.StringConstant(name), StringType)
        return self.domethod(code, "__getattr__", name)

    def delattr(self, code, name):
        name = Object(jast.StringConstant(name), StringType)
        return self.domethod(code, "__delattr__", name).getStatement()

    def setattr(self, code, name, value):
        PyObject.attributes[name] = value
        name = Object(jast.StringConstant(name), StringType)
        ret = self.domethod(code, "__setattr__", name, value)
        #print ret      
        return ret.getStatement()

    def call(self, code, args, keyargs=None):
        nargs = len(args)
        if keyargs is None or len(keyargs) == 0:
            if nargs == 0:
                return self.domethod(code, "__call__")
            elif nargs == 1:
                return self.domethod(code, "__call__", args[0])
            elif nargs == 2:
                return self.domethod(code, "__call__", args[0], args[1])
            elif nargs == 3:
                return self.domethod(code, "__call__",
                                     args[0], args[1], args[2])
            else:
                return self.domethod(code, "__call__", PyObjectArray(args))
        else:
            keynames = []
            for name, value in keyargs:
                PyObject.attributes[name] = value
                keynames.append(name)
                args.append(value)

            return self.domethod(code, "__call__", PyObjectArray(args),
                                 makeStringArray(keynames))

    def call_extra(self, code, args, keyargs, starargs, kwargs):
        keynames = []
        for name, value in keyargs:
            PyObject.attributes[name] = value
            keynames.append(name)
            args.append(value)

        if not starargs:
            starargs = Object(jast.Null, self)
        if not kwargs:
            kwargs = Object(jast.Null, self)

        return self.domethod(code, "_callextra",
                             PyObjectArray(args),
                             makeStringArray(keynames),
                             starargs,
                             kwargs)

    def invoke(self, code, name, args, keyargs):
        if keyargs:
            return self.getattr(code, name).call(args, keyargs)

        name = Object(jast.StringConstant(name), StringType)
        nargs = len(args)
        if nargs == 0:
            return self.domethod(code, "invoke", name)
        elif nargs == 1:
            return self.domethod(code, "invoke", name, args[0])
        elif nargs == 2:
            return self.domethod(code, "invoke", name, args[0], args[1])
        else:
            return self.domethod(code, "invoke", name, PyObjectArray(args))

    def doraise(self, code, exc_value=None, exc_traceback=None):
        args = [code]
        if exc_value is not None:
            args.append(exc_value.asAny())
        if exc_traceback is not None:
            args.append(exc_traceback.asAny())
        return jast.Throw(jast.InvokeStatic("Py", "makeException", args))

    def mergeWith(self, other):
        # In simplest world, all types can be merged with each other
        return self

    def makeTemp(self, frame):
        return PyObject(jast.Set(frame.gettemp(self.type),
                                 self.asAny()), self.parent)
    def freeTemp(self, frame):
        frame.freetemp(self.value)



types = {}
def findType(type):
    if types.has_key(type):
        return types[type]

    if type == java.lang.Integer.TYPE:
        ret = JavaInteger()
    elif type == java.lang.String:
        ret = JavaString()
    elif type == org.python.core.PyObject:
        ret = PyObject()
    elif type == org.python.core.PyString:
        ret = PyObject()
    else:
        ret = JavaObject(type)

    types[type] = ret
    return ret



Generic = findType(org.python.core.PyObject)
IntType = findType(java.lang.Integer.TYPE)
StringType = findType(java.lang.String)



if __name__ == '__main__':
    foo = Object(jast.Identifier("foo"), Generic)
    one = Object(jast.IntegerConstant(1), IntType)
    hello = Object(jast.StringConstant("hello"), StringType)

    print foo, one, hello
    print foo.binop("add", foo)
    print foo.binop("add", one)
    print foo.binop("add", hello)
    print foo.nonzero()
    print foo.getitem(foo)
    print foo.getitem(one)
    print foo.call([one, hello])
    print foo.call([one, hello, foo])
