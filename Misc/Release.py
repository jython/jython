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

    --prep
    -p
        Prep the release for running the Jshield installer

    --build
    -b
        Do the build by running the Jshield installer

    --clean
    -c
        Clean up after ourselves

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

PYLIB = 'pylib152b.jar'

program = sys.argv[0]

def usage(status, msg=''):
    print __doc__ % globals()
    if msg:
        print msg
    sys.exit(status)


# CVS related commands and other utils

def cvsdo(cvscmd, indir=None):
    curdir = os.getcwd()
    try:
        if indir:
            os.chdir(indir)
        os.system('cvs %s' % cvscmd)
    finally:
        os.chdir(curdir)

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

def listdir_ex(dir):
    try: return os.listdir(dir)
    except OSError, e:
        print e
        return []

def unlink_ex(filename):
    try: os.unlink(filename)
    except OSError, e:
        print e

def rmdir_ex(dirname):
    try: os.rmdir(dirname)
    except OSError, e:
        print e

def rmdirhier(dirname):
    def zapit(dirs, dirname, names):
        for file in names:
            filename = os.path.join(dirname, file)
            if os.path.isfile(filename):
                unlink_ex(filename)
            else:
                dirs.append(filename)
    dirs = []
    os.path.walk(dirname, zapit, dirs)
    dirs.reverse()
    dirs.append(dirname)
    for dir in dirs:
        rmdir_ex(dir)



# preparations

def make_jpython_jar(withoro=1, jarname='jpython.jar'):
    # change dirs no manifest file, no zip compression
    jarcmd = ['jar cvfM0', jarname]
    jarcmd.append('-C dist')
    jarcmd.append('dummy')                        # JDK 1.2 jar bug workaround
    #
    # add all the .class files in these dirs inside org/python
    for dir in ['core', 'modules', 'parser', 'compiler', 'util', 'rmi']:
        jarcmd.append(os.path.join('dist/org/python/', dir, '*.class'))
    #
    # add all the classes from ORO
    if withoro:
        jarcmd.append('-C nondist/ORO')
        jarcmd.append('dummy')                    # JDK 1.2 jar bug workaround
        jarcmd.append('nondist/ORO/com/oroinc/text/regex/*.class')
    #
    # invoke to build jpython.jar
    cmd = string.join(jarcmd)
    os.system(cmd)


def make_pylib_jar():
    # TBD: Jim only includes these files from the standard library.  Why?  I
    # guess every download byte counts!  I still don't like explicitly
    # including specific files; probably better to explicitly exclude those we 
    # don't want
    files = ['BaseHTTPServer.py', 'binhex.py', 'bisect.py', 'calendar.py',
             'cgi.py',
             'CGIHTTPServer.py', 'cmd.py', 'cmp.py', 'cmpcache.py',
             'colorsys.py',
             'commands.py', 'compileall.py', 'ConfigParser.py', 'copy.py',
             'copy_reg.py',
             'dircache.py', 'dircmp.py', 'dospath.py', 'dumbdbm.py',
             'dump.py', 'exceptions.py',
             'fileinput.py', 'find.py', 'fnmatch.py', 'formatter.py',
             'fpformat.py', 'ftplib.py', 
             'getopt.py', 'glob.py', 'gopherlib.py', 'grep.py',
             'htmlentitydefs.py', 'htmllib.py',
             'httplib.py', 'imghdr.py', 'keyword.py', 'linecache.py',
             'macpath.py', 'macurl2path.py',
             'mailbox.py', 'mailcap.py', 'mhlib.py', 'mimetools.py',
             'mimetypes.py', 'MimeWriter.py',
             'mimify.py', 'multifile.py', 'mutex.py', 'newdir.py',
             'nntplib.py', 'ntpath.py',
             'nturl2path.py', 'packmail.py', 'pickle.py', 'pipes.py',
             'poly.py', 'popen2.py',
             'posixfile.py', 'posixpath.py', 'pprint.py', 'pyclbr.py',
             'Queue.py', 'quopri.py',
             'rand.py', 'random.py', 'reconvert.py', 'repr.py', 'rfc822.py',
             'sched.py',
             'sgmllib.py', 'shelve.py', 'shutil.py', 'SimpleHTTPServer.py',
             'sndhdr.py',
             'SocketServer.py', 'stat.py', 'StringIO.py', 'symbol.py',
             'tb.py',
             'telnetlib.py', 'tempfile.py', 'token.py', 'tokenize.py',
             'traceback.py',
             'tzparse.py', 'urllib.py', 'urlparse.py', 'user.py',
             'UserDict.py', 'UserList.py',
             'whrandom.py', 'xdrlib.py', 'xmllib.py', 'zmod.py',
             'poplib.py', 'smtplib.py', 'imaplib.py',
             'bdb.py', 'pdb.py', 'profile.py', 'anydbm.py',
             ]
    src = '/home/bwarsaw/projects/python/pristine'
    libsrc = os.path.join(src, 'Lib')
    print 'updating in', libsrc
    cvsdo('-q up -P -d', libsrc)
    includep = {}
    for file in files:
        includep[file] = 1
    # glom up the jar command
    jarcmd = ['jar cvfM', PYLIB]
    # add the __run__.py file
    jarcmd.append('-C dist/util')
    jarcmd.append('doesnotexist')                 # JDK 1.2 jar bug workaround
    jarcmd.append('dist/util/__run__.py')
    jarcmd.append('-C ' + src)
    jarcmd.append('doesnotexist')                 # JDK 1.2 jar bug workaround
    for file in os.listdir(libsrc):
        if file[-3:] <> '.py':
