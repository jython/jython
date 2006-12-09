# copyright 2004-2005 Samuele Pedroni
import cStringIO

import java_parser
from java_parser import UnknownScheme, make as jast_make
from java_parser import make_id,make_literal
import java_nodes as jast

from java_pretty import JavaPretty, NodeVisitTracker

# templating JavaPretty

class JavaTemplatePretty(JavaPretty):

    bindings = {}
        
    # -*-
    # placeholders, fragments

    def get_binding(self,placeholder_tok):
        return self.bindings.get(placeholder_tok.value[1:],None)

    def make_generic_plh_visit(to_expect,add_semicolon=[],needs_nl=0):
        def plh_visit(self,node,ctxt):
            plh = node.Placeholder.PLACEHOLDER
            binding = self.get_binding(plh)
            if binding:
                paren = []
                if node.Placeholder.has('Fragments'):
                    paren.append(node.Placeholder.PLHSTARTPARMS)
                    args = []
                    frags = node.Placeholder.Fragments
                    for j in range(0,len(frags),2):
                        frag = frags[j]
                        if isinstance(frag,jast.OneProtectedFragment):
                            frag = frag.Fragment
                        args.append(JavaTemplate(frag,bindings=self.bindings))
                    paren.append(node.Placeholder.RPAREN)                        
                else:
                    args = None

                if not isinstance(node.children[0],jast.Placeholder):
                    # CASE, IMPORT ...
                    self.emit_tok(node.children[0],'ia')

                if paren:
                    self.emit_tok(plh,'ia')
                    self.emit_tok(paren[0],'i')
                else:
                    self.emit_tok(plh,'i')                    

                kind,r = binding.tvisit(self,args=args,paren=paren,
                                        expect=to_expect,
                                        ctxt=ctxt)

                if paren:
                    self.emit_tok(paren[1],'a')
                else:
                    self.emit_tok(plh,'a')

                if node.has('SEMICOLON'):
                    if kind in add_semicolon:
                        self.emit_tok(node.SEMICOLON)
                    else:
                        self.emit_tok(node.SEMICOLON,'ia')
                return r
            else:
                r = self.default_visit(node,ctxt)
                if needs_nl:
                    self.nl()
                return r
        return plh_visit
               
    visit_BlockStatementPlaceholder = make_generic_plh_visit(
        ('BlockStatements','Expression'),
        add_semicolon=('Expression',))

    visit_StatementPlaceholder = make_generic_plh_visit(
        ('Statement','Expression'),
        add_semicolon=('Expression',))

    visit_SwitchBlockStatementGroupInSeqPlaceholder = make_generic_plh_visit(
        ('SwitchBlockStatementGroupInSeq',),needs_nl=1)

    visit_ExpressionInSeqPlaceholder = make_generic_plh_visit(
        ('ExpressionInSeq',))

    visit_ClassBodyDeclarationPlaceholder = make_generic_plh_visit(
        ('ClassBodyDeclarations',))

    visit_InterfaceBodyDeclarationPlaceholder = make_generic_plh_visit(
        ('ClassBodyDeclarations',))

    visit_FormalParameterInSeqPlaceholder = make_generic_plh_visit(
        ('FormalParameterInSeq',))

    visit_IdentifierOptPlaceholder = make_generic_plh_visit(
        ('Identifier','Empty'))

    visit_IdentifierPlaceholder = make_generic_plh_visit(
        ('Identifier',))

    visit_TypePlaceholder = make_generic_plh_visit(
        ('TypeOrVOID',))
   
    visit_PrimaryPlaceholder = make_generic_plh_visit(
        ('Primary',))

    visit_SelectorPlaceholder = make_generic_plh_visit(
        ('Selector',))
    

# - * -


_recast_table = {}

def recast(fragment,paren,expect):
    spec = fragment.spec
    for kind in expect:
        recast_func = _recast_table.get((spec,kind),None)
        if recast_func:
            node = recast_func(fragment,paren)
            if node is not None: return kind,node
    raise Exception,"cannot recast %s as %s" % (spec,"|".join(expect))

def fill_recast_table():
   for name,func in globals().items():
       if name.startswith('recast_'):
           spec,kind = name[len('recast_'):].split('__')
           if spec == '':
               spec = ()
           else:
               spec = tuple(spec.split('_'))
           #print name,"->",spec,kind
           _recast_table[(spec,kind)] = func

