from java.awt import Panel, Dimension, Frame, Color
class a(Panel):
        pass

class b(a):
        def paint(self, g):
                w = self.getSize().width
                h = self.getSize().height
                g.setColor(Color.black)
                g.fillRect(0, 0, w, h)

test = b()

f = Frame()
f.add(test)
f.setSize(400,400)
f.setVisible(1)
