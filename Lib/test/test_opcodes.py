# Python test set -- part 2, opcodes

from test_support import *


print_test('Opcodes (test_opcodes.py)', 1)
print_test('try inside for loop', 2)

n = 0
for i in range(10):
        n = n+i
        try: 1/0
        except NameError: pass
        except ZeroDivisionError: pass
        except TypeError: pass
        try: pass
        except: pass
        try: pass
        finally: pass
        n = n+i
        
assert n == 90, 'try inside for'

print_test('raise class exceptions')

class AClass: pass
class BClass(AClass): pass
class CClass: pass
class DClass(AClass):
    def __init__(self, ignore):
        pass

try: raise AClass()
except: pass

try: raise AClass()
except AClass: pass

try: raise BClass()
except AClass: pass

try: raise BClass()
except CClass: raise TestFailed
except: pass

a = AClass()
b = BClass()

try: raise AClass, b
except BClass, v:
        assert v == b, 'class exceptions'
else: raise TestFailed

try: raise b
except AClass, v:
        assert v == b, 'class exceptions'

# not enough arguments
try:  raise BClass, a
except TypeError: pass

try:  raise DClass, a
except DClass, v:
    assert isinstance(v, DClass)

