#! /usr/bin/env python

"""Regression test.

This will find all modules whose name is "test_*" in the test
directory, and run them.  Various command line options provide
additional facilities.

Command line options:

-v: verbose   -- run tests in verbose mode with output to stdout
-q: quiet     -- don't print anything except if a test fails
-g: generate  -- write the output file for a test instead of comparing it
-a: all       -- execute tests in all test(/regrtest.py) dirs on sys.path
  : broad     -- execute tests in all test(/regrtest.py) dirs on sys.path
-x: exclude   -- arguments are tests to *exclude*
-r: random    -- randomize test execution order
-m: memo      -- save results to file
-l: findleaks -- if GC is available detect tests that leak memory
-u: use       -- specify which special resource intensive tests to run
-h: help      -- print this text and exit
-e: expected  -- run only tests that are expected to run and pass

If non-option arguments are present, they are names for tests to run,
unless -x is given, in which case they are names for tests not to run.
If no test names are given, all tests are run.

-v is incompatible with -g and does not compare test output files.

-u is used to specify which special resource intensive tests to run,
such as those requiring large file support or network connectivity.
The argument is a comma-separated list of words indicating the
resources to test.  Currently only the following are defined:

    all -       Enable all special resources.

    curses -    Tests that use curses and will modify the terminal's
                state and output modes.

    largefile - It is okay to run some test that may create huge
                files.  These tests can take a long time and may
                consume >2GB of disk space temporarily.

    network -   It is okay to run tests that use external network
                resource, e.g. testing SSL support for sockets.

    subprocess  Run tests that invoke subprocesses, in particular
                test_subprocess.

To enable all resources except one, use '-uall,-<resource>'.  For
example, to run all the tests except for the network tests, give the
option '-uall,-network'.
"""

import sys
import os
import getopt
import traceback
import random
import StringIO

from test import test_support
sys.modules['test_support'] = test_support

# interim stuff

# !!! minimal (std) tests?

def clean_sys_path():
    new_sys_path = []
    for p in sys.path:
        if os.path.isfile(os.path.join(p,'regrtest.py')):
            continue
        new_sys_path.append(p)
    sys.path = new_sys_path

ALL = 0

def findtestdirs(all=ALL):
    testdirs = []
    j = os.path.join
    for p in sys.path:
        p = j(p,'test')
        if os.path.isfile(j(p,'regrtest.py')):
            testdirs.append(p)
            if not all:
                break
    return testdirs

def findalltests(testdir=None, stdtests=[], nottests=[],all=ALL):
    if testdir or not all:
        return findtests(testdir, stdtests, nottests)
    else:
        tests = {}
        testdirs = findtestdirs(all)
        for dir in testdirs:
            for test in findtests(dir,stdtests,nottests):
               tests[test] = 1

        tests =  tests.keys()
        tests.sort()
        
        return tests

def savememo(memo,good,bad,skipped):
    f = open(memo,'w')
    try:
        for n,l in [('good',good),('bad',bad),('skipped',skipped)]:
            print >>f,"%s = [" % n
            for x in l:
                print >>f,"    %r," % x
            print >>f," ]"
    finally:
        f.close()
    
class _Args:

    def __init__(self,txt):
        self.args = []
        for s in txt.split('\n'):
            self.pos = 0
            self.chunk = 0
            self.s = s
            self.state = self.space
            for ch in s:
                self.state(ch)
                self.pos += 1
            if self.state == self.quoted:
                raise Exception,"Expected closing quote for arg"
            self.state('"')

    def space(self,ch):
        if ch.isspace():
            pass
        elif ch == '"':
            self.state = self.quoted
        else:
            self.chunk = 1
            self.state = self.unquoted

    def quoted(self,ch):
        if ch == '"':
            if not self.s[self.pos-1] == '\\':
                arg = self.s[self.pos-self.chunk:self.pos];
                arg = arg.replace('\\"','"').replace('\\\\','\\')
                self.args.append(arg)
                self.chunk = 0
                self.state = self.space
                return
        self.chunk += 1

    def unquoted(self,ch):
        if ch.isspace():
            self.state = self.space
        elif ch == '"':
            self.state = self.quoted
        else:
            self.chunk += 1
            return
        self.args.append(self.s[self.pos-self.chunk:self.pos])
        self.chunk = 0

