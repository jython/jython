"""
test for
[ 730156 ] java.lang.VerifyError with very simple Python source
"""
import support

code = """
def method():
     try:
         for dummy in [1,2,3]:
             try:
                 return "result"
             except:
                 pass
     finally:
         pass
"""

import java.lang

try:
    c = compile(code,"<snippet>","exec")
except java.lang.VerifyError,e:
    raise support.TestWarning("try-for-try-finally still produces invalid bytecode")

d = {}

exec code in d

if d['method']() != 'result':
    raise support.TestError("wrong result")