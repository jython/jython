"""
Check sane error when importing TKinter.
"""

import support


import sys
sys.path.append(r"i:\Python-1.5.2\Lib\lib-tk")

try:
    from Tkinter import *
except ImportError, e:
    support.compare(e, "_tkinter|Tkinter")
