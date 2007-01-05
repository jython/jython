
import sys, string, traceback, getopt, support, os, glob

failures = {}
warnings = {}


def runTests(seq):
  def report(msg, errors_dict, loud=1):
    print n, msg
    errors_dict[n] = 1
    if loud:
      if m and hasattr(m, "__doc__"):
        print  m.__doc__.strip()
      print "   ", sys.exc_info()[0]
      print "   ", sys.exc_info()[1]
      traceback.print_tb(sys.exc_info()[2], file=sys.stdout)

  for n in seq:
      m = None
      try:
          stdout = sys.stdout
          if os.path.isfile(n + ".py"):
              m = __import__(n)
              sys.stdout = stdout
              print n, "OK"
          else:
              print n, "Skipped"
      except support.TestWarning:
          sys.stdout = stdout
          report("Warning", warnings, loud=loud_warnings)
      except:
          sys.stdout = stdout
          report("Failed", failures)
  
  summarize(failures, "failures")
  summarize(warnings, "warnings")

def summarize(errors_dict, description):
    t = errors_dict.keys()
    t.sort()
    print "%d %s" % (len(t), description)
    print t

if __name__ == '__main__':
  opts, args = getopt.getopt(sys.argv[1:], 'w')
  loud_warnings = ('-w',"") in opts

  if loud_warnings: 
      print "LOUD warnings"

  sys.path[:0] = ['classes']

  if len(args) > 0:
    tests = [int(test) for test in args[0].split(',')]
  else:
    testfiles = glob.glob('test???.py')
    testfiles.sort()
    lastTest = testfiles[-1]
    tests = range(int(lastTest[4:7]) + 1)# upper bound: last test + 1
  runTests(["test%3.3d" % i for i in tests])

  if len(failures) + len(warnings) > 0: 
    rc = 1
  else:
    rc = 0

  sys.exit(rc)