# fragment recasts

def recast_Identifier__Identifier(frag,paren):
    return frag

def recast_Literal__Primary(frag,paren):
    return jast_make(jast.Primary,Literal=frag)

recast_Literal__ExpressionInSeq = recast_Literal__Primary

def recast_Primary__Primary(frag,paren):
    return frag

def recast_Primary__Expression(frag,paren):
    return frag

def recast_Primary__ExpressionInSeq(frag,paren):
    return frag

def recast_QualifiedIdentifier__TypeOrVOID(frag,paren):
    return jast_make(jast.Type,
                     QualifiedIdentifier = frag,
                     BracketsOpt = jast_make(jast.Brackets))

def recast_QualifiedIdentifier__Identifier(frag,paren):
    if len(frag.QualifiedIdentifier) == 1:
        return frag.QualifiedIdentifier[0]

def recast_QualifiedIdentifier__Primary(frag,paren):
    if len(frag.QualifiedIdentifier) == 1:
        return jast_make(jast.Primary,Identifier=frag.QualifiedIdentifier[0],ArgumentsOpt=None)

def recast_QualifiedIdentifier__ExpressionInSeq(frag,paren):
    if len(frag.QualifiedIdentifier) == 1:
        return jast_make(jast.Primary,Identifier=frag.QualifiedIdentifier[0],ArgumentsOpt=None)

def recast_Placeholder_Selectors__BlockStatements(frag,paren):
    if not frag.Selectors: # !!!
        return jast_make(jast.BlockStatements,
                         jast_make(jast.BlockStatementPlaceholder,frag.Placeholder))

def recast_Placeholder_Selectors__Identifier(frag,paren):
    if not frag.Selectors: # !!!
        return jast_make(jast.IdentifierPlaceholder,frag.Placeholder)

def recast___Identifier(frag,paren):
    return make_id("")

def recast___BlockStatements(frag,paren):
    return jast_make(jast.BlockStatements)

def recast_BlockStatements__BlockStatements(frag,paren):
    return frag.BlockStatements

def recast_Expressions__Expression(frag,paren):
    if len(frag.Expressions) == 1:
        return frag.Expressions[0]

def recast_Expression__Primary(frag,paren):
    return jast_make(jast.Primary,Expression=frag.Expression)

def recast___ExpressionInSeq(frag,paren):
    return jast_make(jast.Expressions)

def recast_Placeholder_Selectors__ExpressionInSeq(frag,paren):
    if not frag.Selectors: # !!!
        return jast_make(jast.ExpressionInSeqPlaceholder,frag.Placeholder)

def recast_Expression__ExpressionInSeq(frag,paren):
    return frag.Expression

def recast_Expressions__ExpressionInSeq(frag,paren):
    return frag.Expressions

def recast___ClassBodyDeclarations(frag,paren):
    return jast_make(jast.ClassBodyDeclarations)

def recast_CLASS_LBRACE_ClassBodyDeclarations_RBRACE__ClassBodyDeclarations(frag,paren):
    # comments !!!    
    return frag.ClassBodyDeclarations

def recast_VOID_LPAREN_FormalParameterListOpt_RPAREN__FormalParameterInSeq(frag,paren):
    # comments !!!
    return frag.FormalParameterListOpt

def recast_Placeholder_Selectors__FormalParameterInSeq(frag,paren):
    if not frag.Selectors: # !!!
        return jast_make(jast.FormalParameterInSeqPlaceholder,frag.Placeholder)

def recast___SwitchBlockStatementGroupInSeq(frag,paren):
    return jast_make(jast.SwitchBlockStatementGroups)

def recast_SwitchBlockStatementGroups__SwitchBlockStatementGroupInSeq(frag,paren):
    return frag.SwitchBlockStatementGroups

fill_recast_table()

# - * -
        
