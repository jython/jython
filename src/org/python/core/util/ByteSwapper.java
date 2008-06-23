/*
 * Copyright 2005 Andrew Howard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.python.core.util;

import java.lang.reflect.Array;

/**
 * Simple class that provides the capability to swap or reverse the byte order
 * of all elements of an <code>Array</code>. Used to convert from one endian
 * type to another. The class swaps the following types:
 * <ul>
 * <li>short</li>
 * <li>integer</li>
 * <li>long</li>
 * <li>float</li>
 * <li>double</li>
 * </ul>
 * <p />
 * Note this functionality is provided in the base types since 1.5.
 * 
 * @author Andrew Howard
 */
public class ByteSwapper {

    /**
     * Reverses the byte order of all elements in the supplied array, converting
     * between little and big endian byte order.
     * 
     * @param array the input array for type sensitive byte swapping.
     */
    public static void swap(Object array) {
        Class arrayType = array.getClass().getComponentType();

        if (arrayType.isPrimitive()) {
            if (arrayType == Boolean.TYPE) {
                return;
            } else if (arrayType == Byte.TYPE) {
                return;
            } else if (arrayType == Character.TYPE) {
                return;
            } else if (arrayType == Short.TYPE) {
                swapShortArray(array);
            } else if (arrayType == Integer.TYPE) {
                swapIntegerArray(array);
            } else if (arrayType == Long.TYPE) {
                swapLongArray(array);
            } else if (arrayType == Float.TYPE) {
                swapFloatArray(array);
            } else if (arrayType == Double.TYPE) {
                swapDoubleArray(array);
            }
        }

    }

    /**
     * Byte order reverses an <code>Array</code> of <code>doubles</code>
     * 
     * @param array input array
     */
    private static void swapDoubleArray(Object array) {
        int len = Array.getLength(array);
        double dtmp;
        long tmp;
        long b1, b2, b3, b4, b5, b6, b7, b8;

        for (int i = 0; i < len; i++) {
            dtmp = Array.getDouble(array, i);
            tmp = Double.doubleToLongBits(dtmp);

            b1 = (tmp >> 0) & 0xff;
            b2 = (tmp >> 8) & 0xff;
            b3 = (tmp >> 16) & 0xff;
            b4 = (tmp >> 24) & 0xff;
            b5 = (tmp >> 32) & 0xff;
            b6 = (tmp >> 40) & 0xff;
            b7 = (tmp >> 48) & 0xff;
            b8 = (tmp >> 56) & 0xff;
            tmp = b1 << 56 | b2 << 48 | b3 << 40 | b4 << 32 | b5 << 24
                    | b6 << 16 | b7 << 8 | b8 << 0;

            dtmp = Double.longBitsToDouble(tmp);
            Array.setDouble(array, i, dtmp);
        }
    }

    /**
     * Byte order reverses an <code>Array</code> of <code>floats</code>
     * 
     * @param array input array
     */
    private static void swapFloatArray(Object array) {
        int len = Array.getLength(array);
        float ftmp;
        int tmp;
        int b1, b2, b3, b4;

        for (int i = 0; i < len; i++) {
            ftmp = Array.getFloat(array, i);
            tmp = Float.floatToIntBits(ftmp);

            b1 = (tmp >> 0) & 0xff;
            b2 = (tmp >> 8) & 0xff;
            b3 = (tmp >> 16) & 0xff;
            b4 = (tmp >> 24) & 0xff;
            tmp = b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;

            ftmp = Float.intBitsToFloat(tmp);
            Array.setFloat(array, i, ftmp);
        }
    }

    /**
     * Byte order reverses an <code>Array</code> of <code>ints</code>
     * 
     * @param array input array
     */
    private static void swapIntegerArray(Object array) {
        int len = Array.getLength(array);
        int tmp;
        int b1, b2, b3, b4;

        for (int i = 0; i < len; i++) {
            tmp = Array.getInt(array, i);

            b1 = (tmp >> 0) & 0xff;
            b2 = (tmp >> 8) & 0xff;
            b3 = (tmp >> 16) & 0xff;
            b4 = (tmp >> 24) & 0xff;
            tmp = b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;

            Array.setInt(array, i, tmp);
        }
    }

    /**
     * Byte order reverses an <code>Array</code> of <code>longs</code>
     * 
     * @param array input array
     */
    private static void swapLongArray(Object array) {
        int len = Array.getLength(array);
        long tmp;
        long b1, b2, b3, b4, b5, b6, b7, b8;

        for (int i = 0; i < len; i++) {
            tmp = Array.getLong(array, i);

            b1 = (tmp >> 0) & 0xff;
            b2 = (tmp >> 8) & 0xff;
            b3 = (tmp >> 16) & 0xff;
            b4 = (tmp >> 24) & 0xff;
            b5 = (tmp >> 32) & 0xff;
            b6 = (tmp >> 40) & 0xff;
            b7 = (tmp >> 48) & 0xff;
            b8 = (tmp >> 56) & 0xff;
            tmp = b1 << 56 | b2 << 48 | b3 << 40 | b4 << 32 | b5 << 24
                    | b6 << 16 | b7 << 8 | b8 << 0;

            Array.setLong(array, i, tmp);
        }
    }

    /**
     * Byte order reverses an <code>Array</code> of <code>shorts</code>
     * 
     * @param array input array
     */
    private static void swapShortArray(Object array) {
        int len = Array.getLength(array);
        short tmp;
        int b1, b2;

        for (int i = 0; i < len; i++) {
            tmp = Array.getShort(array, i);

            b1 = (tmp >> 0) & 0xff;
            b2 = (tmp >> 8) & 0xff;
            tmp = (short) (b1 << 8 | b2 << 0);

            Array.setShort(array, i, tmp);
        }
    }
}
