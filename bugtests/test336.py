"""
[ #451552 ] case insensitivity on import causes prob
"""

import support
import java

support.compileJava("classes/test336p/Data.java")
support.compileJava("classes/test336p/data/MyData.java")

from test336p.data import MyData
