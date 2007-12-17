def test(x):
    try:
        res = 1/x
    except ArithmeticError, e:
        print "except-block"
        return "fail: %s" % e
    else:
        print "else-block"
        return "pass: %s" % res
    finally:
        print "done"

print test(1)
print test(0)
