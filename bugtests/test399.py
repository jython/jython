'''
Confirms the correct bevahior of Python int to Java byte coercion.  Even though
a Python int can be expressed in a byte like format such as 0xFF, Java bytes
only range from -128 to 127 so any int greater than 0x80 should throw a
TypeError if it's passed to something expecting a Java byte.  
'''
import support

support.compileJava("classes/test399j.java")

import test399j

test399j.takesByteArray([0x7E, 0x12])
try:
    test399j.takesByteArray([0xFF, 0x00])
    raise support.TestError('0xFF should not be acceptable as a Java byte')
except TypeError:
    pass#expected since 0xFF can't be coerced to a Java byte
