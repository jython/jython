def foo(x,y,z):  #GOOD
 a=1;

def foo(x,y,z,): #GOOD trailing comma allowed
 a=1;

def bar(*baz):   # STILL GOOD
 a=1;
 foo(3,4,5,)