def _loadargs(fn):
    f=open(fn,'r')
    txt = f.read()
    f.close()
    return _Args(txt).args

_INDIRECTARGS = '@'

def with_indirect_args(args):
    new_args = []
    for arg in args:
        if arg.startswith(_INDIRECTARGS):
            new_args.extend(_loadargs(arg[1:]))
        else:
            new_args.append(arg)
    return new_args


# - * -



RESOURCE_NAMES = ['curses', 'largefile', 'network', 'subprocess']


def usage(code, msg=''):
    print __doc__
    if msg: print msg
    sys.exit(code)


def main(tests=None, testdir=None, verbose=0, quiet=0, generate=0,
         exclude=0, single=0, randomize=0, findleaks=0,
         use_resources=None, expected=0, allran=1):
    """Execute a test suite.

    This also parses command-line options and modifies its behavior
    accordingly.

    tests -- a list of strings containing test names (optional)
    testdir -- the directory in which to look for tests (optional)

    Users other than the Python test suite will certainly want to
    specify testdir; if it's omitted, the directory containing the
    Python test suite is searched for.

    If the tests argument is omitted, the tests listed on the
    command-line will be used.  If that's empty, too, then all *.py
    files beginning with test_ will be used.

    The other default arguments (verbose, quiet, generate, exclude,
    randomize, findleaks, and use_resources) allow programmers
    calling main() directly to set the values that would normally be
    set by flags on the command line.

    """

    test_support.record_original_stdout(sys.stdout)
    try:
        args = with_indirect_args(sys.argv[1:])
        opts, args = getopt.getopt(args, 'hvgqxrlu:am:e',
                                   ['help', 'verbose', 'quiet', 'generate',
                                    'exclude', 'random', 'expected',
                                    'findleaks', 'use=','all','memo=',
                                    'broad','oneonly='])
    except getopt.error, msg:
        usage(2, msg)

    # Defaults
    if use_resources is None:
        use_resources = []

    all = 0
    memo = None
    oneonly = []

    def strip_py(args):
        for i in range(len(args)):
            # Strip trailing ".py" from arguments
            if args[i][-3:] == os.extsep+'py':
                args[i] = args[i][:-3]
        return None
    
    for o, a in opts:
        if o in ('-h', '--help'):
            usage(0)
        elif o in ('-v', '--verbose'):
            verbose += 1
        elif o in ('-q', '--quiet'):
            quiet = 1;
            verbose = 0
        elif o in ('-g', '--generate'):
            generate = 1
        elif o in ('-a', '--all'):
            all = 1
        elif o in ('--broad',):
            all = 1
        elif o in ('--oneonly',):
            oneonly = a.split(',')
            strip_py(oneonly)
            allran = 0
        elif o in ('-x', '--exclude'):
            exclude = 1
            allran = 0
        elif o in ('-e', '--expected'):
            expected = 1
            allran = 0
        elif o in ('-r', '--randomize'):
            randomize = 1
        elif o in ('-l', '--findleaks'):
            findleaks = 1
        elif o in ('-m', '--memo'):
            memo = a
        elif o in ('-u', '--use'):
            u = [x.lower() for x in a.split(',')]
            for r in u:
                if r == 'all':
                    use_resources[:] = RESOURCE_NAMES
                    continue
                remove = False
                if r[0] == '-':
                    remove = True
                    r = r[1:]
                if r not in RESOURCE_NAMES:
                    usage(1, 'Invalid -u/--use option: ' + a)
                if remove:
                    if r in use_resources:
                        use_resources.remove(r)
                elif r not in use_resources:
                    use_resources.append(r)
    if generate and verbose:
        usage(2, "-g and -v don't go together!")

    good = []
    bad = []
    skipped = []
    resource_denieds = []

    if findleaks:
        try:
            import gc
        except ImportError:
            print 'No GC available, disabling findleaks.'
            findleaks = 0
        else:
            # Uncomment the line below to report garbage that is not
            # freeable by reference counting alone.  By default only
            # garbage that is not collectable by the GC is reported.
            #gc.set_debug(gc.DEBUG_SAVEALL)
            found_garbage = []

    strip_py(args)

    stdtests = STDTESTS[:]
    nottests = NOTTESTS[:]
    if exclude:
        for arg in args:
            if arg in stdtests:
                stdtests.remove(arg)
                allran = 0
        nottests[:0] = args
        args = []

    clean_sys_path()
        
    if args:
        allran = 0
    tests = tests or args or findalltests(testdir, stdtests, nottests,all=all)
    testdirs = findtestdirs(all)
    
    if randomize:
        random.shuffle(tests)
    test_support.verbose = verbose      # Tell tests to be moderately quiet
    test_support.use_resources = use_resources
    save_modules = sys.modules.keys()

    saved_sys_path = sys.path
    skips = _Expected(_skips)
    failures = _Expected(_failures)
    for test in tests:
        if expected and (test in skips or test in failures):
            continue
        test_basename = test
        consider_dirs = testdirs
        if test_basename in oneonly:
            consider_dirs = testdirs[:1]
        for testdir in consider_dirs:
            test = test_basename
            if not os.path.isfile(os.path.join(testdir,test+'.py')):
                continue

            test_spec = "[%s]%s" % (testdir,test)
            
            if not quiet:
                print test
                sys.stdout.flush()

            sys.path.insert(0,testdir)
            
            ok = runtest(test, generate, verbose, quiet, testdir)

            sys.path = saved_sys_path

