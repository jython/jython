"""

"""

import support

import exceptions

class A(exceptions.Exception):
    def __init__(self, args):
	exceptions.Exception.__init__(self, args)


support.compare(A, "test141.A|__main__.A")

#print support.TestError

