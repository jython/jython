#
# test that cPickle_exception gets included in the jar.
#

import cPickle

cPickle.dumps([1,2,3,"abc"])
