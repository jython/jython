
import support

import java.awt.geom
d = dir(java.awt.geom)
#print d
if "EllipseIterator" not in d:
    raise support.TestWarning("Non-public class should by visible when " +
                        "respectJava is false")
