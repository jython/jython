"""

"""

import support

import test162m
support.compare(test162m.ParentlessClass.__module__, "test162m")
support.compare(test162m.DerivedPyClass.__module__, "test162m")
support.compare(test162m.DerivedJClass.__module__, "test162m")

#raise support.TestError("" + `x`)
