
import string
s = "elem1 elem2"
try:
    (a, b, c) = string.split(s)
    (d, e, f) = string.split(s)
    pass
except ValueError:
   pass
else:
   print support.TestError("Should raise a ValueError")


