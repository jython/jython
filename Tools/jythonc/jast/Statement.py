# Copyright (c) Corporation for National Research Initiatives
from Output import SourceFile

COMMASPACE = ', '



class Statement:
    def __repr__(self):
        out = SourceFile('Foo')
        self.writeSource(out)
        return str(out)

    def exits(self):
        return 0


class Class(Statement):
    def __init__(self, name, access, superclass, interfaces, body):
        self.name = name
        self.access = access
        self.superclass = superclass
        self.interfaces = interfaces
        self.body = body

    def writeSource(self, out):
        out.write("%s class %s extends %s ", self.access, self.name,
                  self.superclass)
        if len(self.interfaces) > 0:
            out.write("implements %s ", COMMASPACE.join(self.interfaces))
        self.body.writeSource(out)

    def __repr__(self):
        return "Class(%s, %s, %s, %s, %s)" % (self.name, self.access,
                                              self.superclass,
                                              self.interfaces, self.body)


class Method(Statement):
    def __init__(self, name, access, args, body, throws=[]):
        self.name = name
        self.access = access
        self.args = args
        self.body = body
        self.throws = throws

    def writeSource(self, out):
        argtext = []
        for type, name in self.args[1:]:
            argtext.append(type+" "+name)
        if len(self.throws) == 0:
            throwstext = ""
        else:
            throwstext = " throws " + COMMASPACE.join(self.throws)

        out.write("%s %s %s(%s)%s ", self.access, self.args[0], self.name,
                  COMMASPACE.join(argtext), throwstext)
        self.body.writeSource(out)
        out.writeln()

    def __repr__(self):
        return "Method(%s, %s, %s, %s)" % (self.name, self.access,
                                           self.args, self.body)


class Constructor(Statement):
    def __init__(self, name, access, args, body, throws=[]):
        self.name = name
        self.access = access
        self.args = args
        self.body = body
        self.throws = throws

    def writeSource(self, out):
        argtext = []
        for type, name in self.args:
            argtext.append(type+" "+name)
        if len(self.throws) == 0:
            throwstext = ""
        else:
            throwstext = " throws " + COMMASPACE.join(self.throws)

        out.write("%s %s(%s)%s ", self.access, self.name,
                  COMMASPACE.join(argtext), throwstext)
        self.body.writeSource(out)
        out.writeln()

    def __repr__(self):
        return "Constructor(%s, %s, %s, %s)" % (self.name, self.access,
                                                self.args, self.body)



class BlankLine(Statement):
    def writeSource(self, out):
        out.writeln()

    def __repr__(self):
        return "BlankLine()"

Blank = BlankLine()



class Import(Statement):
    def __init__(self, package):
        self.package = package

    def writeSource(self, out):         
        out.writeln("import %s;", self.package)

    def __repr__(self):
        return "Import(%s)" % (self.package)



class SimpleComment(Statement):
    """ ??? """
    def __init__(self, text):
        self.text = text

    def writeSource(self, out):         
        out.writeln("// "+self.text)

    def __repr__(self):
        return "SimpleComment(%s)" % (self.text)



class Comment(Statement):
    """ ??? """
    def __init__(self, text):
        self.text = text

    def writeSource(self, out):
        lines = self.text.split('\n')
        if len(lines) == 1:
            out.writeln("/* %s */", self.text)
            return

        out.writeln("/* %s", lines[0])
        for line in lines[1:-1]:
            out.writeln(line.replace("\\", "\\\\"))
        out.writeln("%s */", lines[-1])

    def __repr__(self):
        return "Comment(%s)" % (self.package)



class Declare(Statement):
    def __init__(self, type, name, value=None):
        self.type = type
        self.name = name
        self.value = value

    def writeSource(self, out):
        if self.value is not None:
            out.writeln("%s %s = %s;", self.type, self.name.sourceString(),
                        self.value.sourceString())
        else:
            out.writeln("%s %s;", self.type, self.name.sourceString())

    def __repr__(self):
        return "Declare(%s, %s, %s)" % (self.type, self.name, self.value)



class Pass(Statement):
    def writeSource(self, out):
        pass

    def __repr__(self):
        return "Pass()"



