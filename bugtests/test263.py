
import cPickle, sys
import test263m

a = test263m.A()
b = test263m.B()


s = cPickle.dumps([a, b])

del sys.modules['test263m']
del test263m

cPickle.loads(s)
