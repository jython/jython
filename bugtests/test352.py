"""
[ #495602 ] os.path.dirname() can result in an NPE
"""

import support
import os

try:
    os.path.dirname(None)
except TypeError:
    pass
    
try:
    os.path.basename(None)
except TypeError:
    pass
    
try:
    os.path.exists(None)
except TypeError:
    pass
    
try:
    os.path.isabs(None)
except TypeError:
    pass
    
try:
    os.path.isfile(None)
except TypeError:
    pass
    
try:
    os.path.isdir(None)
except TypeError:
    pass
    
try:
    os.path.join(None)
except TypeError:
    pass
    
try:
    os.path.join(None, None)
except TypeError:
    pass
    
try:
    os.path.normcase(None)
except (TypeError, AttributeError):
    pass
    
try:
    if hasattr(os.path, "samefile"):
        os.path.samefile(None, None)
except TypeError:
    pass
    
try:
    os.path.abspath(None)
except TypeError:
    pass
    
try:
    os.path.getsize(None)
except TypeError:
    pass
    
try:
    os.path.getmtime(None)
except TypeError:
    pass
    
try:
    os.path.getatime(None)
except TypeError:
    pass

