def a(b,c):
    None

a(1,c=2)
a(*[1,2])
a(**dict(b=1,c=2))
a(*[1],**dict(c=2))
a(c=1,*[2])
a(b=1,**dict(c=2))
a(1,2,*[],**dict())
