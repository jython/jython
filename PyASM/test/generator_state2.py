def gen(l):
    for x in xrange(l):
        try:
            yield x
        except:
            print "err"

it = gen(4)
print it.next()
print it.throw(RuntimeError('probe'))
