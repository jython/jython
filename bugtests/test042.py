"""
sys.exit can only be called with an integer argument
"""

import support

import sys
try:
    sys.exit("leaving now")
except SystemExit, e:
    support.compare(e, "leaving now")
