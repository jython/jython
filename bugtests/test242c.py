
import support

# list comprehension tests
nums = [1, 2, 3, 4, 5]
strs = ["Apple", "Banana", "Coconut"]
spcs = ["  Apple", " Banana ", "Coco  nut  "]

if [s.strip() for s in spcs] != ['Apple', 'Banana', 'Coco  nut']:
   raise support.TestError, "Wrong value #1"

if [3 * x for x in nums] != [3, 6, 9, 12, 15]:
   raise support.TestError, "Wrong value #2"

if [x for x in nums if x > 2] != [3, 4, 5]:
   raise support.TestError, "Wrong value #3"

if len([(i, s) for i in nums for s in strs]) != len(nums) * len(strs):
   raise support.TestError, "Wrong value #4"

if [(i, s) for i in nums for s in [f for f in strs if "n" in f]] != [
            (1, 'Banana'), (1, 'Coconut'), (2, 'Banana'), (2, 'Coconut'), 
            (3, 'Banana'), (3, 'Coconut'), (4, 'Banana'), (4, 'Coconut'), 
            (5, 'Banana'), (5, 'Coconut')]:
   raise support.TestError, "Wrong value #5"


try:
    eval("[i, s for i in nums for s in strs]")
    print "FAIL: should have raised a SyntaxError!"
except SyntaxError:
    pass

try:
    eval("[x if y]")
    print "FAIL: should have raised a SyntaxError!"
except SyntaxError:
    pass

suppliers = [
  (1, "Boeing"),
  (2, "Ford"),
  (3, "Macdonalds")
]

parts = [
  (10, "Airliner"),
  (20, "Engine"),
  (30, "Cheeseburger")
]

suppart = [
  (1, 10), (1, 20), (2, 20), (3, 30)
]

l = [
  (sname, pname)
    for (sno, sname) in suppliers
      for (pno, pname) in parts
        for (sp_sno, sp_pno) in suppart
          if sno == sp_sno and pno == sp_pno
]


if l != [('Boeing', 'Airliner'), ('Boeing', 'Engine'), 
         ('Ford', 'Engine'), ('Macdonalds', 'Cheeseburger')]:
   raise support.TestError, "Wrong value #6"
