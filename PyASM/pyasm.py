import opcode, re

from org.python.newcompiler.pyasm import BytecodeVisitor as Visitor,\
    Operator, CodeFlags as Flags
from org.python.newcompiler.asm import OffsetTracer
from org.python.core.BytecodeLoader import makeCode
from org.objectweb import asm
from org.objectweb.asm import Type, Opcodes as Op
from org.objectweb.asm.commons import GeneratorAdapter, Method,\
    TableSwitchGenerator
from org.objectweb.asm.tree import analysis
from org.python import core

from jarray import array
import java

from java.io import PrintWriter
from java.lang.System import out
stdout = PrintWriter(out)

__debugging__ = False

def reallyContains(dct, item):
    if item in dct:
        keys = list(dct.keys())
        return item is keys[keys.index(item)]
    return False

def getType(clazz):
    if isinstance(clazz,(str,unicode)):
        return Type.getObjectType(clazz)
    else:
        return Type.getType(clazz)
def getArrayType(typ):
    if not isinstance(typ,Type):
        typ = getType(typ)
    return Type.getType("[%s" % typ.getDescriptor())

def stringArray(*lst):
    return array(lst,java.lang.String)

pyObjectType = getType(object)
pyFrameType = getType(core.PyFrame)
pyCodeType = getType(core.PyCode)
pyType = getType(core.Py)
pySysType = getType(core.PySystemState)
pyExceptionType = getType(core.PyException)
pyBooleanType = getType(bool)
pyTupleType = getType(tuple)
pyListType = getType(list)
pyDictType = getType(dict)
pyStringType = getType(str)
pyBuiltin = getType(core.__builtin__)
imp = getType(core.imp)
compilerResources = getType(core.NewCompilerResources)
objectType = getType(java.lang.Object)
stringType = getType(java.lang.String)
throwableType = getType(java.lang.Throwable)

class Switch(TableSwitchGenerator):
    def __init__(self, generator):
        self.__generator = generator
    def generateCase(self, key, end):
        self.__generator(key, end)
    def generateDefault(self):
        self.__generator(None, None)

class CodeReference(object):
    def __init__(self, name, index):
        self.__name = name
        self.index = index

    def getName(self):
        return self.__name

    def __repr__(self):
        return "CodeReference(%s, %s)" % (self.__name, self.index)

magicNumbers = {
    '1.5':   20121,
    '1.5.1': 20121,
    '1.5.2': 20121,
    '1.6':   50428,
    '2.0':   50823,
    '2.0.1': 50823,
    '2.1':   60202,
    '2.1.1': 60202,
    '2.1.2': 60202,
    '2.2':   60717,
    '2.3a0': 62011,
    '2.3a0': 62021,
    '2.3a0': 62011, # OBS!
    '2.4a0': 62041,
    '2.4a3': 62051,
    '2.4b1': 62061,
    '2.5a0': 62071,
    '2.5a0': 62081,
    '2.5a0': 62091,
    '2.5a0': 62092,
    '2.5b3': 62101,
    '2.5b3': 62111,
    '2.5c1': 62121,
    '2.5c2': 62131,
    '2.6a0': 62141,
    }

