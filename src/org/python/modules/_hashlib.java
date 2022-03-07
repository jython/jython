/* Copyright (c) Jython Developers */
package org.python.modules;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyFrozenSet;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.Untraversable;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * The Python _hashlib module: provides hashing algorithms via
 * java.security.MessageDigest.
 *
 * The 'openssl' method prefix is to match CPython and provide what the pure python
 * hashlib.py module expects.
 */
public class _hashlib implements ClassDictInit {

    /** A mapping of Python algorithm names to MessageDigest names. */
    private static final Map<String, String> algorithmMap = new HashMap<String, String>() {{
            put("sha1", "sha-1");
            put("sha224", "sha-224");
            put("sha256", "sha-256");
            put("sha384", "sha-384");
            put("sha512", "sha-512");
    }};

    public static final PyFrozenSet openssl_md_meth_names =
            new PyFrozenSet(new PyTuple(Py.newString("md5"), Py.newString("sha1"),
            Py.newString("sha224"), Py.newString("sha256"), Py.newString("sha384"),
            Py.newString("sha512")));

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", Py.newString("_hashlib"));
        dict.__setitem__("algorithmMap", null);
        dict.__setitem__("classDictInit", null);
    }

    public static PyObject new$(String name) {
        return new$(name, null);
    }

    public static PyObject new$(String name, PyObject obj) {
        name = name.toLowerCase();
        // NOTE: we're not disallowing other MessageDigest algorithms
        if (algorithmMap.containsKey(name)) {
            name = algorithmMap.get(name);
        }

        Hash hash = new Hash(name);
        if (obj != null) {
            hash.update(obj);
        }
        return hash;
    }

    public static PyObject openssl_md5() {
        return openssl_md5(null);
    }

    public static PyObject openssl_md5(PyObject obj) {
        return new$("md5", obj);
    }

    public static PyObject openssl_sha1() {
        return openssl_sha1(null);
    }

    public static PyObject openssl_sha1(PyObject obj) {
        return new$("sha1", obj);
    }

    public static PyObject openssl_sha224() {
        return openssl_sha224(null);
    }

    public static PyObject openssl_sha224(PyObject obj) {
        return new$("sha224", obj);
    }

    public static PyObject openssl_sha256() {
        return openssl_sha256(null);
    }

    public static PyObject openssl_sha256(PyObject obj) {
        return new$("sha256", obj);
    }

    public static PyObject openssl_sha384() {
        return openssl_sha384(null);
    }

    public static PyObject openssl_sha384(PyObject obj) {
        return new$("sha384", obj);
    }

    public static PyObject openssl_sha512() {
        return openssl_sha512(null);
    }

    public static PyObject openssl_sha512(PyObject obj) {
        return new$("sha512", obj);
    }

    /**
     * A generic wrapper around a MessageDigest.
     */
    @Untraversable
    @ExposedType(name = "_hashlib.HASH")
    public static class Hash extends PyObject {

        public static final PyType TYPE = PyType.fromClass(Hash.class);

        /** The hash algorithm name. */
        @ExposedGet
        public String name;

        /** The hashing engine. */
        private MessageDigest digest;

        /** Supposed block sizes of algorithms for the block_size attribute. */
        private static final Map<String, Integer> blockSizes = new HashMap<String, Integer>() {{
                put("md5", 64);
                put("sha-1", 64);
                put("sha-224", 64);
                put("sha-256", 64);
                put("sha-384", 128);
                put("sha-512", 128);
            }};

        public Hash(String name) {
            this(name, getDigest(name));
        }

        private Hash(String name, MessageDigest digest) {
            super(TYPE);
            this.name = name;
            this.digest = digest;
        }

        private static MessageDigest getDigest(String name) {
            try {
                return MessageDigest.getInstance(name);
            } catch (NoSuchAlgorithmException nsae) {
                throw Py.ValueError("unsupported hash type");
            }
        }

        /**
         * Clone the underlying MessageDigest.
         *
         * @return a copy of MessageDigest
         */
        private MessageDigest cloneDigest() {
            try {
                synchronized (this) {
                    return (MessageDigest)digest.clone();
                }
            } catch (CloneNotSupportedException cnse) {
                throw Py.RuntimeError(String.format("_hashlib.HASH (%s) internal error", name));
            }
        }

        /**
         * Safely calculate the digest without resetting state.
         *
         * @return a byte[] calculated digest
         */
        private byte[] calculateDigest() {
            return cloneDigest().digest();
        }

        public void update(PyObject obj) {
            HASH_update(obj);
        }

        @ExposedMethod
        final void HASH_update(PyObject obj) {
            String string;
            if (obj instanceof PyUnicode) {
                string = ((PyUnicode)obj).encode();
            } else if (obj instanceof PyString) {
                string = obj.toString();
            } else if (obj instanceof PyArray) {
                string = ((PyArray)obj).tostring();
            } else {
                throw Py.TypeError("update() argument 1 must be string or read-only buffer, not "
                                   + obj.getType().fastGetName());
            }
            byte[] input = StringUtil.toBytes(string);
            synchronized (this) {
                digest.update(input);
            }
        }

        public PyObject digest() {
            return HASH_digest();
        }

        @ExposedMethod
        final PyObject HASH_digest() {
            return Py.newString(StringUtil.fromBytes(calculateDigest()));
        }

        public PyObject hexdigest() {
            return HASH_hexdigest();
        }

        @ExposedMethod
        final PyObject HASH_hexdigest() {
            byte[] result = calculateDigest();
            // Make hex version of the digest
            char[] hexDigest = new char[result.length * 2];
            for (int i = 0, j = 0; i < result.length; i++) {
                int c = ((result[i] >> 4) & 0xf);
                c = c > 9 ? c + 'a' - 10 : c + '0';
                hexDigest[j++] = (char)c;
                c = result[i] & 0xf;
                c = c > 9 ? c + 'a' - 10 : c + '0';
                hexDigest[j++] = (char)c;
            }
            return Py.newString(new String(hexDigest));
        }

        public PyObject copy() {
            return HASH_copy();
        }

        @ExposedMethod
        final PyObject HASH_copy() {
            return new Hash(name, cloneDigest());
        }

        @ExposedGet(name = "digestsize")
        public synchronized int getDigestSize() {
            return digest.getDigestLength();
        }

        @ExposedGet(name = "digest_size")
        public int getDigest_size() {
            return getDigestSize();
        }

        @ExposedGet(name = "block_size")
        public PyObject getBlockSize() {
            Integer size = blockSizes.get(name);
            if (size == null) {
                return Py.None;
            }
            return Py.newInteger(size);
        }

        @Override
        public String toString() {
            return String.format("<%s HASH object @ %s>", name, Py.idstr(this));
        }
    }
}
