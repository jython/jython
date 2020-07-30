# Copyright � Corporation for National Research Initiatives
import sys, os, java, jarray

# This file is retained only for backward compatibility.
#
# This supports the jython -jar <zip-or-jar-file> command, under a mechanism by which
# This file must be copied into the root of a ZIP/JAR file system.
# See org/python/util/jython.java for an explanation and alternatives.

def fixname(s):
    result = []
    lasti = 0
    for i in range(len(s)):
        if s[i] == '/':
            result.append(s[lasti:i])
            lasti = i+1
    result.append(s[lasti:])
    return apply(os.path.join, tuple(result))


# The global `zipfile' is magically inserted when processing jython -jar,
entries = zipfile.entries()  # noqa: F821

outdir = sys.prefix #os.path.join(sys.prefix, 'Lib')

print 'Installing to:', outdir

buffer = jarray.zeros(1024, 'b')

testdir = os.path.join(outdir, 'Lib', 'test')
if not os.path.exists(testdir):
    os.mkdir(testdir)

while entries.hasMoreElements():
    entry = entries.nextElement()

    if entry.isDirectory():
        continue

    infile = zipfile.getInputStream(entry)  # noqa: F821

    name = fixname(entry.getName())

    if name == '__run__.py':
        continue

    outname = os.path.join(outdir, name)

    outfile = java.io.FileOutputStream(outname)

##    print entry.getSize(), entry.getMethod(), entry.DEFLATED

##    if entry.getMethod() == entry.DEFLATED:
##        infile = java.util.zip.GZIPInputStream(infile)

    bytes = entry.getSize() #infile.available()

    print 'copying %s to %s (%d bytes)' % (name, outname, bytes)

    while 1:
        n = infile.read(buffer)
        if n == -1:
            break
        outfile.write(buffer, 0, n)

    infile.close()
    outfile.close()


print 'Now precompiling library...'
print

import sys, os
import compileall

lib = os.path.join(sys.prefix, 'Lib')
compileall.compile_dir(lib)

print
print 'Installation of Python libraries is complete'