#            test = test_spec
            
            if ok > 0:
                good.append(test)
            elif ok == 0:
                bad.append(test)
            else:
                skipped.append(test)
                if ok == -2:
                    resource_denieds.append(test)
            if findleaks:
                gc.collect()
                if gc.garbage:
                    print "Warning: test created", len(gc.garbage),
                    print "uncollectable object(s)."
                    # move the uncollectable objects somewhere so we don't see
                    # them again
                    found_garbage.extend(gc.garbage)
                    del gc.garbage[:]
            # Unload the newly imported modules (best effort finalization)
            for module in sys.modules.keys():
                if module not in save_modules and (
                    module.startswith("test.") or module.startswith('test_')):
                    test_support.unload(module)

    # The lists won't be sorted if running with -r
    good.sort()
    bad.sort()
    skipped.sort()

    if good and not quiet:
        if not bad and not skipped and len(good) > 1:
            print "All",
        print count(len(good), "test"), "OK."
        if verbose:
            print "CAUTION:  stdout isn't compared in verbose mode:  a test"
            print "that passes in verbose mode may fail without it."
    surprises = 0
    if skipped and not quiet:
        print count(len(skipped), "test"), "skipped:"
        surprises += countsurprises(skips, skipped, 'skip', 'ran', allran, resource_denieds)
    if bad:
        print count(len(bad), "test"), "failed:"
        surprises += countsurprises(failures, bad, 'fail', 'passed', allran, resource_denieds)

    if memo:
        savememo(memo,good,bad,skipped)

    return surprises > 0


STDTESTS = [
    'test_grammar',
    'test_opcodes',
    'test_operations',
    'test_builtin',
    'test_exceptions',
    'test_types',
   ]

NOTTESTS = [
    'test_support',
    'test_b1',
    'test_b2',
    'test_future1',
    'test_future2',
    'test_future3',
    ]

def findtests(testdir=None, stdtests=STDTESTS, nottests=NOTTESTS):
    """Return a list of all applicable test modules."""
    if not testdir: testdir = findtestdir()
    names = os.listdir(testdir)
    tests = []
    for name in names:
        if name[:5] == "test_" and name[-3:] == os.extsep+"py":
            modname = name[:-3]
            if modname not in stdtests and modname not in nottests:
                tests.append(modname)
    tests.sort()
    return stdtests + tests

