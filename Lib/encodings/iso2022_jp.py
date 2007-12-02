"""
iso2022_jp.py: Python Unicode Codec for iso2022_jp

Written by 
    NISHIO Hirokazu <python@nishiohirokazu.org>

\x1b)B signals the following characters will be ASCII, and since there are
no ASCII characters at the end of the s, the signifier is optional.  Java's
encoder doesn't add it, but CPython's does.

>>> u = u'\u3053\u3093\u306b\u3061\u306f'
>>> s = u.encode("iso2022_jp")
>>> assert s == '\x1b$B$3$s$K$A$O' or s == '\x1b$B$3$s$K$A$O\x1b)B'
>>> u2 = s.decode("iso2022_jp")
>>> assert u2 == u
"""

from encodings import java_charset_wrapper

getregentry = java_charset_wrapper.create_getregentry("iso2022jp")
