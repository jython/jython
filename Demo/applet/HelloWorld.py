from java.applet import Applet
import sys

class HelloWorld(Applet):
    def paint(self, g):
	g.drawString("Hello from Jython %s!" % sys.version, 20, 30)


if __name__ == '__main__':
    import pawt
    pawt.test(HelloWorld())
