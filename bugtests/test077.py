"""
Basic test of exec
"""

import support

exec "a = 'spam'" in globals()

support.compare(a, "spam")


