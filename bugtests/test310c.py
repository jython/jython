"""
[ #444292 ] local var binding overrides local import
"""

import support

def foo(pickle): 
    assert pickle == 1
    import pickle 
    assert pickle != 1
    
foo(1) 