def flatten(lst):
    ret = []
    for item in lst:
        if isinstance(item, type(lst)):
            ret.extend(flatten(item))
        elif isinstance(item, Block):
            ret.extend(item.code)
        else:
            ret.append(item)
    return ret



class FreeBlock(Statement):
    def __init__(self, code=None):
        if code is None:
            code = []
        self.code = flatten(code)
        self.locals = {}
        self.tempindex = 0

    def addlocal(self, name, type):
        self.locals[name] = type

    def writeSource(self, out):
        for name, type in self.locals.items():
            tn = type.typename
            if tn == 'PyObject':
                out.writeln('%s %s = null;', tn, name)
            elif tn == 'int':
                out.writeln('%s %s = 0;', tn, name)
            elif tn == 'char':
                out.writeln("%s %s = '\\000';", tn, name)
            else:
                out.writeln('%s %s;', tn, name)

        if len(self.locals) > 0:
            out.writeln()

        for stmt in self.code:
            #print stmt
            stmt.writeSource(out)
            if stmt.exits():
                break

    def exits(self):
        for i in range(len(self.code)):
            if not hasattr(self.code[i], 'exits'):
                print self.code[i]
                print 'oops'
                continue
            if self.code[i].exits():
                 del self.code[i+1:]
                 return 1
        return 0

    def __repr__(self):
        return "FreeBlock(%s)" % (self.code)



class Block(FreeBlock):
    def writeSource(self, out):
        out.beginBlock()
        FreeBlock.writeSource(self, out)
        out.endBlock()

    def __repr__(self):
        return "Block(%s)" % (self.code)



class TryCatch(Statement):
    def __init__(self, body, exctype, excname, catchbody):
        self.body = body
        self.exctype = exctype
        self.excname = excname
        self.catchbody = catchbody

    def writeSource(self, out):
        out.write("try ")
        self.body.writeSource(out)
        out.write("catch (%s %s) ", self.exctype, self.excname.sourceString())
        self.catchbody.writeSource(out)

    def exits(self):
        return self.body.exits() and self.catchbody.exits()



class TryCatches(Statement):
    def __init__(self, body, catches):
        self.body = body
        self.catches = catches

    def writeSource(self, out):
        out.write("try ")
        self.body.writeSource(out)
        for exctype, excname, catchbody in self.catches:
            out.write("catch (%s %s) ", exctype, excname.sourceString())
            catchbody.writeSource(out)

    def exits(self):
        r = self.body.exits()
        for exctype, excname, catchbody in self.catches:
            r = r and catchbody.exits()
        return r


class TryFinally(Statement):
    def __init__(self, body, finalbody):
        self.body = body
        self.finalbody = finalbody

    def writeSource(self, out):
        out.write("try ")
        self.body.writeSource(out)
        out.write("finally ")
        self.finalbody.writeSource(out)

    def exits(self):
        return self.body.exits() or self.finalbody.exits()



class If(Statement):
    def __init__(self, test, thenBody, elseBody=None):
        self.test = test
        self.thenBody = thenBody
        self.elseBody = elseBody

    def writeSource(self, out):
        out.write("if (%s) ", self.test.sourceString())
        self.thenBody.writeSource(out)
        if self.elseBody is not None:
            out.write("else ")
            self.elseBody.writeSource(out)

    def exits(self):
        if self.elseBody is None:
            return 0
        return self.thenBody.exits() and self.elseBody.exits()



class MultiIf(Statement):
    def __init__(self, tests, elseBody=None):
        self.tests = tests
        self.elseBody = elseBody

    def writeSource(self, out):
        for i in range(len(self.tests)):
            test, body = self.tests[i]
            if i > 0:
                out.write("else ")

            out.write("if (%s) ", test.sourceString())
            body.writeSource(out)
        if self.elseBody is not None:
            out.write("else ")
            self.elseBody.writeSource(out)

    def exits(self):
        if self.elseBody is None:
            return 0
        if not self.elseBody.exits():
            return 0
        for test, body in self.tests:
            if not body.exits():
                return 0
        return 1



class While(Statement):
    def __init__(self, test, body):
        self.test = test
        self.body = body

    def writeSource(self, out):
        out.write("while (%s) ", self.test.sourceString())
        self.body.writeSource(out)



