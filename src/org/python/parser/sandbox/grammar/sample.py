from __future__ import with_statement

class X(object):
    def __init__(self, x, y): pass
    def compute(self): return 10 * 20L

    @stacked(10, 12)
    @stacked2(5)
    def foo(self, a, b):
        return a + b * (a - b) | a

obj = X(1, 2)
obj.prop = 3
obj.prop += 2

another = 5

# f(10)^2 = 20

for x in [1,2,3]:
    pass
#    print x

with open('foo') as blah:
    for x in blah:
        fum(x)

try:
    foo()
except Foo:
    pass
else:
    y = 10 * 20
finally:
    foo()

