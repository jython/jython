def dec(fun):
    def ret(*arg, **kwarg):
        print "%s(%s)" % (fun.func_name, ", ".join(
                [str(x) for x in arg] + ["%s=%s"%x for x in kwarg.items()]))
        return fun(*arg, **kwarg)
    return ret

@dec
def a(b,c):
    return c

print a(1,2)
