def dec(fun):
    def ret(*arg, **kwarg):
        return fun(*arg, **kwarg)
    return ret

@dec
def a(b,c):
    return c

print a(1,2)
