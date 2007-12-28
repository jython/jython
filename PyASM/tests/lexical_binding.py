def f(x, y, z):
    print x
    print y
    def g():
        print x
        print z
    g()

def g():
    x = 1
    lambda: x

f(3, 4, 5)

