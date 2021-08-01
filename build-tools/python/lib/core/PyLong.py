# PyLong.py: A generator for Java files that define the Python float

# This generator writes PyLongMethods.java and PyLongBinops.java .

from dataclasses import dataclass
from typing import Callable

from . import ImplementationGenerator, TypeInfo, WorkingType, OpInfo


@dataclass
class IntTypeInfo(TypeInfo):
    "Information about a type and templates for conversion to int types"
    # There is a template (a function) to generate an expression
    # that converts *from* this type to each named Java type.
    # Template for expression that converts to BigInteger
    as_big: Callable = None
    # Template for expression that converts to primitive Java long
    as_long: Callable = None
    # Template for expression that converts to primitive Java int
    as_int: Callable = None


# Useful in cases where an argument is already the right type
itself = lambda x: x

PY_LONG_CLASS = IntTypeInfo('PyLong', WorkingType.BIG,
                    lambda x: f'{x}.value')
OBJECT_CLASS = IntTypeInfo('Object', WorkingType.OBJECT,
                    lambda x: f'toBig({x})')
BIG_INTEGER_CLASS = IntTypeInfo('BigInteger', WorkingType.BIG,
                    itself)
INTEGER_CLASS = IntTypeInfo('Integer', WorkingType.INT,
                    lambda x: f'BigInteger.valueOf({x})',
                    lambda x: f'((long) {x})',
                    itself)
BOOLEAN_CLASS = IntTypeInfo('Boolean', WorkingType.INT,
                    lambda x: f'({x} ? ONE : ZERO)',
                    lambda x: f'({x} ? 1L : 0L)',
                    lambda x: f'({x} ? 1 : 0)')
DOUBLE_CLASS = IntTypeInfo('Double', WorkingType.OBJECT)

PRIMITIVE_BOOLEAN = IntTypeInfo('boolean', WorkingType.BOOLEAN)
PRIMITIVE_INT = IntTypeInfo('int', WorkingType.INT)


@dataclass
class UnaryOpInfo(OpInfo):
    # There is a template (a function) to generate an expression
    # for each working Java type to which argument may be converted.
    # Working type is Java BigInteger
    big_op: Callable
    # Working type is Java long
    long_op: Callable
    # Working type is Java int
    int_op: Callable


@dataclass
class BinaryOpInfo(OpInfo):
    # There is a template (a function) to generate the body
    body_method: Callable
    # There is a template (a function) to generate an expression
    # for each working Java type to which arguments may be converted.
    # Working type is Java BigInteger
    big_op: Callable
    # Working type is Java long
    long_op: Callable
    # Working type is Java int
    int_op: Callable
    # Also create class-specific binop specialisations
    class_specific: bool = False


