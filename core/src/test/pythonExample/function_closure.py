# function_closure.py
#
# We define a nest of functions and call them.


# Each outer function returns its nested function for inspection
def f1(a, b, u=1):
    # cell vars are (cell|free): (a, b, z |)
    z = 1
    a = (a + b) * u
    def f2(c = -1, v = 2):
        # cell vars are (cell|free): (c, y | a, b, z)
        nonlocal z
        y = 11 + z
        z = v
        def f3(d, e):
            # cell vars are (cell|free): (| a, b, c, y, z)
            nonlocal y, z
            w = 111
            x = w + y + z
            y = 3 + w
            z = 4 + a
            return (a, b, c, d, e, x, y, z)
        return f3
    return f2


### Tests of inner function definition and closure handling.

f2 = f1(2, 4, 7)
f3 = f2()
t = f3(3, 5)

f2_name = f2.__name__
f2_qualname = f2.__qualname__
f2_varnames = f2.__code__.co_varnames  # regular args/vars
f2_cellvars = f2.__code__.co_cellvars  # made in f2
f2_freevars = f2.__code__.co_freevars  # from outer

f2_closure = f2.__closure__  # corresponding to names in freevars
f2_closure_len = len(f2_closure)

f3_name = f3.__name__
f3_qualname = f3.__qualname__
f3_varnames = f3.__code__.co_varnames  # regular args/vars
f3_cellvars = f3.__code__.co_cellvars  # made in f3
f3_freevars = f3.__code__.co_freevars  # from outer

f3_closure = f3.__closure__  # corresponding to names in freevars
f3_closure_len = len(f3_closure)


# Delete since function object not marshallable:
del f1, f2, f3

# Cells are not marshallable either.
del f2_closure, f3_closure

