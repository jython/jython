from test192c1 import test192c1

class test192c2(test192c1):
        def __init__(self):
                test192c1.__init__(self, "This is test192c2")
                self.setSize(100,100); self.setLocation(300,300)