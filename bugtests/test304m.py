
import support

import java.lang
d = dir(java.lang)
#print d
if "FDBigInt" not in d:
    raise support.TestWarning("Non-public class should by visible when " +
                        "respectJava is false")