def runtest(test, generate, verbose, quiet, testdir = None):
    """Run a single test.
    test -- the name of the test
    generate -- if true, generate output, instead of running the test
    and comparing it to a previously created output file
    verbose -- if true, print more messages
    quiet -- if true, don't print 'skipped' messages (probably redundant)
    testdir -- test directory
    """
    test_support.unload(test)
    if not testdir: testdir = findtestdir()
    outputdir = os.path.join(testdir, "output")
    outputfile = os.path.join(outputdir, test)
    if verbose:
        cfp = None
    else:
        cfp = StringIO.StringIO()
    try:
        save_stdout = sys.stdout
        try:
            if cfp:
                sys.stdout = cfp
                print test              # Output file starts with test name
            the_module = __import__(test, globals(), locals(), [])
            # Most tests run to completion simply as a side-effect of
            # being imported.  For the benefit of tests that can't run
            # that way (like test_threaded_import), explicitly invoke
            # their test_main() function (if it exists).
            indirect_test = getattr(the_module, "test_main", None)
            if indirect_test is not None:
                indirect_test()
        finally:
            sys.stdout = save_stdout
    except test_support.ResourceDenied, msg:
        if not quiet:
            print test, "skipped --", msg
            sys.stdout.flush()
        return -2
    except (ImportError, test_support.TestSkipped), msg:
        if not quiet:
            print test, "skipped --", msg
            sys.stdout.flush()
        return -1
    except KeyboardInterrupt:
        raise
    except test_support.TestFailed, msg:
        print "test", test, "failed --", msg
        sys.stdout.flush()
        return 0
    except:
        type, value = sys.exc_info()[:2]
        print "test", test, "crashed --", str(type) + ":", value
        sys.stdout.flush()
        if verbose:
            traceback.print_exc(file=sys.stdout)
            sys.stdout.flush()
        return 0
    else:
        if not cfp:
            return 1
        output = cfp.getvalue()
        if generate:
            if output == test + "\n":
                if os.path.exists(outputfile):
                    # Write it since it already exists (and the contents
                    # may have changed), but let the user know it isn't
                    # needed:
                    print "output file", outputfile, \
                          "is no longer needed; consider removing it"
                else:
                    # We don't need it, so don't create it.
                    return 1
            fp = open(outputfile, "w")
            fp.write(output)
            fp.close()
            return 1
        if os.path.exists(outputfile):
            fp = open(outputfile, "r")
            expected = fp.read()
            fp.close()
        else:
            expected = test + "\n"
        if output == expected:
            return 1
        print "test", test, "produced unexpected output:"
        sys.stdout.flush()
        reportdiff(expected, output)
        sys.stdout.flush()
        return 0

def reportdiff(expected, output):
    import difflib
    print "*" * 70
    a = expected.splitlines(1)
    b = output.splitlines(1)
    sm = difflib.SequenceMatcher(a=a, b=b)
    tuples = sm.get_opcodes()

    def pair(x0, x1):
        # x0:x1 are 0-based slice indices; convert to 1-based line indices.
        x0 += 1
        if x0 >= x1:
            return "line " + str(x0)
        else:
            return "lines %d-%d" % (x0, x1)

    for op, a0, a1, b0, b1 in tuples:
        if op == 'equal':
            pass

        elif op == 'delete':
            print "***", pair(a0, a1), "of expected output missing:"
            for line in a[a0:a1]:
                print "-", line,

        elif op == 'replace':
            print "*** mismatch between", pair(a0, a1), "of expected", \
                  "output and", pair(b0, b1), "of actual output:"
            for line in difflib.ndiff(a[a0:a1], b[b0:b1]):
                print line,

        elif op == 'insert':
            print "***", pair(b0, b1), "of actual output doesn't appear", \
                  "in expected output after line", str(a1)+":"
            for line in b[b0:b1]:
                print "+", line,

        else:
            print "get_opcodes() returned bad tuple?!?!", (op, a0, a1, b0, b1)

    print "*" * 70

def findtestdir():
    if __name__ == '__main__':
        file = sys.argv[0]
    else:
        file = __file__
    testdir = os.path.dirname(file) or os.curdir
    return testdir

def count(n, word):
    if n == 1:
        return "%d %s" % (n, word)
    else:
        return "%d %ss" % (n, word)

def printlist(x, width=70, indent=4):
    """Print the elements of a sequence to stdout.

    Optional arg width (default 70) is the maximum line length.
    Optional arg indent (default 4) is the number of blanks with which to
    begin each line.
    """

    line = ' ' * indent
    x = list(x)
    x.sort()
    for one in map(str, x):
        w = len(line) + len(one)
        if line[-1:] == ' ':
            pad = ''
        else:
            pad = ' '
            w += 1
        if w > width:
            print line
            line = ' ' * indent + one
        else:
            line += pad + one
    if len(line) > indent:
        print line

