import sys
def usage():
    print 'Usage: jython jython_checker.py <module name created by make_checker>'
    sys.exit(1)

if not len(sys.argv) == 2:
    usage()
checker_name = sys.argv[1].split('.')[0]#pop off the .py if needed
try:
    checker = __import__(checker_name)
except:
    print 'No module "%s" found' % checker_name
    usage()
    
import make_checker

ignored_types = ['frame',
		 'code',
		 'traceback']
checks = []
for check in checker.checks:
    index, expected_type, expected_bases, expected_dict = check
    if checker.names[index] in ignored_types:
	print 'Skipping', checker.names[index]
	continue
    checks.append(check)
    
    
ignored_members = ['__getattribute__', '__doc__']

ok, missing, bad_type, different = make_checker.do_check(checker.names, checks)

def strip_ignored(differences, key, ignored):
    if not key in differences:
	return
    problems = differences[key]
    for member in ignored_members:
	if member in problems:
	    problems.remove(member)
	    
for t, name, differences in different:
    strip_ignored(differences, 'missing', ignored_members)
    strip_ignored(differences, 'extras', ignored_members)

make_checker.report(ok, missing, bad_type, different)
