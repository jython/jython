names = {
	1024: 'abstract',
	  16: 'final',
	 512: 'interface',
	 256: 'native',
	   2: 'private',
	   4: 'protected',
	   1: 'public',
	   8: 'static',
	  32: 'synchronized',
	 128: 'transient',
	  64: 'volatile'
}

import string

numbers = {}
for number, name in names.items():
	numbers[name] = number

def Modifier(base=0, **kw):
	"""Construct the appropriate integer to represent
	modifiers for a method, class, or field declaration.
	This should probably do some error checking:
	"public private" doesn't make much sense.
	"""
	 
	for name, value in kw.items():
		if value:
			base = base | numbers[name]
			
	return base
	
def ModifierString(modifier):
	if type(modifier) == type(""): return modifier
	text = []
	base = 1
	while base <= modifier:
		if modifier & base:
			text.append(names[base])
		base = base * 2

	return string.join(text, ' ')
	
if __name__ == '__main__':
	m0 = Modifier(0)
	m1 = Modifier(public=1, final=1)
	m2 = Modifier(1023)
	print ModifierString(m0)
	print ModifierString(m1)
	print ModifierString(m2)