class ClassKeeper(object):
    asType = property(lambda self: Type.getType(
            "L%s;" % self.__name.replace('.','/')))
    def __init__(self, name):
        self.__name = name
        self.__cw = asm.ClassWriter(asm.ClassWriter.COMPUTE_MAXS
                                    # Frames need to be computed for
                                    # jvm1.6 features
                                    #+ asm.ClassWriter.COMPUTE_FRAMES
                                    )
        self.cv = self.__cw
        if __debugging__:
            ## add this line to get a printout of the generated java bytecode
            self.cv = asm.util.TraceClassVisitor(self.cv, stdout)
            ## add this line to get a printout of the java bytecode with offsets
            #self.cv = OffsetTracer(self.cv, stdout)
            ## add this line to verify that the code generation is well behaved
            #self.cv = asm.util.CheckClassAdapter(self.cv)
        self.cv.visit(Op.V1_4, Op.ACC_PUBLIC, name, None,
                      "org/python/core/PyFunctionTable",
                      stringArray("org/python/core/PyRunnable"))
        self.__filename = None
        self.__const = 0
        self.__constantPool = {}
        self.__complexConstants = {}
        self.__constantOrder = []
        self.__func = 0
        self.__functionTable = []

        self.clinit = GeneratorAdapter(Op.ACC_STATIC, Method.getMethod(
                "void <clinit> ()"), None, None, self.cv)
        self.clinit.visitCode()
        self.init = GeneratorAdapter(Op.ACC_PUBLIC, Method.getMethod(
                "void <init> (String)"), None, None, self.cv)
        self.init.visitCode()
        self.init.loadThis()
        self.init.invokeConstructor(Type.getType(core.PyFunctionTable),
                               Method.getMethod("void <init> ()"))
        #self.cv.visitField(Op.ACC_PUBLIC + Op.ACC_FINAL, '__file__',
        #                   Type.getType(java.lang.String).getDescriptor(),
        #                   None, None)
        #self.init.loadThis()
        #self.init.loadArg(0)
        #self.init.putField(self.asType, '__file__',
        #                   Type.getType(java.lang.String))

        
    def isCodeConstant(self, name):
        return name in self.__functionTable

    def func2method(self, name):
        if name == '?':
            return 'main$code'
        return name.replace('<','').replace('>','')

    def codeField(self, id, name):
        return "%s$%s" % (self.func2method(name), id)
    
    def newFunction(self, name):
        name = self.codeField(self.__func, name)
        self.__functionTable.append(name)
        self.__lastCode = CodeReference(name, self.__func)
        self.__func += 1
        self.cv.visitField(Op.ACC_STATIC + Op.ACC_PRIVATE,
                           name, getType(core.PyCode).getDescriptor(),
                           None, None)
        ## generate a field for the constants of the code object
        #self.cv.visitField(Op.ACC_STATIC + Op.ACC_PUBLIC, name + "$co_consts",
        #                   getArrayType(core.PyObject).getDescriptor(),
        #                   None, None)
        return GeneratorAdapter(Op.ACC_PRIVATE, Method.getMethod(
                "org.python.core.PyObject %s (org.python.core.PyFrame)"%name),
                                None, None, self.cv)
    
    def getCodeReference(self):
        return self.__lastCode

    def getConstant(self, value):
        if value in self.__constantPool:
            return self.__constantPool[value]
        if isinstance(value,(list,dict,set)):
            raise TypeError("PyASM cannot handle mutable constants (yet).")
        if isinstance(value,(tuple,frozenset)):
            parts = []
            for part in value:
                if reallyContains(self.constants,part):
                    parts.append(part)
                else:
                    parts.append(self.getConstant(part))
            self.__complexConstants[value] = parts
        name = "c$$%s" % self.__const
        self.__const += 1
        self.cv.visitField(Op.ACC_PRIVATE + Op.ACC_STATIC + Op.ACC_FINAL,
                           name, pyObjectType.getDescriptor(), None, None)
        self.__constantPool[value] = name
        self.__constantOrder.append(value)
        return name

    def getCode(self, mainName):
        self.generateSupport(mainName)
        self.cv.visitEnd()
        byteArray = self.__cw.toByteArray()
        cn = asm.tree.ClassNode()
        ## add this line to verify the generated java bytecode
        #asm.ClassReader(byteArray).accept(cn, 0)

        error = False

        # Choose the level of error checking in the bytecode verification
        #analyzer = analysis.Analyzer(analysis.BasicInterpreter())
        analyzer = analysis.Analyzer(analysis.BasicVerifier())
        #### These don't seem to work
        ###analyzer = analysis.Analyzer(analysis.SimpleVerifier())
        ###analyzer = analysis.Analyzer(analysis.SourceInterpreter())
        for m in cn.methods:
            # analyze the bytecode of each method and output the bytecode,
            # with offsets, when an error is detected.
            try:
                analyzer.analyze(self.__name, m)
            except analysis.AnalyzerException, ae:
                print 'Error in "%s %s": %s' % (m.name, m.desc, ae)
                mp = OffsetTracer.createTMV(len(str(m.instructions.size())))
                m.accept(mp)
                getattr(mp,'print')( stdout )
                stdout.flush()
                error = True
        if error:
            raise RuntimeError, "An error occured, see output"
        
        return makeCode(self.__name, byteArray, self.__filename)

    def setFilename(self, filename):
        if self.__filename is not None:
            assert filename == self.__filename
        else:
            self.__filename = filename
            self.cv.visitSource(filename, None)

    def generateSupport(self, mainName):
        self.initConstants()

        self.clinit.returnValue()
        self.clinit.endMethod()
        self.init.returnValue()
        self.init.endMethod()
        
        getMain = GeneratorAdapter(Op.ACC_PUBLIC, Method.getMethod(
                "org.python.core.PyCode getMain ()"),
            None, None, self.cv)
        getMain.visitCode()
        getMain.getStatic(self.asType, mainName, pyCodeType)
        getMain.returnValue()
        getMain.endMethod()

        call_function = GeneratorAdapter( Op.ACC_PUBLIC, Method.getMethod(
                "org.python.core.PyObject call_function (%s)" % ", ".join(
                    ['int', 'org.python.core.PyFrame'])),
            None, None, self.cv)
        call_function.visitCode()
        call_function.loadThis()
        call_function.loadArg(1) # the frame
        call_function.loadArg(0) # the function id
        def switch(key, end):
            if key is not None:
                call_function.invokeVirtual(self.asType, Method.getMethod(
                        "org.python.core.PyObject %s (org.python.core.PyFrame)"
                        % self.__functionTable[key]))
                call_function.returnValue()
            else:
                call_function.visitInsn(Op.ACONST_NULL)
                call_function.returnValue()
        call_function.tableSwitch(range(self.__func), Switch(switch))
        call_function.endMethod()

    constants = {
        None:          ("None",          pyObjectType),
        False:         ("False",         pyBooleanType),
        True:          ("True",          pyBooleanType),
        StopIteration: ("StopIteration", pyObjectType),
        Ellipsis:      ("Ellipsis",      pyObjectType),
        ():            ("EmptyTuple",    pyTupleType),
        "":            ("EmptyString",   pyStringType),
        }
    __compositTypes = {
        tuple:     pyTupleType,
        list:      pyListType,
        dict:      pyDictType,
        set:       pyTupleType,
        frozenset: pyTupleType,
        }
    __compositConstructor = Method.getMethod(
        "void <init> (org.python.core.PyObject[])")
    __setConstructor = Method.getMethod(
        "void <init> (org.python.core.PyObject)")
    __constantFactory = {
        int:     Method.getMethod(
            "org.python.core.PyObject newInteger (long)"),
        float:   Method.getMethod(
            "org.python.core.PyFloat newFloat (double)"),
        str:     Method.getMethod(
            "org.python.core.PyString newString (String)"),
        unicode: Method.getMethod(
            "org.python.core.PyUnicode newUnicode (String)"),
        }
    def initConstants(self):
        # Possible constants that marshal loads:
        # NULL - raise error, not valid outside of marshal
        # None - simple singleton => can even be optimized at load time
        # boolean values: True/False - simple singleton
        # StopIteration - simple singleton
        # Ellipsis - simple singleton
        # int - immutable
        # float - immutable
        # complex - immutable
        # long - immutable
        # string - immutable
        # unicode - immutable
        # code - allready in place...
        # tuple - build from other constants
        # list - !MUTABLE! - build from other constants
        # dict - !MUTABLE! - build from other constants
        # set - !MUTABLE! - build from other constants
        # frozenset - build from other constants
        for value in self.__constantOrder:
            name = self.__constantPool[value]
            if reallyContains(self.constants, value):
                raise ValueError("Constant %s:%s slipped through!" %
                                 (name,value))
            if type(value) in self.__compositTypes:
                if isinstance(value, (set,frozenset)):
                    self.clinit.newInstance(getType(type(value)))
                    self.clinit.dup()
                self.clinit.newInstance(self.__compositTypes[type(value)])
                self.clinit.dup()
                self.clinit.push(java.lang.Integer(len(value)))
                self.clinit.newArray(pyObjectType)
                i = 0
                for part in self.__complexConstants[value]:
                    self.clinit.dup() # array
                    self.clinit.push(java.lang.Integer(i)) # index
                    if reallyContains(self.constants, part):
                        partName, partType = self.constants[part]
                        self.clinit.getStatic(pyType, partName, partType)
                    else:
                        self.clinit.getStatic(self.asType, part, pyObjectType)
                    self.clinit.arrayStore(pyObjectType) # store
                    i += 1
                self.clinit.invokeConstructor(
                    self.__compositTypes[type(value)],
                    self.__compositConstructor)
                if isinstance(value, (set,frozenset)):
                    self.clinit.invokeConstructor(getType(type(value)),
                                                  self.__setConstructor)
            elif isinstance(value, complex):
                self.clinit.newInstance(getType(complex))
                self.clinit.dup()
                self.clinit.push(value.real)
                self.clinit.push(value.imag)
                self.clinit.invokeConstructor(
                    getType(complex), Method.getMethod(
                        "void <init> (double, double)"))
            elif isinstance(value, long):
                self.clinit.push(str(value))
                self.clinit.invokeStatic(pyType, Method.getMethod(
                        "org.python.core.PyObject newLong (String)"))
            elif type(value) in self.__constantFactory:
                self.clinit.push(value)
                self.clinit.invokeStatic(
                    pyType, self.__constantFactory[type(value)])
            else:
                raise TypeError("Invalid constant type %s (%s)" %
                                (type(value), value))
            self.clinit.putStatic(self.asType, name, pyObjectType)

# FIXME: The block objects need to be refactored, se notes in ASMVisitor code.
# FIXME: The contract of the block objects needs to be thoroughly documented.
class Block(object):
    def __init__(self, asm, stackSize=0, *stateVariables):
        self.asm = asm
        self.stackSize = stackSize
        self.stateVariables = stateVariables
    nestingStackSize = property(lambda self: self.stackSize)
    def end(self):
        """Called when the block scope ends."""
        for variable in self.stateVariables:
            variable.end()
    def exit(self,hasState=None):
        """Called when a return, yeild, continue or break instruction
        terminates the block.
        hasState is True when there is a variable on the stack"""
        return False
    def loadState(self):
        """load the stack state that this block has persisted into variables
        onto the stack."""
        pass
    def storeState(self):
        """store the stack state that this block persists into variables from
        the stack into the persistance variables."""
        pass
    def exclude(self,start,end):
        pass

class LoopBlock(Block):
    def __init__(self, asm, endLabel):
        # endLabel is an ASM label
        Block.__init__(self, asm)
        self.endLabel = endLabel
    def end(self):
        #self.asm.visitLabel(self.endLabel)
        Block.end(self)

class ForBlock(Block):
    def __init__(self, asm):
        Block.__init__(self, asm, 1)
    def exit(self, hasState=None):
        self.asm.pop() # pop the iterator from the stack.
        return Block.exit(self, hasState)

class TryBlock(Block):
    def __init__(self, asm, catch, endLabel, handlerBlock, stackPersistence,
                 stackSize=0, *stateVariables):
        # endLabel is an ASM label
        Block.__init__(self, asm, stackSize, *stateVariables)
        self.__catch = catch
        self.endLabel = endLabel
        self.handlerBlock = handlerBlock
        self.__stackPersistence = stackPersistence
    surroundingStackSize = property(lambda self: len(self.__stackPersistence))
    nestingStackSize = property(lambda self: (self.stackSize -
                                              self.surroundingStackSize))
    def end(self):
        self.asm.visitLabel(self.endLabel)
        Block.end(self)
    def endVariables(self):
        for variable in self.__stackPersistence:
            variable.end()
    def loadState(self):
        for variable in self.__stackPersistence:
            variable.load()
    def storeState(self):
        for i in xrange(len(self.__stackPersistence) -1,-1,-1):
            variable = self.__stackPersistence[i]
            variable.store()
    def exclude(self, start, end):
        self.__catch.exclude(start, end)

