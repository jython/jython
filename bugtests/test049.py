"""
Playing games with a package's __name__ can cause a NullPointerException.
"""

import support

import test049p
del test049p.__name__
hasattr(test049p, 'missing')


