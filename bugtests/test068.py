"""
__getslice__ method never called for Python instances
"""

import support

class F:
   def __getitem__(self,*args): return '__getitem__ '+`args`
   def __getslice__(self,*args): return '__getslice__ '+`args`

f=F()

support.compare(f[1:1], "__getslice__ \(1, 1\)")
