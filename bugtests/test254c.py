
from java import awt, applet
import java

print "Hi! One stacktrace expected:"
try:
 raise java.lang.Exception()
except java.lang.Exception,e:
 e.printStackTrace()

class test254c(applet.Applet):
    def paint(self, g):
	g.setColor(awt.Color.black)
	g.fill3DRect(5,5,590,100,0)
	g.setFont(awt.Font('Arial', 0, 80))
	g.setColor(awt.Color.blue)
	g.drawString('Hello World', 90, 80)
