"""
Check name of the pawt.swing package.
"""

import support

from pawt import swing


support.compare(swing, "java package")
support.compare(swing.__name__, "javax.swing")
support.compare(swing.__jpythonc_name__, "pawt.swing")
support.compare(swing.__file__, r"Lib\\pawt\\swing.py")

