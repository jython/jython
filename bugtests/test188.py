"""

"""

import support

support.compileJPythonc("test188c.py", output="test188.err")

if support.grep("jpywork/test188c.java", "extends .*ListCellRenderer", count=1) != 1:
    raise support.TestWarning("test188c should extends ListCellRenderer (but properly never will)")

