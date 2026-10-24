// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.internal.EmptyException;
import org.python.internal.Util;
import org.python.support.InterpreterError;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * A test of the apparatus used to form {@code MethodHandle}s from
 * special and other methods.
 */
public class MethodHandleFormationTest {

    /**
     * A test that the method handles we place in nominally empty slots, do
     * in fact raise the exception used internally to detect them.
     */
    @SuppressWarnings({"unused", "static-method"})
    @Test
    void testSlotsEmptyException() {

        // Call to handle that fills "empty" UNARY slot.
        Object v = 100;
        assertThrows(EmptyException.class, () -> { //
            Object r = SpecialMethod.Signature.UNARY.empty.invokeExact(v);
        });

        // Call to handle that fills "empty" LEN slot.
        assertThrows(EmptyException.class, () -> { //
            int r = (int)SpecialMethod.Signature.LEN.empty.invokeExact(v);
        });

        // Call to handle that fills "empty" PREDICATE slot.
        assertThrows(EmptyException.class, () -> { //
            boolean r = (boolean)SpecialMethod.Signature.PREDICATE.empty.invokeExact(v);
        });

        // Call to handle that fills "empty" BINARY slot.
        Object w = 200;
        assertThrows(EmptyException.class, () -> { //
            Object r = SpecialMethod.Signature.BINARY.empty.invokeExact(v, w);
        });

        // Call to handle that fills "empty" SETITEM slot.
        Object u = PyTuple.of(v, w);
        assertThrows(EmptyException.class, new Executable() { //

            @Override
            public void execute() throws Throwable {
                Object i = 1;
                SpecialMethod.Signature.SETITEM.empty.invokeExact(u, i, w);
            }
        });
    }

    /** Python object implementation for tests */
    static class BasicallyEmpty implements WithClass {

        static final PyType TYPE = PyType.fromSpec( //
                new TypeSpec("BasicallyEmpty", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }
    }

