"""
[ #444292 ] local var binding overrides local import
"""

import support

def foo(pickle): 
    assert pickle == 1
    import pickle 
    if pickle == 1:
        raise support.TestWarning("An import should override a local")
    
foo(1) 

