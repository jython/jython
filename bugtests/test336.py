"""
[ #451552 ] case insensitivity on import causes prob
"""

import support
import java

support.compileJava("classes/test336p/Data.java")
support.compileJava("classes/test336p/data/MyData.java")

try:
    from test336p.data import MyData
except java.lang.NoClassDefFoundError:
    raise support.TestWarning("Should not fail")
