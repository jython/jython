# Tests for basic class-type data model.
# See http://docs.python.org/reference/datamodel.html

class MyClass:
  """My doc string"""
  def __init__(self):
    print self.__doc__


class MyClassNoDoc:
  def __init__(self):
    print self.__doc__
