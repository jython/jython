"""
Check a manually created PyModule.
"""

import support
import org, sys

s = """
def foo():
        pass
"""
name = "test127m"

mod = org.python.core.PyModule("test127m", {})
#mod = org.python.core.PyInstance.__tojava__(mod, org.python.core.PyModule)
                
code = compile(s, "test127m.py", "exec" )
exec s in mod.__dict__, mod.__dict__

sys.modules["test127m"] = mod
import test127m

test127m.foo()

