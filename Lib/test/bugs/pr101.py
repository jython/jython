# test of reload -- PR#101
#
# javac must be on your $PATH

from java.lang import Runtime
rt = Runtime.getRuntime()

def makejavaclass(s):
    fp = open('pr101j.java', 'w')
    fp.write('''
// Java side of the PR#101 test -- reload of a Java class
public class pr101j {
    public static String doit() {
        return "%s";
    }
}
''' % s)
    fp.close()
    proc = rt.exec("javac pr101j.java")
    status = proc.waitFor()
    if status <> 0:
        raise RuntimeError, 'javac process failed'

try:
    makejavaclass("first")
    import pr101j
    ret = pr101j.doit()
    if ret <> 'first':
        print 'unexpected first doit() result:', ret

    makejavaclass("second")
    pr101j = reload(pr101j)
    ret = pr101j.doit()
    if ret <> 'second':
        print 'unexpected second doit() result:', ret
finally:
    import os
    os.unlink('pr101j.java')
    os.unlink('pr101j.class')
