# copyright 2004-2005 Samuele Pedroni
import sys

from java_lexer import Token

import java_parser
import java_nodes

from simpleprettyspec import simplepretty,p_space,p_nl,p_repeat,p_dependent
from simpleprettyspec import p_indent,p_dedent,p_morep,P_LAST,P_EFFECT
from simpleprettyspec import p_expect_skip

class IndentPrinter:

    clashes = ()

    def __init__(self,output):
        self.out = output
        self.col = 0
        self.last = '\n'
        self.soft_nl = 0

        self.indent_level = [0]
        self.indent_step = 4

    def write(self,text):
        if self.last == '\n':
            self.out.write(' '*self.col)
        self.out.write(text)
        self.col += len(text)
        self.last = text

    def linehome(self):
        return self.last == '\n'

    def nl(self,soft=0):
        if self.last != '\n' or not self.soft_nl:
            self.out.write('\n')
        self.soft_nl = soft
        self.col = self.indent_level[-1]
        self.last = '\n'

    def emit(self,text):
        if not text: return
        if text.isspace():
            if self.last in " \n":
                return
            self.write(" ")
        else:
            tail = self.last[-1]
            head = text[0]
            if (tail+head in self.clashes or
                (tail.isalnum() or tail in "$_") and
                (head.isalnum() or head in "$_`")): # !!! factor out `
                self.write(" ")
            self.write(text)

    def here(self,delta=0):
        self.indent_level.append(self.col+delta)

    def indent(self):
        assert self.last == '\n'
        self.col = self.indent_level[-1] + self.indent_step
        self.indent_level.append(self.col)

    def dedent(self):
        self.indent_level.pop()
        self.col = self.indent_level[-1]
        
class JavaPrinter(IndentPrinter):

    clashes = ("//","/*","++","--")

    def __init__(self,output):
        IndentPrinter.__init__(self,output)

    def space(self):
        self.emit(" ")

    def vertical_space(self,delta,soft=0):
        if delta > 1:
            self.nl()
        self.nl(soft)
            
    def emit_comment(self,comment, subst): # doesn't write final \n!
        col = comment.col
        comment = comment.value
        if subst is not None:
            comment = comment % subst
        lines = comment.split('\n')
        first = lines[0]
        self.write(first)
        lines = lines[1:]
        tostrip = col
        prefix = ' '*col
        for line in lines:
            if not line.startswith(prefix):
                tostrip = 0
        for line in lines:
            self.nl()
            self.write(line[tostrip:])

    def emit_horizontal_comments(self,comments, subst):
        # space + space separated comments...
        for comment in comments:
            self.space()
            self.emit_comment(comment, subst)

    def emit_vertical_comments(self,comments, subst):
        end = comments[0].end
        self.emit_comment(comments[0], subst)
        for comment in comments[1:]:
            self.vertical_space(comment.start - end)
            end = comment.end
            self.emit_comment(comment, subst)

    def emit_infront_comments(self,tok, subst):
        if not self.linehome():
            self.nl()
        comments = tok.infront_comments
        self.emit_vertical_comments(comments, subst)
        self.vertical_space(tok.lineno - comments[-1].end)

    def emit_attached_comments(self,tok, subst):
        comments = tok.attached_comments
        start = comments[0].start
        horizontal = 1 # all on one line
        for comment in comments:
            if comment.end != start:
                horizontal = 0
                break
        if horizontal:
            self.emit_horizontal_comments(comments, subst)
        else:
            self.space()
            self.here()
            self.emit_vertical_comments(comments, subst)
            self.dedent()
        if comments[-1].value.startswith("//"): # // needs newline
            delta = 1
        else:
            delta = 0
        delta = max(delta,tok.attached_line_delta)
        if delta == 0:
            self.space()
        else:
            self.vertical_space(delta,soft=1)
        
    def emit_tok(self,tok,ctl='ika', subst=None):
        # ctl: i=>infront k=>token a=>attached

        if 'i' in ctl and tok.infront_comments:
            self.emit_infront_comments(tok, subst)

        if 'k' in ctl:
            self.emit(tok.value)

        if 'a' in ctl and tok.attached_comments:
            self.emit_attached_comments(tok, subst)

# ~~~

def hierarchy(cl):
    hier = [cl.__name__]
    while cl.__bases__:
        assert len(cl.__bases__) == 1
        cl = cl.__bases__[0]
        hier.append(cl.__name__)
    return hier

