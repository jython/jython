"""

"""

import support

def mkspam(a):
    f = open("test080m.py", "w")
    f.write("def Spam(): return '%s'" % a)
    f.close()

mkspam("foo")

import test080m
spam1 = test080m.Spam()
support.compare(spam1, "foo")

import time
time.sleep(2)

mkspam("bar")
reload(test080m)

spam2 = test080m.Spam()
support.compare(spam2, "bar")


