"""\
This is a very simple example of creating a Java applet from JPython
This applet should display in any JDK 1.1 compliant browser 
At the moment that means it will only work in SUN's "appletviewer"

To use this applet, you must perform the following steps:

1) Put the JPython classes in your Java classpath <JPython>/JavaCode
2) Make a real Java class corresponding to this applet
	jpython Tools/mkjava.py PythonApplet HelloApplet <JPython>/Demo/applet
3) Point your browser at PythonApplet.html in this directory.
	cd to this directory
	appletviewer PythonApplet.html
	

Future releases will contain better support for bundling up applets for
release to the web.
"""

from java import awt, applet

class HelloApplet(applet.Applet):
	def paint(self, g):
		g.setColor(awt.Color.black)
		g.fill3DRect(5,5,590,100,0)
		g.setFont(awt.Font('Arial', 0, 80))
		g.setColor(awt.Color.blue)
		g.drawString('Hello World', 90, 80)
		