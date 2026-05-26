// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class for loading compiled Python modules and Java classes defined in Python modules.
 */
public class BytecodeLoader {

    /**
     * Backing field of {@link #getIgnoreSerialVersionUID()} and {@link #setIgnoreSerialVersionUID(boolean)}.
     * Default value is {@code true}.
     *
     * @see #getIgnoreSerialVersionUID()
     * @see #setIgnoreSerialVersionUID(boolean)
     */
    private static boolean ignoreSerialVersionUID = true;

    /**
     * Only relevant in presence of oversized methods or functions, this tells whether or not
     * to ignore {@code serialVersionUID} associated with the corresponding code objects.
     * Default value is {@code true}.
     * See {@link #setIgnoreSerialVersionUID(boolean)} for details.
     *
     * @see #setIgnoreSerialVersionUID(boolean)
     * @see #fixPyBytecode(Class)
     *
     * @return whether or not {@code serialVersionUID} data is ignored when {@link PyBytecode}
     *         objects are parsed via Java serialization in {@link #fixPyBytecode(Class)},
     *         default is {@code true}
     */
    public static boolean getIgnoreSerialVersionUID() {
        return ignoreSerialVersionUID;
    }

    /**
     * Only relevant in presence of oversized methods or functions, this controls whether or
     * not to ignore {@code serialVersionUID} associated with the corresponding code objects.
     * Methods and functions in Python might not be compilable to JVM bytecode if they exceed a
     * certain size. In that case, Jython can use compiled CPython bytecode (as in {@code .pyc}
     * files) instead. This bytecode is embedded into the module's {@code $py.class} file by
     * storing the {@link PyBytecode} object via Java serialization in string constants.
     * This mechanism can break compatibility with older Jython versions if computed values
     * of {@code serialVersionUID} changed due to subtle code adjustments. Usually, compatibility
     * is not actually broken, so Jython bypasses the check for {@code serialVersionUID} as far
     * as objects comprised by a {@link PyBytecode} object are concerned.
     * This option allows to control that behavior. By setting it to {@code false}, a strict check
     * for {@code serialVersionUID} is required. By default, the value of this option is
     * {@code true} and the check is bypassed.
     *
     * @see #getIgnoreSerialVersionUID()
     * @see #fixPyBytecode(Class)
     * @see <a href="https://github.com/jython/jython/issues/416">Solve serialization backward
     *      incompatibility of oversized methods and functions.</a>
     * @see <a href="https://github.com/jython/jython/issues/234">Clarification on compile
     *      (Module or method too large)</a>
     *
     * @param value indicates whether or not {@code serialVersionUID} data is to be ignored when
     *              {@link PyBytecode} objects are parsed via Java serialization in
     *              {@link #fixPyBytecode(Class)}, default is {@code true}.
     */
    public static void setIgnoreSerialVersionUID(boolean value) {
        ignoreSerialVersionUID = value;
    }

