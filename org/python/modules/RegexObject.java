// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;
import com.oroinc.text.regex.*;

public class RegexObject extends PyObject {
    private static Perl5Compiler compiler = new Perl5Compiler();

    private static synchronized Pattern compile(String pattern, int flags) {
        try {
            return compiler.compile(pattern, flags);
        } catch (MalformedPatternException e) {
            throw re.ReError(e.getMessage());
        }
    }

    private static synchronized Perl5Matcher getMatcher() {
        Perl5Matcher matcher = new Perl5Matcher();
        //matcher.setMultiline(false);
        return matcher;
    }

    public String pattern;
    public int flags;
    public PyObject groupindex;
    private Pattern code;

    public RegexObject(String pattern, int flags) {
        this.pattern = pattern;
        this.flags = flags;
        groupindex = new PyDictionary();
        code = compile(fixPattern(pattern, groupindex), flags);
    }

    public MatchObject match(String string) {
        MatchResult result = doMatch(string);
        if (result == null)
            return null;
        return new MatchObject(this, string, 0, string.length(), result);
    }

    public MatchObject match(String s, int pos) {
        return match(s, pos, s.length());
    }

    public MatchObject match(String string, int pos, int endpos) {
        if (endpos > string.length())
            endpos = string.length();
        if (endpos < pos)
            endpos = pos;

        MatchResult result =
            doMatch(new PatternMatcherInput(string, pos, endpos-pos));
        if (result == null)
            return null;
        return new MatchObject(this, string, pos, endpos, result);
    }

    private MatchResult doMatch(Object input) {
        Perl5Matcher matcher = getMatcher();
        if (input instanceof String) {
            if (!matcher.matchesPrefix((String)input, code))
                return null;
        } else {
            if (!matcher.matchesPrefix((PatternMatcherInput)input, code))
                return null;
        }
        return matcher.getMatch();
    }

    public MatchObject search(String string) {
        MatchResult result = doSearch(string);
        if (result == null)
            return null;
        return new MatchObject(this, string, 0, string.length(), result);
    }

    public MatchObject search(String s, int pos) {
        return search(s, pos, s.length());
    }

    public MatchObject search(String string, int pos, int endpos) {
        if (endpos > string.length())
            endpos = string.length();
        if (endpos < pos)
            endpos = pos;

        MatchResult result =
            doSearch(new PatternMatcherInput(string, pos, endpos-pos));
        if (result == null)
            return null;
        return new MatchObject(this, string, pos, endpos, result);
    }

    private MatchResult doSearch(Object input) {
        Perl5Matcher matcher = getMatcher();
        
        if (input instanceof String) {
            if (!matcher.contains((String)input, code))
                return null;
        } else {
            if (!matcher.contains((PatternMatcherInput)input, code))
                return null;
        }
        return matcher.getMatch();
    }
    
    public PyString sub(PyObject repl, String string) {
        return sub(repl, string, 0);
    }
    
    public PyString sub(PyObject repl, String string, int count) {
        return (PyString)subn(repl, string, count).__getitem__(0);
    }

    public PyTuple subn(PyObject repl, String string) {
        return subn(repl, string, 0);
    }
    
    public PyTuple subn(PyObject repl, String string, int count) {
        // Real work is done here
        String srepl = null;
        boolean expand = false;
        if (repl instanceof PyString) {
            srepl = repl.toString();
            expand = (srepl.indexOf('\\') != -1);
        }
        if (count < 0) {
            throw re.ReError("negative substitution count");
        }
        if (count == 0) {
            count = Integer.MAX_VALUE;
        }
        
        // How to handle repl as String vs. callable?
        int n=0;
        StringBuffer buf = new StringBuffer();
        Perl5Matcher matcher = getMatcher();
        PatternMatcherInput match = new PatternMatcherInput(string);
        int lastmatch = 0;
        
        while (n < count && !match.endOfInput()) {
            if (!matcher.contains(match, code))
                break;
            n++;
            int offset = match.getMatchBeginOffset();
            //System.err.println("off: "+offset+", "+lastmatch);
            if (offset > lastmatch) {
                buf.append(match.substring(lastmatch, offset));
            }
            if (srepl == null) {
                MatchObject m = new MatchObject(this, string, lastmatch,
                                                string.length(), 
                                                matcher.getMatch());
                PyObject ret = repl.__call__(m);
                buf.append(ret.toString());
            } else {
                if (expand)
                    buf.append(expandMatch(matcher.getMatch(), srepl));
                else
                    buf.append(srepl);
            }
            lastmatch = match.getMatchEndOffset();
        }
        if (lastmatch < match.getEndOffset()) {
            buf.append(match.substring(lastmatch, match.getEndOffset()));
        }
        return new PyTuple(
            new PyObject[] {
                new PyString(buf.toString()),
                new PyInteger(n)
            });
    }
    
