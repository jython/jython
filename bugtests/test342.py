"""
__import__(_) does an unwanted relative search 
"""

import support

from test342p import doimp

support.compare(doimp.kind,"absolute")
