def f((a,b), x, y=3, *args, **kwargs):
    return x * 2

a = f((1,2), 3,)
b = (1,2,3)[0]
c = [1,2,4]
d = {'a': 1, 'b': 2}
e = 1, 2
f = 1,
