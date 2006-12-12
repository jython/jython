
import sys, string, traceback, getopt, support, os, glob

failures = {}


def runTests(seq):
  def reportError():
      print n , "Failed!"
      if m and hasattr(m, "__doc__"):
         print  m.__doc__.strip()
      print "   ", sys.exc_info()[0]
      print "   ", sys.exc_info()[1]
      failures[n] = 1
      traceback.print_tb(sys.exc_info()[2])
  for n in seq:
      m = None
      try:
          stdout = sys.stdout
          if os.path.isfile(n + ".py"):
              m = __import__(n)
              sys.stdout = stdout
              print n, "OK!"
          else:
              print n, "Skipped"
  #    except ImportError, e:
  #        sys.stdout = stdout
  #        if string.lower(str(e)[:20]) == "no module named test":
  #            break
  #        print n, str(e)
      except support.TestWarning:
          sys.stdout = stdout
          if warnings:
              reportError()
          else: 
              print n, "Ok"
      except:
          sys.stdout = stdout
          reportError()
  
  t = failures.keys()
  t.sort()
  print 
  print "%d tests failed" % len(t)
  print t

if __name__ == '__main__':
  opts, args = getopt.getopt(sys.argv[1:], 'w')
  warnings = ('-w',"") in opts

  if warnings: print "LOUD warnings"
  sys.path[:0] = ['classes']

  if len(args) > 0:
    tests = [int(test) for test in args[0].split(',')]
  else:
    testfiles = glob.glob('test???.py')
    testfiles.sort()
    lastTest = testfiles[-1]
    tests = range(int(lastTest[4:7]) + 1)# upper bound: last test + 1
  runTests(["test%3.3d" % i for i in tests])
  sys.exit(1)


