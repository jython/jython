
try:
  from java.util.regex import Pattern
  p = Pattern.compile("xxx")
  m = p.split("ABCDEFG")
except ImportError, e:
  import support
  raise support.TestWarning("JVM version >= 1.4 needed to test PyString -> CharSequence")

