/* Dummy strop for now to satisfy regrtest
    Obviously this should be fleshed out to provide string ops performance
*/

package org.python.modules;
import org.python.core.*;

public class strop implements InitModule {
    public void initModule(PyObject dict) {
        char c;
        StringBuffer whitespace = new StringBuffer();
        StringBuffer lowercase = new StringBuffer();
        StringBuffer uppercase = new StringBuffer();
        StringBuffer letters = new StringBuffer();
        StringBuffer digits = new StringBuffer();
        StringBuffer hexdigits = new StringBuffer();
        StringBuffer octdigits = new StringBuffer();
        
        for(c=0;c<256; c++) {
            if (Character.isWhitespace(c)) whitespace.append(c);
            if (Character.isLowerCase(c)) lowercase.append(c);
            if (Character.isUpperCase(c)) uppercase.append(c);
            if (Character.isLetter(c)) letters.append(c);
            if (Character.isDigit(c)) digits.append(c);
            if (Character.digit(c, 16) != -1) hexdigits.append(c);
            if (Character.digit(c, 8) != -1) octdigits.append(c);
        }        
        
        dict.__setitem__("whitespace", new PyString(whitespace.toString()));
        dict.__setitem__("lowercase", new PyString(lowercase.toString()));
        dict.__setitem__("uppercase", new PyString(uppercase.toString()));
        dict.__setitem__("digits", new PyString(digits.toString()));
        dict.__setitem__("letters", new PyString(letters.toString()));
        dict.__setitem__("hexdigits", new PyString(hexdigits.toString()));
        dict.__setitem__("octdigits", new PyString(octdigits.toString()));
    }
    
    public static String lower(String s) {
        return s.toLowerCase();
    }

    public static String upper(String s) {
        return s.toUpperCase();
    }

