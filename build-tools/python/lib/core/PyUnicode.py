# PyUnicode.py: A generator for Java files that define the Python str

# Copyright (c)2021 Jython Developers.
# Licensed to PSF under a contributor agreement.

# This generator writes PyUnicodeMethods.java and PyUnicodeBinops.java .

# At the time of this writing, only the comparison operations are
# generated. Unlike arithmetic types, str does not have a large
# set of operations with a uniform pattern, so it is more effective
# to hand-craft the small number of cases needed.

from dataclasses import dataclass
from typing import Callable

from . import ImplementationGenerator, TypeInfo, WorkingType, OpInfo


@dataclass
class StrTypeInfo(TypeInfo):
    "Information about a type and templates for conversion to str types"
    # There is a template (a function) to generate an expression
    # that converts *from* this type to each named Java type.
    # Template for expression that converts to PySequence
    as_seq: Callable = None
    # Template for expression that converts to String
    as_str: Callable = None

# Useful in cases where an argument is already the right type
itself = lambda x: x

PY_UNICODE_CLASS = StrTypeInfo('PyUnicode', WorkingType.SEQ,
                    lambda x: f'{x}.adapt()',
                    itself)
STRING_CLASS = StrTypeInfo('String', WorkingType.STRING,
                    lambda x: f'adapt({x})',
                    itself)
OBJECT_CLASS = StrTypeInfo('Object', WorkingType.OBJECT,
                    lambda x: f'adapt({x})')


@dataclass
class UnaryOpInfo(OpInfo):
    # There is a template (a function) to generate an expression
    # for each working Java type to which argument may be converted.
    # Working type is Java String
    str_op: Callable


@dataclass
class BinaryOpInfo(OpInfo):
    # There is a template (a function) to generate the body
    body_method: Callable
    # There is a template (a function) to generate an expression
    # for each working Java type to which arguments may be converted.
    # Working type is Java String
    str_op: Callable
    # Working type is PySequence
    seq_op: Callable
    # Also create class-specific binop specialisations
    class_specific: bool = False


