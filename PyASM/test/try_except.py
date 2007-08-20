def test(x):
    try:
        res = 1/x
    except ArithmeticError, e:
        return "fail: %s" % e
    else:
        return "pass: %s" % res

print test(1)
print test(0)
