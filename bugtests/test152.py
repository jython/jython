"""

"""

import support

import tokenize

f = open("test152.out", "w")
f.write("for i in range(1,10):")
f.close()

f = open("test152.out")

s = ""
def gettoken(type, token, (srow, scol), (erow, ecol), line): # for testing
    global s
    s = s + " " + token

r = tokenize.tokenize(f.readline, gettoken)

support.compare(s, "for")

#raise support.TestError("Should raise")
