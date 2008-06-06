"""
 test compile. derived from test_codeop
"""
import unittest
from test_support import run_unittest


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

    assertIncomplete = assertInvalid

    def test_valid(self):
        av = self.assertValid

        av("")
        av("\n")
        av("\n\n")
        av("# a\n")

        av("a = 1")
        av("\na = 1")
        av("a = 1\n")
        av("a = 1\n\n")
        av("\n\na = 1\n\n",values={'a':1})

        av("def x():\n  pass\n")
        av("if 1:\n pass\n")

        av("\n\nif 1: pass\n")
        av("\n\nif 1: a=1\n\n",values={'a':1})

        av("def x():\n  pass")
        av("def x():\n  pass\n ")
        av("def x():\n  pass\n  ")
        av("\n\ndef x():\n  pass")

        av("def x():\n\n pass\n") # failed under 2.1
        av("def x():\n  pass\n  \n")
        av("def x():\n  pass\n \n")

        av("pass\n")
        av("3**3\n")

        av("if 9==3:\n   pass\nelse:\n   pass")
        av("if 9==3:\n   pass\nelse:\n   pass\n")
        av("if 1:\n pass\n if 1:\n  pass\n else:\n  pass")
        av("if 1:\n pass\n if 1:\n  pass\n else:\n  pass\n")

        av("#a\n#b\na = 3\n")
        av("#a\n\n   \na=3\n",values={'a':3})
        av("a=3\n\n")
        av("a = 9+ \\\n3")

        av("3**3","eval")
        av("(lambda z: \n z**3)","eval")

        av("9+ \\\n3","eval")
        av("9+ \\\n3\n","eval")

        # these failed under 2.1
        self.eval_d = {'a': 2}
        av("\n\na**3","eval",value=8)
        av("\n \na**3","eval",value=8)
        av("#a\n#b\na**3","eval",value=8)

        # this failed under 2.2.1
        av("def f():\n try: pass\n finally: [x for x in (1,2)]")

    def test_incomplete(self):
        ai = self.assertIncomplete

        ai("(a **")
        ai("(a,b,")
        ai("(a,b,(")
        ai("(a,b,(")
        ai("a = (")
        ai("a = {")
        ai("b + {")

        ai("if 9==3:\n   pass\nelse:")
        ai("if 9==3:\n   pass\nelse:\n")

        ai("if 1:")
        ai("if 1:\n")
        ai("if 1:\n pass\n if 1:\n  pass\n else:")
        ai("if 1:\n pass\n if 1:\n  pass\n else:\n")          
        
	ai("def x():")
        ai("def x():\n")
        ai("def x():\n\n")

        ai("a = 9+ \\")
        ai("a = 'a\\")
        ai("a = '''xy")

        ai("","eval")
        ai("\n","eval")
        ai("(","eval")
        ai("(\n\n\n","eval")
        ai("(9+","eval")
        ai("9+ \\","eval")
        ai("lambda z: \\","eval")

    def test_invalid(self):
        ai = self.assertInvalid
        
        ai("a b")

        ai("a @")
        ai("a b @")
        ai("a ** @")
        
        ai("a = ")
        ai("a = 9 +")

        ai("def x():\n\npass\n")

        ai("\n\n if 1: pass\n\npass") # valid for 2.1 ?!

        ai("a = 9+ \\\n")
        ai("a = 'a\\ ")
        ai("a = 'a\\\n")

        ai("a = 1","eval")
        ai("a = (","eval")
        ai("]","eval")
        ai("())","eval")
        ai("[}","eval")
        ai("9+","eval")
        ai("lambda z:","eval")
        ai("a b","eval")

    def test_filename(self):
        self.assertEquals(compile_("a = 1\n", "abc").co_filename,
                          compile("a = 1\n", "abc", 'single').co_filename)
        self.assertNotEquals(compile_("a = 1\n", "abc").co_filename,
                             compile("a = 1\n", "def", 'single').co_filename)


def test_main():
    run_unittest(CompileTests)


if __name__ == "__main__":
    test_main()