def unary_method(op:UnaryOpInfo, t:StrTypeInfo):
    "Template generating the body of a unary operation."
    # Decide the width at which to work with this type and op
    iw = max(op.min_working_type.value, t.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.STRING:
        return _unary_method_str(op, t)
    elif w == WorkingType.SEQ:
        return _unary_method_seq(op, t)
    elif w == WorkingType.OBJECT:
        return _unary_method_obj(op, t)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _unary_method_str(op:UnaryOpInfo, t:StrTypeInfo):
    "Template for unary methods when the working type is STRING"
    return f'''
        return {op.str_op(t.as_str("self"))};
    '''

def _unary_method_seq(op:UnaryOpInfo, t:StrTypeInfo):
    "Template for unary methods when the working type is LONG"
    return f'''
        return {op.seq_op(t.as_seq("self"))};
    '''

def _unary_method_obj(op:UnaryOpInfo, t:StrTypeInfo):
    "Template for unary methods when the working type is BIG"
    return f'''
        return {op.seq_op(t.as_seq("self"))};
    '''


def binary_method(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    "Template generating the body of a binary operation."
    # Decide the width at which to work with these typse and op
    iw = max(op.min_working_type.value,
            t1.min_working_type.value,
            t2.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.STRING:
        return _binary_method_str(op, t1, n1, t2, n2)
    elif w == WorkingType.SEQ:
        return _binary_method_seq(op, t1, n1, t2, n2)
    elif w == WorkingType.OBJECT:
        return _binary_method_obj(op, t1, n1, t2, n2)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")


def _binary_method_str(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    return f'''
        return {op.str_op(t1.as_str(n1), t2.as_str(n2))};
    '''

def _binary_method_seq(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    return f'''
        return {op.seq_op(t1.as_seq(n1), t2.as_seq(n2))};
    '''

def _binary_method_obj(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    return f'''
        try {{
            return {op.seq_op(t1.as_seq(n1), t2.as_seq(n2))};
        }} catch (NoConversion e) {{
            return Py.NotImplemented;
        }}
    '''

def comparison(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    "Template generating the body of a comparison operation."
    iw = max(op.min_working_type.value,
            t1.min_working_type.value,
            t2.min_working_type.value)
    w = WorkingType(iw)
    if w == WorkingType.STRING:
        return _comparison_str(op, t1, n1, t2, n2)
    elif w == WorkingType.SEQ:
        return _comparison_seq(op, t1, n1, t2, n2)
    elif w == WorkingType.OBJECT:
        return _comparison_obj(op, t1, n1, t2, n2)
    else:
        raise ValueError(
            f"Cannot make method body for {op.name} and {w}")

def _comparison_guard(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    if t2.name == "Object" or t2.name == t1.name:
        # The objects might be identical, permitting a shortcut
        name = op.name
        if name == "__eq__" or name == "__le__" or name == "__ge__":
            return f'{n1} == {n2} || '
        elif name == "__ne__" or name == "__lt__" or name == "__gt__":
            return f'{n1} != {n2} && '
    return ""

def _comparison_str(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    guard = _comparison_guard(op, t1, n1, t2, n2)
    return f'''
        return {guard}{op.int_op(t1.as_str(n1), t2.as_str(n2))};
    '''

def _comparison_seq(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    guard = _comparison_guard(op, t1, n1, t2, n2)
    return f'''
        return {guard}{op.seq_op(t1.as_seq(n1), t2.as_seq(n2))};
    '''

def _comparison_obj(op:BinaryOpInfo, t1:StrTypeInfo, n1, t2:StrTypeInfo, n2):
    guard = _comparison_guard(op, t1, n1, t2, n2)
    return f'''
        try {{
            return {guard}{op.seq_op(t1.as_seq(n1), t2.as_seq(n2))};
        }} catch (NoConversion e) {{
            return Py.NotImplemented;
        }}
    '''

class PyUnicodeGenerator(ImplementationGenerator):

    # The canonical and adopted implementations in PyUnicode.java,
    # as there are no further accepted self-classes.
    ACCEPTED_CLASSES = [
        PY_UNICODE_CLASS,
        STRING_CLASS,
    ]
    OPERAND_CLASSES = ACCEPTED_CLASSES + [
    ]

    # Operations have to provide versions in which long and
    # BigInteger are the common type to which arguments are converted.

    UNARY_OPS = [
        # Arguments are: name, min_working_type,
        #       body_method,
        #       str_op
    ]

    BINARY_OPS = [
        # Arguments are: name, return_type, working_type,
        #       body_method,
        #       str_op, seq_op,
        #       class_specific

        #         BinaryOpInfo('__add__', OBJECT_CLASS, WorkingType.STRING,
        #             binary_method,
        #             lambda x, y: f'{x} + ({y})',
        #             lambda x, y: f'{x}.concat({y})',
        #             True),

        BinaryOpInfo('__lt__', OBJECT_CLASS, WorkingType.STRING,
            comparison,
            lambda x, y: f'{x}.compareTo({y}) < 0',
            lambda x, y: f'{x}.compareTo({y}) < 0'),
        BinaryOpInfo('__le__', OBJECT_CLASS, WorkingType.STRING,
            comparison,
            lambda x, y: f'{x}.compareTo({y}) <= 0',
            lambda x, y: f'{x}.compareTo({y}) <= 0'),
        BinaryOpInfo('__eq__', OBJECT_CLASS, WorkingType.STRING,
            comparison,
            lambda x, y: f'eq({x}, {y})',
            lambda x, y: f'eq({x}, {y})'),
        BinaryOpInfo('__ne__', OBJECT_CLASS, WorkingType.STRING,
            comparison,
            lambda x, y: f'!eq({x}, ({y})',
            lambda x, y: f'!eq({x}, {y})'),
        BinaryOpInfo('__gt__', OBJECT_CLASS, WorkingType.STRING,
            comparison,
            lambda x, y: f'{x}.compareTo({y}) > 0',
            lambda x, y: f'{x}.compareTo({y}) > 0'),
        BinaryOpInfo('__ge__', OBJECT_CLASS, WorkingType.STRING,
            comparison,
            lambda x, y: f'{x}.compareTo({y}) >= 0',
            lambda x, y: f'{x}.compareTo({y}) >= 0'),
    ]

    # Emit methods selectable by a single type
    def special_methods(self, e):

        # Emit the unary operations
        for op in self.UNARY_OPS:
            e.emit_line(f'// {"-"*(60-len(op.name))} {op.name}')
            e.emit_line()
            for t in self.ACCEPTED_CLASSES:
                self.special_unary(e, op, t)

        # Emit the binary operations op(T, Object)
        for op in self.BINARY_OPS:
            e.emit_line(f'// {"-"*(60-len(op.name))} {op.name}')
            e.emit_line()
            for vt in self.ACCEPTED_CLASSES:
                self.special_binary(e, op, vt, OBJECT_CLASS)

    # Emit methods selectable by a pair of types (for call sites)
    def special_binops(self, e):

        # Emit the binary operations and comparisons
        for op in self.BINARY_OPS:
            if op.class_specific:
                e.emit_line(f'// {"-"*(60-len(op.name))} {op.name}')
                e.emit_line()
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


