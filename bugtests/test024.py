"""
Check strange indexes/slices.
"""

# This is no a error
"""
import support
import test024j

t = test024j()


s = str(t)
r = repr(t)
i = t[43]

support.compare(s, "A __str__ string")
support.compare(r, "A __repr__ string")
support.compare(i, "A __getitem__ string")
"""