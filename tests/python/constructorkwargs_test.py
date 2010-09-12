from org.python.tests.constructor_kwargs import KWArgsObject

x = KWArgsObject(a=1, b=2, c=3)

assert x.getValue('a') == 1
assert x.getValue('b') == 2
assert x.getValue('c') == 3

