"""
[ #448398 ] open('test.txt','w').write('test') fails
"""

import support

support.runJython("test322m.py")

import os

l = os.stat("test322.out")[6]

if l != 7:
    raise support.TestWarning('The file should have been auto flushed')
