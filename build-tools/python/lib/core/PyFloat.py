# PyFloat.py: A generator for Java files that define the Python float

# Copyright (c)2021 Jython Developers.
# Licensed to PSF under a contributor agreement.

# This generator writes PyFloatMethods.java and PyFloatBinops.java .

from dataclasses import dataclass
from typing import Callable

from . import ImplementationGenerator, TypeInfo, WorkingType, OpInfo

@dataclass
class FloatTypeInfo(TypeInfo):
    "Information about a type and templates for conversion to float types"
    # There is a template (a function) to generate an expression
    # that converts *from* this type to each named Java type that may be
    # a "working type" when implementing an operation.

    # That's only 'double', but conceivably primitive 'float' later.

    # Template for expression that converts to primitive double
    as_double: str = None


# Useful in cases where an argument is already the right type
itself = lambda x: x

# A constant FloatTypeInfo for each argument type that we might have to
# convert to a "working type" when implementing an operation.
# Arguments are: name, min_working_type,
#            as_double
PY_FLOAT_CLASS = FloatTypeInfo('PyFloat', WorkingType.DOUBLE,
                    lambda x: f'{x}.value')
OBJECT_CLASS = FloatTypeInfo('Object', WorkingType.OBJECT,
                    lambda x: f'toDouble({x})')
DOUBLE_CLASS = FloatTypeInfo('Double', WorkingType.DOUBLE,
                    itself)

# Accepted types that may appear as the other operand in binary
# operations specialised to both types.
PY_LONG_CLASS = FloatTypeInfo('PyLong', WorkingType.DOUBLE,
                    lambda x: f'convertToDouble({x}.value)')
BIG_INTEGER_CLASS = FloatTypeInfo('BigInteger', WorkingType.DOUBLE,
                    lambda x: f'convertToDouble({x})')
INTEGER_CLASS = FloatTypeInfo('Integer', WorkingType.DOUBLE,
                    lambda x: f'{x}.doubleValue()')
BOOLEAN_CLASS = FloatTypeInfo('Boolean', WorkingType.DOUBLE,
                    lambda x: f'({x} ? 1.0 : 0.0)')

# A constant FloatTypeInfo for types appearing as return types only.
#(No conversion to a working type is expected.)
# convert to a "working type" when implementing an operation.
PRIMITIVE_BOOLEAN = FloatTypeInfo('boolean', WorkingType.BOOLEAN)
PRIMITIVE_INT = FloatTypeInfo('int', WorkingType.INT)
PRIMITIVE_DOUBLE = FloatTypeInfo('double', WorkingType.DOUBLE)


@dataclass
class UnaryOpInfo(OpInfo):
    # There is a template (a function) to generate an expression for
    # each Java working type in which the result may be evaluated.

    # That's only 'double', but conceivably primitive 'float' later.

    # Template for when the working type is Java double
    double_op: Callable


@dataclass
class BinaryOpInfo(OpInfo):
    # There is a template (a function) to generate the body
    body_method: Callable

    # There is a template (a function) to generate an expression for
    # each Java working type in which the result may be evaluated.
    # That's only 'double', but conceivably primitive 'float' later.

    # Template for when the working type is Java double
    double_op: Callable

    # Also create class-specific binop specialisations
    class_specific: bool = False