def unary_method(op:UnaryOpInfo, t:IntTypeInfo):
    "Template generating the body of a unary operation."
    # Decide the width at which to work with this type and op
    iw = max(op.min_working_type.value, t.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.INT:
        return _unary_method_int(op, t)
    elif w == WorkingType.LONG:
        return _unary_method_long(op, t)
    elif w == WorkingType.BIG:
        return _unary_method_big(op, t)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _unary_method_int(op:UnaryOpInfo, t:IntTypeInfo):
    "Template for unary methods when the working type is INT"
    return f'''
        return {op.int_op(t.as_int("self"))};
    '''

def _unary_method_long(op:UnaryOpInfo, t:IntTypeInfo):
    "Template for unary methods when the working type is LONG"
    return f'''
        long r = {op.long_op(t.as_long("self"))};
        int s = (int) r;
        return s == r ? s : BigInteger.valueOf(r);
    '''

def _unary_method_big(op:UnaryOpInfo, t:IntTypeInfo):
    "Template for unary methods when the working type is BIG"
    return f'''
        return {op.big_op(t.as_big("self"))};
    '''


def binary_intmethod(op:BinaryOpInfo,
                     t1:IntTypeInfo, n1,
                     t2:IntTypeInfo, n2):
    "Template generating the body of a binary operation with int result."
    # Decide the width at which to work with these typse and op
    iw = max(op.min_working_type.value,
            t1.min_working_type.value,
            t2.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.INT:
        return _binary_intmethod_int(op, t1, n1, t2, n2)
    elif w == WorkingType.LONG:
        return _binary_intmethod_long(op, t1, n1, t2, n2)
    elif w == WorkingType.BIG:
        return _binary_intmethod_big(op, t1, n1, t2, n2)
    elif w == WorkingType.OBJECT:
        return _binary_intmethod_obj(op, t1, n1, t2, n2)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _binary_intmethod_int(op:BinaryOpInfo,
                          t1:IntTypeInfo, n1,
                          t2:IntTypeInfo, n2):
    "Template for binary int methods when the working type is INT"
    return f'''
        return {op.int_op(t1.as_int(n1), t2.as_int(n2))};
    '''

def _binary_intmethod_long(op:BinaryOpInfo,
                           t1:IntTypeInfo, n1,
                           t2:IntTypeInfo, n2):
    "Template for binary int methods when the working type is LONG"
    return f'''
        long r = {op.long_op(t1.as_long(n1), t2.as_long(n2))};
        int s = (int) r;
        return s == r ? s : BigInteger.valueOf(r);
    '''

def _binary_intmethod_big(op:BinaryOpInfo,
                          t1:IntTypeInfo, n1,
                          t2:IntTypeInfo, n2):
    "Template for binary int methods when the working type is BIG"
    return f'''
        return toInt({op.big_op(t1.as_big(n1), t2.as_big(n2))});
    '''

def _binary_intmethod_obj(op:BinaryOpInfo,
                          t1:IntTypeInfo, n1,
                          t2:IntTypeInfo, n2):
    "Template for binary int methods when the working type is OBJECT"
    return f'''
        try {{
            return toInt({op.big_op(t1.as_big(n1), t2.as_big(n2))});
        }} catch (NoConversion e) {{
            return Py.NotImplemented;
        }}
    '''


def binary_method(op:BinaryOpInfo,
                  t1:IntTypeInfo, n1,
                  t2:IntTypeInfo, n2):
    "Template generating the body of a binary operation result."
    # Decide the width at which to work with these typse and op
    iw = max(op.min_working_type.value,
            t1.min_working_type.value,
            t2.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.INT:
        return _binary_method_int(op, t1, n1, t2, n2)
    elif w == WorkingType.LONG:
        return _binary_method_long(op, t1, n1, t2, n2)
    elif w == WorkingType.BIG:
        return _binary_method_big(op, t1, n1, t2, n2)
    elif w == WorkingType.OBJECT:
        return _binary_method_obj(op, t1, n1, t2, n2)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _binary_method_int(op:BinaryOpInfo,
                       t1:IntTypeInfo, n1,
                       t2:IntTypeInfo, n2):
    "Template for binary methods when the working type is INT"
    return f'''
        return {op.int_op(t1.as_int(n1), t2.as_int(n2))};
    '''

def _binary_method_long(op:BinaryOpInfo,
                        t1:IntTypeInfo, n1,
                        t2:IntTypeInfo, n2):
    "Template for binary methods when the working type is LONG"
    return f'''
        return {op.long_op(t1.as_long(n1), t2.as_long(n2))};
    '''

def _binary_method_big(op:BinaryOpInfo,
                       t1:IntTypeInfo, n1,
                       t2:IntTypeInfo, n2):
    "Template for binary methods when the working type is BIG"
    return f'''
        return {op.big_op(t1.as_big(n1), t2.as_big(n2))};
    '''

def _binary_method_obj(op:BinaryOpInfo,
                       t1:IntTypeInfo, n1,
                       t2:IntTypeInfo, n2):
    "Template for binary methods when the working type is OBJECT"
    return f'''
        try {{
            return {op.big_op(t1.as_big(n1), t2.as_big(n2))};
        }} catch (NoConversion e) {{
            return Py.NotImplemented;
        }}
    '''


class PyLongGenerator(ImplementationGenerator):

    # The canonical and adopted implementations in PyInteger.java,
    # as there are no further accepted self-classes.
    ACCEPTED_CLASSES = [
        PY_LONG_CLASS,
        BIG_INTEGER_CLASS,
        INTEGER_CLASS,
        BOOLEAN_CLASS,
    ]
    OPERAND_CLASSES = ACCEPTED_CLASSES + [
    ]

    # Operations have to provide versions in which long and
    # BigInteger are the common type to which arguments are converted.

    UNARY_OPS = [
        # Arguments are: name, return_type, min_working_type,
        # big_op, long_op, int_op[, method]
        UnaryOpInfo('__abs__', OBJECT_CLASS, WorkingType.LONG,
            lambda x: f'{x}.abs()',
            lambda x: f'Math.abs({x})',
            lambda x: f'Math.abs({x})'),
        UnaryOpInfo('__index__', OBJECT_CLASS, WorkingType.INT,
            itself,
            itself,
            itself),
        UnaryOpInfo('__int__', OBJECT_CLASS, WorkingType.INT,
            itself,
            itself,
            itself),
        UnaryOpInfo('__neg__', OBJECT_CLASS, WorkingType.LONG,
            lambda x: f'{x}.negate()',
            lambda x: f'-{x}',
            lambda x: f'-{x}'),
        UnaryOpInfo('__float__', OBJECT_CLASS, WorkingType.INT,
            lambda x: f'PyLong.convertToDouble({x})',
            lambda x: f'((double) {x})',
            lambda x: f'((double) {x})'),
        UnaryOpInfo('__bool__', PRIMITIVE_BOOLEAN, WorkingType.BOOLEAN,
            lambda x: f'{x}.signum() != 0',
            lambda x: f'{x} != 0L',
            lambda x: f'{x} != 0'),
        UnaryOpInfo('__hash__', PRIMITIVE_INT, WorkingType.INT,
            lambda x: f'{x}.hashCode()',
            lambda x: f'{x}.hashCode()',
            lambda x: f'{x}'),
    ]

    BINARY_OPS = [
        # Arguments are: name, return_type, working_type,
        #            body_method,
        #            big_op, long_op, int_op
        #            with_class_specific_binops
        BinaryOpInfo('__add__', OBJECT_CLASS, WorkingType.LONG,
            binary_intmethod,
            lambda x, y: f'{x}.add({y})',
            lambda x, y: f'{x} + {y}', 
            lambda x, y: f'{x} + {y}',
            True),
        BinaryOpInfo('__radd__', OBJECT_CLASS, WorkingType.LONG,
            binary_intmethod,
            lambda x, y: f'{y}.add({x})',
            lambda x, y: f'{y} + {x}', 
            lambda x, y: f'{y} + {x}',
            True),
        BinaryOpInfo('__sub__', OBJECT_CLASS, WorkingType.LONG,
            binary_intmethod,
            lambda x, y: f'{x}.subtract({y})',
            lambda x, y: f'{x} - {y}', 
            lambda x, y: f'{x} - {y}',
            True),
        BinaryOpInfo('__rsub__', OBJECT_CLASS, WorkingType.LONG,
            binary_intmethod,
            lambda x, y: f'{y}.subtract({x})',
            lambda x, y: f'{y} - {x}', 
            lambda x, y: f'{y} - {x}',
            True),
        BinaryOpInfo('__mul__', OBJECT_CLASS, WorkingType.LONG,
            binary_intmethod,
            lambda x, y: f'{x}.multiply({y})',
            lambda x, y: f'{x} * {y}', 
            lambda x, y: f'{x} * {y}',
            True),
        BinaryOpInfo('__rmul__', OBJECT_CLASS, WorkingType.LONG,
            binary_intmethod,
            lambda x, y: f'{y}.multiply({x})',
            lambda x, y: f'{y} * {x}', 
            lambda x, y: f'{y} * {x}',
            True),
        BinaryOpInfo('__floordiv__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'divide({x}, {y})',
            lambda x, y: f'divide({x}, {y})',
            lambda x, y: f'divide({x}, {y})',
            True),
        BinaryOpInfo('__rfloordiv__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'divide({y}, {x})',
            lambda x, y: f'divide({y}, {x})',
            lambda x, y: f'divide({y}, {x})',
            True),
        BinaryOpInfo('__mod__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'modulo({x}, {y})',
            lambda x, y: f'modulo({x}, {y})',
            lambda x, y: f'modulo({x}, {y})',
            True),
        BinaryOpInfo('__rmod__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'modulo({y}, {x})',
            lambda x, y: f'modulo({y}, {x})',
            lambda x, y: f'modulo({y}, {x})',
            True),

        BinaryOpInfo('__divmod__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'divmod({x}, {y})',
            lambda x, y: f'divmod({x}, {y})',
            lambda x, y: f'divmod({x}, {y})',
            True),
        BinaryOpInfo('__rdivmod__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'divmod({y}, {x})',
            lambda x, y: f'divmod({y}, {x})',
            lambda x, y: f'divmod({y}, {x})',
            True),

        BinaryOpInfo('__truediv__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'trueDivide({x}, {y})',
            lambda x, y: f'trueDivide({x}, {y})',
            lambda x, y: f'(double){x} / (double){y}',
            True),
        BinaryOpInfo('__rtruediv__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'trueDivide({y}, {x})',
            lambda x, y: f'trueDivide({y}, {x})',
            lambda x, y: f'(double){y} / (double){x}',
            True),

        BinaryOpInfo('__and__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'{x}.and({y})',
            lambda x, y: f'{x} & {y}', 
            lambda x, y: f'{x} & {y}',
            True),
        BinaryOpInfo('__rand__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'{y}.and({x})',
            lambda x, y: f'{y} & {x}', 
            lambda x, y: f'{y} & {x}',
            True),
        BinaryOpInfo('__or__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'{x}.or({y})',
            lambda x, y: f'{x} | {y}', 
            lambda x, y: f'{x} | {y}',
            True),
        BinaryOpInfo('__ror__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'{y}.or({x})',
            lambda x, y: f'{y} | {x}', 
            lambda x, y: f'{y} | {x}'),
        BinaryOpInfo('__xor__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'{x}.xor({y})',
            lambda x, y: f'{x} ^ {y}', 
            lambda x, y: f'{x} ^ {y}',
            True),
        BinaryOpInfo('__rxor__', OBJECT_CLASS, WorkingType.INT,
            binary_intmethod,
            lambda x, y: f'{y}.xor({x})',
            lambda x, y: f'{y} ^ {x}', 
            lambda x, y: f'{y} ^ {x}',
            True),

        BinaryOpInfo('__lt__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'{x}.compareTo({y}) < 0',
            lambda x, y: f'{x} < {y}', 
            lambda x, y: f'{x} < {y}'),
        BinaryOpInfo('__le__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'{x}.compareTo({y}) <= 0',
            lambda x, y: f'{x} <= {y}', 
            lambda x, y: f'{x} <= {y}'),
        BinaryOpInfo('__eq__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'{x}.compareTo({y}) == 0',
            lambda x, y: f'{x} == {y}', 
            lambda x, y: f'{x} == {y}'),
        BinaryOpInfo('__ne__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'{x}.compareTo({y}) != 0',
            lambda x, y: f'{x} != {y}', 
            lambda x, y: f'{x} != {y}'),
        BinaryOpInfo('__gt__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'{x}.compareTo({y}) > 0',
            lambda x, y: f'{x} > {y}', 
            lambda x, y: f'{x} > {y}'),
        BinaryOpInfo('__ge__', OBJECT_CLASS, WorkingType.INT,
            binary_method,
            lambda x, y: f'{x}.compareTo({y}) >= 0',
            lambda x, y: f'{x} >= {y}', 
            lambda x, y: f'{x} >= {y}'),
    ]

    # Emit methods selectable by a single type
    def special_methods(self, e):

        # Emit the unary operations
        for op in self.UNARY_OPS:
            self.emit_heading(e, op.name)
            for t in self.ACCEPTED_CLASSES:
                self.special_unary(e, op, t)

        # Emit the binary operations op(T, Object)
        for op in self.BINARY_OPS:
            self.emit_heading(e, op.name)
            for vt in self.ACCEPTED_CLASSES:
                self.special_binary(e, op, vt, OBJECT_CLASS)

    # Emit methods selectable by a pair of types (for call sites)
    def special_binops(self, e):

        # Emit the binary operations and comparisons
        for op in self.BINARY_OPS:
            if op.class_specific:
                self.emit_heading(e, op.name)
                for vt in self.ACCEPTED_CLASSES:
                    for wt in self.OPERAND_CLASSES:
                        self.special_binary(e, op, vt, wt)

    def left_justify(self, text):
        lines = list()
        # Find common leading indent
        common = 999
        for line in text.splitlines():
            # Discard trailing space
            line = line.rstrip()
            # Discard empty lines
            if (n:=len(line)) > 0:
                space = n - len(line.lstrip())
                if space < common: common = space
                lines.append(line)
        if common == 999: common = 0
        # Remove this common prefix
        clean = list()
        for line in lines:
            clean.append(line[common:])
        return clean

    def special_unary(self, e, op:UnaryOpInfo, t):
        e.emit('static ').emit(op.return_type.name).emit(' ')
        e.emit(op.name).emit('(').emit(t.name).emit(' self) {')
        with e.indentation():
            method = unary_method(op, t)
            method = self.left_justify(method)
            e.emit_lines(method)
        e.emit_line('}').emit_line()

    def special_binary(self, e, op:BinaryOpInfo, t1, t2):
        reflected = op.name.startswith('__r') and \
            op.name not in ("__rrshift__", "__round__", "__repr__")
        n1, n2 = 'vw' if not reflected else 'wv'
        e.emit('static ').emit(op.return_type.name).emit(' ')
        e.emit(op.name).emit('(')
        e.emit(t1.name).emit(' ').emit(n1).emit(', ')
        e.emit(t2.name).emit(' ').emit(n2).emit(') {')
        with e.indentation():
            method = op.body_method(op, t1, n1, t2, n2)
            method = self.left_justify(method)
            e.emit_lines(method)
        e.emit_line('}').emit_line()


