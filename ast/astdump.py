#!/usr/bin/env python
import os
import globwalk
import astview

def makepath(path):
    """
    from holger@trillke.net 2002/03/18
    See: http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/117243
    """

    from os import makedirs
    from os.path import normpath,dirname,exists,abspath

    dpath = normpath(dirname(path))
    if not exists(dpath): makedirs(dpath)
    return normpath(abspath(path))

def main(code_path, output_dir, testfile=False):
    if os.path.exists(output_dir):
        print "%s already exists, exiting" % output_dir
        sys.exit(1)
    os.mkdir(output_dir)
    if testfile:
        pyfiles = [f.rstrip() for f in file(code_path)]
    elif os.path.isdir(code_path):
        pyfiles = globwalk.GlobDirectoryWalker(code_path, "*.py")
    else:
        pyfiles = [code_path]

    for pyfile in pyfiles:
        import pprint
        path = pyfile.split(os.path.sep)
        print "%s to %s: %s" % (pyfile, output_dir, os.path.join(output_dir, *path))
        fh = open(makepath(os.path.join(output_dir, *path)), 'w')
        print fh
        pprint.pprint(astview.tree(pyfile), fh)

if __name__ == '__main__':
    import sys
    import getopt

    usage = """\
Usage: python %s [-t] code_path output_dir
       output_dir must not exist (it will be created)
       unless -t is specified, if codepath is a file test it, if codepath is a directory
             test all .py files in and below that directory.
""" % sys.argv[0]

    testfile = False
    try:
        opts, args = getopt.getopt(sys.argv[1:], 'th')
    except:
        print usage
        sys.exit(1)
    for o, v in opts:
        if o == '-h':
            print usage
            sys.exit(0)
        if o == '-t':
            testfile = True
    if len(args) < 2 or len(args) > 3:
        print usage
        sys.exit(1)

    main(args[0], args[1], testfile)
