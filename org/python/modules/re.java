// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;
import com.oroinc.text.regex.*;

public class re implements InitModule {
    public static PyObject error = new PyString("re.error");
    public static PyException ReError(String message) {
        return new PyException(error, message);
    }
    
    public void initModule(PyObject dict) {
        dict.__setitem__("IGNORECASE",
			 new PyInteger(Perl5Compiler.CASE_INSENSITIVE_MASK));
        dict.__setitem__("I",
			 new PyInteger(Perl5Compiler.CASE_INSENSITIVE_MASK));
        dict.__setitem__("MULTILINE",
			 new PyInteger(Perl5Compiler.MULTILINE_MASK));
        dict.__setitem__("M", new PyInteger(Perl5Compiler.MULTILINE_MASK));
        dict.__setitem__("DOTALL",
			 new PyInteger(Perl5Compiler.SINGLELINE_MASK));
        dict.__setitem__("S", new PyInteger(Perl5Compiler.SINGLELINE_MASK));
        dict.__setitem__("VERBOSE", new PyInteger(0));
        dict.__setitem__("X", new PyInteger(0));
        dict.__setitem__("LOCALE", new PyInteger(0));
        dict.__setitem__("L", new PyInteger(0));
    }

    // Skip caching for now...
    private static RegexObject cachecompile(String pattern, int flags) {
        return compile(pattern, flags);
    }

    private static RegexObject cachecompile(String pattern) {
        return cachecompile(pattern, 0);
    }

    public static MatchObject match(String pattern, String string) {
        return match(pattern, string, 0);
    }

    public static MatchObject match(String pattern, String string, int flags) {
        return cachecompile(pattern, flags).match(string);
    }

    public static MatchObject search(String pattern, String string) {
        return search(pattern, string, 0);
    }

    public static MatchObject search(String pattern, String string, int flags)
    {
        return cachecompile(pattern, flags).search(string);
    }

    private static RegexObject getPattern(PyObject pattern) {
        if (pattern instanceof PyString) {
            return cachecompile(pattern.toString());
        } else if (pattern instanceof RegexObject) {
            return (RegexObject)pattern;
        }
        throw Py.TypeError("pattern must be string or RegexObject");
    }

    public static PyString sub(PyObject pattern, PyObject repl, String string)
    {
        return sub(pattern, repl, string, 0);
    }
    
    public static PyString sub(PyObject pattern, PyObject repl,
			       String string, int count)
    {
        return getPattern(pattern).sub(repl, string, count);
    }

    public static PyTuple subn(PyObject pattern, PyObject repl, String string)
    {
        return subn(pattern, repl, string, 0);
    }
    
    public static PyTuple subn(PyObject pattern, PyObject repl,
			       String string, int count)
    {
        return getPattern(pattern).subn(repl, string, count);
    }

    public static PyList split(PyObject pattern, String string) {
        return split(pattern, string, 0);
    }    
    
    public static PyList split(PyObject pattern, String string, int maxsplit) {
        return getPattern(pattern).split(string, maxsplit);
    }
    
    public static String escape(String s) {
        return Perl5Compiler.quotemeta(s);
    }

    public static RegexObject compile(String pattern) {
        return new RegexObject(pattern, 0);
    }

    public static RegexObject compile(String pattern, int flags) {
        return new RegexObject(pattern, flags);
    }
}
