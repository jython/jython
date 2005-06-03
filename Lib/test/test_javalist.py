from test_support import *
from javatests import ListTest

class PyListTest(ListTest):
       
   def __init__(self):
       ListTest.__init__(self)
       
   def newInstance(self, coll):
       if coll is None:
           return list()
       else:
           return list(coll)
       
   def isReadOnly(self):
       return False
       

class PyTupleTest(ListTest):
       
   def __init__(self):
       ListTest.__init__(self)
       
   def newInstance(self, coll):
       if coll is None:
           return tuple()
       else:
           return tuple(coll)
       
   def isReadOnly(self):
       return True       


# these first two tests just verify that we have a good unit test
print_test("ListTest.java driver (test_javalist.py)", 1)
print_test("running test on ArrayList", 2)
alt = ListTest.getArrayListTest(False)
alt.testAll()

print_test("running test on ArrayList (read-only)", 2)
alt = ListTest.getArrayListTest(True)
alt.testAll()


# Now run the critical tests

print_test("running test on PyListTest", 2)       
plt = PyListTest()
plt.testAll()       

print_test("running test on PyTupleTest", 2)       
ptt = PyTupleTest()
ptt.testAll()  