# Test differentiating call/new/ref

def myfunc():
  pass

myfunc    # ref to func
myfunc()  # call to func

class MyClass():
  def mymethod(self):
    pass

MyClass        # ref to class
a = MyClass()  # instantiation
a.mymethod     # ref to method
a.mymethod()   # call to method
