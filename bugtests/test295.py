"""
[ #437809 ] traceback error
"""

import support

import traceback 
try:
    traceback.extract_stack() 
except AttributeError, msg:
    raise support.TestWarning(msg)


