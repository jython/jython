"""\
Create a panel showing all of the colors defined in the pawt.colors module
Display the names of bright colors in black and of dark colors in white
"""

from java import awt
from pawt import colors, test
from math import sqrt

p = awt.Panel()
for name in dir(colors):
	color = getattr(colors, name)
	if isinstance(color, awt.Color):
		l = awt.Label(name, awt.Label.CENTER, background=color)
		intensity = sqrt(color.red**2 + color.green**2 + color.blue**2)/3		
		if intensity < 90: l.foreground = colors.white
		p.add(l)

test(p, size=(700,500))