    public static String swapcase(String s) {
        char[] chars = s.toCharArray();
        int n=chars.length;
        for(int i=0; i<n; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c);
            } else if (Character.isLowerCase(c)) {
                chars[i] = Character.toUpperCase(c);
            }
        }
        return new String(chars);
    }

    public static String strip(String s) {
        char[] chars = s.toCharArray();
        int n=chars.length;
        int start=0;
        while (start < n && Character.isWhitespace(chars[start])) start++;

        int end=n-1;
        while (end >= 0 && Character.isWhitespace(chars[end])) end--;

        if (end >= start) {
            return (end < n-1 || start > 0) ? s.substring(start, end+1) : s;
        } else {
            return "";
        }
    }

    public static String lstrip(String s) {
        char[] chars = s.toCharArray();
        int n=chars.length;
        int start=0;
        while (start < n && Character.isWhitespace(chars[start])) start++;

        return (start > 0) ? s.substring(start, n) : s;
    }

    public static String rstrip(String s) {
        char[] chars = s.toCharArray();
        int n=chars.length;
        int end=n-1;
        while (end >= 0 && Character.isWhitespace(chars[end])) end--;

        return (end < n-1) ? s.substring(0, end+1) : s;
    }


    public static PyList split(String s, String sep) {
        return split(s, sep, 0);
    }

    public static PyList split(String s) {
        return split(s, null, 0);
    }

    public static PyList split(String s, String sep, int maxsplit) {
        if (sep != null) return splitfields(s, sep, maxsplit);

        PyList list = new PyList();

        char[] chars = s.toCharArray();
        int n=chars.length;

        if (maxsplit <= 0) maxsplit = n;

        int splits=0;
        int index=0;
        while (index < n && splits < maxsplit) {
            while (index < n && Character.isWhitespace(chars[index])) index++;
            if (index == n) break;
            int start = index;

            while (index < n && !Character.isWhitespace(chars[index])) index++;
            list.append(new PyString(s.substring(start, index)));
            splits++;
        }
        if (index < n) {
            while (index < n && Character.isWhitespace(chars[index])) index++;
            list.append(new PyString(s.substring(index, n)));
        }
        return list;
    }

    public static PyList splitfields(String s) {
        return split(s, null, 0);
    }

    public static PyList splitfields(String s, String sep) {
        return splitfields(s, sep, 0);
    }

    public static PyList splitfields(String s, String sep, int maxsplit) {
        if (sep == null) return split(s, sep, maxsplit);
        
        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        PyList list = new PyList();

        char[] chars = s.toCharArray();
        int n=chars.length;

        if (maxsplit <= 0) maxsplit = n;

        int splits=0;
        int index=0;
        char firstChar = sep.charAt(0);
        int nsep = sep.length();
        int lastbreak;
        while (index < n && splits < maxsplit) {
            lastbreak = index;
            while (index < n) {
                if (chars[index] == firstChar) {
                    if (nsep == 1) break;
                    int j;
                    for(j=1; j<nsep; j++) {
                        if (chars[index+j] != sep.charAt(j)) break;
                    }
                    if (j == nsep) break;
                }
                index++;
            }
            list.append(new PyString(s.substring(lastbreak, index)));
            index += nsep;
            splits++;
        }
        if (index <= n) {
            list.append(new PyString(s.substring(index, n)));
        }
        return list;
    }

    public static String join(PyObject seq) {
        return join(seq, " ");
    }

    public static String join(PyObject seq, String sep) {
        int i=0;
        StringBuffer buf = new StringBuffer();
        PyObject obj;
        boolean addSep=false;

        while ((obj = seq.__finditem__(i++)) != null) {
            if (addSep) buf.append(sep);
            buf.append(obj.toString());
            addSep = true;
        }
        return buf.toString();
    }

    public static String joinfields(PyObject seq) {
        return join(seq, " ");
    }

    public static String joinfields(PyObject seq, String sep) {
        return join(seq, sep);
    }

    public static int index(String s, String sub) {
        return index(s, sub, 0, s.length());
    }

    public static int index(String s, String sub, int start) {
        return index(s, sub, start, s.length());
    }

    public static int index(String s, String sub, int start, int end) {
        int n = s.length();

        if (start < 0) start = n+start;
        if (end < 0) end = n+end;

        int index;
        if (end < n) {
            index = s.substring(start, end).indexOf(sub);
        } else {
            index = s.indexOf(sub, start);
        }
        if (index == -1) throw Py.ValueError("substring not found in string.index");
        return index;
    }

    public static int rindex(String s, String sub) {
        return rindex(s, sub, 0, s.length());
    }

    public static int rindex(String s, String sub, int start) {
        return rindex(s, sub, start, s.length());
    }

    public static int rindex(String s, String sub, int start, int end) {
        int n = s.length();

        if (start < 0) start = n+start;
        if (end < 0) end = n+end;

        int index;
        if (start > 0) {
            index = s.substring(start, end).lastIndexOf(sub);
        } else {
            index = s.lastIndexOf(sub, end);
        }
        if (index == -1) throw Py.ValueError("substring not found in string.rindex");
        return index;
    }

    public static int count(String s, String sub) {
        return count(s, sub, 0, s.length());
    }
    public static int count(String s, String sub, int start) {
        return count(s, sub, start, s.length());
    }

    public static int count(String s, String sub, int start, int end) {
        int n = s.length();
        if (start < 0) start = n+start;
        if (end < 0) end = n+end;
        if (end > n) end = n;
        if (start > end) start = end;

        int slen = sub.length();
        //end = end-slen;

        int count=0;
        while (start < end) {
            int index = s.indexOf(sub, start);
            if (index >= end || index == -1) break;
            count++;
            start = index+slen;
        }
        return count;
    }
 
    public static int find(String s, String sub) {
        return find(s, sub, 0, s.length());
    }
    
    public static int find(String s, String sub, int start) {
        return find(s, sub, start, s.length());
    }
    
    public static int find(String s, String sub, int start, int end) {
        int n = s.length();
        if (start < 0) start = n+start;
        if (end < 0) end = n+end;
        if (end > n) end = n;
        if (start > end) start = end;
        int slen = sub.length();
        end = end-slen;

        int index = s.indexOf(sub, start);
        if (index > end) return -1;
        return index;
    }
    
    public static int rfind(String s, String sub) {
        return rfind(s, sub, 0, s.length());
    }
    
    public static int rfind(String s, String sub, int start) {
        return rfind(s, sub, start, s.length());
    }
    
    public static int rfind(String s, String sub, int start, int end) {
        int n = s.length();
        if (start < 0) start = n+start;
        if (end < 0) end = n+end;
        if (end > n) end = n;
        if (start > end) start = end;
        int slen = sub.length();
        end = end-slen;

        int index = s.lastIndexOf(sub, end);
        if (index < start) return -1;
        return index;
    }


    public static double atof(String s) {
        try {
            return Double.valueOf(s.trim()).doubleValue();
        } catch (NumberFormatException exc) {
            throw Py.ValueError("non-float argument to string.atof");
        }
    }

    public static int atoi(String s) {
        return atoi(s, 10);
    }

    public static int atoi(String s, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atoi()");
        }
        
        s = s.trim();
        boolean neg = false;
        try {
            char sign = s.charAt(0);
            if (sign == '+') {
                s = s.substring(1,s.length()).trim();
            }
            if (base == 0 || base == 16) {
                if (sign == '-') {
                    neg = true;
                    s = s.substring(1, s.length()).trim();
                }
                if (s.charAt(0) == '0') {
                    if (s.charAt(1) == 'x') {
                        if (base == 0) base = 16;
                        s = s.substring(2,s.length());
                    } else {
                        if (base == 0) base = 8;
                    }
                }  
            }
            if (base == 0) base = 10;
        } catch (IndexOutOfBoundsException ex) {
            throw Py.ValueError("non-integer argument to string.atoi");
        }
        
        try {
            int value = Integer.valueOf(s, base).intValue();
            if (neg) return -value;
            else return value;
        } catch (NumberFormatException exc) {
            throw Py.ValueError("non-integer argument to string.atoi");
        }
    }

    public static PyLong atol(String s) {
        return atol(s, 10);
    }

    public static PyLong atol(String s, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atol()");
        }
        
        s = s.trim();
        boolean neg = false;
        try {
            char sign = s.charAt(0);
            if (sign == '+') {
                s = s.substring(1,s.length()).trim();
            }
            if (base == 0 || base == 16) {
                if (sign == '-') {
                    neg = true;
                    s = s.substring(1, s.length()).trim();
                }
                if (base == 0) {
                    char lastChar = s.charAt(s.length()-1);                    
                    if (lastChar == 'l' || lastChar == 'L') 
                        s = s.substring(0, s.length()-1);                    
                }
                if (s.charAt(0) == '0') {
                    if (s.charAt(1) == 'x') {
                        if (base == 0) base = 16;
                        s = s.substring(2,s.length());
                    } else {
                        if (base == 0) base = 8;
                    }
                }
            }
            if (base == 0) base = 10;
        } catch (IndexOutOfBoundsException ex) {
            throw Py.ValueError("non-integer argument to string.atol 1");
        }
        try {
            java.math.BigInteger value = new java.math.BigInteger(s, base);
            if (neg) return new PyLong(value.negate());
            else return new PyLong(value);
        } catch (NumberFormatException exc) {
            throw Py.ValueError("non-integer argument to string.atol");
        }
    }

    private static String spaces(int n) {
        char[] chars = new char[n];
        for(int i=0; i<n; i++) chars[i] = ' ';
        return new String(chars);
    }

    public static String ljust(String s, int width) {
        int n = width-s.length();
        if (n <= 0) return s;
        return s+spaces(n);
    }

    public static String rjust(String s, int width) {
        int n = width-s.length();
        if (n <= 0) return s;
        return spaces(n)+s;
    }

    public static String center(String s, int width) {
        int n = width-s.length();
        if (n <= 0) return s;
        int half = n/2;
        if (n%2 > 0 &&  width%2 > 0) half += 1;
        return spaces(half)+s+spaces(n-half);
    }

    public static String zfill(String s, int width) {
        int n = s.length();
        if (n >= width) return s;
        char start = s.charAt(0);
        char[] chars = new char[width];
        int nzeros = width-n;
        int i=0;
        int sStart=0;
        if (start == '+' || start == '-') {
            chars[0] = start;
            i += 1;
            nzeros++;
            sStart=1;
        }
        for(;i<nzeros; i++) {
            chars[i] = '0';
        }
        s.getChars(sStart, s.length(), chars, i);
        return new String(chars);
    }

    public static String zfill(PyObject o, int width) {
        return zfill(o.toString(), width);
    }
    
    public static String expandtabs(String s) {
        return expandtabs(s, 8);
    }
    
    public static String expandtabs(String s, int tabsize) {
        StringBuffer buf = new StringBuffer((int)(s.length()*1.5));
        char[] chars = s.toCharArray();
        int n = chars.length;
        int position = 0;
        
        for(int i=0; i<n; i++) {
            char c = chars[i];
            if (c == '\t') {
                int spaces = tabsize-position%tabsize;
                position += spaces;
                while (spaces-- > 0) {
                    buf.append(' ');
                }
                continue;
            } 
            if (c == '\n') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }
    
    public static String capitalize(String s) {
        if (s.length() == 0) return s;
        String first = s.substring(0,1).toUpperCase();
        return first.concat(s.substring(1,s.length()).toLowerCase());
    }   
    
    public static String replace(String str, String oldPiece, String newPiece) {
        return replace(str, oldPiece, newPiece, str.length());
    }
    
    public static String replace(String str, String oldPiece, String newPiece, int maxsplit) {
        return joinfields(split(str, oldPiece, maxsplit), newPiece);
    }
    

}