
def f1():
     l = [x for x in "abcdef"]
     assert str(l) == "['a', 'b', 'c', 'd', 'e', 'f']"
     
qs = "x.html&a=1&b=2;c=3"

def f2():
    pairs = [s2 for s1 in qs.split('&') for s2 in s1.split(';')]
    assert str(pairs) == "['x.html', 'a=1', 'b=2', 'c=3']"

f1()
f2()