    /**
     * Turn the Java class file data into a Java class.
     *
     * @param name fully-qualified binary name of the class
     * @param data a class file as a byte array
     * @param referents super-classes and interfaces that the new class will reference.
     */
    @SuppressWarnings("unchecked")
    public static Class<?> makeClass(String name, byte[] data, Class<?>... referents) {
        @SuppressWarnings("resource")
        Loader loader = new Loader();
        for (Class<?> referent : referents) {
            try {
                loader.addParent(referent.getClassLoader());
            } catch (SecurityException e) {}
        }
        Class<?> c = loader.loadClassFromBytes(name, data);
        if (ContainsPyBytecode.class.isAssignableFrom(c)) {
            try {
                fixPyBytecode((Class<? extends ContainsPyBytecode>) c);
            } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException
                    | IOException e) {
                throw new RuntimeException(e);
            }
        }
        BytecodeNotification.notify(name, data, c);
        return c;
    }

    /**
     * Turn the Java class file data into a Java class.
     *
     * @param name the name of the class
     * @param referents super-classes and interfaces that the new class will reference.
     * @param data a class file as a byte array
     */
    public static Class<?> makeClass(String name, List<Class<?>> referents, byte[] data) {
        if (referents != null) {
            return makeClass(name, data, referents.toArray(new Class[referents.size()]));
        }
        return makeClass(name, data);
    }

    /**
     * See {@link #setIgnoreSerialVersionUID(boolean)} for details of this mechanism.
     * Restores a {@link PyBytecode} object serialized into a string.
     *
     * @see #getIgnoreSerialVersionUID()
     * @see #setIgnoreSerialVersionUID(boolean)
     * @see #fixPyBytecode(Class)
     */
    private static PyBytecode parseSerializedCode(String code_str)
            throws IOException, ClassNotFoundException {
        // From Java 8 use: byte[] b = Base64.getDecoder().decode(code_str);
        byte[] b = base64decode(code_str);
        ByteArrayInputStream bi = new ByteArrayInputStream(b);
        ObjectInputStream si;
        if (ignoreSerialVersionUID) {
            // https://github.com/jython/jython/issues/416
            si = new ObjectInputStream(bi) {
                @Override
                protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
                    ObjectStreamClass incoming = super.readClassDescriptor();
                    ObjectStreamClass local = ObjectStreamClass.lookup(Class.forName(incoming.getName()));
                    if (local != null) {
                        if (incoming.getSerialVersionUID() != local.getSerialVersionUID()) {
                            // Replace the descriptor to bypass serialVersionUID mismatch
                            return local;
                        }
                    }
                    return incoming;
                }
            };
        } else {
            si = new ObjectInputStream(bi);
        }
        try {
            return (PyBytecode) si.readObject();
        } catch (InvalidClassException e) {
            throw Py.ImportError(
                    "compiled unit contains incompatible serialized objects (for oversized function handling): "
                    + e.getMessage());
        } finally {
            si.close();
            bi.close();
        }
    }

    /**
     * Implement a restricted form of base64 decoding compatible with the encoding in Module. This
     * decoder treats characters outside the set of 64 necessary to encode data as errors, including
     * the pad "=". As a result, the length of the argument exactly determines the size of array
     * returned.
     *
     * @param src to decode
     * @return a new byte array
     * @throws IllegalArgumentException if src has an invalid character or impossible length.
     */
    private static byte[] base64decode(String src) throws IllegalArgumentException {

        // Length L is a multiple of 4 plus 0, 2 or 3 tail characters (bearing 0, 8, or 16 bits)
        final int L = src.length();
        final int tail = L % 4; // 0 to 3 where 1 (an extra 6 bits) is invalid.
        if (tail == 1) {
            throw new IllegalArgumentException("Input length invalid (4n+1)");
        }

        // src encodes exactly this many bytes:
        final int N = (L / 4) * 3 + (tail > 0 ? tail - 1 : 0);
        byte[] data = new byte[N];

        // Work through src in blocks of 4
        int s = 0, b = 0, quantum;
        while (s <= L - 4) {
            // Process src[s:s+4]
            quantum = (base64CharToBits(src.charAt(s++)) << 18)
                    + (base64CharToBits(src.charAt(s++)) << 12)
                    + (base64CharToBits(src.charAt(s++)) << 6) + base64CharToBits(src.charAt(s++));
            data[b++] = (byte) (quantum >> 16);
            data[b++] = (byte) (quantum >> 8);
            data[b++] = (byte) quantum;
        }

        // Now deal with 2 or 3 tail characters, generating one or two bytes.
        if (tail >= 2) {
            // Repeat the loop body, but everything is 8 bits to the right.
            quantum = (base64CharToBits(src.charAt(s++)) << 10)
                    + (base64CharToBits(src.charAt(s++)) << 4);
            data[b++] = (byte) (quantum >> 8);
            if (tail == 3) {
                quantum += (base64CharToBits(src.charAt(s++)) >> 2);
                data[b++] = (byte) quantum;
            }
        }

        return data;
    }

    /**
     * Helper for {@link #base64decode(String)}, converting one character.
     *
     * @param c to convert
     * @return value 0..63
     * @throws IllegalArgumentException if not a base64 character
     */
    private static int base64CharToBits(char c) throws IllegalArgumentException {
        if (c >= 'a') {
            if (c <= 'z') {
                return c - 71; // c - 'a' + 26
            }
        } else if (c >= 'A') {
            if (c <= 'Z') {
                return c - 'A';
            }
        } else if (c >= '0') {
            if (c <= '9') {
                return c + 4; // c - '0' + 52
            }
        } else if (c == '+') {
            return 62;
        } else if (c == '/') {
            return 63;
        }
        throw new IllegalArgumentException("Invalid character " + c);
    }

    /**
     * This method looks for Python bytecode stored in string literals.
     * While Java supports rather long strings, constrained only by
     * {@code int}-addressing of arrays, it supports only up to 65535 characters
     * in literals (not sure how escape-sequences are counted).
     * To circumvent this limitation, the code is automatically split
     * into several literals with the following naming-scheme.
     * <p>
     * The marker-interface {@link ContainsPyBytecode} indicates that a class
     * contains ({@code static final}) literals of the following scheme:
     * <ul>
     * <li>a prefix of '{@code ___}' indicates a bytecode-containing string literal
     * <li>a number indicating the number of parts follows
     * <li>'{@code 0_}' indicates that no splitting occurred
     * <li>otherwise another number follows, naming the index of the literal
     * <li>indexing starts at {@code 0}
     * </ul>
     * <p>
     * Examples:
     * <ul>
     * <li>{@code ___0_method1}   contains bytecode for {@code method1}
     * <li>{@code ___2_0_method2} contains first part of {@code method2}'s bytecode
     * <li>{@code ___2_1_method2} contains second part of {@code method2}'s bytecode
     * </ul>
     * <p>
     * Note that this approach is provisional. In future, Jython might contain
     * the bytecode directly as bytecode objects. The current approach was
     * feasible with much less complicated JVM bytecode manipulation, but needs
     * special treatment after class loading.
     *
     * @see ContainsPyBytecode
     */
    public static void fixPyBytecode(Class<? extends ContainsPyBytecode> c)
            throws IllegalAccessException, NoSuchFieldException, java.io.IOException,
            ClassNotFoundException {
        Field[] fields = c.getDeclaredFields();
        for (Field fld: fields) {
            String fldName = fld.getName();
            if (fldName.startsWith("___")) {
                fldName = fldName.substring(3);

                String[] splt = fldName.split("_");
                if (splt[0].equals("0")) {
                    fldName = fldName.substring(2);
                    Field codeField = c.getDeclaredField(fldName);
                    if (codeField.get(null) == null) {
                        codeField.set(null, parseSerializedCode((String) fld.get(null)));
                    }
                } else {
                    if (splt[1].equals("0")) {
                        fldName = fldName.substring(splt[0].length()+splt[1].length()+2);
                        Field codeField = c.getDeclaredField(fldName);
                        if (codeField.get(null) == null) {
                            // assemble original code-string:
                            int len = Integer.parseInt(splt[0]);
                            StringBuilder blt = new StringBuilder((String) fld.get(null));
                            int pos = 1, pos0;
                            String partName;
                            while (pos < len) {
                                pos0 = pos;
                                for (Field fldPart: fields) {
                                    partName = fldPart.getName();
                                    if (partName.length() != fldName.length() &&
                                            partName.startsWith("___") &&
                                            partName.endsWith(fldName)) {
                                        String[] splt2 = partName.substring(3).split("_");
                                        if (Integer.parseInt(splt2[1]) == pos) {
                                            blt.append((String) fldPart.get(null));
                                            pos += 1;
                                            if (pos == len) {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (pos0 == pos) {
                                    throw new RuntimeException(
                                            "Invalid PyBytecode splitting in " + c.getName()
                                                    + ":\nSplit-index " + pos + " wasn't found.");
                                }
                            }
                            codeField.set(null, parseSerializedCode(blt.toString()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Turn the Java class file data for a compiled Python module into a {@code PyCode} object, by
     * constructing an instance of the named class and calling the instance's
     * {@link PyRunnable#getMain()}.
     *
     * @param name fully-qualified binary name of the class
     * @param data a class file as a byte array
     * @param filename to provide to the constructor of the named class
     * @return the {@code PyCode} object produced by the named class' {@code getMain}
     */
    public static PyCode makeCode(String name, byte[] data, String filename) {
        try {
            Class<?> c = makeClass(name, data);
            // A compiled module has a constructor taking a String filename argument.
            Constructor<?> cons = c.getConstructor(new Class<?>[] {String.class});
            Object instance = cons.newInstance(new Object[] {filename});
            PyCode result = ((PyRunnable) instance).getMain();
            return result;
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
    }

    public static class Loader extends URLClassLoader {

        private LinkedList<ClassLoader> parents = new LinkedList<>();

        public Loader() {
            super(new URL[0]);
            parents.add(imp.getSyspathJavaLoader());
        }

        /** 
         * Add given loader at the front of the list of the parent list (if not {@code null}).
         */
        public void addParent(ClassLoader referent) {
            if (referent != null && !parents.contains(referent)) {
                parents.addFirst(referent);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            for (ClassLoader loader : parents) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException cnfe) {}
            }
            // couldn't find the .class file on sys.path
            throw new ClassNotFoundException(name);
        }

        /**
         * Define the named class using the class file data provided, and resolve it. (See JVM
         * specification.) For class names ending "{@code $py}", this method may adjust that
         * name to that found in the class file itself.
         *
         * @param name fully-qualified binary name of the class
         * @param data a class file as a byte array
         * @return the defined and resolved class
         */
        public Class<?> loadClassFromBytes(String name, byte[] data) {
            if (name.endsWith("$py")) {
                try {
                    // Get the real class name: we might request a 'bar'
                    // Jython module that was compiled as 'foo.bar', or
                    // even 'baz.__init__' which is compiled as just 'baz'
                    ClassReader cr = new ClassReader(data);
                    name = cr.getClassName().replace('/', '.');
                } catch (RuntimeException re) {
                    // Probably an invalid .class, fallback to the
                    // specified name
                }
            }
            Class<?> c = defineClass(name, data, 0, data.length, getClass().getProtectionDomain());
            resolveClass(c);
            return c;
        }
    }
}
