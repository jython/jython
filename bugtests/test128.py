"""
Check a manually created PyModule, with unwanted hack.
"""

import support
import org, sys

s = """
def foo():
        return "foo bar zot"
"""
name = "test128m"

mod = org.python.core.PyModule("test128m", {})
#mod = org.python.core.PyInstance.__tojava__(mod, org.python.core.PyModule)
                
code = compile(s, "test128m.py", "exec" )
exec s in mod.__dict__, mod.__dict__

sys.modules["test128m"] = mod
import test128m

support.compare(test128m.foo(), "foo bar zot")
