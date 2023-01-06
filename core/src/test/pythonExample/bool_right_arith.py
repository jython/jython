# bool_right_arith.py
# binary operations invoked as number op bool

t = True
f = False

# Note bool is a sub-class of int
u = 42
a = u + t
b = u * t
c = u * f
d = -t

# Note bool is *not* a sub-class of float
u = 42.
a1 = u + t
b1 = u * t
c1 = u * f

