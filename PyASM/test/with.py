from __future__ import with_statement

#print "with_statement:", with_statement

class ContextGuard(object):
    def __init__(self, name, ret):
        self.__name = name
        self.__ret = ret

    def __enter__(self):
        print "Enter", self.__name
        return "ContextGuard(%s)" % repr(self.__name)

    def __exit__(self, *args):
        print "Exit", self.__name, args
        return self.__ret

with ContextGuard("guard 1", True) as guard:
    print "inside", guard

with ContextGuard("guard 2", False) as guard:
    print "inside", guard

with ContextGuard("guard 3", True) as guard:
    print "inside", guard
    raise RuntimeError("test")

with ContextGuard("guard 4", False) as guard:
    print "inside", guard
    raise RuntimeError("test")