class NodeVisitTracker: # !!! polish

    def __init__(self,visitor,node,left=None,right=None):
        self.node = node
        self.visitor = visitor
        self.children = node.children
        self.index = 0
        self.stop = len(self.children)
        self.left,self.right = left,right

    def parent(self):
        return self.node

    def prev(self):
        index = self.index
        if index == 0:
            return self.left
        else:
            return self.children[index-1]

    def cur(self):
        return self.children[self.index]

    def next(self):
        index = self.index
        if index == self.stop-1:
            return self.right
        else:
            return self.children[index+1]

    def flag(self,name):
        return self.flags.get(name,0)

    def expect_skip(self):
        self.index = -1

    def skip(self):
        # !!! but comments?
        self.index += 1

    def visit_cur_forward(self,**flags):
        self.flags = flags
        r = self.visitor.visit(self.children[self.index],self)
        self.index += 1
        return r

    def exhausted(self):
        return self.stop == self.index

    def go(self):
        r = 0
        stop = self.stop
        visit = self.visitor.visit
        children = self.children
        index = self.index
        while index != stop:
            self.flags = {}
            r |= visit(children[index], self)
            index = self.index = self.index + 1
        return r

class JavaPretty:

    def __init__(self,output=sys.stdout):
        self.printer = JavaPrinter(output)
        # shortcuts
        self.nl = self.printer.nl
        self.indent = self.printer.indent
        self.dedent = self.printer.dedent
        self.space = self.printer.space

        self._separators = []

        self._disp_cache = {}

    def visit(self,node, ctxt=None):
        cl = node.__class__
        try:
            before, do_visit, after = self._disp_cache[cl.__name__]
        except KeyError:
            hier = hierarchy(cl)
            for name in hier:
                do_visit = getattr(self,'visit_%s' % name,None)
                if do_visit is not None:
                    break
            else:
                do_visit = self.default_visit
            after = []
            for name in hier:
                after_meth = getattr(self,'after_%s' % name,None)
                if after_meth is not None:
                    after.append(after_meth)
            hier.reverse()
            before = []
            for name in hier:
                before_meth = getattr(self,'before_%s' % name,None)
                if before_meth is not None:
                    before.append(before_meth)
            self._disp_cache[cl.__name__] = before, do_visit, after
                
        for meth in before:
            meth(node,ctxt)
        r = do_visit(node,ctxt)
        for meth in after:
            meth(node,ctxt)
        if r is None:
            return 1
        return r

    def push_separator(self,tok):
        self._separators.append(tok)

    def pop_separator(self):
        tok = self._separators.pop()
        if tok is not None:
            self.printer.emit_tok(tok,'ia')

    def emit_tok(self,tok,ctl='ika', subst=None):
        if 'k' in ctl:
            seps = self._separators
            for i in range(len(seps)-1,-1,-1):
                if seps[i] is not None:
                    self.printer.emit_tok(seps[i])
                    seps[i] = None
                else:
                    break
        self.printer.emit_tok(tok,ctl, subst)

    def default_visit(self,node,ctxt):
        if node is None: return
        if isinstance(node,Token):
            self.emit_tok(node)
            return

        return NodeVisitTracker(self,node).go()

    # specialized cases

    # !!! Assigment: space = space

    def visit_Expressions(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        if tracker.exhausted(): return 0
        prev = 0
        last = tracker.visit_cur_forward()
        while not tracker.exhausted():
            tok = tracker.cur()
            tracker.skip()
            prev |= last
            if prev:
                self.push_separator(tok)
            else:
                self.emit_tok(tok,'ia')
            last = tracker.visit_cur_forward()
            if prev:
                self.pop_separator()
        return last|prev

    visit_FormalParameterList = visit_Expressions


    def visit_ClassCreator(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()
        if node.ClassBodyOpt:
            self.printer.here(+1)
            tracker.visit_cur_forward()
            self.dedent()

    def visit_ArrayCreator(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()
        if node.has('ArrayInitializer'):
            self.space()
        tracker.visit_cur_forward()

    def before_Block(self,node,ctxt):
        if ctxt and isinstance(ctxt.parent(),java_parser.TakesBlock):
            self.space()

    def after_Block(self,node,ctxt):
        if (ctxt and isinstance(ctxt.parent(),java_parser.TakesBlock)
            and ctxt.flag('chain')):
            self.space()
        
    visit_Block = simplepretty[
        '{',p_nl,p_indent,
        'BlockStatements',
        p_dependent[p_nl],
        p_dedent,'}']

    visit_BlockStatements = simplepretty[P_LAST,
        p_repeat[p_dependent[p_nl],
                 'BlockStatement',]]

    def before_Statement(self,node,ctxt):
        if isinstance(ctxt.parent(),java_parser.TakesBlock):
            self.nl()
            self.indent()              

    def after_Statement(self,node,ctxt):
        if isinstance(ctxt.parent(),java_parser.TakesBlock):
            self.dedent()
            if ctxt.flag('chain'):
                self.nl()

    def visit_IfStatement(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        tracker.visit_cur_forward()
        self.space()
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()
        
    def visit_IfElseStatement(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        tracker.visit_cur_forward()
        self.space()
        tracker.visit_cur_forward()
        tracker.visit_cur_forward(chain=1)
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()        

    def visit_SwitchStatement(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        tracker.visit_cur_forward()
        self.space()
        tracker.visit_cur_forward()
        self.space()
        tracker.visit_cur_forward()
        self.nl()
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()

    visit_SwitchBlockStatementGroup = simplepretty[
        'SwitchLabel',
        p_nl,p_indent,
        'BlockStatements',
        p_dependent[p_nl],
        p_dedent]

    def visit_SwitchLabel(self,node,ctxt):
        if node.has('CASE'):
            tracker = NodeVisitTracker(self,node)            
            tracker.visit_cur_forward()
            self.space()
            tracker.visit_cur_forward()
            tracker.visit_cur_forward()
        else:
            self.default_visit(node,ctxt)

    def visit_TryStatement(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        tracker.visit_cur_forward()
        self.space()
        while not tracker.exhausted():
            tracker.visit_cur_forward()

    def visit_CatchClause(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        self.space()
        tracker.visit_cur_forward()
        self.space()
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()
        tracker.visit_cur_forward()        
        self.space()        
        tracker.visit_cur_forward()

    visit_FinallyClause = simplepretty[p_space,"finally",p_space,"Block"]
        
    visit_ClassBody = simplepretty[p_space,'{',
                                   p_nl,p_indent,p_nl,
                                   'ClassBodyDeclarations',
                                   p_dependent[p_nl,p_nl],
                                   p_dedent,'}']

    visit_InterfaceBody = simplepretty[p_space,'{',
                                       p_nl,p_indent,p_nl,
                                       'InterfaceBodyDeclarations',
                                       p_dependent[p_nl,p_nl],                                       
                                       p_dedent,'}']

    visit_ClassBodyDeclarations = simplepretty[P_LAST,
        p_repeat[p_dependent[p_nl,p_nl],'ClassBodyDeclaration',
                 ]]

    visit_InterfaceBodyDeclarations = simplepretty[P_LAST,
        p_repeat[p_dependent[p_nl,p_nl],'InterfaceBodyDeclaration',
                 ]]

    def visit_InitBody(self,node,ctxt):
        tracker = NodeVisitTracker(self,node)
        if node.has('STATIC'):
            tracker.visit_cur_forward()
            self.space()
        tracker.visit_cur_forward()

    visit_CompilationUnit = simplepretty[
        'PackageClauseOpt',
        p_dependent[p_nl,p_nl],
        'ImportDeclarations',
        p_dependent[p_nl,p_nl],        
        'TypeDeclarations',      
        ]

    visit_ImportDeclarations =  simplepretty[P_LAST,
        p_repeat[p_dependent[p_nl],'ImportDeclaration',
                 ]]

    visit_TypeDeclarations =  simplepretty[P_LAST,
        p_repeat[p_dependent[p_nl,p_nl],'TypeDeclaration',
                 ]]


def pretty(source,start='Statement',output=None):
    if isinstance(source,java_parser.Node):
        ast = source
    else:
        ast = java_parser.parse(source,start)
    if output is None:
        JavaPretty().visit(ast)
    else:
        JavaPretty(output).visit(ast)

     
# Statement
TEST_STUFF = """
{

  x = xeon(1,2,3);
  
  `a `([`x],[`y,`z]);
  
}
"""

TEST_CREATOR = """
{

  x = xeon(new Beh()) +3 ;
  
  x = xeon(new Beh() {
          public void evoke() {} }) + 2;

  x = new Object[][] { {a,b},
                       {c,d} };

}
"""

TEST_IF = """
{ if (cond) A(); if(cond) {} if (cond) { A(); } 
  if (cond) { A(); { B(); } } 
if (cond) A(); else B(); if(cond) {} else {} if (cond) { A(); } else B(); 
  if (cond) { A(); { B(); } } else B(); } """

TEST_SWITCH = """
{ if(cond) switch(x) {}
  switch(x) { case 0: A(); }
  switch(x) { default: A(); }
  switch(x) { case +1: default: { A(); } }
  switch(x) { case 0: A(); case 1: default: { A(); } }
  switch(x) { case 0: A(); case 1: case 2: { } }
  switch(x) { default: { A(); }  }
} """

TEST_TRY = """
{
  try { a(); } finally {}
  try { a(); } catch(E e) {}
  try { a(); } catch(E e) {} finally {}
  try { a(); } catch(E e) {} catch(F e) {} finally {}
}
"""

# other

TEST_COMPUNIT0 = 'CompilationUnit',"""
package a;

import b.B;
import c.*;

"""


TEST_COMPUNIT = 'CompilationUnit',"""
package a;

import b.B;
import c.*;

public class Y {
}

class X {
}
"""

TEST_COMPUNIT1 = 'CompilationUnit',"""
package a;

public class Y {
}

"""

TEST_METH = 'MethodDecl',"""
public void method(int x,int y) {
} """

TEST_CLASS = 'ClassDeclaration',"""
public class B extends C {

  public int a,b;

  public abstract method x();

  public method y(int[] a) {
  }

  public class Z { }

  {
    while(false) {}
    x();
  }

  static {
    X();
  }

}"""

def test():
    for name,test in globals().items():
        if name.startswith('TEST_'):
            print name
            if isinstance(test,tuple):
                start, source = test
                pretty(source,start)
            else:
                pretty(test)
