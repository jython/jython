from java.applet import Applet

class test214a(Applet):
    def paint(self, g):
        g.drawString("Hello world", 20, 30)

if __name__ == '__main__':
    import pawt
    pawt.test(HelloWorld())
