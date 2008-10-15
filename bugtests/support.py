import sys
is_jython = sys.platform[:4] == "java"

import re, exceptions, thread, os, shutil
import support_config as cfg

if is_jython:
    import jarray
    from java.io import FileInputStream
    from java.io import FileOutputStream
    from java.util.jar import JarEntry
    from java.util.jar import JarFile
    from java.util.jar import JarInputStream
    from java.util.jar import JarOutputStream
    from java.util.jar import Manifest

UNIX = os.pathsep == ":"
WIN  = os.pathsep == ";"
test_jythonc = 1

if not UNIX ^ WIN:
  raise TestError("Unknown platform")

class TestError(exceptions.Exception):
  def __init__(self, args):
    exceptions.Exception.__init__(self, args)

class TestWarning(exceptions.Exception):
  def __init__(self, args):
    exceptions.Exception.__init__(self, args)

class TestSkip(exceptions.Exception):
  def __init__(self, args):
    exceptions.Exception.__init__(self, args)

def compare(s, pattern):
  m = re.search(pattern, str(s))
  if m is None:
    raise TestError("string compare error\n   '" + str(s) + "'\n   '" + pattern + "'")

def StreamReader(instream, outstream):
  while 1:
    ch = instream.read()
    if ch == -1: break
    outstream.write(ch)

def execCmd(cmd, kw):
  __doc__ = """execute a command, and wait for its results
returns 0 if everything was ok
raises a TestError if the command did not end normally"""
  if kw.has_key("verbose") and kw["verbose"]:
    print cmd
  import java
  r = java.lang.Runtime.getRuntime()
  e = getattr(r, "exec")
  p = e(cmd)

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

def compileJava(src, **kw):
  classfile = src.replace('.java', '.class')
  if not 'force' in kw and os.path.exists(classfile) and os.stat(src).st_mtime < os.stat(classfile).st_mtime:
    return 0
  classpath = cfg.classpath
  if "classpath" in kw:
    classpath = os.pathsep.join([cfg.classpath, kw["classpath"]])
  if UNIX:
    cmd = "%s/bin/javac -classpath %s %s" % (cfg.java_home, classpath, src)
  elif WIN:
    src = src.replace("/", "\\")
    cmd = 'cmd /C "%s/bin/javac.exe -classpath %s %s"' % (cfg.java_home, classpath, src)
  return execCmd(cmd, kw)

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
  return execCmd(cmd, kw)

def runJavaJar(jar, *args, **kw):
  argString = " ".join(args)
  if UNIX:
    cmd = ['/bin/sh', '-c', "%s/bin/java -jar %s %s" % (cfg.java_home, jar, argString)]
  elif WIN:
    cmd = 'cmd /C "%s/bin/java.exe -jar %s %s"' % (cfg.java_home, jar, argString)
  return execCmd(cmd, kw)

def runJython(cls, **kw):
  javaargs = ''
  if 'javaargs' in kw:
      javaargs = kw['javaargs']
  classpath = cfg.classpath
  if "classpath" in kw:
    classpath = os.pathsep.join([cfg.classpath, kw["classpath"]])
  if UNIX:
    cmd = "%s/bin/java -classpath %s %s -Dpython.home=%s org.python.util.jython %s" % (cfg.java_home, classpath, javaargs, cfg.jython_home, cls)
  elif WIN:
    cmd = 'cmd /C "%s/bin/java.exe -classpath %s %s -Dpython.home=%s org.python.util.jython %s"' % (cfg.java_home, classpath, javaargs, cfg.jython_home, cls)
  return execCmd(cmd, kw)

def compileJPythonc(*files, **kw):
  if not test_jythonc:
     raise TestSkip('Skipping pythonc')
  if os.path.isdir("jpywork") and not kw.has_key("keep"):
     shutil.rmtree("jpywork", 1) 

  cmd = "-i "
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
  return execCmd(cmd, kw)

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

class JarPacker:
  __doc__ = """helper class to pack stuff into a jar file -
  the terms 'file' and 'dir' mean java.io.File here """
  
  def __init__(self, jarFile, bufsize=1024):
    self._jarFile = jarFile
    self._bufsize = bufsize
    self._manifest = None
    self._jarOutputStream = None

  def close(self):
    self.getJarOutputStream().close()

  def addManifestFile(self, manifestFile):
    __doc__ = """only one manifest file can be added"""
    self.addManifest(Manifest(FileInputStream(manifestFile)))
    
  def addManifest(self, manifest):
    if not self._manifest:
      self._manifest = manifest
    
  def addFile(self, file, parentDirName=None):
    buffer = jarray.zeros(self._bufsize, 'b')
    inputStream = FileInputStream(file)
    jarEntryName = file.getName()
    if parentDirName:
      jarEntryName = parentDirName + "/" + jarEntryName
    self.getJarOutputStream().putNextEntry(JarEntry(jarEntryName))
    read = inputStream.read(buffer)
    while read <> -1:
        self.getJarOutputStream().write(buffer, 0, read)
        read = inputStream.read(buffer)
    self.getJarOutputStream().closeEntry()
    inputStream.close()
    
  def addDirectory(self, dir, parentDirName=None):
    if not dir.isDirectory():
      return
    filesInDir = dir.listFiles()
    for currentFile in filesInDir:
      if currentFile.isFile():
        if parentDirName:
          self.addFile(currentFile, parentDirName + "/" + dir.getName())
        else:
          self.addFile(currentFile, dir.getName())
      else:
        if parentDirName:
          newParentDirName = parentDirName + "/" + dir.getName()
        else:
          newParentDirName = dir.getName()
        self.addDirectory(currentFile, newParentDirName)
        
  def addJarFile(self, jarFile):
    __doc__ = """if you want to add a .jar file with a MANIFEST, add it first"""
    jarJarFile = JarFile(jarFile)
    self.addManifest(jarJarFile.getManifest())
    jarJarFile.close()

    jarInputStream = JarInputStream(FileInputStream(jarFile))
    jarEntry = jarInputStream.getNextJarEntry()
    while jarEntry:
      self.getJarOutputStream().putNextEntry(jarEntry)
      buffer = jarray.zeros(self._bufsize, 'b')
      read = jarInputStream.read(buffer)
      while read <> -1:
        self.getJarOutputStream().write(buffer, 0, read)
        read = jarInputStream.read(buffer)
      self.getJarOutputStream().closeEntry()
      jarEntry = jarInputStream.getNextJarEntry()

  def getJarOutputStream(self):
    if not self._jarOutputStream:
      if self._manifest:
        self._jarOutputStream = JarOutputStream(FileOutputStream(self._jarFile), self._manifest)
      else:
        self._jarOutputStream = JarOutputStream(FileOutputStream(self._jarFile))
    return self._jarOutputStream
