"""
Check calling class methods with an explicit self.
"""

import support
import java

m = java.lang.Class.getModifiers(java.awt.event.ActionEvent)

if m != java.lang.reflect.Modifier.PUBLIC:
    raise support.TestError('Wrong redirected stdout ' + `s`)