class HandlerBlock(Block):
    def __init__(self, asm, sourceBlock, stackSize=0, *stateVariables):
        Block.__init__(self, asm, stackSize, *stateVariables)
        self.sourceBlock = sourceBlock
    def end(self):
        self.sourceBlock.loadState()
        self.sourceBlock.endVariables()

class TryExceptBlock(TryBlock):
    def __init__(self, asm, catch, endLabel, stackPersistence):
        # endLabel is an ASM label
        TryBlock.__init__(
            self, asm, catch, endLabel, ExceptBlock(asm, self),
            stackPersistence)
    def end(self):
        TryBlock.end(self)
        self.loadState()

class ExceptBlock(HandlerBlock):
    def __init__(self, asm, sourceBlock):
        HandlerBlock.__init__(self, asm, sourceBlock, 3)
    nestingStackSize = property(lambda self: self.stackSize - 3)
    def end(self):
        """stack is: ... exc_traceback exc_value exc_type"""
        self.asm.dupX2()
        self.asm.pop()
        self.asm.swap()
        self.asm.invokeStatic(pyType, Method.getMethod(
                "org.python.core.PyException makeException (%s)" % ", ".join([
                    "org.python.core.PyObject"]*3)))
        self.asm.throwException()
        HandlerBlock.end(self)

class TryFinallyBlock(TryBlock):
    def __init__(self, asm, catch, endLabel, handlerLabel, afterLabel,
                 retVariable, stackPersistence):
        # Labels are ASM labels
        TryBlock.__init__(
            self, asm, catch, endLabel, 
            FinallyBlock(asm,self,afterLabel,retVariable),
            stackPersistence, 0, retVariable)
        self.handlerLabel = handlerLabel
    retVariable = property(lambda self: self.handlerBlock.retVariable)
    resumeLabels = property(lambda self: self.handlerBlock.resumeLabels)
    def end(self):
        self.exit(False)
        TryBlock.end(self)
    def exit(self,hasState=True):
        """hasState is True if there is a (return-) variable on the stack."""
        self.asm.push(java.lang.Integer(len(self.resumeLabels)))
        self.retVariable.store()
        if not hasState:
            self.asm.visitInsn(Op.ACONST_NULL)
        label = self.asm.newLabel()
        self.resumeLabels.append(label)
        self.asm.goTo(self.handlerLabel)
        self.asm.visitLabel(label)
        if not hasState:
            self.asm.pop()
        return True

class FinallyBlock(HandlerBlock):
    def __init__(self, asm, sourceBlock, afterLabel, retVariable):
        HandlerBlock.__init__(self, asm, sourceBlock, 1, retVariable)
        self.afterLabel = afterLabel
        self.retVariable = retVariable
        self.resumeLabels = []
    nestingStackSize = property(
        lambda self: self.stackSize - self.sourceBlock.surroundingStackSize)
    def end(self):
        throw = self.asm.newLabel()
        if self.resumeLabels:
            reRaise = self.asm.newLabel()
            self.retVariable.load()
            self.retVariable.end()
            self.asm.visitTableSwitchInsn(0, len(self.resumeLabels)-1, reRaise,
                                          array(self.resumeLabels, asm.Label))
            self.asm.visitLabel(reRaise)
        self.asm.dup()
        self.asm.instanceOf(throwableType)
        self.asm.visitJumpInsn(Op.IFNE, throw)
        self.asm.pop()
        self.asm.goTo(self.afterLabel)
        self.asm.visitLabel(throw)
        self.asm.checkCast(throwableType)
        self.asm.throwException()
        self.asm.visitLabel(self.afterLabel)
        HandlerBlock.end(self)


class LocalVariable(object):
    def __init__(self, asm, varType, name):
        self.asm = asm
        self.varType = varType
        self.name = name
        varno = self.__varno = asm.newLocal(varType)
        #start = self.__start = asm.newLabel()
        #end = self.__end = asm.newLabel()
        #asm.visitLabel(start)
    def store(self):
        self.asm.storeLocal(self.__varno, self.varType)
    def load(self):
        self.asm.loadLocal(self.__varno, self.varType)
    def __iadd__(self, value):
        self.asm.iinc(self.__varno, value)
    def __int__(self):
        return self.__varno
    def end(self):
        #self.asm.visitLabel(self.__end)
        #self.asm.visitLocalVariable(self.name, self.varType.getDescriptor(),
        #                            None,self.__start,self.__end,self.__varno)
        pass
    def ret(self):
        self.asm.ret(self.__varno)
        self.end()

class TryCatch(object):
    def __init__(self, start, end, handle, excType):
        self.start = start
        self.end = end
        self.handle = handle
        self.excType = excType
        self.exclusions = []
    def exclude(self, start, end):
        self.exclusions.append((start,end))
    def accept(self, asm):
        start = self.start
        for exStart, exEnd in self.exclusions:
            asm.visitTryCatchBlock(start, exStart, self.handle, self.excType)
            start = exEnd
        asm.visitTryCatchBlock(start, self.end, self.handle, self.excType)

