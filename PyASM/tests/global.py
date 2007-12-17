g = 5
def f():
    global g
    g = 7

print g
f()
print g