    public PyList split(String string) {
        return split(string, 0);
    }    
    
    public PyList split(String string, int maxsplit) {
        if (maxsplit < 0) {
            throw re.ReError("maxsplit < 0");
        }
        if (maxsplit == 0) {
            maxsplit = Integer.MAX_VALUE;
        }
        
        int n=0;
        Perl5Matcher matcher = getMatcher();
        PatternMatcherInput match = new PatternMatcherInput(string);
        int lastmatch = 0;
        PyList results = new PyList();
        
        while (n < maxsplit && !match.endOfInput()) {
            if (!matcher.contains(match, code))
                break;
            n++;
            
            int begin = match.getMatchBeginOffset();
            int end = match.getMatchEndOffset();
            
            if (begin == end) {
                // More needed?
                continue;
            }
            
            results.append(new PyString(match.substring(lastmatch, begin)));
            
            MatchResult m = matcher.getMatch();
            int ngroups = m.groups();
            if (ngroups > 1) {
                for(int j=1; j<ngroups; j++) {
                    String tmp = m.group(j);
                    if (tmp == null) {
                        results.append(Py.None);
                    } else {
                        results.append(new PyString(tmp));
                    }
                }
            }
            lastmatch = end;
        }
        
        results.append(
            new PyString(match.substring(lastmatch, match.getEndOffset())));
        return results;
    }
    
    private int getindex(PyString s) {
        PyInteger v = (PyInteger)groupindex.__finditem__(s);
        if (v == null) {
            try {
                v = s.__int__();
            } catch (PyException exc) {
                throw Py.IndexError("group "+s.__repr__()+" is undefined");
            }
        }
        return v.getValue();
    }    

    private String fixPattern(String pattern, PyObject groupindex) {
        char[] chars = pattern.toCharArray();

        int index=0;
        int group=1;
        int lasti=0;
        int n = chars.length;

        StringBuffer buf = new StringBuffer();

        while (index < n) {
            if (chars[index++] == '(') {
                // Ignore \( because these are literal parens
                if (index > 2 && chars[index-2] == '\\')
                    continue;
                
                if (index < n && chars[index] == '?') {
                    index++;
                    if (index < n && chars[index] == 'P') {
                        index++;
                        if (index == n)
                            break;
                        char c = chars[index++];
                        int start = index;
                        if (c == '<') {
                            while (index < n && chars[index] != '>')
                                index++;
                            if (index == n)
                                throw re.ReError("unmatched <");
                            String name =
                                new String(chars, start, index-start);
                            groupindex.__setitem__(new PyString(name),
                                                   new PyInteger(group));
                            buf.append(chars, lasti, start-3-lasti);
                            index++;
                            lasti = index;
                            group++;
                            continue;
                        }
                        else {
                            if (c == '=') {
                                while (index < n && chars[index] != ')') {
                                    c = chars[index];
                                    if (Character.isJavaIdentifierPart(c) &&
                                        c != '$')
                                    {
                                        index++;
                                    } else {
                                        throw re.ReError(
                                            "illegal character in symbol");
                                    }
                                }
                                if (index == n)
                                    throw re.ReError("?P= not closed");
                                if (!(Character.isJavaIdentifierStart(
                                    chars[start])))
                                {
                                    throw re.ReError(
                                        "illegal character starting symbol");
                                }
                                String name = new String(chars, start,
                                                         index-start);
                                PyString pname = new PyString(name);
                                buf.append(chars, lasti, start-4-lasti);
                                buf.append('\\');
                                buf.append(getindex(pname));
                                index++;
                                lasti=index;
                            } else {
                                throw re.ReError("invalid ?P grouping");
                            }
                        }
                    } else {
                        if (chars[index] == ':')
                            continue;
                        while (index < n && chars[index] != ')')
                            index++;
                    }
                } else {
                    group++;
                }
            }
        }
        if (lasti > 0) {
            buf.append(chars, lasti, n-lasti);
            //System.err.println("pat: "+buf.toString());
            return buf.toString();
        } else {
            //System.err.println("untouched: "+pattern);
            return pattern;
        }
    }
    
