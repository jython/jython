"""

"""

import support

src = """
package com;
public class Blob {
    int value = %d;
}
"""

def makeBlob(value):
    f = open("test273p/com/Blob.java", "w")
    f.write(src % value);
    f.close();

    support.compileJava(r"test273p/com/Blob.java")
    support.compileJava(r"test273p/com/BlobWriter.java", classpath="test273p")

makeBlob(1)

import jreload
XLS = jreload.makeLoadSet("XLS",['test273p'])

from XLS import com

v = com.BlobWriter.write(com.Blob())
support.compare(v, "1")

makeBlob(2)

jreload.reload(XLS)

v = com.BlobWriter.write(com.Blob())
support.compare(v, "2")
