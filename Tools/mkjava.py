from org.python.compiler import JavaMaker, ProxyMaker
import sys, types

def make(name, directory, jclass, pyclass, module, properties={}):
	ps = ProxyMaker.getFile(directory, 'org.python.proxies.'+jclass)
	ProxyMaker.makeProxy(jclass, ps)
	ps.close()
		
	props = []
	for key, value in properties.items():
		props.append(key)
		props.append(value)
	
	jm = JavaMaker(jclass, pyclass, module, name, props)
	jm.build()
	s = ProxyMaker.getFile(directory, name)
	jm.classfile.write(s)
	s.close()
	
def compile(module, directory):
	pass
	
if __name__ == '__main__':
	if len(sys.argv) != 4:
		print 'usage: jpython mkjava.py java_name python_name directory'
		sys.exit(-1)
	
	directory = sys.argv[-1]
	java_name = sys.argv[1]
	python_name = sys.argv[2]
	
	module = __import__(python_name)
	c = getattr(module, python_name)
	java_class = None
	for base in c.__bases__:
		if type(base) == types.JavaClassType:
			java_class = base.__name__
		
	print java_class
	prefix = 'org.python.proxies.'
	if java_class[:len(prefix)] == prefix:
		java_class = java_class[len(prefix):]
	print java_class
	
	props = {}
	if sys.registry.getProperty('python.registry') != None:
		props['python.registry'] = sys.registry.getProperty('python.registry')

	make(java_name, directory, java_class, python_name, python_name, props)