def unary_method(op:UnaryOpInfo, t:FloatTypeInfo):
    "Template generating the body of a unary operation."
    # Decide the width at which to work with this type and op
    iw = max(op.min_working_type.value, t.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.DOUBLE:
        return _unary_method_double(op, t)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _unary_method_double(op:UnaryOpInfo, t:FloatTypeInfo):
    "Template for unary methods when the working type is DOUBLE"
    return f'''
        return {op.double_op(t.as_double("self"))};
    '''


def binary_floatmethod(op:BinaryOpInfo,
                     t1:FloatTypeInfo, n1,
                     t2:FloatTypeInfo, n2):
    """Template for a binary operation with float result.

    Argument coercions are made according to their static type then
    the operation is applied which must yield a result in the working
    type. This is only appropriate where the return from the generated
    method should be a Python float (e.g. not comparisons, __divmod__).
    """
    # Decide the width at which to work with these types and op
    iw = max(op.min_working_type.value,
            t1.min_working_type.value,
            t2.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.DOUBLE:
        return _binary_floatmethod_double(op, t1, n1, t2, n2)
    elif w == WorkingType.OBJECT:
        return _binary_floatmethod_obj(op, t1, n1, t2, n2)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _binary_floatmethod_double(op:BinaryOpInfo,
                          t1:FloatTypeInfo, n1,
                          t2:FloatTypeInfo, n2):
    "Template for binary float methods when the working type is DOUBLE"
    return f'''
        return {op.double_op(t1.as_double(n1), t2.as_double(n2))};
    '''

def _binary_floatmethod_obj(op:BinaryOpInfo,
                          t1:FloatTypeInfo, n1,
                          t2:FloatTypeInfo, n2):
    "Template for binary float methods when the working type is OBJECT"
    return f'''
        try {{
            return {op.double_op(t1.as_double(n1), t2.as_double(n2))};
        }} catch (NoConversion e) {{
            return Py.NotImplemented;
        }}
    '''


def binary_method(op:BinaryOpInfo,
                  t1:FloatTypeInfo, n1,
                  t2:FloatTypeInfo, n2):
    """Template for a binary operation with any result type.

    Argument coercions are made according to their static type then
    the operation is applied and the result returned without further
    processing."""
    # Decide the width at which to work with these types and op
    iw = max(op.min_working_type.value,
            t1.min_working_type.value,
            t2.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.DOUBLE:
        return _binary_method_double(op, t1, n1, t2, n2)
    elif w == WorkingType.OBJECT:
        return _binary_method_obj(op, t1, n1, t2, n2)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _binary_method_double(op:BinaryOpInfo,
                       t1:FloatTypeInfo, n1,
                       t2:FloatTypeInfo, n2):
    "Template for binary methods when the working type is DOUBLE"
    return f'''
        return {op.double_op(t1.as_double(n1), t2.as_double(n2))};
    '''

def _binary_method_obj(op:BinaryOpInfo,
                       t1:FloatTypeInfo, n1,
                       t2:FloatTypeInfo, n2):
    "Template for binary methods when the working type is OBJECT"
    return f'''
        try {{
            return {op.double_op(t1.as_double(n1), t2.as_double(n2))};
        }} catch (NoConversion e) {{
            return Py.NotImplemented;
        }}
    '''


class PyFloatGenerator(ImplementationGenerator):

    # The canonical and adopted implementations in PyFloat.java.
    ACCEPTED_CLASSES = [PY_FLOAT_CLASS, DOUBLE_CLASS]

    # These classes may occur as the second operand in binary
    # operations. Order is not significant.
    OPERAND_CLASSES = ACCEPTED_CLASSES + [
        # XXX Consider *not* specialising ...
        # Although PyLong and BigInteger are accepted operands, we
        # decline to specialise, since the implementation would be
        # equivalent to the one in  PyFloatMethods.
        PY_LONG_CLASS,
        BIG_INTEGER_CLASS,
        INTEGER_CLASS,
        BOOLEAN_CLASS,
    ]

    # Operations may simply be codified as a return expression, since
    # all operand types may be converted to primitive double.

    UNARY_OPS = [
        # Arguments are: name, return_type, min_working_type,
        # double_op
        UnaryOpInfo('__abs__', OBJECT_CLASS, WorkingType.DOUBLE,
            lambda x: f'Math.abs({x})'),
        UnaryOpInfo('__neg__', OBJECT_CLASS, WorkingType.DOUBLE,
            lambda x: f'-{x}'),
        UnaryOpInfo('__pos__', OBJECT_CLASS, WorkingType.DOUBLE,
            lambda x: f'{x}'),
        UnaryOpInfo('__bool__', PRIMITIVE_BOOLEAN, WorkingType.DOUBLE,
            lambda x: f'{x} != 0.0'),
        UnaryOpInfo('__hash__', PRIMITIVE_INT, WorkingType.DOUBLE,
            lambda x: f'Double.hashCode({x})'),
    ]
    BINARY_OPS = [
        # Arguments are: name, return_type, working_type,
        #            body_method,
        #            double_op,
        #            with_class_specific_binops
        BinaryOpInfo('__add__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{x} + {y}',
            True),
        BinaryOpInfo('__radd__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{y} + {x}',
            True),
        BinaryOpInfo('__sub__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{x} - {y}',
            True),
        BinaryOpInfo('__rsub__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{y} - {x}',
            True),
        BinaryOpInfo('__mul__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{x} * {y}',
            True),
        BinaryOpInfo('__rmul__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{y} * {x}',
            True),

        BinaryOpInfo('__truediv__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{x} / nonzero({y})',
            True),
        BinaryOpInfo('__rtruediv__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'{y} / nonzero({x})',
            True),

        BinaryOpInfo('__floordiv__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'floordiv({x}, {y})',
            False),
        BinaryOpInfo('__rfloordiv__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'floordiv({y}, {x})',
            False),
        BinaryOpInfo('__mod__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'mod({x}, {y})',
            False),
        BinaryOpInfo('__rmod__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_floatmethod,
            lambda x, y: f'mod({y}, {x})',
            False),

        BinaryOpInfo('__divmod__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'divmod({x}, {y})',
            False),
        BinaryOpInfo('__rdivmod__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'divmod({y}, {x})',
            False),

        BinaryOpInfo('__lt__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'{x} < {y}'),
        BinaryOpInfo('__le__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'{x} <= {y}'),
        BinaryOpInfo('__eq__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'{x} == {y}'),
        BinaryOpInfo('__ne__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'{x} != {y}'),
        BinaryOpInfo('__gt__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
            lambda x, y: f'{x} > {y}'),
        BinaryOpInfo('__ge__', OBJECT_CLASS, WorkingType.DOUBLE,
            binary_method,
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

    def special_unary(self, e, op:UnaryOpInfo, t):
        e.emit('static ').emit(op.return_type.name).emit(' ')
        e.emit(op.name).emit('(').emit(t.name).emit(' self) {')
        with e.indentation():
            method = unary_method(op, t)
            method = self.left_justify(method)
            e.emit_lines(method)
        e.emit_line('}').emit_line()

    # Emit one binary operation, for example:
    #    private static Object __add__(Double v, Integer w) {
    #        return v.doubleValue() + w.doubleValue();
    #    }
    def special_binary(self, e, op:BinaryOpInfo, t1, t2):
        reflected = op.name.startswith('__r') and \
            op.name not in ("__rshift__", "__round__", "__repr__")
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

