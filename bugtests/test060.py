"""
break in while/else clause incorrectly assigned to inner loop
"""

import support

while 1:
    i = 0
    while i<10:
        i = i+1
    else:
        break

