"""

"""

import support

f = open("test207.out", "w")
f.write(u"\u20ac")
f.close()

f = open("test207.out", "r")
if ord(f.read()) != 0x20AC:
   raise support.TestError("EURO sign didnøt survive a text file")
f.close()



f = open("test207.out", "wb")
f.write(u"\u20ac")
f.close()

f = open("test207.out", "rb")
if ord(f.read()) != 0xAC:
   raise support.TestError("EURO sign should have been truncated.")
f.close()


#raise support.TestError("" + `x`)
