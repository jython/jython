from java.applet import Applet

class HelloWorld(Applet):
    def paint(self, g):
	g.drawString("Hello from JPython!", 20, 30)


if __name__ == '__main__':
    import pawt
    pawt.test(HelloWorld())
