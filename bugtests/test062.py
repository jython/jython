"""
'is' operator doesn't always work right with Java instances
"""

import support


from java.awt import Color
red = Color.red
if red is not red:
    raise support.TestError("xcxx")
if red is not Color.red:
    raise support.TestError("xcxx")


