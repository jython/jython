

import test.regrtest

import os, sys



skipped = [
    'test_al',
    'test_asynchat',
    'test_audioop',
    'test_b1',
    'test_b2',
    'test_bastion',
    'test_bsddb',
    'test_capi',
    'test_cd',
    'test_cl',
    'test_cmath',
    'test_commands',
    'test_crypt',
    'test_curses',
    'test_dbm',
    'test_dl',
    'test_fcntl',
    'test_fork1',
    'test_frozen',
    'test_gc',
    'test_gettext',
    'test_gdbm',
    'test_gl',
    'test_grp',
    'test_hotshot',
    'test_imageop',
    'test_imgfile',
    'test_linuxaudiodev',
    'test_locale',
    'test_longexp',
    'test_minidom',
    'test_mmap',
    'test_nis',
    'test_openpty',
    'test_parser',
    'test_poll',
    'test_pty',
    'test_pwd',
    'test_regex',
    'test_rgbimg',
    'test_rotor',
    'test_sax',
    'test_select',
    'test_signal',
    'test_socketserver',
    'test_socket_ssl',
    'test_strop',
    'test_sundry',
    'test_sunaudiodev',
    'test_symtable',
    'test_timing',
    'test_unicodedata',
    'test_wave',
    'test_winreg',
    'test_winsound',
]

failures = [
    'test_array',
    'test_binop',
    'test_codeop',
    'test_compare',
    'test_cookie',
    'test_cpickle',
    'test_descr',
    'test_descrtut',
    'test_doctest2',
    'test_email',
    'test_extcall',
    'test_fpformat',
    'test_funcattrs',
    'test_future1',
    'test_future2',
    'test_future3',
    'test_generators',
    'test_getargs',
    'test_hmac',
    'test_inspect',
    'test_iter',
    'test_largefile',
    'test_long',
    'test_long_future',
    'test_mailbox',
    'test_marshal',
    'test_mhlib',
    'test_mutants',
    'test_ntpath',
    'test_os',
    'test_operations',
    'test_operator',
    'test_pickle',
    'test_pkgimport',
    'test_popen2',
    'test_pow',
    'test_pprint',
    'test_profile',
    'test_profilehooks',
    'test_pyclbr',
    'test_pyexpat',
    'test_repr',
    'test_richcmp',
    'test_scope',
    'test_socket',
    'test_struct',
    'test_tempfile',
    'test_threaded_import',
    'test_threadedtempfile',
    'test_traceback',
    'test_types',
    'test_ucn',
    'test_unary',
    'test_unicode',
    'test_unicode_file',
    'test_urllib2',
    'test_userlist',
    'test_uu',
    'test_weakref',
    'test_zlib',
]




def usage():
    print "jython stdtest.py [options] [tests]"
    print "  -h, --help     : print this help"
    print "  -v, --verbose  : turn on verbosity"
    print "  -s, --skipped  : Run the tests that is normally skipped"
    print "  -f, --failures : Run the tests that normally fails"
    print "  -t, --test     : Run the tests listed as arguments"

def main():
    import getopt
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hvsft:", [
                    "help", "verbose", "skipped", "failures", "test="])
    except getopt.GetoptError:
        # print help information and exit:
        usage()
        sys.exit(2)


    alltests = [ f[:-3] for f in os.listdir("../dist/Lib/test")
                     if f.startswith("test_") and f.endswith(".py") ]
    tests = [s for s in alltests if s not in failures and s not in skipped]
    verbose = 0

    for o, a in opts:
        if o in ("-h", "--help"):
            usage()
            sys.exit()
        if o in ("-v", "--verbose"):
            verbose = 1
        if o in ("-s", "--skipped"):
            tests = skipped
        if o in ("-f", "--failures"):
            tests = failures
        if o in ("-t", "--test"):
            tests = a.split(",")

    sys.argv = []

    if tests.count("test_largefile") > 0:
        tests.remove("test_largefile")

    test.regrtest.main(tests, verbose=verbose)
    

if __name__ == "__main__":
    main()


#test.regrtest.main(tests, verbose=0)
#test.regrtest.main(skipped, verbose=0)
test.regrtest.main(failures, verbose=0)
