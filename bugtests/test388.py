'''
This checks that Python imports precedes the loading of Java directories from the
classpath.

Reported in bug 1421812.
'''
import support

import test388m

if not hasattr(test388m, 'x'):
    raise support.TestError, 'Python modules should be imported before directories for Java'


