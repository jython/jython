import croc

class Snake:
  def eats(self, thing):
    return False

class Python(Snake):
  def eats(self, thing):
    return isinstance(thing, croc.Gavial)