    /**
     * Test that caches for methods applicable to all objects (CPython
     * {@code tp_*} slots) accept only the right type of method handles.
     */
    @SuppressWarnings("static-method")
    @Test
    void basicObjectSlots() {
        // Type defining none of the reserved names
        final BaseType basic = BaseType.cast(BasicallyEmpty.TYPE);
        Object o = new BasicallyEmpty();

        assertThrows(EmptyException.class, () -> SpecialMethod.op_call.handle(basic).invokeExact(o,
                Util.EMPTY_ARRAY, Util.EMPTY_STRING_ARRAY));

        // Make method handles of the shape corresponding to caches
        MethodHandle length = MethodHandles.empty(SpecialMethod.Signature.LEN.empty.type());
        final MethodHandle unary = MethodHandles.empty(SpecialMethod.Signature.UNARY.empty.type());
        final MethodHandle binary =
                MethodHandles.empty(SpecialMethod.Signature.BINARY.empty.type());
        final MethodHandle ternary =
                MethodHandles.empty(SpecialMethod.Signature.TERNARY.empty.type());

        // These are allowed (but have no effect if not caching)
        SpecialMethod.op_hash.setCache(basic, length);
        SpecialMethod.op_str.setCache(basic, unary);

        // Preserve current settings for test at end
        MethodHandle hash0 = SpecialMethod.op_hash.handle(basic);
        MethodHandle str0 = SpecialMethod.op_str.handle(basic);

        // These should be prevented
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_str.setCache(basic, length);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_hash.setCache(basic, unary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_hash.setCache(basic, binary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_hash.setCache(basic, ternary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_hash.setCache(basic, (MethodHandle)null);
        });

        // And the slots should have the value read earlier
        assertEquals(hash0, SpecialMethod.op_hash.handle(basic), "slot modified");
        assertEquals(str0, SpecialMethod.op_str.handle(basic), "slot modified");
    }

    /**
     * Test that caches for methods applicable to numbers (CPython
     * {@code nb_*} slots) accept only the right type of method handles.
     */
    @SuppressWarnings("static-method")
    @Test
    void numericSlots() {
        // Type defining none of the reserved names
        final BaseType number = BaseType.cast(BasicallyEmpty.TYPE);
        Object o = new BasicallyEmpty();

        assertThrows(EmptyException.class,
                () -> SpecialMethod.op_neg.handle(number).invokeExact(o));
        assertThrows(EmptyException.class,
                () -> SpecialMethod.op_add.handle(number).invokeExact(o, o));

        // Make method handles of the shape corresponding to caches
        final MethodHandle length = MethodHandles.empty(SpecialMethod.Signature.LEN.empty.type());
        final MethodHandle unary = MethodHandles.empty(SpecialMethod.Signature.UNARY.empty.type());
        final MethodHandle binary =
                MethodHandles.empty(SpecialMethod.Signature.BINARY.empty.type());
        final MethodHandle ternary =
                MethodHandles.empty(SpecialMethod.Signature.TERNARY.empty.type());

        // These are allowed
        SpecialMethod.op_neg.setCache(number, unary);
        SpecialMethod.op_add.setCache(number, binary);

        // Preserve current settings for test at end
        MethodHandle neg0 = SpecialMethod.op_neg.handle(number);
        MethodHandle add0 = SpecialMethod.op_add.handle(number);

        // These should be prevented
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_neg.setCache(number, length);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_neg.setCache(number, binary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_neg.setCache(number, ternary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_neg.setCache(number, (MethodHandle)null);
        });

        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_add.setCache(number, length);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_add.setCache(number, unary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_add.setCache(number, ternary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_add.setCache(number, (MethodHandle)null);
        });

        // And the slots should have the value read earlier
        assertEquals(neg0, SpecialMethod.op_neg.handle(number), "slot modified");
        assertEquals(add0, SpecialMethod.op_add.handle(number), "slot modified");
    }

    /**
     * Test that caches for methods applicable to sequences (CPython
     * {@code sq_*} slots) accept only the right type of method handles.
     */
    @SuppressWarnings("static-method")
    @Test
    void sequenceSlots() {
        // Type defining none of the reserved names
        final BaseType sequence = BaseType.cast(BasicallyEmpty.TYPE);

        // Make method handles of the shape corresponding to caches
        final MethodHandle length = MethodHandles.empty(SpecialMethod.Signature.LEN.empty.type());
        final MethodHandle unary = MethodHandles.empty(SpecialMethod.Signature.UNARY.empty.type());
        final MethodHandle binary =
                MethodHandles.empty(SpecialMethod.Signature.BINARY.empty.type());
        final MethodHandle ternary =
                MethodHandles.empty(SpecialMethod.Signature.TERNARY.empty.type());

        // This is allowed
        SpecialMethod.op_len.setCache(sequence, length);

        // Preserve current setting for test at end
        MethodHandle len0 = SpecialMethod.op_len.handle(sequence);

        // These should be prevented
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_len.setCache(sequence, unary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_len.setCache(sequence, binary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_len.setCache(sequence, ternary);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_len.setCache(sequence, (MethodHandle)null);
        });

        // And the slot should have the value read earlier
        assertEquals(len0, SpecialMethod.op_len.handle(sequence), "slot modified");
    }

    /**
     * Test that caches for methods applicable to mappings (CPython
     * {@code mp_*} slots) accept only the right type of method handles.
     */
    @SuppressWarnings("static-method")
    @Test
    void mappingSlots() {
        // Type defining none of the reserved names
        final BaseType mapping = BaseType.cast(BasicallyEmpty.TYPE);

        // Make method handles of the shape corresponding to caches
        final MethodHandle getitem =
                MethodHandles.empty(SpecialMethod.Signature.BINARY.empty.type());
        final MethodHandle setitem =
                MethodHandles.empty(SpecialMethod.Signature.SETITEM.empty.type());
        final MethodHandle bad1 = MethodHandles.empty(SpecialMethod.Signature.SETATTR.empty.type());
        final MethodHandle bad2 = MethodHandles.empty(SpecialMethod.Signature.GETATTR.empty.type());

        // These are allowed
        SpecialMethod.op_getitem.setCache(mapping, getitem);
        SpecialMethod.op_setitem.setCache(mapping, setitem);

        // Preserve current settings for test at end
        MethodHandle getitem0 = SpecialMethod.op_getitem.handle(mapping);
        MethodHandle setitem0 = SpecialMethod.op_setitem.handle(mapping);

        // These should be prevented
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_getitem.setCache(mapping, bad1);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_getitem.setCache(mapping, bad2);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_setitem.setCache(mapping, bad2);
        });
        assertThrows(InterpreterError.class, () -> { //
            SpecialMethod.op_setitem.setCache(mapping, (MethodHandle)null);
        });

        // And the slots should have the value read earlier
        assertEquals(getitem0, SpecialMethod.op_getitem.handle(mapping), "slot modified");
        assertEquals(setitem0, SpecialMethod.op_setitem.handle(mapping), "slot modified");
    }
}
