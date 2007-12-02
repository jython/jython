"""
euc_jp.py: Python Unicode Codec for euc_jp

Written by 
    NISHIO Hirokazu <python@nishiohirokazu.org>

>>> u = u'\u3053\u3093\u306b\u3061\u306f'
>>> s = u.encode("euc_jp")
>>> assert s == '\xa4\xb3\xa4\xf3\xa4\xcb\xa4\xc1\xa4\xcf'
>>> u2 = s.decode("euc_jp")
>>> assert u2 == u
"""

import java_charset_wrapper

getregentry = java_charset_wrapper.create_getregentry("euc_jp")
