
import sys, string, traceback, getopt, support, os

failures = {}

def reportError():
    print n , "Failed!"
    if m and hasattr(m, "__doc__"):
       print  m.__doc__.strip()
    print "   ", sys.exc_info()[0]
    print "   ", sys.exc_info()[1]
    failures[n] = 1
    traceback.print_tb(sys.exc_info()[2])


opts, argv = getopt.getopt(sys.argv[1:], 'w')
warnings = "-w" in opts

for i in range(350):
    m = None
    n = "test%3.3d" % i
    try:
	stdout = sys.stdout
        if os.path.isfile(n + ".py"):
	    m = __import__(n)
	    sys.stdout = stdout
	    print n, "OK!"
	else:
	    print n, "Skipped"
#    except ImportError, e:
#	sys.stdout = stdout
#	if string.lower(str(e)[:20]) == "no module named test":
#	    break
#	print n, str(e)
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
sys.exit(1)
