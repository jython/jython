"""
[ #416871 ] proxy fails to have all needed methods
"""

import support

support.compileJava('test294j.java')

import test294j


class PyTestA(test294j):
	def __init__(self):
		output.append( "inited")
	def doStart(self):
		output.append( "started")
	def doEnd(self):
		output.append( "completed")
	def finalize(self):
		pass
	def clone(self):
		return self.__class__()

class PyTestB(test294j):
	def __init__(self):
		output.append( "inited")
	def doStart(self):
		output.append( "started")
	def doEnd(self):
		output.append( "completed")

output = []

a = PyTestA()
a.doStart()
aa = a.clone()
aa.doStart()

assert output == ['inited', 'started', 'inited', 'started']

output = []

a = PyTestB()
a.doStart()
aa = a.clone()
aa.doStart()

assert output == ['inited', 'started', 'started']
