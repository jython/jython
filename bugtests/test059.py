"""
JPython proxies not properly returned back from Java code
"""

import support


import java

class MyVector(java.util.Vector):
    bar = 99

ht = java.util.Hashtable()
mv = MyVector()
ht.put("a", mv)

mv = ht.get("a")