##            print '    skipping', file
            continue
        if includep.has_key(file):
            jarcmd.append(os.path.join(libsrc, file))
        else:
            print '    skipping', file
    jarcmd.append(os.path.join(libsrc, 'test', 'pystone.py'))
    cmd = string.join(jarcmd)
    os.system(cmd)


def make_jars():
    print 'making all-inclusive jar...'
    make_jpython_jar()
    print 'making jpython-only jar...'
    make_jpython_jar(withoro=0, jarname='jpython-only.jar')
    print 'making pylib jar...'
    make_pylib_jar()



def prep_distro(tagname):
    # This function creates a directory image that the Jshield installer gets
    # ponted at.  Jshield then gloms everything under this directory into the
    # Java installer.
    #
    print 'exporting dist...',
    # watch out for dots in the name
    table = string.maketrans('.', '_')
    # To be done from writeable repository
    relname = '"Release_' + string.translate(tagname, table) + '"'
    cvsdo('export -k kv -r %s -d export jpython/dist' % relname)
    #
    print 'preparing distribution...'
    make_jars()
    #
    # The directory structure laid out by the installer isn't the same as the
    # CVS tree, so we need to move some things around
    #
    # get rid of Tools/freeze and tools/mkjava.py
    freezedir = 'export/Tools/freeze'
    for file in listdir_ex(freezedir):
        unlink_ex(os.path.join(freezedir, file))
    rmdir_ex(freezedir)
    unlink_ex('export/Tools/mkjava.py')
    # get rid of the experiments subtree
    rmdirhier('export/experiments')
    rmdirhier('export/tests')
    unlink_ex('export/jpython.bat')
    unlink_ex('export/jpython.isj')
    unlink_ex('export/mkisj.bat')



def do_build(tagname):
    trans = string.maketrans('', '')
    tagname = string.translate(tagname, trans, '.')
    print 'tagname:', tagname
    # make non-ORO distro
    os.rename('export/LICENSE-ORO.txt', 'LICENSE.txt')
    os.system('cp jpython-only.jar export/jpython.jar')
    os.system('nondist/Jshield/bin/jshield dist/jpython.isj')
    os.rename('JPython'+tagname+'.class', 'JPythonONLY'+tagname+'.class')
    # make full distribution
    os.rename('LICENSE.txt', 'export/LICENSE.txt')
    os.unlink('export/jpython.jar')
    os.system('cp -f jpython.jar export')
    os.system('nondist/Jshield/bin/jshield dist/jpython.isj')
    

def do_clean():
    rmdirhier('export')
    unlink_ex('jpython.jar')
    unlink_ex('jpython-only.jar')
    # clean up the CVS/Entries file
    os.rename('CVS/Entries', 'CVS/Entries.old')
    infp = open('CVS/Entries.old')
    outfp = open('CVS/Entries', 'w')
    while 1:
        line = infp.readline()
        if not line:
            break
        if line[:8] == 'D/export':
            continue
        outfp.write(line)
    infp.close()
    outfp.close()
    os.remove('CVS/Entries.old')
    unlink_ex(PYLIB)



def main():
    try:
	opts, args = getopt.getopt(
            sys.argv[1:], 'htTpbc',
            ['help', 'tag', 'TAG', 'prep', 'build', 'clean'])
    except getopt.error, msg:
	usage(1, msg)

    # make sure we're in the proper directory
    dirs = os.listdir('.')
    if 'dist' not in dirs or 'nondist' not in dirs:
        usage(1, 'run this script from the top of the JPython CVS tree')

    # default options
    tag = 0
    retag = 0
    prep = 0
    build = 0
    clean = 0

    for opt, arg in opts:
	if opt in ('-h', '--help'):
	    usage(0)
	elif opt in ('-t', '--tag'):
	    tag = 1
	elif opt in ('-T', '--TAG'):
	    tag = 1
	    retag = 1
        elif opt in ('-p', '--prep'):
            prep = 1
        elif opt in ('-b', '--build'):
            build = 1
        elif opt in ('-c', '--clean'):
            clean = 1

    # required minor rev number
    if tag or prep or build:
        if len(args) <> 1:
            usage(1, 'tagname argument is required')
        else:
            tagname = args[0]

    # very important!!!
    omask = os.umask(0)
    try:
        if tag:
            tag_release(tagname, retag)

        if prep:
            prep_distro(tagname)

        if build:
            do_build(tagname)

        if clean:
            do_clean()
    finally:
        os.umask(omask)

if __name__ == '__main__':
    main()