class WhileElse(Statement):
    def __init__(self, test, body, else_body, while_temp):
        self.test = test
        self.body = body
        self.else_body = else_body
        self.while_temp = while_temp

    def writeSource(self, out):
        out.write("while (%s=%s) ", self.while_temp.sourceString(),
                  self.test.sourceString())
        self.body.writeSource(out)
        out.write("if (!%s) ", self.while_temp.sourceString())
        self.else_body.writeSource(out)



class Switch(Statement):
    def __init__(self, index, cases, defaultCase=None):
        self.index = index
        self.cases = cases
        self.defaultCase = defaultCase

    def writeSource(self, out):
        out.write("switch (%s)", self.index.sourceString())
        out.beginBlock()
        for label, code in self.cases:
            out.writeln("case %s:", label.sourceString())
            code.writeSource(out)
        if self.defaultCase is not None:
            out.writeln("default:")
            self.defaultCase.writeSource(out)
        out.endBlock()



class Return(Statement):
    def __init__(self, value=None):
        self.value = value

    def writeSource(self, out):
        if self.value is None:
            out.writeln("return;")
        else:
            out.writeln("return %s;", self.value.sourceString())

    def exits(self):
        return 1



class Throw(Statement):
    def __init__(self, value):
        self.value = value

    def writeSource(self, out):
        out.writeln("throw %s;", self.value.sourceString())

    def exits(self):
        # Interesting question...
        return 1



class Break(Statement):
    def writeSource(self, out):
        out.writeln("break;")

class Continue(Statement):
    def writeSource(self, out):
        out.writeln("continue;")



class Expression:
    def __repr__(self):
        return self.sourceString()

    def safeSourceString(self):
        return self.sourceString()

    def writeSource(self, out):
        out.writeln(self.sourceString()+";")

    def _calcargs(self):
        args = []
        for a in self.args:
            try:
                ss = a.sourceString()
            except AttributeError, msg:
                print '=====>', msg
                print '=====>', a.__class__
                print '=====>', a
                raise
            args.append(ss)
        return COMMASPACE.join(args)

    def exits(self):
        return 0



class UnsafeExpression(Expression):
    def safeSourceString(self):
        return "("+self.sourceString()+")"      



class Cast(UnsafeExpression):
    def __init__(self, totype, value):
        self.totype = totype
        self.value = value

    def sourceString(self):
        return "(%s)%s" % (self.totype, self.value.safeSourceString())



class Set(UnsafeExpression):
    def __init__(self, lvalue, value):
        self.lvalue = lvalue
        self.value = value

    def sourceString(self):
        return "%s = %s" % (self.lvalue.safeSourceString(),
                            self.value.safeSourceString())      



class Constant(Expression):
    def __init__(self, value):
        self.value = value

    def sourceString(self):
        return repr(self.value)


class IntegerConstant(Constant):
    pass

class FloatConstant(Constant):
    pass

class CharacterConstant(Constant):
    pass



class StringConstant(Constant):
    def sourceString(self):
        ret = ['"']
        for c in self.value:
            oc = ord(c)
            if c == '"':
                ret.append('\\"')
            elif c == '\\':
                ret.append('\\\\')
            elif oc >= 032 and oc <= 0177:
                ret.append(c)
            elif oc <= 0377:
                ret.append("\\%03o" % oc)
            else:
                ret.append("\\u%04x" % oc)
        ret.append('"')
        return ''.join(ret)



class Operation(UnsafeExpression):
    def __init__(self, op, x, y=None):
        self.op = op
        self.x = x
        self.y = y

    def sourceString(self):
        if self.y is None:
            return "%s%s" % (self.op, self.x.safeSourceString())
        else:
            return "%s %s %s" % (self.x.safeSourceString(), self.op,
                                 self.y.safeSourceString())


class TriTest(UnsafeExpression):
    def __init__(self, test, x, y):
        self.test = test
        self.x = x
        self.y = y

    def sourceString(self):
        return "%s ? %s : %s" % (self.test.safeSourceString(),
                                 self.x.safeSourceString(),
                                 self.y.safeSourceString())



