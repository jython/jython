def a1():
    raise TypeError("probe")

def a2():
    a1()

def a3():
    a2()

a3()
