
import re, exceptions, thread, os, shutil

import support_config as cfg

UNIX = os.pathsep == ":"
WIN  = os.pathsep == ";"

if not UNIX ^ WIN:
  raise TestError("Unknown platform")

class TestError(exceptions.Exception):
  def __init__(self, args):
    exceptions.Exception.__init__(self, args)

class TestWarning(exceptions.Exception):
  def __init__(self, args):
    exceptions.Exception.__init__(self, args)

def compare(s, pattern):
  m = re.search(pattern, str(s))
  if m is None:
    raise TestError("string compare error\n   '" + str(s) + "'\n   '" + pattern + "'")

def execCmd(cmd):
  print cmd
  import java
  r = java.lang.Runtime.getRuntime()
  e = getattr(r, "exec")
  p = e(cmd)
  return p

def compileJava(src, **kw):

  classpath = cfg.classpath
  if "classpath" in kw:
    classpath = os.pathsep.join([cfg.classpath, kw["classpath"]])
  if UNIX:
    cmd = "%s/bin/javac -classpath %s %s" % (cfg.java_home, classpath, src)
  elif WIN:
    src = src.replace("/", "\\")
    cmd = 'cmd /C "%s/bin/javac.exe -classpath %s %s"' % (cfg.java_home, classpath, src)
  p = execCmd(cmd)

  import java
  if kw.has_key("output"):
    outstream = java.io.FileOutputStream(kw['output'])
  else:
    outstream = java.lang.System.out

  thread.start_new_thread(StreamReader, (p.inputStream, outstream))
  thread.start_new_thread(StreamReader, (p.errorStream, outstream))
  ret = p.waitFor()
  if ret != 0:
    raise TestError, "%s failed with %d" % (cmd, ret)

def runJava(cls, **kw):

  classpath = cfg.classpath
  if "classpath" in kw:
    classpath = os.pathsep.join([cfg.classpath, kw["classpath"]])
  if kw.get('pass_jython_home', 0):
    defs = "-Dpython.home=%s" % cfg.jython_home
  else:
    defs = ''
  if UNIX:
    cmd = ['/bin/sh', '-c', "%s/bin/java -classpath %s %s %s" % (cfg.java_home, classpath, defs, cls)]
  elif WIN:
    cmd = 'cmd /C "%s/bin/java.exe -classpath %s %s %s"' % (cfg.java_home, classpath, defs, cls)
  p = execCmd(cmd)

  import java
  if kw.has_key("output"):
    outstream = java.io.FileOutputStream(kw['output'])
  else:
    outstream = java.lang.System.out
  if kw.has_key("error"):
    errstream = java.io.FileOutputStream(kw['error'])
  else:
    errstream = java.lang.System.out

  thread.start_new_thread(StreamReader, (p.inputStream, outstream))
  thread.start_new_thread(StreamReader, (p.errorStream, errstream))

  ret = p.waitFor()
  if ret != 0 and not kw.has_key("expectError"):
    raise TestError, "%s failed with %d" % (cmd, ret)

  return ret

def runJython(cls, **kw):

  classpath = cfg.classpath
  if "classpath" in kw:
    classpath = os.pathsep.join([cfg.classpath, kw["classpath"]])
  if UNIX:
    cmd = "%s/bin/java -classpath %s -Dpython.home=%s org.python.util.jython %s" % (cfg.java_home, classpath, cfg.jython_home, cls)
  elif WIN:
    cmd = 'cmd /C "%s/bin/java.exe -classpath %s -Dpython.home=%s org.python.util.jython %s"' % (cfg.java_home, classpath, cfg.jython_home, cls)
  p = execCmd(cmd)

  import java
  if kw.has_key("output"):
    outstream = java.io.FileOutputStream(kw['output'])
  else:
    outstream = java.lang.System.out
  if kw.has_key("error"):
    errstream = java.io.FileOutputStream(kw['error'])
  else:
    errstream = java.lang.System.out

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

  cmd = ""
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

  classpath = cfg.classpath
  if "classpath" in kw:
    classpath = os.pathsep.join([cfg.classpath, kw["classpath"]])
  
  jythonc = "%s/Tools/jythonc/jythonc.py %s" % (cfg.jython_home, cmd)
  if UNIX:
    cmd = "%s/bin/java -classpath %s -Dpython.home=%s org.python.util.jython %s" % (cfg.java_home, classpath, cfg.jython_home, jythonc)
  elif WIN:
    cmd = 'cmd /C "%s/bin/java.exe -classpath \"%s\" -Dpython.home=%s org.python.util.jython %s"' % (cfg.java_home, classpath, cfg.jython_home, jythonc)
  p = execCmd(cmd)

  import java
  if kw.has_key("output"):
    outstream = java.io.FileOutputStream(kw['output'])
  else:
    outstream = java.lang.System.out

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

