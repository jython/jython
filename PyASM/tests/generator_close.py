from java.lang import System

def gen():
    try:
        yield None
    except Exception, e:
        print "Caught", e
        raise

it = gen()
it.next()
it.close()
it = gen()
it.next()
del it

System.gc()
