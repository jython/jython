"""
Check simple __import__ call with 4 args.
"""

import support

mod = __import__("pawt", globals(), locals(), "swing")

import pawt

if pawt != mod:
    raise support.TestError("__import__ returned wrong module")

