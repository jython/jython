
import re, exceptions, thread, os, shutil

class TestError(exceptions.Exception):
    def __init__(self, args):
	exceptions.Exception.__init__(self, args)

class TestWarning(exceptions.Exception):
    def __init__(self, args):
	exceptions.Exception.__init__(self, args)


def compare(s, pattern):
    m = re.search(pattern, str(s))
    if m is None:
	raise TestError("string compare error\n   " + str(s) + "\n   " + pattern)



def execCmd(cmd):
    #print cmd
    import java
    r = java.lang.Runtime.getRuntime()
    e = getattr(r, "exec")
    p = e(cmd)
    return p


def compileJava(src, **kw):
    cmd = "javac "
    if kw.has_key("classpath"):
	cmd = cmd + "-classpath %s;%%CLASSPATH%% " % kw['classpath']
    src = src.replace('/', '\\');
    p = execCmd('cmd /C "%s %s"' % (cmd, src))

    import java
    if kw.has_key("output"):
	outstream = java.io.FileOutputStream(kw['output'])
    else:
	outstream = java.lang.System.out

    import java
    thread.start_new_thread(StreamReader, (p.inputStream, outstream))
    thread.start_new_thread(StreamReader, (p.errorStream, outstream))
    ret = p.waitFor()
    if ret != 0:
	raise TestError, "%s failed with %d" % (cmd, ret)




def runJava(cls, **kw):
    cmd = "java "
    if kw.has_key("classpath"):
	cmd = cmd + "-classpath %s;%%CLASSPATH%% " % kw['classpath']
    if kw.has_key("cp"):
	cmd = cmd + "-classpath %s" % kw['cp']
    p = execCmd('cmd /C "%s %s"' % (cmd, cls))

    import java
    if kw.has_key("output"):
	outstream = java.io.FileOutputStream(kw['output'])
    else:
	outstream = java.lang.System.out
    if kw.has_key("error"):
	errstream = java.io.FileOutputStream(kw['error'])
    else:
	errstream = java.lang.System.out

    import java
    thread.start_new_thread(StreamReader, (p.inputStream, outstream))
    thread.start_new_thread(StreamReader, (p.errorStream, errstream))
    ret = p.waitFor()
    if ret != 0 and not kw.has_key("expectError"):
	raise TestError, "%s failed with %d" % (cmd, ret)
    return ret


def runJython(cls, **kw):
    cmd = "jython "
    p = execCmd('cmd /C "%s %s"' % (cmd, cls))

    import java
    if kw.has_key("output"):
	outstream = java.io.FileOutputStream(kw['output'])
    else:
	outstream = java.lang.System.out
    if kw.has_key("error"):
	errstream = java.io.FileOutputStream(kw['error'])
    else:
	errstream = java.lang.System.out

    import java
    thread.start_new_thread(StreamReader, (p.inputStream, outstream))
    thread.start_new_thread(StreamReader, (p.errorStream, errstream))
    ret = p.waitFor()
    if ret != 0 and not kw.has_key("expectError"):
	raise TestError, "%s failed with %d" % (cmd, ret)
    return ret


def StreamReader(instream, outstream):
    while 1:
        ch = instream.read()
	if ch == -1: break
	outstream.write(ch);

def compileJPythonc(*files, **kw):

    if os.path.isdir("jpywork") and not kw.has_key("keep"):
         shutil.rmtree("jpywork", 1) 

    import java
    if kw.has_key("output"):
	outstream = java.io.FileOutputStream(kw['output'])
    else:
	outstream = java.lang.System.out

    cmd = "jythonc "
    if kw.has_key("core"):
	cmd = cmd + "--core "
    if kw.has_key("deep"):
	cmd = cmd + "--deep "
    if kw.has_key("all"):
	cmd = cmd + "--all "
    if kw.has_key("package"):
	cmd = cmd + "--package %s " % kw['package']
    if kw.has_key("addpackages"):
	cmd = cmd + "--addpackages %s " % kw['addpackages']
    if kw.has_key("jar"):
	cmd = cmd + "--jar %s " % kw['jar']
	if os.path.isfile(kw['jar']):
	    os.remove(kw['jar'])
    cmd = cmd + " ".join(files)

    p = execCmd('cmd /C "%s"' % cmd)
    import java
    thread.start_new_thread(StreamReader, (p.inputStream, outstream))
    thread.start_new_thread(StreamReader, (p.errorStream, outstream))
    ret = p.waitFor()
    if ret != 0 and not kw.has_key("expectError"):
	raise TestError, "%s failed with %d" % (cmd, ret)
    return ret


def grep(file, text, count=0):
    f = open(file, "r")
    lines = f.readlines()
    f.close()

    result = []
    for line in lines:
        if re.search(text, line):
	     result.append(line)

    if count:
        return len(result)
    return result
