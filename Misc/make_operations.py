"""Generate most of the code for the operations module"""

import string

binops = [
  ('add', ('__add__', 'add', '__concat__', 'concat')),
  ('and', ('__and__', 'and_')),
  ('div', ('__div__', 'div')),
  ('lshift', ('__lshift__', 'lshift')),
  ('mod', ('__mod__', 'mod')),
  ('mul', ('__mul__', 'mul', '__repeat__', 'repeat')),
  ('or', ('__or__', 'or_')),
  ('rshift', ('__rshift__', 'rshift')),
  ('sub', ('__sub__', 'sub')),
  ('xor', ('__xor__', 'xor')),
]

unops = [
  ('abs', ('__abs__', 'abs')),
  ('invert', ('__inv__', 'inv')),
  ('neg', ('__neg__', 'neg')),
  ('not', ('__not__', 'not_')),
  ('pos', ('__pos__', 'pos')),
]

tests = [
  ('__nonzero__', ('truth',)),
  ('isCallable', ('isCallable',)),
  ('isMappingType', ('isMappingType',)),
  ('isNumberType', ('isNumberType',)),
  ('isSequenceType', ('isSequenceType',)),
]

voidTemplate = "arg1.__%(name)s__(%(args)s); return Py.None;"
seqTemplate = "return arg1.__%(name)s__(%(args)s);"

seq = [  
  ('contains', 1, ('contains', 'sequenceIncludes'), 
      "return Py.newBoolean(arg1.__%(name)s__(%(args)s));"),
  ('delitem', 1, ('__delitem__', 'delitem'), voidTemplate),
  ('delslice', 2, ('__delslice__', 'delslice'), voidTemplate),
  ('getitem', 1, ('__getitem__', 'getitem'), seqTemplate),
  ('getslice', 2, ('__getslice__', 'getslice'), seqTemplate),
  ('setitem', 2, ('__setitem__', 'setitem'), voidTemplate),
  ('setslice', 3, ('__setslice__', 'setslice'), voidTemplate),
]

special = [
  ('countOf', 2, ('countOf',)),
  ('indexOf', 2, ('indexOf',)),
]

init = []
init_template = '        dict.__setitem__("%(name)s", new OperatorFunctions().init("%(name)s", %(id)d, %(nargs)s));'

functions = [None, [], [], [], []]
funcid = 0
func_template = "            case %(id)d: %(text)s"
def make_func(nargs, text):
	global funcid
	functions[nargs].append(func_template % {'id':funcid, 'text':text})
	funcid = funcid + 1
	return funcid-1

binops_template = "return arg1._%(name)s(arg2);"
for sname, names in binops:
	id = make_func(2, binops_template % {'name':sname})
	for name in names:
		init.append(init_template % {'id':id, 'nargs':2, 'name':name})
		
unops_template = "return arg1.__%(name)s__();"
for sname, names in unops:
	id = make_func(1, unops_template % {'name':sname})
	for name in names:
		init.append(init_template % {'id':id, 'nargs':1, 'name':name})
		
tests_template = "return Py.newBoolean(arg1.%(name)s());"
for sname, names in tests:
	id = make_func(1, tests_template % {'name':sname})
	for name in names:
		init.append(init_template % {'id':id, 'nargs':1, 'name':name})
		

for sname, nargs, names, seq_template in seq:
	nargs = nargs+1
	args = map(lambda x: "arg%s" % x, range(2,nargs+1))
	print args
	args = string.join(args, ", ")
	print args
	id = make_func(nargs, seq_template % {'name':sname, 'args':args})
	for name in names:
		init.append(init_template % {'id':id, 'nargs':nargs, 'name':name})

bifunc_template = """\
    public PyObject __call__(%(args)s) {
        switch(index) {
%(body)s        
            default:
                throw argCountError(%(nargs)s);
        }
    }
"""

fp = open("c:\\jpython\\src\\org\\python\\modules\\operations.txt", "w")

for nargs in range(1,5):
	args = map(lambda x: "PyObject arg%d" % x, range(1,nargs+1))
	args = string.join(args, ", ")
	
	body = string.join(functions[nargs], "\n")
	fp.write(bifunc_template % {'args':args, 'body':body, 'nargs':nargs})
	fp.write("\n")

fp.write(string.join(init, "\n"))
fp.close()