def countsurprises(expected, actual, action, antiaction, allran, resource_denieds):
    """returns the number of items in actual that aren't in expected."""
    printlist(actual)
    if not expected.isvalid():
        print "Ask someone to teach regrtest.py about which tests are"
        print "expected to %s on %s." % (action, sys.platform)
        return 1#Surprising not to know what to expect....
    good_surprise = expected.getexpected() - set(actual)
    if allran and good_surprise:
        print count(len(good_surprise), 'test'), antiaction, 'unexpectedly:'
        printlist(good_surprise)
    bad_surprise = set(actual) - expected.getexpected() - set(resource_denieds)
    if bad_surprise:
        print count(len(bad_surprise), action), "unexpected:"
        printlist(bad_surprise)
    return len(bad_surprise)

_skips = {
    'win32':
        """
        test_al
        test_cd
        test_cl
        test_commands
        test_crypt
        test_curses
        test_dbm
        test_dl
        test_email_codecs
        test_fcntl
        test_fork1
        test_gdbm
        test_gl
        test_grp
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_mhlib
        test_nis
        test_openpty
        test_poll
        test_pty
        test_pwd
        test_signal
        test_socket_ssl
        test_socketserver
        test_sunaudiodev
        test_timing
        """,
    'linux2':
        """
        test_al
        test_cd
        test_cl
        test_curses
        test_dl
        test_email_codecs
        test_gl
        test_imgfile
        test_largefile
        test_nis
        test_ntpath
        test_socket_ssl
        test_socketserver
        test_sunaudiodev
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'mac':
        """
        test_al
        test_bsddb
        test_cd
        test_cl
        test_commands
        test_crypt
        test_curses
        test_dbm
        test_dl
        test_email_codecs
        test_fcntl
        test_fork1
        test_gl
        test_grp
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_locale
        test_mmap
        test_nis
        test_ntpath
        test_openpty
        test_poll
        test_popen2
        test_pty
        test_pwd
        test_signal
        test_socket_ssl
        test_socketserver
        test_sunaudiodev
        test_sundry
        test_timing
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'unixware7':
        """
        test_al
        test_bsddb
        test_cd
        test_cl
        test_dl
        test_email_codecs
        test_gl
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_minidom
        test_nis
        test_ntpath
        test_openpty
        test_pyexpat
        test_sax
        test_socketserver
        test_sunaudiodev
        test_sundry
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'openunix8':
        """
        test_al
        test_bsddb
        test_cd
        test_cl
        test_dl
        test_email_codecs
        test_gl
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_minidom
        test_nis
        test_ntpath
        test_openpty
        test_pyexpat
        test_sax
        test_socketserver
        test_sunaudiodev
        test_sundry
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'sco_sv3':
        """
        test_al
        test_asynchat
        test_bsddb
        test_cd
        test_cl
        test_dl
        test_email_codecs
        test_fork1
        test_gettext
        test_gl
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_locale
        test_minidom
        test_nis
        test_ntpath
        test_openpty
        test_pyexpat
        test_queue
        test_sax
        test_socketserver
        test_sunaudiodev
        test_sundry
        test_thread
        test_threaded_import
        test_threadedtempfile
        test_threading
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'riscos':
        """
        test_al
        test_asynchat
        test_bsddb
        test_cd
        test_cl
        test_commands
        test_crypt
        test_dbm
        test_dl
        test_email_codecs
        test_fcntl
        test_fork1
        test_gdbm
        test_gl
        test_grp
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_locale
        test_mmap
        test_nis
        test_ntpath
        test_openpty
        test_poll
        test_popen2
        test_pty
        test_pwd
        test_socket_ssl
        test_socketserver
        test_strop
        test_sunaudiodev
        test_sundry
        test_thread
        test_threaded_import
        test_threadedtempfile
        test_threading
        test_timing
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'darwin':
        """
        test_al
        test_cd
        test_cl
        test_curses
        test_dl
        test_email_codecs
        test_gdbm
        test_gl
        test_imgfile
        test_largefile
        test_locale
        test_linuxaudiodev
        test_minidom
        test_nis
        test_ntpath
        test_poll
        test_socket_ssl
        test_socketserver
        test_sunaudiodev
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'sunos5':
        """
        test_al
        test_bsddb
        test_cd
        test_cl
        test_curses
        test_dbm
        test_email_codecs
        test_gdbm
        test_gl
        test_gzip
        test_imgfile
        test_linuxaudiodev
        test_mpz
        test_openpty
        test_socketserver
        test_zipfile
        test_zlib
        """,
    'hp-ux11':
        """
        test_al
        test_bsddb
        test_cd
        test_cl
        test_curses
        test_dl
        test_gdbm
        test_gl
        test_gzip
        test_imgfile
        test_largefile
        test_linuxaudiodev
        test_locale
        test_minidom
        test_nis
        test_ntpath
        test_openpty
        test_pyexpat
        test_sax
        test_socketserver
        test_sunaudiodev
        test_zipfile
        test_zlib
        """,
    'freebsd4':
        """
        test_al
        test_cd
        test_cl
        test_curses
        test_email_codecs
        test_gdbm
        test_gl
        test_imgfile
        test_linuxaudiodev
        test_locale
        test_minidom
        test_nis
        test_pyexpat
        test_sax
        test_socket_ssl
        test_socketserver
        test_sunaudiodev
        test_unicode_file
        test_winreg
        test_winsound
        """,
    'java':
        """
        test_aepack
        test_al
        test_audioop
        test_bsddb
        test_bsddb185
        test_bsddb3
        test_bz2
        test_capi
        test_cd
        test_cl
        test_codeccallbacks
        test_codeop
        test_commands
        test_crypt
        test_csv
        test_curses
        test_dbm
        test_dl
        test_dummy_thread
        test_dummy_threading
        test_email_codecs
        test_fcntl
        test_fork1
        test_gdbm
        test_getargs2
        test_gl
        test_grp
        test_hotshot
        test_imageop
        test_imgfile
        test_import
        test_ioctl
        test_largefile
        test_linuxaudiodev
        test_locale
        test_longexp
        test_macfs
        test_macostools
        test_mmap
        test_mpz
        test_nis
        test_normalization
        test_openpty
        test_ossaudiodev
        test_parser
        test_plistlib
        test_poll
        test_pty
        test_pwd
        test_pyexpat
        test_regex
        test_resource
        test_rgbimg
        test_robotparser
        test_rotor
        test_scriptpackages
        test_signal
        test_socket_ssl
        test_socketserver
        test_stringprep
        test_strop
        test_strptime
        test_sunaudiodev
        test_sundry
        test_symtable
        test_timeout
        test_timing
        test_unicode_file
        test_unicodedata
        test_wave
        test_winreg
        test_winsound
        """
}
if test_support.is_jython:
    if test_support.underlying_system.startswith('win'):
        _skips['java'] += 'test_mhlib'
    else:
        _skips['java'] += 'test_pep277'

_failures = {
    'java':
    '''
    test_ast
    test_class
    test_copy
    test_dis
    test_descrtut
    test_eof
    test_frozen
    test_gc
    test_hexoct
    test_iterlen
    test_marshal
    test_pep263
    test_pkgimport
    test_profilehooks
    test_pyclbr
    test_quopri
    test_random
    test_softspace
    test_syntax
    test_trace
    test_ucn
    test_unicode
    test_zipimport
    ''',
}

class _Expected:
    def __init__(self, expect_dict):
        self.valid = 0
        platform = sys.platform
        if platform[:4] == 'java':
            platform = 'java'
        if expect_dict.has_key(platform):
            s = expect_dict[platform]
            self.expected = set(s.split())
            self.valid = 1

    def isvalid(self):
        "Return true iff _Expected knows about the current platform."
        return self.valid

    def getexpected(self):
        """Return set of test names we expect to skip on current platform.

        self.isvalid() must be true.
        """

        assert self.isvalid()
        return self.expected

    def __contains__(self, key):
        return key in self.expected

if __name__ == '__main__':
    sys.exit(main())
