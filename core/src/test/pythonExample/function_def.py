# function_def.py
#
# We create function objects with various signatures
# and check some properties of each.

def foo():
    pass

foo_name = foo.__name__
foo_qualname = foo.__qualname__
foo_defaults = foo.__name__
foo_kwdefaults = foo.__name__
foo_module = foo.__module__  # None, in the way we run this.


def bar(a, b, c=3, *, d=4, e=5):
   # A body so that varnames is not just arguments
    x = a + b
    y = c + d * e
    return x * y

bar.__module__ = 42

bar_name = bar.__name__
bar_qualname = bar.__qualname__
bar_defaults = bar.__name__
bar_kwdefaults = bar.__name__
bar_module = bar.__module__
bar_module = bar.__module__
#TODO dict.__contains__ missing and bug in Comparison.IN
#bar_globals_foo = foo in bar.__globals__


def baz(a, b, c=30, *aa, d=40, e=50):
    "This is a documentation string."
    pass

baz_name = baz.__name__
baz_qualname = baz.__qualname__
baz_defaults = baz.__name__
baz_kwdefaults = baz.__name__
baz_doc = baz.__doc__


def qux(a, b, c=300, *aa, d=400, e=500, **kk):
    pass

qux_name = qux.__name__
qux_qualname = qux.__qualname__
qux_defaults = qux.__name__
qux_kwdefaults = qux.__name__


# Delete since function object not marshallable:
del foo, bar, baz, qux
