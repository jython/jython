"""
Check that "remove" method exists in all class __dict__ which defines it.
"""


import support
from java.awt import Container, MenuContainer, Component

for c in [Container, Component, MenuContainer]:
    f = c.__dict__.get("remove")
    if f == None:
	raise support.TestWarning('remove function expected in %s __dict__' % `c`)
	
