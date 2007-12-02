"""
ms932.py: Python Unicode Codec for ms932

Written by 
    NISHIO Hirokazu <python@nishiohirokazu.org>

>>> u = u'\u3053\u3093\u306b\u3061\u306f'
>>> s = u.encode("ms932")
>>> assert s == '\x82\xb1\x82\xf1\x82\xc9\x82\xbf\x82\xcd'
>>> u2 = s.decode("ms932")
>>> assert u2 == u
"""

import java_charset_wrapper

getregentry = java_charset_wrapper.create_getregentry("ms932")
