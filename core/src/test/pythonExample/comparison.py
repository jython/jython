# comparison.py

# Tests of the order comparisons

a = 2
b = 4

lt = a < b
le = a <= b
eq = a == b
ne = a != b
ge = a >= b
gt = a > b


a = 4
b = 2

lt1 = a < b
le1 = a <= b
eq1 = a == b
ne1 = a != b
ge1 = a >= b
gt1 = a > b


a = 2
b = 2

lt2 = a < b
le2 = a <= b
eq2 = a == b
ne2 = a != b
ge2 = a >= b
gt2 = a > b

# Tests of 'in'

t = ("cow", 2, "pig", None, 42.0)
f0 = 1 in t
f1 = "c" in t
t1x = "c" not in t
f2 = 42.1 in t
f3 = (2,) in t
f4 = "c" in t[2]


t0 = 2 in t
t1 = "pig" in t
f1x = "pig" not in t
t2 = None in t
t3 = 42 in t
t4 = "p" in t[2]


