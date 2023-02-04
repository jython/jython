# bool_left_arith.py
# binary operations invoked as bool op number

t = True
f = False

# Note bool is a sub-class of int
u = 42
a = t + u
b = t * u
c = f * u
d = -f

# Note bool is *not* a sub-class of float
u = 42.
a1 = t + u
b1 = t * u
c1 = f * u

