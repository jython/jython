
package org.python.modules;

import org.python.core.*;

public class _codecs {

    public static void register(PyObject search_function) {
        codecs.register(search_function);
    }


    public static PyTuple lookup(String encoding) {
        return codecs.lookup(encoding);
    }





    /* --- UTF-8 Codec -------------------------------------------------------- */
    private static byte utf8_code_length[] = {
       /* Map UTF-8 encoded prefix byte to sequence length.  zero means
           illegal prefix.  see RFC 2279 for details */
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
    };






    public static PyTuple utf_8_decode(String str) {
        return utf_8_decode(str, null);
    }

    public static PyTuple utf_8_decode(String str, String errors) {
        int size = str.length();
        StringBuffer unicode = new StringBuffer(size);


        /* Unpack UTF-8 encoded data */
        for (int i = 0; i < size; ) {
            int ch = str.charAt(i);
            if (ch > 0xFF) {
                codecs.decoding_error("utf-8", unicode, errors, 
                                      "ordinal not in range(255)");
                i++;
                continue;
            }

            if (ch < 0x80) {
                unicode.append((char) ch);
                i++;
                continue;
            }

            int n = utf8_code_length[ch];

            if (i + n > size) {
                codecs.decoding_error("utf-8", unicode, errors, 
                                      "unexpected end of data");
                i++;
                continue;
            }

	
            switch (n) {
            case 0:
                codecs.decoding_error("utf-8", unicode, errors, 
                                      "unexpected code byte");
                i++;
                continue;
            case 1:
                codecs.decoding_error("utf-8", unicode, errors, 
                                      "internal error");
                i++;
                continue;
            case 2:
                char ch1 = str.charAt(i+1);
                if ((ch1 & 0xc0) != 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors, 
                                          "invalid data");
                    i++;
                    continue;
                }
                ch = ((ch & 0x1f) << 6) + (ch1 & 0x3f);
                if (ch < 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "illegal encoding");
                    i++;
                    continue;
                } else
                    unicode.append((char) ch);
                break;

            case 3:
                ch1 = str.charAt(i+1);
                char ch2 = str.charAt(i+2); 
                if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors, 
                                          "invalid data");
                    i++;
                    continue;
                }
                ch = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);
                if (ch < 0x800 || (ch >= 0xd800 && ch < 0xe000)) {
                    codecs.decoding_error("utf-8", unicode, errors, 
                                          "illegal encoding");
                    i++;
                    continue;
                } else
                   unicode.append((char) ch);
                break;

            case 4:
                ch1 = str.charAt(i+1);
                ch2 = str.charAt(i+2); 
                char ch3 = str.charAt(i+3); 
                if ((ch1 & 0xc0) != 0x80 ||
                    (ch2 & 0xc0) != 0x80 ||
                    (ch3 & 0xc0) != 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors, 
                                          "invalid data");
                    i++;
                    continue;
                }
                ch = ((ch & 0x7) << 18) + ((ch1 & 0x3f) << 12) +
                     ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);
                /* validate and convert to UTF-16 */
                if ((ch < 0x10000) ||   /* minimum value allowed for 4
                                           byte encoding */
                    (ch > 0x10ffff)) {  /* maximum value allowed for
                                           UTF-16 */
                    codecs.decoding_error("utf-8", unicode, errors, 
                                          "illegal encoding");
                    i++;
                    continue;
                }
                /*  compute and append the two surrogates: */

                /*  translate from 10000..10FFFF to 0..FFFF */
                ch -= 0x10000;

                /*  high surrogate = top 10 bits added to D800 */
                unicode.append((char) (0xD800 + (ch >> 10)));

                /*  low surrogate = bottom 10 bits added to DC00 */
                unicode.append((char) (0xDC00 + (ch & ~0xFC00)));
                break;

            default:
                /* Other sizes are only needed for UCS-4 */
                codecs.decoding_error("utf-8", unicode, errors, 
                                      "unsupported Unicode code range");
                i++;
            }
            i += n;
        }

        return codec_tuple(unicode.toString(), size);
    }




    public static PyTuple utf_8_encode(String str) {
        return utf_8_encode(str, null);
    }

    public static PyTuple utf_8_encode(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size * 3);
 
        for (int i = 0; i < size; ) {
            int ch = str.charAt(i++);
            if (ch < 0x80)
                v.append((char) ch);
            else if (ch < 0x0800) {
                v.append((char) (0xc0 | (ch >> 6)));
                v.append((char) (0x80 | (ch & 0x3f)));
            } else {
                if (0xD800 <= ch && ch <= 0xDFFF) {
                    if (i != size) {
                        int ch2 = str.charAt(i);
                        if (0xDC00 <= ch2 && ch2 <= 0xDFFF) {
                            /* combine the two values */
                            ch = ((ch - 0xD800)<<10 | (ch2-0xDC00))+0x10000;

                            v.append((char)((ch >> 18) | 0xf0));
                            v.append((char)(0x80 | ((ch >> 12) & 0x3f)));
                            i++;
                        }
                    }
                } else {
                    v.append((char)(0xe0 | (ch >> 12)));
                }
                v.append((char) (0x80 | ((ch >> 6) & 0x3f)));
                v.append((char) (0x80 | (ch & 0x3f)));
            }
        }
        return codec_tuple(v.toString(), size);
    }
  




    private static PyTuple codec_tuple(String s, int len) {
        return new PyTuple(new PyObject[] {
            Py.java2py(s),
            Py.newInteger(len)
        });
    }






    /* --- Character Mapping Codec -------------------------------------------- */


    public static PyTuple charmap_decode(String str, String errors,
                                         PyObject mapping) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch > 0xFF) {
                codecs.decoding_error("charmap", v, errors, 
                                      "ordinal not in range(255)");
                i++;
                continue;
            }

            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if (x == null) {
                /* No mapping found: default to Latin-1 mapping if possible */
                v.append(ch);
                continue;
            }

            /* Apply mapping */
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > 65535)
                    throw Py.TypeError(
                             "character mapping must be in range(65535)");
                v.append((char) value);
            } else if (x == Py.None) {
                codecs.decoding_error("charmap", v,  errors, 
                                      "character maps to <undefined>");
            } else if (x instanceof PyString) {
                if (x.__len__() != 1) {
                    /* 1-n mapping */
                    throw new PyException(Py.NotImplementedError,
                          "1-n mappings are currently not implemented");
                }
                v.append(x.toString());
            }
            else {
                /* wrong return value */
                throw Py.TypeError(
                     "character mapping must return integer, None or unicode");
            }
        }
        return codec_tuple(v.toString(), size);
    }





    public static PyTuple charmap_encode(String str, String errors, 
                                         PyObject mapping) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);
 
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if (x == null) {
                /* No mapping found: default to Latin-1 mapping if possible */
                if (ch < 256)
                    v.append(ch);
                else
                    codecs.encoding_error("charmap", v, errors, 
                                          "missing character mapping");
                continue;
            }
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > 255)
                    throw Py.TypeError(
                            "character mapping must be in range(256)");
                v.append((char) value);
            } else if (x == Py.None) {
                codecs.encoding_error("charmap", v,  errors, 
                                      "character maps to <undefined>");
            } else if (x instanceof PyString) {
                if (x.__len__() != 1) {
                    /* 1-n mapping */
                    throw new PyException(Py.NotImplementedError,
                          "1-n mappings are currently not implemented");
                }
                v.append(x.toString());
            }
            else {
                /* wrong return value */
                throw Py.TypeError(
                     "character mapping must return integer, None or unicode");
            }
        }
        return codec_tuple(v.toString(), size);
    }
            


    /* --- 7-bit ASCII Codec -------------------------------------------- */

    public static PyTuple ascii_decode(String str, String errors) {
        int size = str.length();
        return codec_tuple(codecs.PyUnicode_DecodeASCII(str, size, errors),
                                                                        size);
    }




    public static PyTuple ascii_encode(String str, String errors) {
        int size = str.length();
        return codec_tuple(codecs.PyUnicode_EncodeASCII(str, size, errors), 
                                                                        size);
    }
           

    /* --- Latin-1 Codec -------------------------------------------- */

    public static PyTuple latin_1_decode(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch < 256) {
                v.append(ch);
            } else {
                codecs.decoding_error("latin-1", v, errors, 
                                      "ordinal not in range(256)");
                i++;
                continue;
            }
        }

        return codec_tuple(v.toString(), size);
    }




    public static PyTuple latin_1_encode(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);
 
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 256) {
                codecs.encoding_error("latin-1", v, errors, 
                                      "ordinal not in range(256)");
            } else
                v.append(ch);
        }
        return codec_tuple(v.toString(), size);
    }


    /* --- UTF16 Codec -------------------------------------------- */


    public static PyTuple utf_16_encode(String str, String errors) {
        return codec_tuple(encode_UTF16(str, errors, 0), str.length());
    }

    public static PyTuple utf_16_encode(String str, String errors,
                                       int byteorder) {
        return codec_tuple(encode_UTF16(str, errors, byteorder), str.length());
    }

    public static PyTuple utf_16_le_encode(String str, String errors) {
        return codec_tuple(encode_UTF16(str, errors, -1), str.length());
    }

    public static PyTuple utf_16_be_encode(String str, String errors) {
        return codec_tuple(encode_UTF16(str, errors, 1), str.length());
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

    


    public static PyTuple utf_16_decode(String str, String errors) {
        int[] bo = new int[] { 0 };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_decode(String str, String errors, 
                                        int byteorder) {
        int[] bo = new int[] { byteorder };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_le_decode(String str, String errors) {
        int[] bo = new int[] { -1 };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_be_decode(String str, String errors) {
        int[] bo = new int[] { 1 };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
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

    private static String decode_UTF16(String str, String errors, 
                                       int[] byteorder) {
        int bo = 0;
        if (byteorder != null)
             bo = byteorder[0];

        int size = str.length();

        if (size % 2 != 0)
            codecs.decoding_error("UTF16", null, errors, "truncated data");

        StringBuffer v = new StringBuffer(size/2);

        for (int i = 0; i < size; i += 2) {
            char ch1 = str.charAt(i);
            char ch2 = str.charAt(i+1);
            if (ch1 == 0xFE && ch2 == 0xFF) {
                bo = 1;
                continue;
            } else if (ch1 == 0xFF && ch2 == 0xFE) {
                bo = -1;
                continue;
            }
            
            char ch;
            if (bo == -1)
                ch = (char) (ch2 << 8 | ch1);
            else
                ch = (char) (ch1 << 8 | ch2);

            if (ch < 0xD800 || ch > 0xDFFF) {
                v.append(ch);
                continue;
            }
            

            /* UTF-16 code pair: */
            if (i == size-1) {
                codecs.decoding_error("UTF-16", v, errors,
                                      "unexpected end of data");
                continue;
            }

            ch = str.charAt(++i);
            if (0xDC00 <= ch && ch <= 0xDFFF) {
                ch = str.charAt(++i);
                if (0xD800 <= ch && ch <= 0xDBFF)
                    /* This is valid data (a UTF-16 surrogate pair), but
                       we are not able to store this information since our
                       Py_UNICODE type only has 16 bits... this might
                       change someday, even though it's unlikely. */
                    codecs.decoding_error("UTF-16", v, errors, 
                                          "code pairs are not supported");
                continue;
            }
            codecs.decoding_error("UTF-16", v, errors, "illegal encoding");
        }

        if (byteorder != null)
            byteorder[0] = bo;

        return v.toString();
    }



    /* --- RawUnicodeEscape Codec ----------------------------------------- */


    public static PyTuple raw_unicode_escape_encode(String str, 
                                                   String errors) {
        return codec_tuple(encodeRawUnicodeEscape(str, errors), str.length());
    }



    static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    private static String encodeRawUnicodeEscape(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(str.length());

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 256) {
                v.append("\\u");
                v.append(hexdigit[(ch >>> 12) & 0xF]);
                v.append(hexdigit[(ch >>> 8) & 0xF]);
                v.append(hexdigit[(ch >>> 4) & 0xF]);
                v.append(hexdigit[ch & 0xF]);
            } else
                v.append(ch);
        }

        return v.toString();
    }



    public static PyTuple raw_unicode_escape_decode(String str, 
                                                    String errors) {
        return codec_tuple(decodeRawUnicodeEscape(str, errors), str.length());
    }


    private static String decodeRawUnicodeEscape(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; ) {
            char ch = str.charAt(i);

            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                i++;
	        continue;
            }

            /* \\u-escapes are only interpreted iff the number of leading
               backslashes is odd */
	    int bs = i;
	    while (i < size) {
                ch = str.charAt(i);
                if (ch != '\\')
                    break;
                v.append(ch);
                i++;
            }
            if (((i - bs) & 1) == 0 || i >= size || ch != 'u') {
                continue;
            }
            v.setLength(v.length() - 1);
            i++;

            /* \\uXXXX with 4 hex digits */
            int x = 0;
            for (int j = 0; j < 4; j++) {
                ch = str.charAt(i+j);
                int d  = Character.digit(ch, 16);
                if (d == -1) {
                    codecs.decoding_error("unicode escape", v, errors,
                                          "truncated \\uXXXX");
            	    break;
	        }
                x = ((x<<4) & ~0xF) + d;
            }
            i += 4;
	    v.append((char) x);
       }
       return v.toString();
    }


    /* --- UnicodeEscape Codec -------------------------------------------- */


    public static PyTuple unicode_escape_encode(String str, String errors) {
        return codec_tuple(PyString.unicodeescape(str, false), str.length());
    }





    public static PyTuple unicode_escape_decode(String str, String errors) {
        return codec_tuple(decodeUnicodeEscape(str, errors), str.length());
    }



    private static ucnhashAPI pucnHash = null;

    private static String decodeUnicodeEscape(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int s = 0; s < size; ) {
            char ch = str.charAt(s);

            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                s++;
	        continue;
            }

    
            /* \ - Escapes */
            s++;
            ch = str.charAt(s++);
            switch (ch) {

            /* \x escapes */
            case '\n': break;
            case '\\': v.append('\\'); break;
            case '\'': v.append('\''); break;
            case '\"': v.append('\"'); break;
            case 'b': v.append('\b'); break;
            case 'f': v.append('\014'); break; /* FF */
            case 't': v.append('\t'); break;
            case 'n': v.append('\n'); break;
            case 'r': v.append('\r'); break;
            case 'v': v.append('\013'); break; /* VT */
            case 'a': v.append('\007'); break; /* BEL, not classic C */

            /* \OOO (octal) escapes */
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':

                int x = Character.digit(ch, 8);
                ch = str.charAt(s++);
                if ('0' <= ch && ch <= '7') {
                    x = (x<<3) + Character.digit(ch, 8);
                    ch = str.charAt(s++);
                    if ('0' <= ch && ch <= '7') {
                        x = (x<<3) + Character.digit(ch, 8);
                    }
                }
                v.append((char) x);
                break;

            /* \ uXXXX with 4 hex digits */
            case 'u':
                int i;
                for (x = 0, i = 0; i < 4; i++) {
                    ch = str.charAt(s + i);
                    int d  = Character.digit(ch, 16);
                    if (d == -1) {
                        codecs.decoding_error("unicode escape", v, errors,
                                              "truncated \\uXXXX");
                        break;
                    }
                    x = ((x<<4) & ~0xF) + d;
                }
                s += i;
                v.append((char) x);
                break;

            case 'N':
                /* Ok, we need to deal with Unicode Character Names now,
                 * make sure we've imported the hash table data...
                 */
                if (pucnHash == null) {
                     PyObject mod = imp.importName("ucnhash", true);
                     pucnHash = (ucnhashAPI) mod.__tojava__(ucnhashAPI.class);
                }

                if (str.charAt(s) == '{') {
                    int start = s + 1;
                    int endBrace = start;

                    /* look for either the closing brace, or we
                     * exceed the maximum length of the unicode character names
                     */
                    int maxLen = pucnHash.getCchMax();
                    while (str.charAt(endBrace) != '}' 
                           && (endBrace - start) <= maxLen
                           && endBrace < size) {
                        endBrace++;
                    }
                    if (endBrace != size && str.charAt(endBrace) == '}') {
                         int value = pucnHash.getValue(str, start, endBrace);
                         if (value < 0) {
                             codecs.decoding_error("unicode escape", v, errors, 
                                 "Invalid Unicode Character Name");
                             v.append('\\');
                             v.append(str.charAt(s-1));
                             break;
                         }

                         if (value < 1<<16) {
                             /* In UCS-2 range, easy solution.. */
                             v.append(value);
                         } else {
                             /* Oops, its in UCS-4 space, */
                             /*  compute and append the two surrogates: */
                             /*  translate from 10000..10FFFF to 0..FFFFF */
                             value -= 0x10000;

                             /* high surrogate = top 10 bits added to D800 */
                             v.append((char) (0xD800 + (value >> 10)));

                             /* low surrogate  = bottom 10 bits added to DC00*/
                             v.append((char) (0xDC00 + (value & ~0xFC00)));
                        }
                        s = endBrace + 1;
                    } else {
                         codecs.decoding_error("unicode escape", v, errors, 
                              "Unicode name missing closing brace");
                         v.append('\\');
                         v.append(str.charAt(s-1));
                         break;
                    }
                    break;
                }
                codecs.decoding_error("unicode escape", v, errors, 
                     "Missing opening brace for Unicode Character Name escape");
 
                /* fall through on purpose */
           default:
               v.append('\\');
               v.append(str.charAt(s-1));
               break;
           }
       }
       return v.toString();
    }



    /* --- UnicodeInternal Codec -------------------------------------------- */


    public static PyTuple unicode_internal_encode(String str, String errors) {
        return codec_tuple(str, str.length());
    }



    public static PyTuple unicode_internal_decode(String str, String errors) {
        return codec_tuple(str, str.length());
    }

}

