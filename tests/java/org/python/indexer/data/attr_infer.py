
# test provisional binding for attribute of
# a union of an unknown type and a known type
def foo(a):
  # this line should make a temp binding for b in foo's scope,
  # and 
  b.__add__(1)
  b = a
  b = 2
