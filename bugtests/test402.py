'''

test402m adds a jar file to sys.path and imports a package from it.  The first
run ensures that, by default, package scanning is enabled for jars added to
sys.path.  The second run turns off package scanning, so it checks that the
package is unimportable without the scan.  Finally, we run test402n which adds
the same jar to its sys.path and imports a fully qualified class from it.  We
run it with package scanning off to make sure that even without package
scanning, jars are correctly added to sys.path and fully qualified class
imports work on them.

'''

import support
import jarmaker

jarmaker.mkjar()

support.runJython('test402m.py')
ret = support.runJython('test402m.py', error='test402.err',
        javaargs='-Dpython.cachedir.skip=true', expectError=1)
if ret == 0:
    raise support.TestError('Successfully imported a package from a jar on sys.path without caching!')
support.runJython('test402n.py', javaargs='-Dpython.cachedir.skip=true')
