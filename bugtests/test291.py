"""
Test keywords to import builtin.
"""

import support

impl_names = ['java'] 

# Will only return anygui :P 
try:
    impls = [__import__('anygui.impl.%sgui' % name, 
	fromlist=['%sgui' % name]) for name in impl_names] 
except TypeError, e:
    support.compare(e, "__import__\(\) takes no keyword arguments");
else:
    support.TestError("Should raise a TypeError")


