"""
[ #488632 ] -c sys.argv diff
"""

import support

support.runJython(
    """-c "import sys; assert sys.argv == ['-c', '-v', 'args']" -v args""")