class JavaTemplate:

    def __init__(self,frag,parms='',bindings=None,start='Fragment'):
        if isinstance(frag,java_parser.Node):
            fragment = frag
        else:
            #print "parsing... <<"
            #print frag
            try:
                fragment = java_parser.parse(frag,start=start)
            except java_parser.JavaSyntaxError,e:
                print frag
                raise
            #print ">>"            
        if (not isinstance(fragment,jast.Fragment) and
            not isinstance(fragment,jast.PlaceholderFragment)):
            child_name = fragment.__class__.__name__
            if child_name == 'FormalParameterList': # !!!
                child_name = 'FormalParameterListOpt'
            try:
                fragment = jast_make(jast.Fragment,**{child_name: fragment})
            except UnknownScheme:
                fragment = jast.Fragment((child_name,),[fragment])

        self.fragment = fragment
            
        if not parms:
            self.parms = []
        else:
            if isinstance(parms,str):
	        self.parms = parms.split(':')
            else:
                self.parms = parms
        
        if bindings is None:
            self.bindings = {}
        else:
            self.bindings = bindings

    def _getfirstnt(self):
        i = 0
        for child in self.fragment.children:
            if not isinstance(child,java_parser.Token):
                return child,i
            i += 1
        raise Exception,"at least a non-terminal expected"

    def _getseqnode(self):
        for child in self.fragment.children:
            if isinstance(child,java_parser.Seq):
                return child
        return None
            
    def __add__(self,other):
        if not isinstance(other,JavaTemplate):
            raise Exception,"expected template"
        if self.parms or other.parms or self.bindings or other.bindings:
            raise Exception,"cannot add non bare templates"
        self_seq = self._getseqnode()
        other_seq = other._getseqnode()
        return self.__class__(java_parser.join_seq_nodes(self_seq,other_seq))

    def tfree(self):
        return JavaTemplate(self.fragment,self.parms,{})

    def tnaked(self):
        nt, i = self._getfirstnt()
        kind = self.fragment._spec[i]
        fragment = jast.Fragment((kind,),[nt])
        return JavaTemplate(fragment,self.parms,self.bindings.copy())

    def tbind(self,bindings):
        new_bindings = self.bindings.copy()
        new_bindings.update(bindings)
        return JavaTemplate(self.fragment,self.parms,new_bindings)


    def texpand(self,bindings,output = None,nindent=0):
        if output is None:
            to_string = 1
            output = cStringIO.StringIO()
        else:
            to_string = 0
        pretty = JavaTemplatePretty(output)

        for i in range(nindent):
            pretty.indent()

        self.tvisit(pretty,bindings=bindings)

        for i in range(nindent):
            pretty.dedent()

        if to_string:
            return output.getvalue()
        else:
            return None
 
    def tvisit(self,visitor,args=None,paren=[],bindings=None,expect=None,ctxt=None):
        before = []
        after = []
        if expect:
            kind, node = recast(self.fragment,paren,expect)
            if isinstance(node,tuple):
                before,after,node = node
        else:
            node = self.fragment
        try:
            saved = visitor.bindings
            new_bindings = self.bindings.copy()
            if bindings is not None:
                new_bindings.update(bindings)
            if args is not None:
                i = 0
                for arg in args:
                    new_bindings[self.parms[i]] = arg
                    i += 1
            visitor.bindings = new_bindings
            for tok,ctl in before:
                visitor.emit_tok(tok,ctl)
                
            r = visitor.visit(node,ctxt)
            
            for tok,ctl in after:
                visitor.emit_tok(tok,ctl)           
        finally:
            visitor.bindings = saved
        if expect:
            return kind,r

def texpand(fragment,bindings):
    output = cStringIO.StringIO()
    pretty = JavaTemplatePretty(output)
    pretty.bindings = bindings
    pretty.visit(fragment)
    return output.getvalue()

class Concat:

    def tvisit(self,visitor,args=None,paren=[],bindings=None,expect=None,ctxt=None):
        frags = []
        self_eval = 0
        for arg in args:
            dummy, frag = recast(arg.fragment,[],('Identifier',))
            if isinstance(frag,jast.Identifier):
                frag = frag.IDENTIFIER.value               
            elif isinstance(frag,jast.IdentifierPlaceholder):
                frag = texpand(frag,arg.bindings)
                if not frag:
                    continue
                if frag[0] == "`": # !!!
                    self_eval = 1
            else:
                raise Exception,"can't concat into an identifier: %s" % arg
            
            frags.append(frag)

        if not self_eval:
            frag = ''.join(frags)
            frag = make_id(frag)
        else:
            frag = "`concat`(%s)" % ','.join(frags)

        return JavaTemplate(frag).tvisit(visitor,paren=paren,expect=expect,ctxt=ctxt)

concat = Concat()

