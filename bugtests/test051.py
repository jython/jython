"""
Trying to assign to a method of a Java instance throws a NullPointerException
"""

import support


from java.awt import Button
b = Button()

try:
    b.setLabel = 4
except TypeError, e:
    support.compare(e, "can't assign to this attribute in java instance: setLabel")

