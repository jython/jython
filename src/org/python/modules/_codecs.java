/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) Corporation for National Research Initiatives.
 * Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */

package org.python.modules;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.codecs;

public class _codecs {

    public static void register(PyObject search_function) {
        codecs.register(search_function);
    }


    public static PyTuple lookup(String encoding) {
        return codecs.lookup(encoding);
    }

    public static PyObject lookup_error(String handlerName) {
        return codecs.lookup_error(handlerName);
    }

    public static void register_error(String name, PyObject errorHandler) {
        codecs.register_error(name, errorHandler);
    }

    private static PyTuple decode_tuple(String s, int len) {
        return new PyTuple(new PyObject[] {
            new PyUnicode(s),
            Py.newInteger(len)
        });
    }

    private static PyTuple decode_tuple_str(String s, int len) {
        return new PyTuple(new PyObject[] {
            new PyString(s),
            Py.newInteger(len)
        });
    }

    private static PyTuple encode_tuple(String s, int len) {
        return new PyTuple(new PyObject[] {
            Py.java2py(s),
            Py.newInteger(len)
        });
    }


    /* --- UTF-8 Codec --------------------------------------------------- */

    public static PyTuple utf_8_decode(String str) {
        return utf_8_decode(str, null);
    }

    public static PyTuple utf_8_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeUTF8(str, errors), size);
    }


    public static PyTuple utf_8_encode(String str) {
        return utf_8_encode(str, null);
    }

    public static PyTuple utf_8_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF8(str, errors), size);
    }


    /* --- UTF-7 Codec --------------------------------------------------- */

    public static PyTuple utf_7_decode(String str) {
        return utf_7_decode(str, null);
    }

    public static PyTuple utf_7_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeUTF7(str, errors), size);
    }


    public static PyTuple utf_7_encode(String str) {
        return utf_7_encode(str, null);
    }

    public static PyTuple utf_7_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF7(str, false, false, errors), size);
    }
    
    public static PyTuple escape_decode(String str){
        return escape_decode(str, null);
    }

    public static PyTuple escape_decode(String str, String errors) {
        return decode_tuple_str(PyString.decode_UnicodeEscape(str,
                                                          0,
                                                          str.length(),
                                                          errors,
                                                          true), str.length());
    }
    
    public static PyTuple escape_encode(String str){
        return escape_encode(str, null);
    }   
    
    public static PyTuple escape_encode(String str, String errors) {
        return encode_tuple(PyString.encode_UnicodeEscape(str, false),
                                                                                str.length());
       
    }

    /* --- Character Mapping Codec --------------------------------------- */

    public static PyTuple charmap_decode(String str,
                                         String errors,
                                         PyObject mapping) {
        return charmap_decode(str, errors, mapping, false);
    }

    public static PyTuple charmap_decode(String str,
                                         String errors,
                                         PyObject mapping, boolean ignoreUnmapped) {
    
    
        int size = str.length();
        StringBuffer v = new StringBuffer(size);
        for(int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if(ch > 0xFF) {
                i = codecs.insertReplacementAndGetResume(v,
                                                         errors,
                                                         "charmap",
                                                         str,
                                                         i,
                                                         i + 1,
                                                         "ordinal not in range(255)") - 1;
                continue;
            }
            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if(x == null) {
                if(ignoreUnmapped){
                    v.append(ch);
                }else{
i = codecs.insertReplacementAndGetResume(v, errors, "charmap", str, i, i + 1, "no mapping found") - 1;
                }
                continue;
            }
            /* Apply mapping */
            if(x instanceof PyInteger) {
                int value = ((PyInteger)x).getValue();
                if(value < 0 || value > PySystemState.maxunicode) {
                    throw Py.TypeError("character mapping must return "
                            + "integer greater than 0 and less than sys.maxunicode");
                }
                v.append((char)value);
            } else if(x == Py.None) {
                i = codecs.insertReplacementAndGetResume(v,
                                                         errors,
                                                         "charmap",
                                                         str,
                                                         i,
                                                         i + 1,
                                                         "character maps to <undefined>") - 1;
            } else if(x instanceof PyString) {
                v.append(x.toString());
            } else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return "
                        + "integer, None or str");
            }
        }
        return decode_tuple(v.toString(), size);
    }





    public static PyTuple charmap_encode(String str, String errors,
                                         PyObject mapping) {
        //Default to Latin-1
        if(mapping == null){
            return latin_1_encode(str, errors);
        }
        return charmap_encode_internal(str, errors, mapping, new StringBuffer(str.length()), true);
    }
    
    private static PyTuple charmap_encode_internal(String str,
                                                   String errors,
                                                   PyObject mapping,
                                                   StringBuffer v,
                                                   boolean letLookupHandleError) {
        int size = str.length();
        for(int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if(x == null) {
                if(letLookupHandleError) {
                    i = handleBadMapping(str, errors, mapping, v, size, i);
                } else {
                    throw Py.UnicodeEncodeError("charmap",
                                                str,
                                                i,
                                                i + 1,
                                                "character maps to <undefined>");
                }
            }else 
            if(x instanceof PyInteger) {
                int value = ((PyInteger)x).getValue();
                if(value < 0 || value > 255)
                    throw Py.TypeError("character mapping must be in range(256)");
                v.append((char)value);
            }  else if(x instanceof PyString  && !(x instanceof PyUnicode)) {
                v.append(x.toString());
            } else if(x instanceof PyNone){
                i = handleBadMapping(str, errors, mapping, v, size, i);
            }else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return "
                        + "integer, None or str");
            }
        }
        return encode_tuple(v.toString(), size);
    }

    private static int handleBadMapping(String str,
                                        String errors,
                                        PyObject mapping,
                                        StringBuffer v,
                                        int size,
                                        int i) {
        if(errors != null) {
            if(errors.equals(codecs.IGNORE)) {
                return i;
            } else if(errors.equals(codecs.REPLACE)) {
                charmap_encode_internal("?", errors, mapping, v, false);
                return i;
            } else if(errors.equals(codecs.XMLCHARREFREPLACE)) {
                charmap_encode_internal(codecs.xmlcharrefreplace(i, i + 1, str)
                        .toString(), errors, mapping, v, false);
                return i;
            } else if(errors.equals(codecs.BACKSLASHREPLACE)) {
                charmap_encode_internal(codecs.backslashreplace(i, i + 1, str)
                        .toString(), errors, mapping, v, false);
                return i;
            }
        }
        PyObject replacement = codecs.encoding_error(errors,
                                                     "charmap",
                                                     str,
                                                     i,
                                                     i + 1,
                                                     "character maps to <undefined>");
        String replStr = replacement.__getitem__(0).toString();
        charmap_encode_internal(replStr, errors, mapping, v, false);
        return codecs.calcNewPosition(size, replacement) - 1;
    }


    public static PyTuple ascii_decode(String str) {
        return ascii_decode(str, null);
    }

    public static PyTuple ascii_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeASCII(str, size, errors),
                                                                        size);
    }


    public static PyTuple ascii_encode(String str) {
        return ascii_encode(str, null);
    }

    public static PyTuple ascii_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeASCII(str, size, errors),
                                                                        size);
    }


    /* --- Latin-1 Codec -------------------------------------------- */

    public static PyTuple latin_1_decode(String str) {
        return latin_1_decode(str, null);
    }

    public static PyTuple latin_1_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeLatin1(str, size, errors),
                                                                        size);
    }


    public static PyTuple latin_1_encode(String str) {
        return latin_1_encode(str, null);
    }

    public static PyTuple latin_1_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeLatin1(str, size, errors), size);
    }


    /* --- UTF16 Codec -------------------------------------------- */


    public static PyTuple utf_16_encode(String str) {
        return utf_16_encode(str, null);
    }

    public static PyTuple utf_16_encode(String str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, 0), str.length());
    }

    public static PyTuple utf_16_encode(String str, String errors,
                                       int byteorder) {
        return encode_tuple(encode_UTF16(str, errors, byteorder),
                           str.length());
    }

    public static PyTuple utf_16_le_encode(String str) {
        return utf_16_le_encode(str, null);
    }

    public static PyTuple utf_16_le_encode(String str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, -1), str.length());
    }

    public static PyTuple utf_16_be_encode(String str) {
        return utf_16_be_encode(str, null);
    }

    public static PyTuple utf_16_be_encode(String str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, 1), str.length());
    }


    private static String encode_UTF16(String str, String errors,
                                      int byteorder) {
        int size = str.length();
        StringBuffer v = new StringBuffer((size +
                                       (byteorder == 0 ? 1 : 0)) * 2);

        if (byteorder == 0) {
            v.append((char) 0xFE);
            v.append((char) 0xFF);
        }

        if (byteorder == 0 || byteorder == 1)
            for (int i = 0; i < size; i++) {
                char ch = str.charAt(i);
                v.append((char) ((ch >>> 8) & 0xFF));
                v.append((char) (ch & 0xFF));
            }
        else {
            for (int i = 0; i < size; i++) {
                char ch = str.charAt(i);
                v.append((char) (ch & 0xFF));
                v.append((char) ((ch >>> 8) & 0xFF));
            }
        }

        return v.toString();
    }




    public static PyTuple utf_16_decode(String str) {
        return utf_16_decode(str, null);
    }

    public static PyTuple utf_16_decode(String str, String errors) {
        return utf_16_decode(str, errors, 0);
    }

    public static PyTuple utf_16_decode(String str, String errors,
                                        int byteorder) {
        int[] bo = new int[] { byteorder };
        return decode_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_le_decode(String str) {
        return utf_16_le_decode(str, null);
    }

    public static PyTuple utf_16_le_decode(String str, String errors) {
        int[] bo = new int[] { -1 };
        return decode_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_be_decode(String str) {
        return utf_16_be_decode(str, null);
    }

    public static PyTuple utf_16_be_decode(String str, String errors) {
        int[] bo = new int[] { 1 };
        return decode_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_ex_decode(String str) {
        return utf_16_ex_decode(str, null);
    }

    public static PyTuple utf_16_ex_decode(String str, String errors) {
        return utf_16_ex_decode(str, errors, 0);
    }

    public static PyTuple utf_16_ex_decode(String str, String errors,
                                           int byteorder) {
        int[] bo = new int[] { 0 };
        String s = decode_UTF16(str, errors, bo);
        return new PyTuple(new PyObject[] {
             Py.newString(s),
             Py.newInteger(str.length()),
             Py.newInteger(bo[0])
        });
    }

    private static String decode_UTF16(String str,
                                       String errors,
                                       int[] byteorder) {
        int bo = 0;
        if(byteorder != null)
            bo = byteorder[0];
        int size = str.length();
        StringBuffer v = new StringBuffer(size / 2);
        for(int i = 0; i < size; i += 2) {
            char ch1 = str.charAt(i);
            if(i + 1 == size) {
                i = codecs.insertReplacementAndGetResume(v,
                                                         errors,
                                                         "utf-16",
                                                         str,
                                                         i,
                                                         i + 1,
                                                         "truncated data");
                continue;
            }
            char ch2 = str.charAt(i + 1);
            if(ch1 == 0xFE && ch2 == 0xFF) {
                bo = 1;
                continue;
            } else if(ch1 == 0xFF && ch2 == 0xFE) {
                bo = -1;
                continue;
            }
            char ch;
            if(bo == -1)
                ch = (char)(ch2 << 8 | ch1);
            else
                ch = (char)(ch1 << 8 | ch2);
            if(ch < 0xD800 || ch > 0xDFFF) {
                v.append(ch);
                continue;
            }
            ch = str.charAt(++i);
            if(0xDC00 <= ch && ch <= 0xDFFF) {
                ch2 = str.charAt(++i);
                if(0xD800 <= ch2 && ch2 <= 0xDBFF) {
                    v.append(ch);
                    v.append(ch2);
                    continue;
                }
                i = codecs.insertReplacementAndGetResume(v,
                                                         errors,
                                                         "utf-16",
                                                         str,
                                                         i,
                                                         i + 1,
                                                         "illegal UTF-16 surrogate");
                continue;
            }
            i = codecs.insertReplacementAndGetResume(v,
                                                     errors,
                                                     "utf-16",
                                                     str,
                                                     i,
                                                     i + 1,
                                                     "illegal encoding");
        }
        if(byteorder != null)
            byteorder[0] = bo;
        return v.toString();
    }



    /* --- RawUnicodeEscape Codec ----------------------------------------- */


    public static PyTuple raw_unicode_escape_encode(String str) {
        return raw_unicode_escape_encode(str, null);
    }

    public static PyTuple raw_unicode_escape_encode(String str,
                                                   String errors) {
        return encode_tuple(codecs.PyUnicode_EncodeRawUnicodeEscape(str,
                                                             errors, false),
                           str.length());
    }


    public static PyTuple raw_unicode_escape_decode(String str) {
        return raw_unicode_escape_decode(str, null);
    }

    public static PyTuple raw_unicode_escape_decode(String str,
                                                    String errors) {
        return decode_tuple(codecs.PyUnicode_DecodeRawUnicodeEscape(str,
                                                             errors),
                           str.length());
    }



    /* --- UnicodeEscape Codec -------------------------------------------- */


    public static PyTuple unicode_escape_encode(String str) {
        return unicode_escape_encode(str, null);
    }

    public static PyTuple unicode_escape_encode(String str, String errors) {
        return encode_tuple(PyString.encode_UnicodeEscape(str, false),
                           str.length());
    }

    public static PyTuple unicode_escape_decode(String str) {
        return unicode_escape_decode(str, null);
    }

    public static PyTuple unicode_escape_decode(String str, String errors) {
        int n = str.length();
        return decode_tuple(PyString.decode_UnicodeEscape(str,
                                                          0,
                                                          n,
                                                          errors,
                                                          true), n);
    }

    /* --- UnicodeInternal Codec ------------------------------------------ */


    public static PyTuple unicode_internal_encode(String str) {
        return unicode_internal_encode(str, null);
    }

    public static PyTuple unicode_internal_encode(String str, String errors) {
        return encode_tuple(str, str.length());
    }

    public static PyTuple unicode_internal_decode(String str) {
        return unicode_internal_decode(str, null);
    }

    public static PyTuple unicode_internal_decode(String str, String errors) {
        return decode_tuple(str, str.length());
    }

}

