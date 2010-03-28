class A:
  a = 1

  def __init__(self):
    self.b = 2

  def hi(self, msg):
    print "%s: %s" % (msg, A.a)

x = A
y = A()
z = y.b
