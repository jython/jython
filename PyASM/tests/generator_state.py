def gen(l):
    for x in xrange(l):
        try:
            yield x
        finally:
            print "one"

for x in gen(4):
    print "res:", x
