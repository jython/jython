"""
Check that UserDict can copy() itself.
"""

import support
import UserDict

u = UserDict.UserDict()
u["a"] = 1
x = u.copy()


