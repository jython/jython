def dec(fun):
    def ret(x,y):
        return fun(y,x)
    return ret

@dec
def a(b,c):
    return c

print a(1,2)
