
import support

a = 1
a += 1 + 2
if a != 4:
   raise support.TestError, "Wrong result #1 %s" % a
 
a=[0,1,2,3,4,5]
if a[1:3] != [1, 2]:
   raise support.TestError, "Wrong result #2 %s" % a[1:3]

a[1:3] += ["a", "b"]
if a != [0, 1, 2, 'a', 'b', 3, 4, 5]:
   raise support.TestError, "Wrong result #3 %s" % a


s = None

class A:
   def __getitem__(self, i):
      global s
      if s is None:
          s = i
      else:
          raise support.TestError, "__getitem__ should only be called once"
      return 11
   def __setitem__(self, i, v):
      if s != i:
          raise support.TestError, "__setitem__ should have same index af __getitem__"

a = A()
a[:, ..., ::, 0:10:2, :10:, 1, 2:, ::-1] += 1