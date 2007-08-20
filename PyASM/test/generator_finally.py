def gen(n):
    def make(v):
        def result():
            return v
        return result
    try:
        for x in xrange(n):
            try:
                data = yield make(x)
                print data
            except GeneratorExit:
                raise
            except Exception, e:
                print "EXCEPTION: ", e
    finally:
        print "done"

g = gen(10)
print "g.next():", g.next()()
print "g.send(14):", g.send(14)()
print "g.throw(...):", g.throw(RuntimeError("FEL"))()
print "g.next():", g.next()()
print "g.close():", g.close()
