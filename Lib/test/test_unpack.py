from test_support import *

print_test("Sequence Unpacking (test_unpack.py)", 2)

t = (1, 2, 3)
l = [4, 5, 6]

class Seq:
    def __getitem__(self, i):
        if i >= 0 and i < 3: return i
        raise IndexError

a = -1
b = -1
c = -1

# unpack tuple
print_test("tuple", 3)
a, b, c = t
assert a == 1 and b == 2 and c == 3

print_test("list", 3)
a, b, c = l
assert a == 4 and b == 5 and c == 6

print_test("inline tuple")
a, b, c = 7, 8, 9
assert a == 7 and b == 8 and c == 9

print_test("string")
a, b, c = 'one'
assert a == 'o' and b == 'n' and c == 'e'

print_test("generic sequence")
a, b, c = Seq()
assert a == 0 and b == 1 and c == 2

# now for some failures
print_test("failures")

print_test("non-sequence", 4)
try:
    a, b, c = 7
    raise TestFailed
except TypeError:
    pass

print_test("wrong size tuple")
try:
    a, b = t
    raise TestFailed
except ValueError:
    pass
    
print_test("wrong size list")
try:
    a, b = l
    raise TestFailed
except ValueError:
    pass


print_test("sequence too short")
try:
    a, b, c, d = Seq()
    raise TestFailed
except ValueError:
    pass


print_test("sequence too long")
try:
    a, b = Seq()
    raise TestFailed
except ValueError:
    pass


# unpacking a sequence where the test for too long raises a different
# kind of error
BozoError = 'BozoError'

class BadSeq:
    def __getitem__(self, i):
        if i >= 0 and i < 3:
            return i
        elif i == 3:
            raise BozoError
        else:
            raise IndexError

print_test("sequence too long, wrong error")
try:
    a, b, c, d, e = BadSeq()
    raise TestFailed
except BozoError:
    pass


print_test("sequence too short, wrong error")
try:
    a, b, c = BadSeq()
    raise TestFailed
except BozoError:
    pass
