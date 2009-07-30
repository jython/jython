"""
 test compile. derived from test_codeop
"""
import codeop
import unittest
from test import test_support
from test.test_support import run_unittest


def compile_(source,name="<input>",symbol="single"):
    return compile(source,name,symbol)

class CompileTests(unittest.TestCase):

    def assertValid(self, str, symbol='single',values=None,value=None):
        '''succeed iff str is a valid piece of code'''
        code = compile_(str, "<input>", symbol)
        if values:
            d = {} 
            exec code in d
            self.assertEquals(d,values)
        elif value is not None:
            self.assertEquals(eval(code,self.eval_d),value)
        else:
            self.assert_(code)
        
    def assertInvalid(self, str, symbol='single', is_syntax=1):
        '''succeed iff str is the start of an invalid piece of code'''
        try:
            compile_(str,symbol=symbol)
            self.fail("No exception thrown for invalid code")
        except SyntaxError:
            self.assert_(is_syntax)
        except OverflowError:
            self.assert_(not is_syntax)

    def test_valid(self):
        av = self.assertValid

        av("\n\n")
        av("# a\n")

        av("\n\na = 1\n\n",values={'a':1})

        av("\n\nif 1: a=1\n\n",values={'a':1})

        av("def x():\n  pass")
        av("def x():\n  pass\n ")
        av("def x():\n  pass\n  ")
        av("\n\ndef x():\n  pass")

        av("if 9==3:\n   pass\nelse:\n   pass")
        av("if 1:\n pass\n if 1:\n  pass\n else:\n  pass")

        av("#a\n\n   \na=3\n",values={'a':3})


        # these failed under 2.1
        self.eval_d = {'a': 2}
        av("\n\na**3","eval",value=8)
        av("\n \na**3","eval",value=8)
        av("#a\n#b\na**3","eval",value=8)

        # this failed under 2.2.1
        av("def f():\n try: pass\n finally: [x for x in (1,2)]")

        # Failed for Jython 2.5a2.  See http://bugs.jython.org/issue1116.
        av("@a.b.c\ndef f():\n pass") 

        av("def f():\n pass\n#foo")

    def test_invalid(self):
        ai = self.assertInvalid
        
        ai("del 1")
        ai("del ()")
        ai("del (1,)")
        ai("del [1]")
        ai("del '1'")
        ai("[i for i in range(10)] = (1, 2, 3)")


class CodeopTests(unittest.TestCase):

    def test_no_universal_newlines(self):
        # previously \r was translated due to universal newlines
        code = codeop.compile_command("'\rfoo\r'", symbol='eval')
        self.assertEqual(eval(code), '\rfoo\r')


def test_main():
    run_unittest(CompileTests,
                 CodeopTests)


if __name__ == "__main__":
    test_main()
