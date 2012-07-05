import sys


def test():
    print dir(sys.modules['testpkg'])
    print 'submodule in sys.modules: %s' % ('testpkg.submodule' in sys.modules)
    import testpkg
    print testpkg.__file__
    print dir(testpkg)
    from testpkg import submodule
