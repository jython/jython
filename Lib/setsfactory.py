from copy_reg import pickle, constructor
from sets import Set as _Set, ImmutableSet as _ImmutableSet

def Set(*args):
  return _Set(*args)
def ImmutableSet(*args):
  return _ImmutableSet(*args)

constructor(Set)
constructor(ImmutableSet)
