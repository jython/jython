"""
Create a new Module.
"""

import support

from org.python.core import PyModule, PyInstance
test = PyModule("test", {})
exec "a = 2" in test.__dict__
support.compare(len(test.__dict__), "3")

#test = PyInstance.__tojava__(test, PyModule)
exec "b = 3" in test.__dict__
support.compare(len(test.__dict__), "4")