class Strfy:

    def tvisit(self,visitor,args=None,paren=[],bindings=None,expect=None,ctxt=None):
        if len(args) != 1:
            raise Exception,"strfy expects one arg"
        self_eval = 0
        arg = args[0]
        dummy, frag = recast(arg.fragment,[],('Identifier',))
        if isinstance(frag,jast.Identifier):
            frag = frag.IDENTIFIER.value               
        elif isinstance(frag,jast.IdentifierPlaceholder):
            frag = texpand(frag,arg.bindings)
            if frag and frag[0] == "`": # !!!
                self_eval = 1
        else:
            raise Exception,"can't recast as identifier for strfy: %s" % arg

        if not self_eval:
            frag = '"%s"' % frag
            frag = make_literal(frag)
        else:
            frag = "`strfy`(%s)" % frag

        return JavaTemplate(frag).tvisit(visitor,paren=paren,expect=expect,ctxt=ctxt)

strfy = Strfy()


class CSub:

    def tvisit(self,visitor,args=None,paren=[],bindings=None,expect=None,ctxt=None):
        if args:
            raise Exception,"csub expects no arguments"
        if not paren:
            raise Exception,"csub expects parenthesis"
        bindings = visitor.bindings
        visitor.emit_tok(paren[0],'a', subst=bindings)
        visitor.emit_tok(paren[1],'i', subst=bindings)
        return None,0

csub = CSub()

def switchgroup(vals,suite):
    vals = [ jast_make(jast.Primary,Literal=make_literal(str(v))) for v in vals ]
    groups = []
    for prim in vals[:-1]:
        lbl = jast_make(jast.SwitchLabel,Expression=prim)
        stmts = jast_make(jast.BlockStatements)
        groups.append(jast_make(jast.SwitchBlockStatementGroup,lbl,stmts))
    groups.append(jast_make(jast.SwitchBlockStatementGroup,
                            jast_make(jast.SwitchLabel,Expression=vals[-1])
                            ,suite))
    return JavaTemplate(jast_make(jast.SwitchBlockStatementGroups,groups))



# - * -


def fragments():
    proto_parser = java_parser.JavaParser()
    to_show = []
    for rule,name in proto_parser.rule2name.items():
        if 'Fragment' in rule[0]:
            to_show.append((
                rule[0],
                "%-50s [%s]"  % ("%s ::= %s" % (rule[0],' '.join(rule[1])),name)))
    to_show.sort(lambda x,y: cmp(x[0],y[0]))
    for rule0,txt in to_show:
        print txt

    

def check():
    c = 0
    supported = 0
    for name in dir(jast):
        if 'Placeholder' in name:
            c  += 1
            if not hasattr(JavaTemplatePretty,'visit_%s' % name):
                print "missing support for %s" % name
            else:
                supported += 1
    print "%s/%s" % (supported,c)



# - * -

def jt(s,parms=''):
    #print "TEMPL",s
    return JavaTemplate(s,parms)

def gen(vals,nil=''):
    n = len(vals)
    cases = []
    for i in range(2**n):
        j = 1
        case = []
        for c in range(n):
            if i&j:
                case.append(vals[c])
            else:
                case.append(nil)
            j *= 2
        cases.append(tuple(case))
    return cases

def commas(templ):
    return ','.join(templ.split())

def test4():
    jt = JavaTemplate("int `cat`(a,`x);")
    assert jt.texpand({'cat': concat, 'x': JavaTemplate('b')}) == 'int ab;';
    jt = JavaTemplate("int `cat`(a,`cat`(b,`x));")  
    assert jt.texpand({'cat': concat, 'x': JavaTemplate('c')}) == 'int abc;';
    jt = JavaTemplate("int `cat`(a,`cat`(b,`x`(c)));")
    assert jt.texpand({'cat': concat, 'x':
                       JavaTemplate('`y',parms='y')}) == 'int abc;';
    jt = JavaTemplate("int `cat`(a,`cat`(b,`x`(c)));")
    assert jt.texpand({'cat': concat, 'x':
                       JavaTemplate('`cat`(y,`y)',
                                    bindings={'cat': concat},parms='y')}) == 'int abyc;';
    
def test3():
    templ=jt("{ `a`([`b],1); }")
    inner=jt("{ `a(`b);  }","a:b")
    print templ.texpand({'a': inner,'b': jt('foo')})
                        
