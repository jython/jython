"""
[ #477608 ] os.path.getmtime() missing
"""

import support
import os

s = os.stat("test331.py")

if s[8] != os.path.getmtime("test331.py"):
    raise support.TestWarning("Modification time was wrong")

if s[7] != os.path.getatime("test331.py"):
    raise support.TestWarning("Access time was wrong")

