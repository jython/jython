"""
Check that module namespace contains dictionary methods.
"""

import support

import thread

thread.__dict__.copy()
thread.__dict__.keys()
thread.__dict__.values()
thread.__dict__.has_key(0)

thread.__dict__.items()
thread.__dict__.update({})
thread.__dict__.get(0, "abc")

thread.__dict__.copy().clear()

