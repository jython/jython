#! /usr/bin/env python

"""Manage releases of JPython.

Usage: %(program)s [options] tagname

Where `options' are:

    --tag
    -t
        Tag all release files with tagname.

    --TAG
    -T
        Like --tag, but relocates any existing tag.  See `cvs tag -F'.  Only
        one of --tag or --TAG can be given on the command line.

    --help
    -h
        Print this help message.

    tagname is used in the various commands above.  It should essentially be
    the version number for the release, and is required.

"""

import sys
import os
import string
import re
import time
import tempfile
import getopt

program = sys.argv[0]

def usage(status, msg=''):
    print __doc__ % globals()
    if msg:
        print msg
    sys.exit(status)


_releasedir = None
def releasedir(tagname=None):
    global _releasedir
    if not _releasedir:
        _releasedir = os.path.join('nondist', 'src')
    return _releasedir
        


# CVS related commands

def cvsdo(cvscmd):
    os.system('cvs %s' % cvscmd)

def tag_release(tagname, retag):
    # watch out for dots in the name
    table = string.maketrans('.', '_')
    # To be done from writeable repository
    relname = '"Release_' + string.translate(tagname, table) + '"'
    print 'Tagging release with', relname, '...'
    option = ''
    if retag:
	option = '-F'
    cvsdo('tag %s %s' % (option, relname))

def export(tagname):
    print 'exporting src...',
    # watch out for dots in the name
    table = string.maketrans('.', '_')
    # To be done from writeable repository
    relname = '"Release_' + string.translate(tagname, table) + '"'
    cvsdo('export -k kv -r %s -d %s jpython' % (relname, releasedir()))



def main():
    try:
	opts, args = getopt.getopt(sys.argv[1:],
                                   'tTh',
                                   ['tag', 'TAG', 'help'])
    except getopt.error, msg:
	usage(1, msg)

    # required minor rev number
    if len(args) <> 1:
	usage(1, 'tagname argument is required')

    tagname = args[0]

    # make sure we're in the proper directory
    dirs = os.listdir('.')
    if 'dist' not in dirs or 'nondist' not in dirs:
        usage(1, 'run this script from the top of the JPython CVS tree')

    # default options
    tag = 0
    retag = 0
    export = 0

    for opt, arg in opts:
	if opt in ('-h', '--help'):
	    usage(0)
	elif opt in ('-t', '--tag'):
	    tag = 1
	elif opt in ('-T', '--TAG'):
	    tag = 1
	    retag = 1
        elif opt in ('-e', '--export'):
            export = 1

    # very important!!!
    omask = os.umask(0)
    try:
        if tag:
            tag_release(tagname, retag)
    finally:
        os.umask(omask)

if __name__ == '__main__':
    main()
