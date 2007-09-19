# Copyright (c) Corporation for National Research Initiatives
import os
from Modifier import ModifierString



class SourceFile:
    def __init__(self, classname):
	self.text = []
	self.indentation = 0
	self.indent = '    '
	self.startLine = 1
	self.classname = classname
	self.filename = classname+'.java'

    def dump(self, directory="c:\\jython\\test\\comp"):
	fp = open(os.path.join(directory, self.filename), 'w')
	for bit in self.text:
	    fp.write(bit)
	fp.close()

    def __repr__(self):
        EMPTYSTRING = ''
	return EMPTYSTRING.join(self.text)

    def writeModifiers(self, modifiers):
	self.write(ModifierString(modifiers))

    def write(self, text, *args):
	newArgs = []
	for arg in args:
	    if hasattr(arg, 'sourceString'):
		arg = arg.sourceString()
	    newArgs.append(arg)

	if self.startLine:
	    self.text.append(self.indent*self.indentation)
	    self.startLine = 0

	if len(args) > 0:
	    text = text % tuple(newArgs)
	self.text.append(text)

    def writeln(self, text="", *args):
	apply(self.write, (text+'\n',)+args)
	self.startLine = 1

    def writeList(self, values):
	if len(values) == 0:
	    return
	text = "%s, "*(len(values)-1) + "%s "
	print text
	apply(self.write, (text, )+tuple(values))

    def beginBlock(self):
	self.writeln('{')
	self.indentation = self.indentation+1

    def endBlock(self):
	self.indentation = self.indentation-1
	self.writeln('}')



if __name__ == '__main__':
    sf = SourceFile('foo')
    sf.write('public class foo')
    sf.beginBlock()
    sf.writeln()
    sf.writeln("int bar;")
    sf.write("abc")
    sf.write("def")
    sf.beginBlock()
    sf.writeln("hi there")
    sf.endBlock()
    sf.writeList(['a', 'b', 'c'])
    sf.writeln(" extra")
    sf.endBlock()

    print sf
