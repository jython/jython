"""generate code for exceptions and for the types module"""

template1 = '        %(name)s = new PyString("%(name)s");'
template2 = '        dict.__setitem__("%(name)s", Py.%(name)s);'
template3 = '''\
        %(name)s = new PyTuple(new PyObject[]
            {%(values)s});
'''
template4 = '''\
        tmp = exceptions.__findattr__("%(name)s");
        if (tmp != null) %(name)s = tmp;'''
import exceptions, types, string

excs = {}
for name in dir(exceptions):
	c = getattr(exceptions, name)
	try:
		if issubclass(c, exceptions.Exception):
			excs[c] = {}
	except: 
		pass

for key, value in excs.items():
	for base in key.__bases__:
		excs[base][key] = 1
		

import sys
fp = open('c:\\jpython\\JavaCode\\org\\python\\core\\excs.txt', 'w')
sys.stdout = fp

for exc in excs.keys():
	print template4 % {'name': exc.__name__}
	
print 
print

for exc in excs.keys():
	print template2 % {'name': exc.__name__}

print
print
	
for exc, values in excs.items():
	if len(values) == 0:
		print template1 % {'name': exc.__name__}
		
for exc, values in excs.items():
	if len(values) != 0:
		vl = []
		for key in values.keys():
			vl.append('Py.'+key.__name__)
			
		print template3 % {'name': exc.__name__, 	'values':string.join(vl, ', ')}
		

print
print
sys.exit()
temp = """\
	public static PyObject %(name)s;
	public static PyException %(name)s(String message) {
	    return new PyException(Py.%(name)s, message);
	}
"""

for exc, values in excs.items():
	if len(values) == 0:
		print temp % {'name': exc.__name__}


print 
print


types = ['ArrayType', 'BuiltinFunctionType', 'BuiltinMethodType', 'ClassType', 'CodeType', 'ComplexType',
 'DictType', 'DictionaryType', 'EllipsisType', 'FileType', 'FloatType', 'FrameType',
  'FunctionType', 'InstanceType', 'IntType', 'LambdaType', 'ListType', 'LongType', 
  'MethodType', 'ModuleType', 'NoneType', 'SliceType', 'StringType', 
  'TracebackType', 'TupleType', 'TypeType', 'UnboundMethodType', 'XRangeType']

line = '\t\tdict.__setitem__("%(name)sType", PyJavaClass.lookup(Py%(name)s.class));'

for name in types:
	name = name[:-4]
	print line % {'name':name}

fp.close()