    public String expandMatch(MatchResult match, String repl) {
        char[] chars = repl.toCharArray();

        int index=0;
        int lasti=0;
        int n = chars.length;

        StringBuffer buf = new StringBuffer();
        try {
            while (index<n) {
                //System.out.println("index: "+index+", "+n+", "+repl);

                if (chars[index++] == '\\') {
                    char ch = 0;
                    switch (chars[index++]) {
                    case '\\':
                        ch = '\\'; break;
                    case 'E':
                    case 'G':
                    case 'L':
                    case 'Q':
                    case 'U':
                    case 'l':
                    case 'u':
                        throw re.ReError("\\"+chars[index-1]+
                                         " is not allowed");
                    case 'n':
                        ch = '\n'; break;
                    case 't':
                        ch = '\t'; break;
                    case 'r':
                        ch = '\r'; break;
                    case 'v':
                        ch = '\013'; break;
                    case 'f':
                        ch = '\f'; break;
                    case 'a':
                        ch = '\007'; break;
                    case 'b':
                        ch = '\b'; break;

                    case 'g':
                        if (chars[index++] != '<') {
                            throw re.ReError(
                                "missing < in symbolic reference");
                        }
                        int start = index;
                        while (index < n && chars[index] != '>') index++;
                        if (index == n) {
                            throw re.ReError("unfinished symbolic reference");
                        }
                        index++;
                        buf.append(chars, lasti, start-3-lasti);
                        PyString str = new PyString(new String(chars, start,
                                                               index-1-start));
                        String tmp = match.group(getindex(str));
                        if (tmp == null) {
                            throw re.ReError("group not in match: "+str);
                        }
                        buf.append(tmp);
                        lasti=index;
                        continue;

                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        start = index-2;
                        int v = chars[index-1]-'0';
                        char ch1;
                        if (index<n) {
                            ch = chars[index];
                            if (ch >= '0' && ch <= '9') {
                                index++;
                                if (index < n && ch <= '7') {
                                    ch1 = chars[index];
                                    if (ch1 >= '0' && ch1 <= '7') {
                                        v = v*64 +
                                            (ch - '0')*8 +
                                            (ch1 - '0');
                                        buf.append(chars, lasti,
                                                   index-2-lasti);
                                        buf.append((char)v);
                                        index++;
                                        lasti=index;
                                    }
                                }
                                v = v*10 + (ch - '0');
                            }
                        }
                        buf.append(chars, lasti, start-lasti);
                        tmp = match.group(v);
                        if (tmp == null) {
                            throw re.ReError("group not in match: "+v);
                        }
                        buf.append(tmp);
                        lasti=index;
                        continue;
                    default:
                        continue;
                    }
                    buf.append(chars, lasti, index-2-lasti);
                    buf.append(ch);
                    lasti=index;
                }
            }
        } catch (ArrayIndexOutOfBoundsException exc) {
            throw re.ReError("invalid expression");
        }
        if (lasti > 0) {
            buf.append(chars, lasti, n-lasti);
            return buf.toString();
        } else {
            return repl;
        }
    }    
}