class PostOperation(UnsafeExpression):
    def __init__(self, x, op):
        self.op = op
        self.x = x

    def sourceString(self):
        return "%s%s" % (self.x.safeSourceString(), self.op)


class Subscript(UnsafeExpression):
    def __init__(self, x, ind):
        self.ind = ind
        self.x = x

    def sourceString(self):
        return "%s[%s]" % (self.x.safeSourceString(), self.ind)



class InvokeLocal(Expression):
    def __init__(self, name, args):
        self.name = name
        self.args = args

    def sourceString(self):
        return '%s(%s)' % (self.name, self._calcargs())



class Invoke(Expression):
    def __init__(self, this, name, args):
        self.name = name
        self.this = this
        self.args = args

    def sourceString(self):
        return '%s.%s(%s)' % (self.this.safeSourceString(),
                              self.name, self._calcargs())



class InvokeStatic(Expression):
    def __init__(self, this, name, args):
        self.name = name
        self.this = this
        self.args = args

    def sourceString(self):
        return '%s.%s(%s)' % (self.this, self.name, self._calcargs())



class GetInstanceAttribute(Expression):
    def __init__(self, this, name):
        self.name = name
        self.this = this

    def sourceString(self):
        return "%s.%s" % (self.this.sourceString(), self.name)



class GetStaticAttribute(Expression):
    def __init__(self, this, name):
        self.name = name
        self.this = this

    def sourceString(self):
        return "%s.%s" % (self.this, self.name)



class SetInstanceAttribute(Expression):
    def __init__(self, this, name, value):
        self.name = name
        self.this = this
        self.value = value

    def sourceString(self):
        return "%s.%s = %s" % (self.this.sourceString(), self.name,
                               self.value.sourceString())



class SetStaticAttribute(Expression):
    def __init__(self, this, name, value):
        self.name = name
        self.this = this
        self.value = value

    def sourceString(self):
        return "%s.%s = %s" % (self.this, self.name, self.value.sourceString())



class Identifier(Expression):
    def __init__(self, name):
        self.name = name

    def sourceString(self):
        return str(self.name)



class New(Expression):
    def __init__(self, name, args):
        self.name = name
        self.args = args

    def sourceString(self):
        return 'new %s(%s)' % (self.name, self._calcargs())



class FilledArray(Expression):
    def __init__(self, type, values):
        self.type = type
        self.values = values

    def sourceString(self):
        return 'new %s[] {%s}' % (
            self.type,
            COMMASPACE.join(map(lambda arg: arg.sourceString(), self.values)))



class NewArray(Expression):
    def __init__(self, mytype, dimensions):
        self.type = mytype
        if type(dimensions) == type(0):
            dimensions = [dimensions]
        self.dimensions = dimensions

    def sourceString(self):
        return 'new %s[%s]' % (
            self.type,
            ']['.join(map(lambda arg: str(arg), self.dimensions)))



class StringArray(FilledArray):
    def __init__(self, values):
        lst = []
        for value in values:
            if value is None:
                jv = Null
            else:
                jv = StringConstant(value)
            lst.append(jv)
        self.values = lst
        self.type = "String"



class ClassReference(Expression):
    def __init__(self, name):
        self.name = name

    def sourceString(self):
        return module.simpleClassName(self.name)



True = Identifier("true")
False = Identifier("false")
Null = Identifier("null")


##class SetIndex(Expression):
##    def __init__(self, lvalue, index, value):

##class SetAttribute(Expression):
##    def __init__(self, lvalue, name, value):



# Python also has a complicated set sequence form here, but that's just sugar
# for ???

if __name__ == '__main__':
    from Output import SourceFile
    out = SourceFile('Foo')

    x = Identifier('x')
    one = IntegerConstant(1)
    s = While(Operation('<', x, one), Set(x, Operation('+', x, one)))
    s.writeSource(out)

    print out

"""
public static int Func1(char CharPar1, char CharPar2) {
    char CharLoc1 = CharPar1;
    char CharLoc2 = CharLoc1;
    if (CharLoc2 != CharPar2) {
        return Ident1;
    } else {
        return Ident2;
    }
}
"""
