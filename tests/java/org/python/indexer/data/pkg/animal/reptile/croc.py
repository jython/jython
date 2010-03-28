# Unit-test stuff.

class Crocodilian:
  def __init__(self):
    self.size = "unknown"

class Crocodile(Crocodilian):
  def __init__(self):
    Crocodilian.__init__(self)
    self.size = "Huge"

class Alligator(Crocodilian):
  def __init__(self):
    Crocodilian.__init__(self)
    self.size = "Large"

class Gavial(Crocodilian):
  def __init__(self):
    Crocodilian.__init__(self)
    self.size = "Medium"
