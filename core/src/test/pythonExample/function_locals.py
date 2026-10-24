# function_locals.py
#
# We define a nest of functions with cell and free variables,
# and investigate how they look to the locals() built-in.


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
            return locals()
        return f3, locals()
    return f2


### Tests of inner function definition and closure handling.

f2 = f1(2, 4, 7)
f3, f2_locals = f2()
f3_locals = f3(3, 5)


# Delete since function object not marshallable:
del f1, f2, f3
del f2_locals['f3']