class ASMVisitor(Visitor):
    def __init__(self, magic, parent=None): # FIXME: change contract?
        if magic is None:
            magic = magicNumbers['2.5c2']
        self.__magic = magic
        self.__blocks = []
        self.__parent = parent
        if parent is None:
            self.__class = ClassKeeper("MODULE")
        else:
            self.__class = parent.__class
        self.__labels = {}
        self.__codeSchedule = {}
        self.__tryBlocks = []

    def visitCode(self, argcount, nlocals, stacksize, flags, constants, names,
                  varnames, freevars, cellvars, filename, name, firstlineno):
        # FIXME: change contract?
        self.__argcount = argcount   # long
        self.__nlocals = nlocals     # long  -- ignored ?
        self.__stacksize = stacksize # long  -- ignored ?
        self.__flags = flags         # long
        self.__constants = constants # (Py)Object[] - PyObject or coderef
        self.__names = names         # String[]
        self.__varnames = list(varnames)   # String[]
        self.__freevars = list(freevars)   # String[]
        self.__cellvars = list(cellvars)   # String[]
        self.__filename = filename   # String
        self.__name = name           # String
        self.__firstlineno = firstlineno # long

        self.__class.setFilename(filename)

        self.asm = self.__class.newFunction(name)

        for variableName in self.__cellvars:
            if variableName not in self.__varnames:
                continue
            self.loadFrame()
            self.push(self.__varnames.index(variableName))
            self.derefIndex(variableName)
            self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                    "void to_cell (int, int)"))

        self.asm.visitCode()
        self.__code = self.__class.getCodeReference()

    def label(self, pyLabel=None):
        if pyLabel is None:
            return self.asm.newLabel()
        elif pyLabel in self.__labels:
            return self.__labels[pyLabel]
        else:
            label = self.asm.newLabel()
            self.__labels[pyLabel] = label
            return label

    def newLocal(self, varType, name):
        return LocalVariable(self.asm, varType, name)

    def scheduleCode(self, label, code=None):
        """Schedule a code generating function to be executed after visiting
        a particular label.
        If code is None, this method returns a decorator..."""
        def addCode(code):
            if label.visited:
                raise RuntimeError("The label '%s' has already been visited."
                                   " Cannot schedule code." % label)
            self.__codeSchedule.setdefault(label, []).append(code)
        if code is None:
            return addCode
        else:
            addCode(code)

    def push(self, value):
        if isinstance(value, int):
            value = java.lang.Integer(value)
        self.asm.push(value)

    def loadFrame(self):
        self.asm.loadArg(0)


    def getCode(self): # FIXME: change contract?
        if self.__parent is None:
            return self.__class.getCode(self.__code.getName())
        else:
            return self.__code

    def getName(self, index):
        return self.__names[index]

    def getVariableName(self, index):
        return self.__varnames[index]

    def getOuterName(self, index):
        if index < len(self.__cellvars):
            return self.__cellvars[index]
        else:
            return self.__freevars[index - len(self.__cellvars)]

    def getConstant(self, index):
        return self.__constants[index]

    binaryOperator = {Operator.ADD:          '_add',
                      Operator.SUBTRACT:     '_sub',
                      Operator.MULTIPLY:     '_mul',
                      Operator.DIVIDE:       '_div',
                      Operator.FLOOR_DIVIDE: '_floordiv',
                      Operator.TRUE_DIVIDE:  '_truediv',
                      Operator.MODULO:       '_mod',
                      Operator.POWER:        '_pow',
                      Operator.LSHIFT:       '_lshift',
                      Operator.RSHIFT:       '_rshift',
                      Operator.AND:          '_and',
                      Operator.OR:           '_or',
                      Operator.XOR:          '_xor',
                      Operator.SUBSCRIPT:    '__getitem__',}
    def visitBinaryOperator(self, operator):
        """TOS1, TOS -- TOS1 op TOS"""
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject %s (org.python.core.PyObject)" %
                self.binaryOperator[operator]))

    inplaceOperator = {Operator.ADD:          '__iadd__',
                       Operator.SUBTRACT:     '__isub__',
                       Operator.MULTIPLY:     '__imul__',
                       Operator.DIVIDE:       '__idiv__',
                       Operator.FLOOR_DIVIDE: '__ifloordiv__',
                       Operator.TRUE_DIVIDE:  '__itruediv__',
                       Operator.MODULO:       '__imod__',
                       Operator.POWER:        '__ipow__',
                       Operator.LSHIFT:       '__ilshift__',
                       Operator.RSHIFT:       '__irshift__',
                       Operator.AND:          '__iand__',
                       Operator.OR:           '__ior__',
                       Operator.XOR:          '__ixor__',}
    def visitInplaceOperator(self, operator):
        """ -- """
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject %s (org.python.core.PyObject)" %
                self.inplaceOperator[operator]))

    unaryOperator = {Operator.INVERT:   '__invert__',
                     Operator.POSITIVE: '__pos__',
                     Operator.NEGATIVE: '__neg__',
                     Operator.NOT:      '__not__',
                     Operator.CONVERT:  '__repr__',}
    def visitUnaryOperator(self, operator):
        """value -- (op value)"""
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject %s ()" %
                self.unaryOperator[operator]))

    compareOperator = {Operator.LESS_THAN: '_lt',
                       Operator.LESS_THAN_OR_EQUAL: '_le',
                       Operator.EQUAL: '_eq',
                       Operator.NOT_EQUAL: '_ne',
                       Operator.GREATER_THAN: '_gt',
                       Operator.GREATER_THAN_OR_EQUAL: '_ge',
                       Operator.IN: '_in',
                       Operator.NOT_IN: '_notin',
                       Operator.IS: '_is',
                       Operator.IS_NOT: '_isnot',}
    def visitCompareOperator(self, operator):
        """element, element -- bool"""
        if operator == Operator.EXCEPTION_MATCH:
            self.asm.swap()
            self.asm.invokeStatic(pyType, Method.getMethod(
                    "org.python.core.PyException makeException (%s)"%", ".join(
                        ['org.python.core.PyObject'])))
            self.asm.swap()
            self.asm.invokeStatic(pyType, Method.getMethod(
                    "boolean matchException (%s)" % ", ".join(
                        ['org.python.core.PyException',
                         'org.python.core.PyObject'])))
            self.asm.invokeStatic(pyType, Method.getMethod(
                    "org.python.core.PyBoolean newBoolean (boolean)"))
        else:
            self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                    "org.python.core.PyObject %s (org.python.core.PyObject)" %
                    self.compareOperator[operator]))

    def visitBuildClass(self):
        """name, bases, dict -- class"""
        sequenceType = getType(core.PySequenceList)
        bases = self.newLocal(getArrayType(pyObjectType), "bases")
        dict = self.newLocal(pyObjectType, "dict")
        dict.store()
        self.asm.checkCast(sequenceType)
        self.asm.invokeVirtual(sequenceType, Method.getMethod(
                "org.python.core.PyObject[] getArray ()"))
        bases.store()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "String toString ()"))
        bases.load(); bases.end()
        dict.load(); dict.end()
        self.asm.invokeStatic(compilerResources, Method.getMethod(
                "org.python.core.PyObject makeClass (%s)" % ", ".join(
                    ["String", "org.python.core.PyObject[]",
                     "org.python.core.PyObject"])))

    def buildArray(self, size, array):
        """helper method for buildSequence.
        Expects `size` elements on the stack, produces an array,
        returns the local value index for the array (NOT on stack)"""
        # store the values in the array
        for i in range(size -1,-1,-1):
            array.load()
            self.asm.swap()
            self.push(i)
            self.asm.swap()
            self.asm.arrayStore(pyObjectType)

    def visitUnpackSequence(self, count):
        """sequence -- (element, )*count"""
        self.push(count)
        self.asm.invokeStatic(pyType, Method.getMethod(
                "org.python.core.PyObject[] unpackSequence (%s)" % ", ".join(
                    ['org.python.core.PyObject', 'int'])))
        for i in range(count -1, -1, -1):
            if i != 0: self.asm.dup()
            self.push(i)
            self.asm.arrayLoad(pyObjectType)
            if i != 0: self.asm.swap()

    def buildSequence(self, seqType, size):
        """helper method for BuildTuple and BuildList"""
        # create an array
        self.push(size)
        self.asm.newArray(pyObjectType)
        # store it for reference
        array = self.newLocal(getArrayType(pyObjectType),"array")
        array.store()
        self.buildArray(size, array)
        # create a PyList/PyTuple from the array
        self.asm.newInstance(seqType)
        self.asm.dup()
        array.load()
        array.end()
        self.asm.invokeConstructor(seqType, Method.getMethod(
                "void <init> (org.python.core.PyObject[])"))

    def visitBuildTuple(self, size):
        """(element,) * size -- tuple"""
        self.buildSequence(getType(core.PyTuple), size)

    def visitBuildList(self, size):
        """(element,) * size -- list"""
        self.buildSequence(getType(core.PyList), size)

    def visitBuildMap(self, zero):
        """ -- dict"""
        assert zero == 0
        self.asm.newInstance(pyDictType)
        self.asm.dup()
        self.asm.invokeConstructor(Type.getType(core.PyDictionary),
                                   Method.getMethod("void <init> ()"))

    def visitBuildSlice(self, numargs):
        """start, stop[, step] -- slice"""
        if numargs == 3:
            pass
        elif numargs == 2:
            self.push(None)
        #elif numargs == 1:
        #    self.push(None)
        #    self.asm.swap()
        #    self.push(None)
        else:
            raise TypeError("Can only build slices from 2 or 3 arguments.")
        #start = self.newLocal(pyObjectType,"start")
        #stop = self.newLocal(pyObjectType,"stop")
        #step = self.newLocal(pyObjectType,"step")
        #step.store()
        #stop.store()
        #start.store()
        step = self.newLocal(pyObjectType,"step")
        step.store()
        self.asm.newInstance(Type.getType(core.PySlice))
        self.asm.dup()
        self.asm.dup2X2()
        self.asm.pop()
        self.asm.pop()
        #start.load()
        #stop.load()
        step.load()
        #start.end()
        #stop.end()
        step.end()
        self.asm.invokeConstructor(
            Type.getType(core.PySlice), Method.getMehtod(
                "void <init> (%s)" % ", ".join(["org.python.core.PyObject"]*3)))

    def setupCallParameters(self, num_pos, num_keyword, arguments, keywords):
        """helper for the various CallFunction methods."""
        for i in range(num_keyword -1,-1,-1):
            arguments.load()
            self.asm.swap()
            self.push(num_pos + i)
            self.asm.swap()
            self.asm.arrayStore(pyObjectType)
            self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                    "String toString ()"))
            keywords.load()
            self.asm.swap()
            self.push(i)
            self.asm.swap()
            self.asm.arrayStore(getType(java.lang.String))
        self.buildArray(num_pos, arguments)        

    def visitCallFunction(self, num_pos, num_keyword):
        """(arg,) * num_pos, (name, arg) * num_keyword -- value"""
        arguments = self.newLocal(getArrayType(pyObjectType),"arguments")
        keywords  = self.newLocal(getArrayType(getType(java.lang.String)),"kw")
        self.push(num_pos + num_keyword)
        self.asm.newArray(pyObjectType)
        arguments.store()
        self.push(num_keyword)
        self.asm.newArray(getType(java.lang.String))
        keywords.store()
        self.setupCallParameters(num_pos, num_keyword, arguments, keywords)
        arguments.load()
        keywords.load()
        arguments.end()
        keywords.end()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __call__ (%s)" % ", ".join([
                        "org.python.core.PyObject[]", "String[]"
                        ])))

    def visitCallFunctionKeyword(self, num_pos, num_keyword):
        """(arg,) * num_pos, (name, arg) * num_keyword, dict -- value"""
        self.push(None)
        self.asm.swap()
        self.visitCallFunctionVarargKeyword(num_pos, num_keyword)

    def visitCallFunctionVararg(self, num_pos, num_keyword):
        """(arg,) * num_pos, (name, arg) * num_keyword, list -- value"""
        self.push(None)
        self.visitCallFunctionVarargKeyword(num_pos, num_keyword)

    def visitCallFunctionVarargKeyword(self, num_pos, num_keyword):
        """(arg,) * num_pos, (name, arg) * num_keyword, list, dict -- value"""
        args = self.newLocal(pyObjectType,"args")
        kwargs = self.newLocal(pyObjectType,"kwargs")
        kwargs.store()
        args.store()
        arguments = self.newLocal(getArrayType(pyObjectType),"arguments")
        keywords  = self.newLocal(getArrayType(getType(java.lang.String)),"kw")
        self.push(num_pos + num_keyword)
        self.asm.newArray(pyObjectType)
        arguments.store()
        self.push(num_keyword)
        self.asm.newArray(getType(java.lang.String))
        keywords.store()
        self.setupCallParameters(num_pos, num_keyword, arguments, keywords)
        arguments.load()
        keywords.load()
        args.load()
        kwargs.load()
        args.end()
        kwargs.end()
        arguments.end()
        keywords.end()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject _callextra (%s)" % ", ".join([
                        "org.python.core.PyObject[]", "String[]",
                        "org.python.core.PyObject", "org.python.core.PyObject",
                        ])))

    def visitBreakLoop(self):
        """ -- """
        # FIXME: in_try_block is a great patch by Nicholas Riley, but should
        # probably be refactored so that it is handeled in the block object.
        in_try_block = False
        for i in xrange(len(self.__blocks) -1,-1,-1):
            block = self.__blocks[i]
            if isinstance(block, LoopBlock):
                self.asm.goTo(block.endLabel)
                break
            elif not in_try_block:
                block.exit(False)
            if isinstance(block, TryExceptBlock):
                in_try_block = True
        else: # No surrounding LoopBlock was found
            raise SyntaxError("break not properly nested in loop.")

    def visitContinueLoop(self, loopStart):
        """ -- """
        # FIXME: in_try_block is a great patch by Nicholas Riley, but should
        # probably be refactored so that it is handeled in the block object.
        in_try_block = False        
        for i in xrange(len(self.__blocks) -1,-1,-1):
            block = self.__blocks[i]
            if isinstance(block, LoopBlock):
                self.asm.goTo(self.label(loopStart))
                break
            elif not in_try_block:
                block.exit(False)
            if isinstance(block, TryExceptBlock):
                block.loadState()
                in_try_block = True
        else: # No surrounding LoopBlock was found
            raise SyntaxError("continue not properly nested in loop.")

    def visitDeleteAttribute(self, attributeName):
        """object -- """
        self.push(attributeName)
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "void __delattr__ (String)"))

    def visitDeleteFast(self, localName):
        """ -- """
        self.loadFrame()
        self.push(self.__varnames.index(localName))
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void dellocal (int)"))

    def visitDeleteGlobal(self, globalName):
        """ -- """
        self.loadFrame()
        self.push(globalName)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void delglobal (String)"))

    def visitDeleteName(self, name):
        """ -- """
        self.loadFrame()
        self.push(name)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void dellocal (String)"))

    def setupSlice(self, plus):
        """help method for (Load|Store|Delete)Slice"""
        if plus > 3: # CPython limit that we don't have to obey
            raise TypeError("Slices only support pluses in range(4).")
        if plus == 0: # stack: -
            self.push(None)
            self.push(None)
            self.push(None)
        elif plus == 1: # stack: start
            self.push(None)
            self.push(None)
        elif plus == 2: # stack: end
            self.push(None)
            self.asm.swap()
            self.push(None)
        elif plus == 3: # stack: start end
            self.push(None)
        ## FUTURE EXTENSION
        elif plus == 4: # stack: step
            self.push(None)
            self.asm.swap()
            self.push(None)
            self.asm.swap()
        elif plus == 5: # stack: start step
            self.push(None)
            self.asm.swap()
        elif plus == 6: # stack: end step
            self.push(None)
            self.rot(3)
        elif plus == 7: # stack: start end step
            pass
        else:
            raise TypeError("Slices only support pluses in range(8).")

    def visitDeleteSlice(self, plus):
        """object [,object[,object]] -- """
        self.setupSlice(plus)
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "void __delslice__ (%s)" % ", ".join(
                    ["org.python.core.PyObject"]*3)))

    def visitDeleteSubscript(self):
        """ object, index -- """
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "void __delitem__ (org.python.core.PyObject)"))

    __dupMax = 5
    def visitDup(self, numElements=1):
        """(item, )*numElements -- (item, )*numElements, (item, )*numElements"""
        if numElements < 1 or numElements > self.__dupMax:
            raise TypeError("DUP can only be performed on depths in range(1,%s)"
                            % (self.__dupMax + 1))
        if numElements == 1:
            self.asm.dup()
        elif numElements == 2:
            self.asm.dup2()
        elif numElements == 3:
            top = self.newLocal(pyObjectType,"top")
            top.store()
            self.asm.dup2()
            top.load()
            top.end()
            self.asm.dupX2()
        elif numElements == 4:
            top = self.newLocal(pyObjectType,"top")
            top2 = self.newLocal(pyObjectType,"top2")
            top.store()
            top2.store()
            self.asm.dup2()
            top2.load()
            top.load()
            top.end()
            top2.end()
            self.asm.dup2X2()
        else: # 5 and more is quite expensive on local variables. O(n)
            # Store all elements in variables
            vars = [self.newLocal(pyObjectType,"var%X"%x)
                    for x in range(numElements)]
            for var in vars:
                var.store()
            # reverse the order to assert correct stack order 
            # and load them twice...
            vars.reverse()
            for var in vars:
                var.load()
            for var in vars:
                var.load()
                var.end()

    def visitExecStatement(self):
        """code, globals, locals -- """
        self.asm.invokeStatic(pyType, Method.getMethod(
                "void exec (%s)" % ", ".join(["org.python.core.PyObject"]*3)))

    def visitForIteration(self, end):
        """iter -- iter value | """
        self.__blocks.append(ForBlock(self.asm))
        #@self.scheduleCode(end)
        def code():
            block = self.__blocks.pop()
            if not isinstance(block, ForBlock):
                raise TypeError("Illegal block state, expected ForBlock, not %s"
                                % type(block))
            block.end()
        self.scheduleCode(end, code)
        body = self.label()
        self.asm.dup()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __iternext__ ()"))
        self.asm.dup()
        self.asm.visitJumpInsn(Op.IFNONNULL, body)
        self.asm.pop2()
        self.asm.goTo(self.label(end))
        self.asm.visitLabel(body)

    def visitGetIterator(self):
        """obj -- iter(obj)"""
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __iter__ ()"))

    def visitImportFrom(self, name):
        """module -- module module.name"""
        self.asm.dup()
        self.push(name)
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __getattr__ (String)"))

    def visitImportName(self, name):
        """[absolutivity] fromlist -- module"""
        # FIXME: move this to the lib
        # Jython cannot handle the fromlist being None
        # here is an ugly hack to avoid that in runtime
        # something like this should be pushed to the lib
        ok = self.asm.newLabel()
        self.asm.dup()
        self.asm.instanceOf(pyTupleType)
        self.asm.visitJumpInsn(Op.IFNE, ok)
        self.asm.pop()
        self.asm.getStatic(pyType, 'EmptyTuple', pyTupleType)
        self.asm.visitLabel(ok)
        if self.__magic >= magicNumbers['2.5a0']: # has absolutivity
            pass
        else: # doesn't have absolutivity, add it
            self.push(-1) # old, standard behaviour
            self.asm.invokeStatic(pyType, Method.getMethod(
                    "org.python.core.PyInteger newInteger (int)"))
            self.asm.swap()
        # FIXME: ignoring absolutivity for now... Absolute import won't work
        self.asm.swap(); self.asm.pop() # <- ignore absolutivity for now
        self.push(name)
        self.asm.swap()
        self.loadFrame()
        self.asm.getField(pyFrameType, 'f_globals', pyObjectType)
        self.asm.swap()
        self.loadFrame()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getf_locals ()"))
        self.asm.swap()
        self.asm.invokeStatic(pyBuiltin, Method.getMethod(
                "org.python.core.PyObject __import__ (%s)" % ", ".join(
                    ["String", "org.python.core.PyObject",
                     "org.python.core.PyObject", "org.python.core.PyObject"])))

    def visitImportStar(self):
        """module -- """
        self.loadFrame()
        self.asm.invokeStatic(compilerResources, Method.getMethod(
                "void importAll (%s)" % ", ".join([
                        "org.python.core.PyObject",
                        "org.python.core.PyFrame"])))

    def visitJump(self, destination):
        """ -- """
        self.asm.goTo(self.label(destination))

    def visitJumpIfFalse(self, label):
        """value -- value"""
        self.asm.dup()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "boolean __nonzero__ ()"))
        self.asm.visitJumpInsn(Op.IFEQ, self.label(label))

    def visitJumpIfTrue(self, label):
        """value -- value"""
        self.asm.dup()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "boolean __nonzero__ ()"))
        self.asm.visitJumpInsn(Op.IFNE, self.label(label))

    def visitLabel(self, label):
        """ -- """
        label.visited = True
        self.asm.visitLabel(self.label(label))
        for code in self.__codeSchedule.get(label,()):
            code()

    def visitLineNumber(self, lineNo):
        """ -- """
        # add line number for java debuger
        label = self.label()
        self.asm.visitLabel(label)
        self.asm.visitLineNumber(lineNo, label)
        # add line number for jython frame
        self.loadFrame()
        self.push(lineNo)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void setline (int)"))

    def visitListAppend(self):
        """list element -- """
        self.asm.swap()
        self.asm.checkCast(pyListType)
        self.asm.swap()
        self.asm.invokeVirtual(pyListType, Method.getMethod(
                "void append (org.python.core.PyObject)"))

    def visitLoadAttribute(self, attributeName):
        """object -- object"""
        self.push(attributeName)
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __getattr__ (String)"))

    def derefIndex(self, variable):
        """Helper for LoadClosure, LoadDeref and StoreDeref"""
        if variable in self.__cellvars:
            self.push(self.__cellvars.index(variable))
        else:
            self.push(len(self.__cellvars) + self.__freevars.index(variable))

    def visitLoadClosure(self, variableName):
        """ -- cellvar"""
        self.loadFrame()
        self.derefIndex(variableName)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getclosure (int)"))

    def visitLoadDeref(self, variableName):
        """ -- object"""
        self.loadFrame()
        self.derefIndex(variableName)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getderef (int)"))

    def visitLoadConstant(self, const):
        """ -- object"""
        if reallyContains(self.__class.constants,const):
            constName, constType = self.__class.constants[const]
            self.asm.getStatic(pyType, constName, constType)
        elif isinstance(const, CodeReference):
            self.asm.getStatic(self.__class.asType, const.getName(), pyCodeType)
        else:
            self.asm.getStatic(self.__class.asType,
                               self.__class.getConstant(const),
                               pyObjectType)

    def visitLoadFast(self, localName):
        """ -- object"""
        self.loadFrame()
        self.push(self.__varnames.index(localName))
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getlocal (int)"))

    def visitLoadGlobal(self, globalName):
        """ -- object"""
        self.loadFrame()
        self.push(globalName)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getglobal (String)"))

    def visitLoadLocals(self):
        """ -- locals"""
        self.loadFrame()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getf_locals ()"))

    def visitLoadName(self, name):
        """ -- object"""
        self.loadFrame()
        self.push(name)
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "org.python.core.PyObject getname (String)"))

    def visitLoadSlice(self, plus):
        """object [,object[,object]] -- value"""
        self.setupSlice(plus)
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __getslice__ (%s)" % ", ".join(
                    ["org.python.core.PyObject"]*3)))

    def visitMakeClosure(self, numDefault):
        """(value, )*numDefault, tuple, code -- function"""
        functionType = getType(core.PyFunction)
        sequenceType = getType(core.PySequenceList)
        code = self.newLocal(pyCodeType,"code")
        cell = self.newLocal(sequenceType,"cell")
        code.store()
        cell.store()
        # create an array
        self.push(numDefault)
        self.asm.newArray(pyObjectType)
        # store it for reference
        default = self.newLocal(getArrayType(pyObjectType),"default")
        default.store()
        self.buildArray(numDefault, default)
        self.asm.newInstance(functionType)
        self.asm.dup()
        self.loadFrame()
        self.asm.getField(pyFrameType, "f_globals", pyObjectType)
        default.load()
        code.load()
        cell.load()
        code.end()
        cell.end()
        default.end()
        self.asm.invokeVirtual(sequenceType, Method.getMethod(
                "org.python.core.PyObject[] getArray ()"))
        self.asm.invokeConstructor(functionType, Method.getMethod(
                "void <init> (%s)" % ", ".join([
                        "org.python.core.PyObject", # globals
                        "org.python.core.PyObject[]", # defaults
                        "org.python.core.PyCode", # code
                        "org.python.core.PyObject[]", # closure_cells
                        ])))

    def visitMakeFunction(self, numDefault):
        """(value, )*numDefault, code -- function"""
        # TODO: the code objects need a "getDoc" method...
        # since that is where python bytecode stores the docstring
        functionType = getType(core.PyFunction)
        sequenceType = getType(core.PySequenceList)
        code = self.newLocal(pyCodeType,"code")
        code.store()
        # create an array
        self.push(numDefault)
        self.asm.newArray(pyObjectType)
        # store it for reference
        default = self.newLocal(getArrayType(pyObjectType),"default")
        default.store()
        self.buildArray(numDefault, default)
        self.asm.newInstance(functionType)
        self.asm.dup()
        self.loadFrame()
        self.asm.getField(pyFrameType, "f_globals", pyObjectType)
        default.load()
        code.load()
        default.end()
        code.end()
        self.asm.invokeConstructor(functionType, Method.getMethod(
                "void <init> (%s)" % ", ".join([
                        "org.python.core.PyObject", # globals
                        "org.python.core.PyObject[]", # defaults
                        "org.python.core.PyCode", # code
                        ])))

    def visitNOP(self):
        """ -- """
        self.asm.visitInsn(Op.NOP) # NOTE: should this be the implementation?

    def visitPop(self):
        """ -- """
        self.asm.pop()

    def visitPrintExpression(self):
        """object -- """
        self.asm.invokeStatic(pyType, Method.getMethod(
                "void println (org.python.core.PyObject)"))

    def visitPrintItem(self):
        """object -- """
        self.asm.invokeStatic(pyType, Method.getMethod(
                "void printComma (org.python.core.PyObject)"))

    def visitPrintItemTo(self):
        """object, object -- """
        self.asm.swap()
        self.asm.invokeStatic(pyType, Method.getMethod(
                "void printComma (%s)" % ", ".join(
                    ["org.python.core.PyObject"]*2)))

    def visitPrintNewline(self):
        """ -- """
        self.asm.invokeStatic(pyType, Method.getMethod(
                "void println ()"))

    def visitPrintNewlineTo(self):
        """ -- """
        self.asm.invokeStatic(pyType, Method.getMethod(
                "void printlnv (org.python.core.PyObject)"))

    def visitRaiseVarargs(self, count):
        """(object, )*count -- """
        # if there is content on the stack: reverse it
        if count == 2:
            self.asm.swap()
        elif count == 3:
            self.asm.swap()
            self.rot()
        elif count < 0 or count > 3:
            raise TypeError("RAISE_VARARGS accepts only arguments in range(4)")
            self.rot(count)
        self.asm.invokeStatic(pyType, Method.getMethod(
                "org.python.core.PyException makeException (%s)" % ", ".join(
                    ["org.python.core.PyObject"]*count)))
        self.asm.throwException()

    def visitReturnValue(self):
        """object -- """
        jumps = False
        # FIXME: in_try_block is a great patch by Nicholas Riley, but should
        # probably be refactored so that it is handeled in the block object.
        in_try_block = False
        for i in xrange(len(self.__blocks) -1,-1,-1):
            block = self.__blocks[i]
            if not in_try_block:
                jumps = block.exit() or jumps
            if isinstance(block, TryExceptBlock):
                in_try_block = True
        self.loadFrame()
        self.push(-1)
        self.asm.putField(pyFrameType, 'f_lasti', Type.INT_TYPE)
        if jumps:
            self.asm.checkCast(pyObjectType)
        self.asm.returnValue()

    __rotMax = 4
    def visitRot(self, depth):
        """(object, )*(depth - 1), element -- element, (object, )*(depth - 1)"""
        if depth < 2 or depth > self.__rotMax:
            raise TypeError("ROT can only be performed on depths in range(2,%s)"
                            % (self.__rotMax + 1))
        if depth == 2: # ROT_2 is swap
            self.asm.swap()
        else:
            top = self.newLocal(pyObjectType,"top")
            self.asm.swap()
            top.store()
            self.visitRot(depth - 1)
            top.load()
            top.end()
    
    def rot(self, depth=3):
        if depth < 2:
            raise TypeError("can only rot 2 or larger depths")
        if depth == 2:
            self.asm.swap()
        else:
            top = self.newLocal(pyObjectType,"top")
            top.store()
            self.rot(depth - 1)
            top.load()
            top.end()
            self.asm.swap()

    def tryCatchBlock(self, start, end, handle, excType):
        # the try/catch-blocks needs to be reversed to assert pythons
        # behaviour for nested try-blocks, this is done by storing them
        # when a try/catch-block is encounterd and emmiting them in reversed
        # order when the end of the code is encounterd.
        catch = TryCatch(start, end, handle, excType)
        self.__tryBlocks.append(catch)
        return catch
        ## Old behaviour:
        #self.asm.visitTryCatchBlock(start, end, handle, excType)

    def visitSetupExcept(self, startExcept):
        """ -- """
        start = self.label()
        end = self.label()
        handle = self.label(startExcept)
        catch = self.tryCatchBlock(start, end, handle, "java/lang/Throwable")
        # assert the stack state of surrounding blocks
        stackDepth = 0
        for block in self.__blocks:
            stackDepth += block.nestingStackSize
        block = TryExceptBlock(self.asm, catch, end,
                               [self.newLocal(pyObjectType,'')
                                for i in xrange(stackDepth)])
        block.storeState() # store the stack state of surrounding variables
        self.__blocks.append(block)
        #@self.scheduleCode(startExcept)
        def code():
            """JavaException -- traceback value type
            Set up the stack when the exception has been rised."""
            self.__blocks.append(block.handlerBlock)
            self.loadFrame()
            self.asm.invokeStatic(pyType, Method.getMethod(
                    "org.python.core.PyException setException (%s)"%", ".join(
                        ["Throwable", "org.python.core.PyFrame"])))
            exception = self.newLocal(pyExceptionType,"exception")
            exception.store()
            block.loadState() # load stack from enclosing blocks
            exception.load()
            self.asm.getField(pyExceptionType, "traceback",
                              getType(core.PyTraceback))
            exception.load()
            self.asm.getField(pyExceptionType, "value", pyObjectType)
            exception.load()
            exception.end()
            self.asm.getField(pyExceptionType, "type", pyObjectType)
        self.scheduleCode(startExcept, code)
        self.asm.visitLabel(start)

    def visitSetupFinally(self, startFinal):
        """ -- """
        start = self.label()
        end = self.label()
        handle = self.label()
        internal = self.label()
        after = self.label()
        retVariable = self.newLocal(Type.INT_TYPE, "retVariable")
        self.push(-1)
        retVariable.store()
        catch = self.tryCatchBlock(start, end, handle, "java/lang/Throwable")
        # assert the stack state of surrounding blocks
        stackDepth = 0
        for block in self.__blocks:
            stackDepth += block.nestingStackSize
        block = TryFinallyBlock(self.asm, catch, end, internal, after,
                                retVariable, [self.newLocal(pyObjectType,'')
                                              for i in xrange(stackDepth)])
        block.storeState() # store the stack state of surrounding variables
        self.__blocks.append(block)
        #@self.scheduleCode(startFinal)
        def code():
            """JavaException -- PythonException
            Set up the stack when the exception has been rised."""
            self.asm.pop()
            self.asm.goTo(after)
            self.__blocks.append(block.handlerBlock)
            self.asm.visitLabel(handle)
            self.loadFrame()
            self.asm.invokeStatic(pyType, Method.getMethod(
                    "org.python.core.PyException setException (%s)"%", ".join(
                        ["Throwable", "org.python.core.PyFrame"])))
            self.asm.visitLabel(internal)
        self.scheduleCode(startFinal, code)
        self.asm.visitLabel(start)

    def visitEndFinally(self):
        """ -- """
        block = self.__blocks.pop()
        if not isinstance(block, (FinallyBlock, ExceptBlock)):
            raise TypeError("EndFinally popped block of type %s" % type(block))
        block.end()

    def visitSetupLoop(self, loopEnd):
        """ -- """
        block = LoopBlock(self.asm, self.label(loopEnd))
        #@self.scheduleCode(loopEnd)
        def code():
            # check for optimized loop block (with constant test expr)
            if block in self.__blocks:
                top = self.__blocks.pop()
                if block is not top:
                    raise TypeError("Encountered badly terminated loop block")
        self.scheduleCode(loopEnd, code)
        self.__blocks.append(block)

    def visitPopBlock(self):
        """ -- """
        block = self.__blocks.pop()
        if not isinstance(block, (LoopBlock, TryFinallyBlock, TryExceptBlock)):
            raise TypeError("PopBlock popped block of type %s" % type(block))
        block.end()

    def visitStopCode(self):
        """ -- """
        raise TypeError("Encountered STOP_CODE in python bytecode")

    def visitStoreAttribute(self, attributeName):
        """value, owner -- """
        self.asm.swap()
        self.push(attributeName)
        self.asm.swap()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "void __setattr__ (String, org.python.core.PyObject)"))

    def visitStoreDeref(self, variableName):
        """object -- """
        self.loadFrame()
        self.asm.swap()
        self.derefIndex(variableName)
        self.asm.swap()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void setderef (int, org.python.core.PyObject)"))

    def visitStoreFast(self, localName):
        """object -- """
        self.loadFrame()
        self.asm.swap()
        self.push(self.__varnames.index(localName))
        self.asm.swap()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void setlocal (int, org.python.core.PyObject)"))

    def visitStoreGlobal(self, globalName):
        """object -- """
        self.loadFrame()
        self.asm.swap()
        self.push(globalName)
        self.asm.swap()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void setglobal (String, org.python.core.PyObject)"))

    def visitStoreName(self, name):
        """object -- """
        self.loadFrame()
        self.asm.swap()
        self.push(name)
        self.asm.swap()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "void setlocal (String, org.python.core.PyObject)"))

    def visitStoreSlice(self, plus):
        """value, object [,object[,object]] -- """
        value = self.newLocal(pyObjectType,"value")
        # start with digging up the value
        if plus == 0:
            self.asm.swap()
        elif plus == 1 or plus == 2 or plus == 4:
            self.rot(3)
        elif plus == 3 or plus == 5 or plus == 6:
            self.rot(4)
        elif plus == 7: # quite inefficient
            top = value # reuse local variable...
            top.store()
            self.rot(4)
            top.load()
            self.asm.swap()
        else:
            raise TypeError("plus for slice may only be in range(8)")
        value.store()

        self.setupSlice(plus)
        value.load()
        value.end()
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __setslice__ (%s)" % ", ".join(
                    ["org.python.core.PyObject"]*4)))

    def visitStoreSubscript(self):
        """value, object, index -- """
        self.rot(3)
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "void __setitem__ (%s)" % ", ".join(
                    ["org.python.core.PyObject"]*2)))

    def visitWithCleanup(self):
        """exitFunction -- """
        self.asm.invokeStatic(pySysType, Method.getMethod(
                "org.python.core.PyTuple exc_info ()"))
        self.asm.invokeVirtual(getType(core.PySequenceList), Method.getMethod(
                "org.python.core.PyObject[] getArray ()"))
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "org.python.core.PyObject __call__ (%s)" % ", ".join(
                    ["org.python.core.PyObject[]"])))
        self.asm.invokeVirtual(pyObjectType, Method.getMethod(
                "boolean __nonzero__ ()"))
        after = self.label()
        self.asm.visitJumpInsn(Op.IFEQ, after)
        self.asm.pop()
        self.asm.getStatic(pyType, "None", pyObjectType)
        self.asm.visitLabel(after)
                

    def visitResumeTable(self, start, labels):
        """ -- """
        self.loadFrame()
        self.asm.getField(pyFrameType, 'f_lasti', Type.INT_TYPE)
        self.asm.visitTableSwitchInsn(1, len(labels), self.label(start),
                                      array([self.label(l) for l in labels],
                                            asm.Label))
        #@self.scheduleCode(start)
        def code():
            """get input on start as well to assert that exceptions thrown
            into the generator at the beginning works as expected."""
            self.loadFrame()
            self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                    "Object getGeneratorInput ()"))
            self.asm.dup()
            self.asm.instanceOf(pyExceptionType)
            done = self.label()
            self.asm.visitJumpInsn(Op.IFEQ, done)
            self.asm.checkCast(throwableType)
            self.asm.throwException()
            self.asm.visitLabel(done)
            self.asm.pop()
        self.scheduleCode(start, code)

    def visitYieldValue(self, index, resume):
        """value -- sendValue"""
        self.storeStackState()
        self.loadFrame()
        self.push(index)
        self.asm.putField(pyFrameType, 'f_lasti', Type.INT_TYPE)
        self.asm.returnValue()
        self.asm.visitLabel(self.label(resume))
        self.loadStackState()
        # escape any try/catch blocks to make it possible to restore state
        # after yield
        end = self.label()
        for block in self.__blocks:
            block.exclude(self.label(resume), end)
        self.asm.visitLabel(end)
        self.loadFrame()
        self.asm.invokeVirtual(pyFrameType, Method.getMethod(
                "Object getGeneratorInput ()"))
        self.asm.dup()
        self.asm.instanceOf(pyExceptionType)
        done = self.label()
        self.asm.visitJumpInsn(Op.IFEQ, done)
        self.asm.checkCast(throwableType)
        self.asm.throwException()
        self.asm.visitLabel(done)
        self.asm.checkCast(pyObjectType)

    def storeStackState(self):
        count = 0
        stateVariables = []
        for block in self.__blocks:
            count += block.stackSize
            stateVariables.extend(block.stateVariables)
        if count > 0:
            ret = self.newLocal(pyObjectType, "temp")
            ret.store()
            for i in xrange(len(self.__blocks) -1,-1,-1):
                block = self.__blocks[i]
                block.loadState()
            state = self.newLocal(getArrayType(pyObjectType), "stackstate")
            self.push(count)
            self.asm.newArray(pyObjectType)
            state.store()
            self.buildArray(count, state)
            self.loadFrame()
            state.load()
            self.asm.putField(pyFrameType, 'f_stackstate',
                              getArrayType(pyObjectType))
            state.end()
            ret.load()
            ret.end()
        if stateVariables:
            self.push(len(stateVariables))
            self.asm.newArray(Type.INT_TYPE)
            for i in xrange(len(stateVariables)):
                variable = stateVariables[i]
                self.asm.dup()
                self.push(i)
                variable.load()
                self.asm.arrayStore(Type.INT_TYPE)
            self.loadFrame()
            self.asm.swap()
            self.asm.putField(pyFrameType, 'f_blockstate',
                              getArrayType(Type.INT_TYPE))

    def loadStackState(self):
        count = 0
        stateVariables = []
        for block in self.__blocks:
            count += block.stackSize
            stateVariables.extend(block.stateVariables)
        if count > 0:
            state = self.newLocal(getArrayType(pyObjectType), "stackstate")
            self.loadFrame()
            self.asm.getField(pyFrameType, 'f_stackstate',
                              getArrayType(pyObjectType))
            state.store()
            for i in xrange(count):
                state.load()
                self.push(i)
                self.asm.arrayLoad(pyObjectType)
            state.end()
        if stateVariables:
            self.loadFrame()
            self.asm.getField(pyFrameType, 'f_blockstate',
                              getArrayType(Type.INT_TYPE))
            for i in xrange(len(stateVariables)):
                variable = stateVariables[i]
                self.asm.dup()
                self.push(i)
                self.asm.arrayLoad(Type.INT_TYPE)
                variable.store()
            self.asm.pop()
        for block in self.__blocks:
            block.storeState()
    
    def visitEnd(self):
        """ --DONE"""
        # emmit stored try/catch-blocks
        while self.__tryBlocks:
            self.__tryBlocks.pop().accept( self.asm )
        init = self.__class.init
        ## gather the constants for this code object (not used)
        #init.push(java.lang.Integer(len(self.__constants)))
        #init.newArray(pyObjectType)
        #for i in xrange(len(self.__constants)):
        #    name = self.__constants[i]
        #    if self.__class.isCodeConstant(name):
        #        typ = pyCodeType
        #    else:
        #        typ = pyObjectType
        #    init.dup()
        #    init.push(java.lang.Integer(i))
        #    init.getStatic(self.__class.asType, name, typ)
        #    init.arrayStore(pyObjectType)
        #init.putStatic(self.__class.asType,
        #               self.__code.getName() + "$co_consts",
        #               getArrayType(core.PyObject))

        # emmit code that creates the code object
        argcount = self.__argcount
        if self.__flags & Flags.CO_VARARGS:
            argcount += 1
        if self.__flags & Flags.CO_VARKEYWORDS:
            argcount += 1
        init.push(java.lang.Integer(argcount))
        # varnames
        init.push(java.lang.Integer(len(self.__varnames)))
        init.newArray(stringType)
        for i in xrange(len(self.__varnames)):
            init.dup()
            init.push(java.lang.Integer(i))
            init.push(self.__varnames[i])
            init.arrayStore(stringType)
        init.loadArg(0) # The file name string argument
        init.push(self.__name) # The name of the function
        init.push(java.lang.Integer(self.__firstlineno)) # The first line no
        init.push(java.lang.Integer(bool(self.__flags & Flags.CO_VARARGS)))
        init.push(java.lang.Integer(bool(self.__flags & Flags.CO_VARKEYWORDS)))
        init.loadThis() # The function table
        init.push(java.lang.Integer(self.__code.index)) # the function id
        # cellvars
        init.push(java.lang.Integer(len(self.__cellvars)))
        init.newArray(stringType)
        for i in xrange(len(self.__cellvars)):
            init.dup()
            init.push(java.lang.Integer(i))
            init.push(self.__cellvars[i])
            init.arrayStore(stringType)
        # freevars
        init.push(java.lang.Integer(len(self.__freevars)))
        init.newArray(stringType)
        for i in xrange(len(self.__freevars)):
            init.dup()
            init.push(java.lang.Integer(i))
            init.push(self.__freevars[i])
            init.arrayStore(stringType)
        init.push(java.lang.Integer(0)) # purecell (what is this?)
        init.push(java.lang.Integer(self.__flags)) # compiler flags
        init.invokeStatic(pyType, Method.getMethod(
                "org.python.core.PyCode newCode (%s)" % ", ".join(
                    ['int', 'String[]', 'String', 'String', 'int', 'boolean',
                     'boolean', 'org.python.core.PyFunctionTable', 'int',
                     'String[]', 'String[]', 'int', 'int'])))
        init.putStatic(self.__class.asType, self.__code.getName(), pyCodeType)
        self.asm.endMethod()
