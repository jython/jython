"""
Test for [ 551888 ] Opening utf-8 files with codecs fails
"""

import support

f = open("test361.out", "w")
f.write("hello")
f.close()

import codecs
f = codecs.open("test361.out", "r", "utf-8")
print f.read()
f.close()

