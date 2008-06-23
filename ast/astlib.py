#!/usr/bin/env python
"""lispify_ast - returns a tuple representation of the AST

Uses 2.5's _ast, not other AST implementations in CPython, since these
are not used by the compilation phase. And that's what we're
interested in.

Since this is a tuple, we can directly compare, and this is going to
be handy when comparing Jython's implementation vs CPython.

"""

import _ast
import os
import globwalk

def lispify_ast(node):
    return tuple(lispify_ast2(node))

def lispify_ast2(node):
    yield node.__class__.__name__
    try:
        for field in node._fields:
            yield tuple(lispify_field(field, getattr(node, field)))
    except:
        pass

def lispify_field(field, child):
    yield field
    if not isinstance(child, list):
        children = [child]
    else:
        children = child

    for node in children:
        if isinstance(node, _ast.AST):
            yield lispify_ast(node)
        else:
            if isinstance(node, float):
                #XXX: stringify floats so they match Java's float representation better
                #This may mask problems for very small numbers.
                if .0001 < node < 10000:
                    yield "%5.5f" % node
                else:
                    yield "%.5e" % node
            else:
                yield node

def main(code_path, jy_exe="jython", testfile=False, print_diff=True, print_fail=False, print_success=False, print_diff_lines=False):
    from pprint import pprint
    from popen2 import popen2
    from StringIO import StringIO
    from difflib import Differ

    if testfile:
        pyfiles = [f.rstrip() for f in file(code_path)]
    elif os.path.isdir(code_path):
        pyfiles = globwalk.GlobDirectoryWalker(code_path, "*.py")
    else:
        pyfiles = [code_path]

    for pyfile in pyfiles:
        ast = compile(open(pyfile).read(), pyfile, "exec", _ast.PyCF_ONLY_AST)
        lispified = lispify_ast(ast)
        sio = StringIO()
        pprint(lispified, stream=sio)

        fin, fout = popen2("%s jastlib.py %s" % (jy_exe, pyfile))

        sio.seek(0)
        pstr = sio.readlines()
        jstr = fin.readlines()

        differs = False
        diffstr = []
        difflines = 0
        diff = Differ()
        results = diff.compare(pstr, jstr)
        for d in results:
            diffstr.append(d)
            if d[0] in ['+', '-']:
                differs = True
                difflines += 1

        if print_success and not differs:
            print "SUCCESS: %s" % pyfile

        if print_fail and differs:
            print "FAIL: %s" % pyfile

        if print_diff_lines:
            print "%s diff lines in %s" % (difflines, pyfile)

        if print_diff and differs:
            print "---------- ouput -------------"
            print "py: %s" % sio.getvalue()
            print "jy: %s" % "".join(jstr)
            print "---------- DIFF -------------"
            print "".join(diffstr)

if __name__ == '__main__':
    import sys
    import getopt

    usage = """\
Usage: python %s [-j jython_exe_name] [-t testfile] [-s -f -d -n] code_path
       where -s = print success messages
             -f = print failure messages
             -n = print number of diff lines
             -d = don't print diffs on failure
             -t = treat codepath as a file containing a list of filenames to test
             -j jython_exe_name = runs jython with jython_exe_name
       unless -t is specified, if codepath is a file test it, if codepath is a directory
             test all .py files in and below that directory.
""" % sys.argv[0]

    testfile = False
    jy_exe = 'jython'
    print_diff = True
    print_diff_lines = False
    print_fail = False
    print_success = False
    try:
        opts, args = getopt.getopt(sys.argv[1:], 'j:tsfdhn')
    except:
        print usage
        sys.exit(1)
    for o, v in opts:
        if o == '-h':
            print usage
            sys.exit(0)
        if o == '-j' and v != '':
            jy_exe = v
        if o == '-t':
            testfile = True
        if o == '-s':
            print_success = True
        if o == '-f':
            print_fail = True
        if o == '-d':
            print_diff = False
        if o == '-n':
            print_diff_lines = True
    if len(args) < 1 or len(args) > 7:
        print usage
        sys.exit(1)

    main(args[0], jy_exe, testfile, print_diff, print_fail, print_success, print_diff_lines)
