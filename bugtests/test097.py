"""
Check properites of the exceptions module.
"""


import support

import exceptions

support.compare(exceptions.__doc__, "standard exception class hierarchy")
support.compare(exceptions.__name__, "exceptions")
#support.compare(exceptions.__file__, r"Lib\\exceptions.py")


