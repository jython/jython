# copyright 2004-2005 Samuele Pedroni
import sys
import types
import cStringIO

class MiniIndentPrinter:

    def __init__(self,output):
        self.out = output
        self.col = 0
        self.indent_step = 4
        
    def writeln(self,text):
        self.out.write(' '*self.col)
        self.out.write(text)
        self.out.write("\n")

    def indent(self):
        self.col += self.indent_step

    def dedent(self):
        self.col -= self.indent_step
        
class PrettySpec:

    def __getitem__(self,parms):
        if not isinstance(parms,types.TupleType):
            parms = (parms,)
        return self.evoke(parms)
            
    def evoke(self,parms):
        return self.__class__(parms)

    def visit_spec_seq(self,printer,seq,in_repeat=0,active=1):
        for elem in seq:
            if isinstance(elem,types.StringType):
                count = len(elem.split())
                for i in xrange(count):
                    if active:
                        printer.writeln("last = tracker.visit_cur_forward()")
                        printer.writeln("effect |= last")                        
                    else:
                        printer.writeln("tracker.skip()")
            else:
                if active:
                    elem.active(printer,in_repeat)
                else:
                    elem.passive(printer,in_repeat)
                    
    def active(self,printer,in_repeat):
        pass

    def passive(self,printer,in_repeat):
        pass

class ExpectSkipPrettySpec(PrettySpec):

    def active(self,printer,in_repeat):
         printer.writeln("if not tracker.exhausted(): tracker.expect_skip()")

    passive = active

p_expect_skip = ExpectSkipPrettySpec()
    

class SpacerPrettySpec(PrettySpec):
    def __init__(self,spacer,effect=0):
        self.spacer = spacer
        self.effect = effect

    def active(self,printer,in_repeat):
        printer.writeln("self.%s()" % self.spacer)
        if self.effect:
            printer.writeln("effect = 1")

p_space = SpacerPrettySpec('space')
p_nl = SpacerPrettySpec('nl',effect=1)
p_indent = SpacerPrettySpec('indent')
p_dedent = SpacerPrettySpec('dedent')

class RepeatPrettySpec(PrettySpec):
    def __init__(self,spec_seq=None):
        self.spec_seq = spec_seq

    def active(self,printer,in_repeat):
        if in_repeat:
            raise Exception,"cannot nest p_repeat in p_repeat"
        printer.writeln("while not tracker.exhausted():")
        printer.indent()
        self.visit_spec_seq(printer,self.spec_seq,in_repeat=1)
        printer.dedent()

    def passive(self,printer,in_repeat):
        raise Exception,"cannot nest p_repeat inside p_dependent"

p_repeat = RepeatPrettySpec()

class MoreP_PrettySpec(PrettySpec):

    def active(self,printer,in_repeat):
        if in_repeat:
            printer.writeln("if tracker.exhausted(): break")
        else:
            printer.writeln("if tracker.exhausted(): return effect")

    passive = active

p_morep = MoreP_PrettySpec()

class DependentPrettySpec(PrettySpec):
    def __init__(self,spec_seq = None):
        self.spec_seq = spec_seq

    def active(self,printer,in_repeat):
        printer.writeln("if last:")
        printer.indent()
        self.visit_spec_seq(printer,self.spec_seq,in_repeat=in_repeat,active=1)
        printer.dedent()
        printer.writeln("else:")
        printer.indent()
        printer.writeln("pass")
        self.visit_spec_seq(printer,self.spec_seq,in_repeat=in_repeat,active=0)        
        printer.dedent()
            
p_dependent = DependentPrettySpec(PrettySpec)

P_LAST = PrettySpec()
P_EFFECT = PrettySpec()

class MainPrettySpec(PrettySpec):

    def evoke(self,spec_seq):
        last = 0
        if spec_seq[0] is P_LAST:
            last = 1
        caller_globals = sys._getframe(2).f_globals
        #print caller_globals['__name__']
        buf = cStringIO.StringIO()
        printer = MiniIndentPrinter(buf)
        printer.writeln("def visit_Foo(self,node,ctxt):")
        printer.indent()
        printer.writeln("tracker = NodeVisitTracker(self,node)")
        printer.writeln("effect = 0")
        printer.writeln("last = 0")
        self.visit_spec_seq(printer,spec_seq)
        if last:
            printer.writeln("return last")
        else:
            printer.writeln("return effect")                
        printer.dedent()        
        defsource = buf.getvalue()
        ns = {}
        #print defsource
        exec defsource in caller_globals,ns
        return ns['visit_Foo']
        
simplepretty = MainPrettySpec()
