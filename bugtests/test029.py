"""
Check split errors.
"""

import support

import string
if string.split("aa::bb::cc:", "::") != ["aa","bb","cc:"]:
    raise support.TestError("split misbehavior")

if 'a'.split(None) != ['a']:
    raise support.TestError("split misbehavior")

if 'a'.split(None,1) != ['a']:
    raise support.TestError("split misbehavior")

if 'a'.split(' ') != ['a']:
    raise support.TestError("split misbehavior")

if 'a'.split(' ',1) != ['a']:
    raise support.TestError("split misbehavior")

if 'a '.split(None) != ['a']:
    raise support.TestError("split misbehavior")

if 'a b '.split(None) != ['a','b']:
    raise support.TestError("split misbehavior")

if 'a '.split(' ') != ['a','']:
    raise support.TestError("split misbehavior")

if 'a b '.split(' ') != ['a','b','']:
    raise support.TestError("split misbehavior")

if 'a '.split(None,1) != ['a']:
    raise support.TestError("split misbehavior")

if 'a b '.split(None,1) != ['a','b ']:
    raise support.TestError("split misbehavior")

if 'a '.split(' ',1) != ['a','']:
    raise support.TestError("split misbehavior")

if 'a b '.split(' ',1) != ['a','b ']:
    raise support.TestError("split misbehavior")