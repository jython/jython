from test194m.test194c1 import test194c1

class test194c2(test194c1):
        def __init__(self):
                test194c1.__init__(self, "This is test194c2")
                self.setSize(100,100); self.setLocation(300,300)