from test_support import *

print_test('jarray module (test_jarray.py)', 1)

from jarray import array, zeros

print_test('array', 2)
from java import awt
hsb = awt.Color.RGBtoHSB(0,255,255, None)
#print hsb
assert hsb == array([0.5,1,1], 'f')

rgb = apply(awt.Color.HSBtoRGB, tuple(hsb))
#print hex(rgb)
assert rgb == 0xff00ffff

print_test('zeros', 2)
hsb1 = zeros(3, 'f')
awt.Color.RGBtoHSB(0,255,255, hsb1)
#print hsb, hsb1
assert hsb == hsb1
