import Output, sys, os
from Freezer import Freezer

class YesNo: pass
class Required: pass


class Options:
	def __init__(self, opts, args=None):
		if args == None:
			args = sys.argv[1:]
		ret = {}
			
		index = 0
		while index < len(args):
			arg = args[index]
			if arg[0] == '-':
				name = arg[1:]
				value = opts[name]
				if value is YesNo:
					ret[name] = 1
					index = index+1
				else:
					ret[name] = args[index+1]
					index = index+2
				continue
			break
		
		for key, value in opts.items():
			if ret.has_key(key): continue
			elif value is YesNo:
				ret[key] = 0
			elif value is Required:
				raise ValueError, "argument %s is required" % key
			else:
				ret[key] = value
			
		for key, value in ret.items():
			setattr(self, key, value)
				
		self.args = args[index:]

opts = Options({'jar':None, 'cab':None, 'dir':None, 'core':YesNo, 'main':YesNo, 'shallow':YesNo})

outs = []
if opts.jar is not None:
	outs.append(Output.ZipOutput(opts.jar))

if opts.cab is not None:
	outs.append(Output.CabOutput(opts.cab))

if opts.dir is not None:
	outs.append(Output.DirectoryOutput(opts.dir))

if len(outs) > 1:
	raise ValueError, 'must specify only one of -jar, -cab, -dir'
	

directory = '.'
if len(opts.args) > 0:
	file = opts.args[0]
	path, name = os.path.split(file)
	if path != '':
		directory = path
		
if len(outs) == 0:
	outs.append(Output.DirectoryOutput(directory))

#Prepend the current directory to the path
sys.path[0:0] = [directory]

#print opts.args
#sys.exit()
names = opts.args

#names = ['HelloWorld', 'ButtonDemo', 'CheckboxDemo', 'ChoiceDemo', 'Converter', 'CoordinatesDemo', 'LabelDemo', 'ListDemo']

f = Freezer(outs[0], opts.shallow)
for name in names:
	name = os.path.split(name)[1]
	n, ext = os.path.splitext(name)
	if ext == '.py':
		name = n
	#print name
	f.freeze(name)
f.finish(opts.main)

if opts.core:
	skiplist = ['org.python.core.parser', 'org.python.core.BytecodeLoader', 'org.python.core.jpython']
	f.addPackage(os.path.join(sys.prefix, 'JavaCode', 'org', 'python', 'core'), skiplist)

f.out.close()
