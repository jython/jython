from org.python.compiler import JavaMaker, ProxyMaker

pkg = 'jjh'
pyclass = 'PythonRandom'
module = 'trand'
jname = 'java.util.Random'

name = 'org.python.proxies.'+jname
ps = ProxyMaker.getFile('c:\\jpython\\JavaCode', name)
ProxyMaker.makeProxy(jname, ps)
ps.close()



name = pkg+'.'+pyclass
jm = JavaMaker('java.util.Random', pyclass, module, name)
jm.build()

s = ProxyMaker.getFile('c:\\jpython\\JavaCode', name)
jm.classfile.write(s)
s.close()