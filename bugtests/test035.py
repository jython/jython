"""
Check sys.platform name
"""

import support
import sys

n = sys.platform

if n[:4] != "java":
    raise support.TestError("sys.platform name check failed: " + `n`)