def test2():
    templs = ["`x `y","a `x `y","`x  a `y","`x `y a"]

    def subst(templ,x,y):
        return (templ.replace("`x",x)
                     .replace("`y",y))

    frags = []
    for xx,xy,yx,yy in gen(['1','2','3','4']):
        frags.append((xx,jt(xx),xy,jt(xy),yx,jt(yx),yy,jt(yy)))
        
    for top in templs:
        ttop = jt(subst(commas(top),"`x`([`xx],[`xy])","`y`([`yx],[`yy])"))
        for x in templs:
            for y in templs:
                tx = jt(commas(x),"x:y")
                ty = jt(commas(y),"x:y")
                for xx,txx,xy,txy,yx,tyx,yy,tyy in frags:
                    x1 = subst(x,xx,xy)
                    y1 = subst(y,yx,yy)
                    top1 = subst(top,x1,y1)
                    expected = commas(top1)
                    bindings = {
                        'x': tx,
                        'y': ty, }
                    res = ttop.texpand(bindings)
                    assert (',' not in (res[0],res[-1])
                            and ' ' not in res
                            and ',,' not in res)
                    bindings = {
                        'x': tx,
                        'y': ty,
                        'xx': txx,
                        'xy': txy,
                        'yx': tyx,
                        'yy': tyy }
                    res = ttop.texpand(bindings)
                    assert expected == res

def test1():
    frags = []
    for triplet in gen(['1','2','3']):
        ttriplet = map(jt,triplet)
        frags.append((triplet,ttriplet))
    for fixed in gen(['a','b','c','d']):
        ex = "%s `a %s `b %s `c %s" % fixed
        tex = jt(commas(ex))
        for triplet,ttriplet in frags:
            expected = (ex.replace("`a",triplet[0])
                     .replace("`b",triplet[1])
                     .replace("`c",triplet[2]))
            expected =  commas(expected)
            res = tex.texpand(dict(zip(('a','b','c'),ttriplet)))
            assert expected == res

def test():
    print jt("{ a(); b(); }").texpand({})

    templs =  [jt("{ `a; }"),jt("{ x(); `a; }"),
                      jt("{ `a; y(); }"),jt("{ x(); `a; y(); }")]
    for frag in [jt("{ a(); b(); }"),jt("`b"),jt(""),
                     jt("a();"),jt("a(); b();")]:
        for templ in templs:
            print templ.texpand({'a': frag})

    templs = [jt("1,2,`x,4"),jt("`x,4"),jt("1,`x"),jt("1,`x,4")]
    for frag in [jt(""),jt("3"),jt("3,3")]:
        for tex in templs:
            print tex.texpand({'x': frag})

    tcl = jt("class A { `a; }")
    tcl1 = jt("class A { `a; static {} }")
    tintf = jt("interface I { `a; }")
    tintf1 = jt("interface I { void method(); `a; }")

    for frag in [jt(""),jt("class { final int A = 2;}"),
                  jt("class { final int A = 2; final int B = 3; }")]:
        for templ in [tcl,tintf,tcl1,tintf1]:
            print templ.texpand({'a': frag})
                  
    tmh0 = jt("interface I { int m(`x); }")
    tmh1 = jt("interface I { int m(`x,int b); }")
    tmh2 = jt("interface I { int m(int a,`x); }")
    tmh3 = jt("interface I { int m(int a,`x,int b); }")

    for frag in [jt("void()"),jt("void(int x)"),
                  jt("void(int x,int y)")]:
        for templ in [tmh0,tmh1,tmh2,tmh3]:
            print templ.texpand({'x': frag})

    with_comments = []
    
    with_comments.append(JavaTemplate("""
{
 {
 /*
  ok
 */
 }
}
"""))

    
    with_comments.append(JavaTemplate("""
{
 {
   `csub`(
   /*
      %(ok)s
   */);
 }
}
"""))

    with_comments.append(JavaTemplate("""
{
 {
   `csub`(
   /*
      %(ok)s
   */);
   break;
 }
}
"""))

    with_comments.append(JavaTemplate("""
{
 {
   invoke(a,`csub`(/* %(ok)s */),b);
   break;
 }
}
"""))

    for templ in with_comments:
        print templ.texpand({'csub': csub, 'ok': "OK"})

    print (JavaTemplate("a(); b();")+JavaTemplate("c(); d();")).texpand({}) 
    print (JavaTemplate("a,b")+JavaTemplate("c,d")).texpand({})
    
    test1()
    print 'TEST1'
    test2()
    print 'TEST2'
    test3()
    print 'TEST3'
    test4()
    print 'TEST4'    
