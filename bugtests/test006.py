"""
Check that multiple BEP can be assigned to a single cast listener.
"""

import support
from java import awt


support.compileJava("test006j.java")

import test006j


def f(evt):
    pass

m = test006j()

m.componentShown = f
m.componentHidden = f

m.fireComponentShown(awt.event.ComponentEvent(awt.Container(), 0))
m.fireComponentMoved(awt.event.ComponentEvent(awt.Container(), 0))

