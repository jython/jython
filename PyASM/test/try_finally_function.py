def tff(x):
    try:
        if x == 0:
            return "zero"
        elif x == 1:
            return "one"
        elif x == 3:
            raise TypeError("works")
        return "dunno"
    finally:
        print "done"

print tff(0)
print tff(1)
print tff(-1)
print tff(2)
print tff(3)
