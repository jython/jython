"""\
This is a very simple example of using Java's AWT from JPython.

Many more examples can be found in Demo/applet.  While all of those
demos are designed as applets, they can also be run as applications
and they all show how to use different parts of the AWT.
"""

import java
from java import awt

def exit(e): java.lang.System.exit(0)

frame = awt.Frame('AWT Example', visible=1)
button = awt.Button('Close Me!', actionPerformed=exit)
frame.add(button, 'Center')
frame.pack()
