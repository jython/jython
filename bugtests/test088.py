"""
Check namespace of code running in a module __init__.py
"""

import support

import test088p

r = test088p.foobase.doit()
support.compare(r, "Done")
