"""generate vistor code for javacc grammar.  code goes in SimpleNode.java"""

import string

file = "c:\\jpython\\JavaCode\\org\\python\\parser\\PythonGrammarTreeConstants.java"

fp = open(file, "r")
lines = fp.readlines()
fp.close()

names = []

in_names = 0
for line in lines:
	if in_names:
		if "}" in line:
			in_names = 0
			continue
		name = string.split(line, '"')[1]
		#if name != 'void':
		names.append(name)
	else:
		if "[" in line:
			in_names = 1
		
print names	


print "\t\tswitch(id) {"

for i in range(len(names)):
	print "\t\tcase %d:\r" % i
	print "\t\t\treturn visitor.%s(this);\r" % names[i]
print "\t\t}\r"


for name in names:
	print "\tpublic Object %s(SimpleNode n) throws Exception {\r" % name
	print '\t\tthrow new ParseException("Unhandled Node: "+n);\r'
	print "\t}\r"
	print '\r'
	