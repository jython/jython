# copyright 2004-2005 Samuele Pedroni
"""
 Java parser
"""

import sys
import new

from spark import GenericParser

from java_lexer import JavaLexer,Token,java_tokens

# helpers for grammar rules

def op(term,lhs,operator,rhs):
    return " %s ::= %s %s %s" % (term,lhs,operator,rhs)

def orelse(*rules):
    return '\n'.join(rules)

def ops(term,lhs,operators,rhs):
    return orelse(*[op(term,lhs,operator,rhs) for operator in operators.split()])

def opexpr(term,operators,subterm):
    return orelse(
        " %s ::= %s" % (term,subterm),
        ops(term,term,operators,subterm))

def opt(term,subterm):
    return orelse(
        " %s ::= " % term,
        " %s ::= %s " % (term,subterm))

def seq(term,interleave,subterm):
    return orelse(
        " %s ::= %s " % (term,subterm),
        " %s ::= %s %s %s" % (term,term,interleave,subterm))

# ast node bases

_term_tables_cache = {}

def build_term_table(spec):
    try:
        return _term_tables_cache[spec]
    except KeyError:
        tbl = {}
        terms = {}
        i = 0
        for t in spec:
            which = terms.setdefault(t,0)
            tbl[t,which] = i
            tbl['%s_%d' % (t,which)] = i
            if which == 0:
                tbl[t] = i
            terms[t] += 1
            i += 1
        _term_tables_cache[spec] = tbl
        return tbl

EMPTYTABLE = {}    

class Node:
    def __init__(self,spec,args):
        self._spec = spec
        # term-name | term_name+'_'+index
        #  | (term-name,index) -> index into children        
        self._term_table = EMPTYTABLE
        self.children = args

    def has(self,*at):
        if len(at) == 1:
            at = at[0]
        tbl = self._term_table
        if tbl is EMPTYTABLE:
            tbl = self._term_table = build_term_table(self._spec)
        return tbl.has_key(at)

    def __getattr__(self,term):
        if term == 'spec': return self._spec
        try:
            return self.children[self._term_table[term]]
        except KeyError:
            self._term_table = build_term_table(self._spec)
            try:
                return self.children[self._term_table[term]]
            except KeyError:
                raise AttributeError,term

    def __getitem__(self,at): # at := term-name [+'_'+index] |(term-name,index)
        try:
            return self.children[self._term_table[at]]
        except KeyError:
            self._term_table = build_term_table(self._spec)
            return self.children[self._term_table[at]]
        
    def __repr__(self):
        return "(%s %s)" % (self.__class__.__name__,self.children)

class Seq(Node):
    def __init__(self,spec,args):
        if args and args[0].__class__ is self.__class__:
            children = args[0].children[:]
            children.extend(args[1:])
        else:
            children = args
        Node.__init__(self,"*",children)

    def __getitem__(self,index):
        return self.children[index]

    def __len__(self):
        return len(self.children)

# markers

class Statement(Node):
    pass

class TakesBlock(Statement):
    pass

# java_nodes synthetic module populated with node classes

java_nodes = new.module('java_nodes')
_node_classes = java_nodes.__dict__
sys.modules['java_nodes'] = java_nodes

def node_maker(name,supercl=None):
    cl = _node_classes.get(name,None)
    if cl is None:
        cl = new.classobj(name,(globals()[supercl],),{'__module__': 'java_nodes'})
        _node_classes[name] = cl
    return cl

# classes to specify actions (i.e. ast construction) for grammar rules;
# implicitly define node classes

class Dummy:
    def pre(self,name):    # name is rule name
        pass
    def make_action(self): # return action function
        def action(self,spec,args):
            print args
        return action

class Nop:
    # pass through node or default class([]) or None, default is a string, class name
    def __init__(self,default=None):
        self.default = default

    def pre(self,name):
        pass

    def make_action(self):
        if self.default is None:
            default = lambda spec,args: None
        else:
            default = node_maker(self.default)
        def action(self,spec,args):
            if not args: return default(spec,[])
            return args[0]
        return action
        
class Make:
    # instatiate (class `RuleName`(supercl))(spec,args)
    # implicitly defines class `RuleName`
    def __init__(self,supercl,name=None,spec=None):
        self.supercl = supercl
        self.name = name
        if spec is not None:
            self.spec = tuple(spec.split()) # str -> tuple
        else:
            self.spec = None

    def pre(self,name):
        name = self.name or name
        self.make = node_maker(name,self.supercl)

    def make_action(self): # decorate action with _spec
        action = self.make_bare_action()
        action._spec = self.spec
        action.making = self.name
        return action

    def make_bare_action(self):
        make = self.make
        def action(self,spec,args):
            node = make(spec,args)
            #print node
            return node
        return action

class Operation(Make):
    # instatiate (class `RuleName`(Node)) with (spec,args)
    # except when just one arg, then pass it through:
    # for precedence expression operation grammar rules.
    # implicitly defines class `RuleName`   
    def __init__(self,name=None,spec="lhs op rhs"):
        Make.__init__(self,'Node',name,spec)
        
    def make_bare_action(self):
        make = self.make
        def action(self,spec,args):
            if len(args) == 1: return args[0]
            node = make(spec,args)
            #print node
            return node
        return action

# a_rules  tuple (grammar_rule,(instance of class with make_action))
# to p_funcs but expecting a spec arg too; add preprocessing
# to make them work

def setup_a_rules_to_p_funcs(ns):
    rules = []
    for name,val in ns.items():
        if name.startswith('a_'):
            name = name[2:]
            rule,action_maker = val
            # call all pre before all make_action, so that synth classes are
            # pre-defined (in pre methods) and can be used freely by make_actions
            action_maker.pre(name)
            rules.append((name,rule,action_maker))
            
    for name,rule,action_maker in rules:
        action = action_maker.make_action()
        name = 'p_%s' % name
        # cannot set __name__ on a function and spark uses func.__name__ to gather rule name so
        ns[name] = new.function(action.func_code,action.func_globals,name,action.func_defaults,action.func_closure)
        if hasattr(action,'_spec'): # copy _spec
            ns[name]._spec = action._spec
        if hasattr(action,'making'): # copy making
            ns[name].making = action.making
        ns[name].__doc__ = rule

    # add preprocessing, produced p_funcs expect a spec argument
    # wrap them appropriately
    def preprocess(self,rule,func):
        if hasattr(func,'_spec') and func._spec is not None:
            spec = func._spec
        else:
            spec = rule[1]
        return rule,lambda args: func(spec,args)

    ns['preprocess'] = preprocess

class JavaSyntaxError(Exception):
    pass

# !!! split placeholder logic into subclass
class JavaParser(GenericParser):
    def __init__(self, start='Statement'):
        GenericParser.__init__(self, start)

    resolve_table = {
        ('BlockStatement', 'BlockStatementPlaceholder'): 'BlockStatementPlaceholder',
        ('Statement', 'StatementPlaceholder'): 'StatementPlaceholder',
        ('ExpressionInSeq', 'ExpressionInSeqPlaceholder'): 'ExpressionInSeqPlaceholder',
        ('TypePlaceholder', 'Type'): 'TypePlaceholder',
        ('TypeInSeq', 'TypeInSeqPlaceholder'): 'TypeInSeqPlaceholder',
        ('IdentifierOpt', 'IdentifierOptPlaceholder'): 'IdentifierOptPlaceholder',
        ('PrimaryPlaceholder','Primary'): 'PrimaryPlaceholder',

        # fragment(s)
        ('EmptyFragment', 'Fragment'): 'EmptyFragment',
        ('ExpressionSimpleFragment', 'QualIdFragment', 'SimpleFragment'): 'QualIdFragment',
        ('Fragment', 'NotSimpleFragment'): 'Fragment',
        ('OneUnprotectedFragment', 'OneProtectedFragment') : 'OneProtectedFragment',
        ('NoFragments','Fragments'): 'NoFragments',
        ('Fragments','ManyFragments') : 'ManyFragments'
    }

    def error(self, token):
        raise JavaSyntaxError, "Syntax error at or near `%s' token, line %d" % (token, token.lineno)


    def resolve(self,list):
        resolved = self.resolve_table.get(tuple(list),None)
        if resolved is None:
            if 'PlaceholderFragment' in list:
                resolved = 'PlaceholderFragment'
            else:
                print 'AMB',list,'DEFAULT!',list[0]
                return list[0]
        return resolved
    
    a_Identifier = (
        " Identifier ::= IDENTIFIER ", Make('Node')
    )

    a_IdentifierOpt = (
       opt('IdentifierOpt','Identifier'), Nop()
    )

    a_IdentifierOptPlaceholder = (
       " IdentifierOpt ::= Placeholder ", Make('Node')
    )
    

    a_Placeholder = ( # placeholders
        """
         Placeholder ::= PLACEHOLDER
         Placeholder ::= PLACEHOLDER PLHSTARTPARMS Fragments RPAREN
        """, Make('Node')
    )


    a_IdentifierPlaceholder = (  # placeholders, in context of  QualId should be replaceable with QualId
        " Identifier ::= Placeholder ", Make('Node')
    )

    a_QualifiedIdentifier = (
        """
         QualifiedIdentifier ::= Identifier
         QualifiedIdentifier ::= QualifiedIdentifier DOT Identifier
        """, Make('Seq')
    )

    a_Literal = (
        ops('Literal','',"INTEGER_LITERAL FLOATING_POINT_LITERAL CHARACTER_LITERAL STRING_LITERAL "+
            "BOOLEAN_LITERAL NULL_LITERAL",''), Make('Node')
    )

    a_BasicType = (
        ops('BasicType','',"BYTE SHORT CHAR INT LONG FLOAT DOUBLE BOOLEAN",''), Make('Node')
    )

    a_Type = (
        """
         Type ::= QualifiedIdentifier BracketsOpt
         Type ::= BasicType BracketsOpt
        """, Make('Node')
    )

    a_TypePlaceholder = ( # placeholders
        """
         Type ::= Placeholder
        """, Make('Node')
    )

    a_Expression = (
        """
         Expression ::= ConditionalExpression
         Expression ::= AssignmentExpression
        """, Nop()
    )

    a_ExpressionOpt = (
       opt('ExpressionOpt','Expression'), Nop()
    )

    a_AssignmentExpression = (
        ops('AssignmentExpression','ConditionalExpression',
            "EQ PLUSEQ MINUSEQ MULTEQ DIVEQ MODEQ ANDEQ OREQ XOREQ LSHIFTEQ "+
            "RSHIFTEQ URSHIFREQ",
            'Expression'), Make('Node',spec="lhs op rhs")
    )

    a_ConditionalExpression = (
        """
         ConditionalExpression ::= ConditionalOrExpression
         ConditionalExpression ::= ConditionalOrExpression QUESTION Expression COLON ConditionalExpression
        """, Operation(spec=None)
    )

    a_ConditionalOrExpression = (
        opexpr('ConditionalOrExpression',"OROR",'ConditionalAndExpression'), Operation()
    )

    a_ConditionalAndExpression = (
        opexpr('ConditionalAndExpression',"ANDAND",'InclusiveOrExpression'), Operation()
    )

    a_InclusiveOrExpression = (
        opexpr('InclusiveOrExpression',"OR",'ExclusiveOrExpression'), Operation()
    )
    
    a_ExclusiveOrExpression = (
        opexpr('ExclusiveOrExpression',"XOR",'AndExpression'), Operation()
    )

    a_AndExpression = (
        opexpr('AndExpression',"AND",'EqualityExpression'), Operation()
    )

    a_EqualityExpression = (
        opexpr('EqualityExpression',"EQEQ NOTEQ",'RelationalExpression'), Operation()
    )

    a_RelationalExpression = (
        orelse(
            opexpr('RelationalExpression',"LT GT LTEQ GTEQ",'ShiftExpression'),
            " RelationalExpression ::= RelationalExpression INSTANCEOF Type "), Operation()
    )

    a_ShiftExpression = (
        opexpr('ShiftExpression',"LSHIFT RSHIFT URSHIFT",'AdditiveExpression'), Operation()
    )

    a_AdditiveExpression = (
        opexpr('AdditiveExpression',"PLUS MINUS",'MultiplicativeExpression'), Operation()
    )

    a_MultiplicativeExpression = (
        opexpr('MultiplicativeExpression',"MULT DIV MOD",'UnaryExpression'), Operation()
    )

    a_UnaryExpression = (
        """
         UnaryExpression ::= PrefixExpression
         UnaryExpression ::= CastExpression
         UnaryExpression ::= PostfixExpression
        """, Nop()
    )

    a_PrefixExpression = (
       ops('PrefixExpression','',"PLUSPLUS MINUSMINUS NOT COMP PLUS MINUS",'UnaryExpression'),
        Make('Node',spec="op operand")
    )

    a_CastExpression = (
        " CastExpression ::= LPAREN Type RPAREN UnaryExpression ", Make('Node')
    )

    a_PostfixExpression = (
        """
         PostfixExpression ::= Primary
         PostfixExpression ::= PostfixExpression PLUSPLUS
         PostfixExpression ::= PostfixExpression MINUSMINUS
        """, Operation(spec="operand op")
    )

    a_Primary = ( # !!! split?
        # there was superfluous Primary ::= QualifiedIdentifier
        """
          Primary ::= LPAREN Expression RPAREN
          Primary ::= THIS ArgumentsOpt
          Primary ::= SUPER ArgumentsOpt
          Primary ::= Literal
          Primary ::= NEW Creator
          Primary ::= Identifier ArgumentsOpt
          Primary ::= Type DOT CLASS
          Primary ::= VOID DOT CLASS
          Primary ::= Primary Selector
        """, Make('Node')
    )

    a_PrimaryPlaceholder = ( # placeholders
        " Primary ::= Placeholder ", Make('Node')
    )

    a_Selector = ( # !!! split?
        """
         Selector ::= DOT Identifier ArgumentsOpt
         Selector ::= DOT THIS
         Selector ::= DOT SUPER ArgumentsOpt
         Selector ::= DOT NEW InnerCreator
         Selector ::= LBRACK Expression RBRACK
        """, Make('Node')
    )

    a_SelectorPlaceholder = ( # placeholders
        " Selector ::= Placeholder ", Make('Node')
    )

    a_ArgumentsOpt = (
        opt('ArgumentsOpt','Arguments'), Nop()
    )

    a_Arguments = (
        " Arguments ::= LPAREN ExpressionsOpt RPAREN ", Make('Node')
    )

    a_ExpressionsOpt = (
        opt('ExpressionsOpt','Expressions'), Nop('Expressions')
    )

    a_Expressions = (
        seq('Expressions',"COMMA",'ExpressionInSeq'), Make('Seq')
    )

    a_ExpressionInSeq = (
        " ExpressionInSeq ::= Expression ", Nop()
    )

    a_ExpressionInSeqPlaceholder = ( # placeholders
        " ExpressionInSeq ::= Placeholder" , Make('Node')
    )

    a_BracketsOpt = (
        opt('BracketsOpt','Brackets'),Nop('Brackets')
    )

    a_Brackets = (
        seq('Brackets','',"LBRACK RBRACK"), Make('Seq')
    )

    a_Creator = (
        """
         Creator ::= ClassCreator
         Creator ::= ArrayCreator
        """, Nop()
    )

    a_InnerCreator = (
        """
         InnerCreator ::= ClassCreator
        """, Make('Node')
    )

    a_ClassCreator = (
        " ClassCreator ::= QualifiedIdentifier Arguments ClassBodyOpt ", Make('Node')
    )

    a_BaseArrayType = (
        """
         BaseArrayType ::= QualifiedIdentifier
         BaseArrayType ::= BasicType
        """, Make('Node')
    )

    a_ArrayCreator = (
        """
         ArrayCreator ::= BaseArrayType Brackets ArrayInitializer
         ArrayCreator ::= BaseArrayType Dims BracketsOpt
        """, Make('Node')
    )

    a_Dims = (
        seq('Dims','','Dim'), Make('Seq')
    )

    a_Dim = (
        " Dim ::= LBRACK Expression RBRACK ", Make('Node')
    )

    a_VariableInitializer = (
        """
         VariableInitializer ::= Expression
         VariableInitializer ::= ArrayInitializer
        """, Make('Node')
    )

    a_ArrayInitializer = (
        " ArrayInitializer ::= LBRACE InitializerList RBRACE ", Make('Node')
    )

    a_InitializerList0 = (
        seq('InitializerList0','COMMA','VariableInitializer'),Make('Seq',name='InitializerList')
    )

    a_InitializerList = (
        """
          InitializerList ::= 
          InitializerList ::= InitializerList0
          InitializerList ::= InitializerList0 COMMA
        """, Make('Seq',name='InitializerList')
    )    
             
    a_ParExpression = (
        " ParExpression ::= LPAREN Expression RPAREN ", Make('Node')
    )

    a_Statement = (
        """
         Statement ::= Block
         Statement ::= IfStatement
         Statement ::= ForStatement
         Statement ::= WhileStatement
         Statement ::= DoWhileStatement                    
         Statement ::= SwitchStatement
         Statement ::= TryStatement
         Statement ::= SynchronizedStatement
         Statement ::= ReturnStatement
         Statement ::= ThrowStatement
         Statement ::= BreakStatement
         Statement ::= ContinueStatement
         Statement ::= EmptyStatement
         Statement ::= LabeledStatement
         Statement ::= ExpressionStatement
        """,        
        Nop()
    )

    a_StatementPlaceholder = ( # placeholders
        """
         Statement ::= Placeholder SEMICOLON
        """, Make('Node')
    )

    a_Statement_IfElse = (
        """
          Statement ::= IfElseStatement
        """, Nop()
    )

    a_ExpressionStatement = (
        " ExpressionStatement ::= Expression SEMICOLON ", Make('Statement')
    )

    a_IfStatement = (
        " IfStatement ::= IF ParExpression Statement ", Make('TakesBlock')
    )

    a_IfElseStatement = (
        " IfElseStatement ::= IF ParExpression Statement ELSE Statement ",
        Make('TakesBlock')
    )

    a_ForStatement = (
        " ForStatement ::= FOR LPAREN ForInit SEMICOLON ExpressionOpt SEMICOLON ForUpdate RPAREN Statement ",
        Make('TakesBlock')
    )

    a_WhileStatement = (
        " WhileStatement ::= WHILE ParExpression Statement ", Make('TakesBlock')
    )

    a_DoWhileElseStatement = (
        " DoWhileStatement ::= DO  Statement WHILE ParExpression SEMICOLON ",
        Make('TakesBlock')
    )

    a_SynchronizedStatement = (
        " SynchronizedStatement ::= SYNCHRONIZED ParExpression Statement ", Make('TakesBlock')
    )
      
    a_ReturnStatement = (
        " ReturnStatement ::= RETURN ExpressionOpt SEMICOLON ", Make('Statement')
    )

    a_ThrowStatement = (
        " ThrowStatement ::= THROW Expression SEMICOLON ", Make('Statement')
    )

    a_BreakStatement = (
        " BreakStatement ::= BREAK IdentifierOpt SEMICOLON ", Make('Statement')
    )

    a_ContinueStatement = (
        " ContinueStatement ::= CONTINUE IdentifierOpt SEMICOLON ", Make('Statement')
    )

    a_EmptyStatement = (
        " EmptyStatement ::= SEMICOLON ", Make('Statement')
    )

    a_LabeledStatement = (
        " LabeledStatement ::= Identifier COLON Statement ", Make('TakesBlock')
    )

    a_ForInit = (
        """
         ForInit ::= ExpressionsOpt
         ForInit ::= VariableDecls
        """, Make('Node')
    )

    a_ForUpdate = (
        " ForUpdate ::= ExpressionsOpt ", Make('Node')
    )
        
    a_SwitchStatement = (
        " SwitchStatement ::= SWITCH ParExpression LBRACE SwitchBlockStatementGroups RBRACE ",
        Make('Statement')
    )

    a_SwitchBlockStatementGroups = (
        seq('SwitchBlockStatementGroups','SwitchBlockStatementGroupInSeq',''),
        Make('Seq')
    )

    a_SwitchBlockStatementGroupInSeq = (
        " SwitchBlockStatementGroupInSeq ::= SwitchBlockStatementGroup", Nop()
    )

    a_SwitchBlockStatementGroupInSeqPlaceholder = ( # placeholders
        " SwitchBlockStatementGroupInSeq ::= CASE Placeholder SEMICOLON" , Make('Node')
    )


    a_SwitchBlockStatementGroup = (
        " SwitchBlockStatementGroup ::= SwitchLabel BlockStatements ",
        Make('Node')
    )

    a_SwitchLabel = (
        """
         SwitchLabel ::= CASE Expression COLON
         SwitchLabel ::= DEFAULT COLON
        """, Make('Node')
    )

    a_Block = (
        " Block ::= LBRACE BlockStatements RBRACE ", Make('Node')
    )

    a_BlockStatements = (
        seq('BlockStatements','BlockStatement',''), Make('Seq')
    )

    a_BlockStatement = (
        """
         BlockStatement ::= Statement
         BlockStatement ::= LocalVariableDeclarationStatement
         BlockStatement ::= ClassOrInterfaceDeclaration
        """, Nop()
    )

    a_LocalVariableDeclarationStatement = (
        " LocalVariableDeclarationStatement ::= VariableDecls SEMICOLON ", Make('Statement')
    )

    a_BlockStatementPlaceholder = ( # placeholders
        " BlockStatement ::= Placeholder SEMICOLON ", Make('Node')
    )

    a_TryStatement = ( # !!! split?
        """
         TryStatement ::= TRY Block Catches
         TryStatement ::= TRY Block FinallyClause
         TryStatement ::= TRY Block Catches FinallyClause
        """, Make('Statement')
    )

    a_Catches = (
        seq('Catches','','CatchClause'), Make('Seq')
    )

    a_CatchClause = (
        " CatchClause ::= CATCH LPAREN FormalParameter RPAREN Block ",
        Make('Node')
    )

    a_FinallyClause = (
        " FinallyClause ::= FINALLY Block ",
        Make('Node')
    )    

    a_FormalParameters = (
        " FormalParameters ::= LPAREN FormalParameterListOpt RPAREN ", Make('Node')
    )

    a_FormalParameterListOpt = (
       opt('FormalParameterListOpt','FormalParameterList'),Nop('FormalParameterList')
    )

    a_FormalParameterList = (
        seq('FormalParameterList','COMMA','FormalParameterInSeq'), Make('Seq')
    )

    a_FormalParameterInSeq = (
        " FormalParameterInSeq ::= FormalParameter ", Nop()
    )

    a_FormalParameterInSeqPlaceholder = (
        " FormalParameterInSeq ::= Placeholder ", Make('Node')
    )

    a_FormalParameter = (
        """
         FormalParameter ::= Type VariableDeclaratorId 
         FormalParameter ::= FINAL Type VariableDeclaratorId
        """, Make('Node')
    )

    a_VariableDecls = (
        """
         VariableDecls ::= Type VariableDeclarators
         VariableDecls ::= FINAL Type VariableDeclarators
        """,
        Make('Node')
    )

    a_VariableDeclarators = (
        seq('VariableDeclarators',"COMMA",'VariableDeclaratorInSeq'), Make('Seq')
    )

    a_VariableDeclaratorInSeq = (
        " VariableDeclaratorInSeq ::= VariableDeclarator ", Nop()
    )

    a_VariableInitializingOpt = (
        opt('VariableInitialingOpt','VariableInitialing'), Nop()
    )

    a_VariableInitializing = (
        " VariableInitialing ::= EQ VariableInitializer ", Make('Node')
    )

    a_VariableDeclarator = (
       " VariableDeclarator ::= VariableDeclaratorId VariableInitialingOpt ",Make('Node')
    )

    a_VariableDeclaratorId = (
        " VariableDeclaratorId ::= Identifier BracketsOpt ", Make('Node')
    )

    a_ModifiersOpt = (
        opt('ModifiersOpt','Modifiers'), Nop('Modifiers')
    )

    a_Modifiers = (
        seq('Modifiers','','Modifier'), Make('Seq')
    )

    a_Modifier = (
        ops('Modifier','',
            """
             PUBLIC PROTECTED PRIVATE STATIC
             ABSTRACT FINAL NATIVE SYNCHRONIZED
             TRANSIENT VOLATILE STRICTFP
            """,''), Make('Node')
    )

    a_ClassOrInterfaceDeclaration = (
        """
         ClassOrInterfaceDeclaration ::= ClassDeclaration
         ClassOrInterfaceDeclaration ::= InterfaceDeclaration
        """, Nop()
    )

    a_ClassExtendsClauseOpt = (
        opt('ClassExtendsClauseOpt','ClassExtendsClause'), Nop()
    ) 

    a_InterfaceExtendsClauseOpt = (
        opt('InterfaceExtendsClauseOpt','InterfaceExtendsClause'), Nop()
    )       

    a_ClassImplementsClauseOpt = (
        opt('ClassImplementsClauseOpt','ClassImplementsClause'), Nop()
    )       

    a_ClassExtendsClause = (
        " ClassExtendsClause ::= EXTENDS Type", Make('Node')
    )

    a_InterfaceExtendsClause = (
        " InterfaceExtendsClause ::= EXTENDS TypeList", Make('Node')
    )
    
    a_ClassImplementsClause = (
        " ClassImplementsClause ::= IMPLEMENTS TypeList", Make('Node')
    )

    a_TypeList = (
        seq('TypeList','COMMA','TypeInSeq'),Make('Seq')
    )

    a_TypeInSeq = (
        " TypeInSeq ::= Type ", Nop()
    )
    
    a_ClassDeclaration = (
        """
         ClassDeclaration ::= ModifiersOpt CLASS Identifier ClassExtendsClauseOpt ClassImplementsClauseOpt ClassBody
        """, Make('Node')
    )

    a_InterfaceDeclaration = (
        " InterfaceDeclaration ::= ModifiersOpt INTERFACE Identifier InterfaceExtendsClauseOpt InterfaceBody ",
        Make('Node')
    )

    a_ClassBodyOpt = (
        opt('ClassBodyOpt','ClassBody'), Nop()
    )

    a_ClassBody = (
        " ClassBody ::= LBRACE ClassBodyDeclarations RBRACE ", Make('Node')
    )

    a_InterfaceBody = (
        " InterfaceBody ::= LBRACE InterfaceBodyDeclarations RBRACE ", Make('Node')
    )
    
    a_ClassBodyDeclarations = (
        seq('ClassBodyDeclarations','ClassBodyDeclaration',''), Make('Seq')
    )

    a_InterfaceBodyDeclarations = (
        seq('InterfaceBodyDeclarations','InterfaceBodyDeclaration',''), Make('Seq')
    )

    a_ClassBodyDeclaration = (
        """
          ClassBodyDeclaration ::= EmptyDecl
          ClassBodyDeclaration ::= InitBody
          ClassBodyDeclaration ::= MethodPureDecl
          ClassBodyDeclaration ::= MethodDecl
          ClassBodyDeclaration ::= FieldDecl
          ClassBodyDeclaration ::= ConstructorDecl
          ClassBodyDeclaration ::= ClassOrInterfaceDeclaration
        """, Nop()
    )

    a_ClassBodyDeclarationPlaceholder = (
        """
          ClassBodyDeclaration ::= Placeholder SEMICOLON
        """, Make('Node')
    )

    a_InterfaceBodyDeclaration = (
        """
          InterfaceBodyDeclaration ::= EmptyDecl
          InterfaceBodyDeclaration ::= MethodPureDecl
          InterfaceBodyDeclaration ::= FieldDecl
          InterfaceBodyDeclaration ::= ClassOrInterfaceDeclaration          
        """, Nop()
    )

    a_InterfaceBodyDeclarationPlaceholder = (
        """
          InterfaceBodyDeclaration ::= Placeholder SEMICOLON
        """, Make('Node')
    )


    a_EmptyDecl = (
        " EmptyDecl ::= SEMICOLON ", Make('Node')
    )

    a_InitBody = (
        """
         InitBody ::= Block
         InitBody ::= STATIC Block
        """, Make('Node')
    )

    a_MethodPureDecl = (
        " MethodPureDecl ::=  MethodHeader SEMICOLON ", Make('Node')
    )        

    a_MethodDecl = (
        " MethodDecl ::=  MethodHeader MethodBody ", Make('Node')
    )        

    a_ConstructorDecl = (
        " ConstructorDecl ::=  ConstructorHeader MethodBody ", Make('Node')
    )        

    a_FieldDecl = (
        " FieldDecl ::= ModifiersOpt Type VariableDeclarators SEMICOLON",
        Make('Node')
    )

    a_ThrowsClauseOpt = (
        opt('ThrowsClauseOpt','ThrowsClause'), Nop()
    )       

    a_ThrowsClause = (
        " ThrowsClause ::= THROWS TypeList", Make('Node')
    )

    a_ConstructorHeader = (
        " ConstructorHeader ::= ModifiersOpt Identifier FormalParameters ThrowsClauseOpt ", Make('Node')
    )

    a_MethodHeader = (
        """
         MethodHeader ::= ModifiersOpt Type Identifier FormalParameters BracketsOpt ThrowsClauseOpt
         MethodHeader ::= ModifiersOpt VOID Identifier FormalParameters ThrowsClauseOpt         
        """, Make('Node')
    )

    a_MethodBody = (
        " MethodBody ::= Block ", Make('TakesBlock')
    )

    a_CompilationUnit = (
        " CompilationUnit ::= PackageClauseOpt ImportDeclarations TypeDeclarations ", Make('Node')
    )

    a_PackageClauseOpt = (
        opt('PackageClauseOpt','PackageClause'), Nop()
    )

    a_PackageClause = (
        " PackageClause ::= PACKAGE QualifiedIdentifier SEMICOLON ", Make('Node')
    )

    a_ImportDeclarations = (
        seq('ImportDeclarations','ImportDeclarationInSeq',''), Make('Seq')
    )

    a_ImportDeclarationInSeq = (
        " ImportDeclarationInSeq ::= ImportDeclaration ", Nop()
    )

    a_ImportDeclarationInSeqPlaceholder = (
        " ImportDeclarationInSeq ::= IMPORT Placeholder SEMICOLON ", Make('Node')
    )

    a_ImportDeclaration = (
        " ImportDeclaration ::= IMPORT QualifiedIdentifier SEMICOLON ", Make('Node')
    )

    a_ImportAllDeclaration = (
        " ImportDeclaration ::= IMPORT QualifiedIdentifier DOT MULT SEMICOLON", Make('Node')
    )

    a_TypeDeclarations = (
        seq('TypeDeclarations','TypeDeclarationInSeq',''), Make('Seq')
    )

    a_TypeDeclarationInSeq = (
        " TypeDeclarationInSeq ::= TypeDeclaration ", Nop()
    )

    a_TypeDeclarationInSeqPlaceholder = (
        " TypeDeclarationInSeq ::= Placeholder ", Make('Node')
    )

    a_TypeDeclaration = (
        """
          TypeDeclaration ::= EmptyDecl
          TypeDeclaration ::= ClassOrInterfaceDeclaration
        """, Nop()
    )
    
    
    # fragment(s)

    a_Selectors = (
        seq('Selectors','Selector',''), Make('Seq')
    )

    # !!! TODO: force kind syntax: `<Kind>:: ...

    a_EmptyFragment = (
        """
          Fragment ::=
        """, Make('Node',name='Fragment')
    )

    a_Fragment = (
        """
          Fragment ::= SimpleFragment
        """, Nop()
    )

    a_NotSimpleFragment = (
        """
          Fragment ::= ExpressionsFragment
        """, Nop()
    )

    a_QualIdFragment = (
        """
         SimpleFragment ::= QualifiedIdentifier
        """, Make('Node',name='Fragment')
    )

    a_SimpleFragment = (
        """
         SimpleFragment ::= VOID
         SimpleFragment ::= Type
         SimpleFragment ::= Selectors
         SimpleFragment ::= BlockStatements
         SimpleFragment ::= SwitchBlockStatementGroups
         SimpleFragment ::= VOID LPAREN FormalParameterListOpt RPAREN
         SimpleFragment ::= CLASS LBRACE ClassBodyDeclarations RBRACE
        """, Make('Node',name='Fragment')
    )

    a_ExpressionSimpleFragment = (
        """
         SimpleFragment ::= Expression        
        """, Make('Node',name='Fragment')
    )

    a_ExpressionsFragment = (
        """
         ExpressionsFragment ::= Expressions        
        """, Make('Node',name='Fragment')
    )

    # QualifiedIdentifier | `a [`b|selector ...]

    a_PlaceholderFragment = ( # !!! change name?
        """
         SimpleFragment ::= Placeholder Selectors
        """, Make('Node')
    )
          

    #  Fragment,...

    a_OneUnprotectedFragment = (
        " OneFragment ::= SimpleFragment", Nop()
    )

    a_OneProtectedFragment = (
        " OneFragment ::= LBRACK Fragment RBRACK ", Make('Node')
    )

    a_NoFragments = (
        """
          Fragments ::=
        """,
        Make('Seq','Fragments')
    )
        

    a_Fragments = (
        """
          Fragments ::= OneFragment
        """,
        Make('Seq','Fragments')
    )

    a_ManyFragments = (
        """
          Fragments ::= Fragments COMMA OneFragment
        """,
        Make('Seq','Fragments')
    )
    
    setup_a_rules_to_p_funcs(locals())


def getparser(reuse, start, cache={}):
    if not reuse:
        return JavaParser(start=start)
    else:
        try:
            return cache[start]
        except KeyError:
            p = JavaParser(start=start)
            p.makeFIRST()
            p.ruleschanged = 0
            cache[start] = p
            return p

# parse: java code string -> ast
def parse(s, start='Statement', reuse=1):
    parser = getparser(reuse, start)
    return parser.parse(JavaLexer(s).scan())


# - * -

# checks and meta tools for development

TO_DEFINE = """
Identifier
QualifiedIdentifier
Literal
Expression
-AssignmentOperator
Type
-StatementExpression
-ConstantExpression
-Expression1
-Expression1Rest
-Expression2Rest
-Infixop
-Expression3
Primary
-IdentifierSuffix
-PrefixOp
-PostfixOp
Selector
-SuperSuffix
BasicType
ArgumentsOpt
Arguments
BracketsOpt
Creator
InnerCreator
-ArrayCreatorRest
-ClassCreatorRest
ArrayInitializer
VariableInitializer
ParExpression
Block
BlockStatements
LocalVariableDeclarationStatement
Statement
Catches
CatchClause
SwitchBlockStatementGroups
SwitchBlockStatementGroup
SwitchLabel
-MoreStatementExpressions
ForInit
ForUpdate
ModifiersOpt
Modifier
VariableDeclarators
-VariableDeclaratorsRest
-ConstantDeclaratorsRest
VariableDeclarator
-ConstantDeclarator
-VariableDeclaratorRest
-ConstantDeclaratorRest
VariableDeclaratorId
CompilationUnit
ImportDeclaration
TypeDeclaration
ClassOrInterfaceDeclaration
ClassDeclaration
InterfaceDeclaration
TypeList
ClassBody
InterfaceBody
ClassBodyDeclaration
-MemberDecl
-MethodOrFieldDecl
-MethodOrFieldRest
InterfaceBodyDeclaration
-InterfaceMemberDecl
-InterfaceMethodOrFieldDecl
-InterfaceMethodOrFieldRest
-MethodDeclaratorRest
-VoidMethodDeclaratorRest
-InterfaceMethodDeclaratorRest
-VoidInterfaceMethodDeclaratorRest
-ConstructorDeclaratorRest
-QualifiedIdentifierList
FormalParameters
FormalParameter
MethodBody
"""

def check():
    p = JavaParser()
    rhs = {}
    lhs = {}
    for g in p.rules.values():
        for r in g:
            lhs[r[0]] = 1
            for x in r[1]:
                if x[1].islower():
                    rhs[x] = 1
    print "- UNDEFINED -"
    for x in rhs.keys():
        if not lhs.has_key(x):
            print x
    print "- UNUSED -"
    for x in lhs.keys():
        if x != "START" and not rhs.has_key(x):
            print x
    print "- TO DEFINE -"
    c = 0
    defined = 0
    for x in TO_DEFINE.split():
        if x.startswith('-'): continue
        c += 1
        if not lhs.has_key(x):
            print x
        else:
            defined += 1
    print "%d done of %d, %d to go" % (defined,c,c-defined)


# - * -

# make nodes
# !!! polish

def fill(proto,subst):
    cpy = proto[:]
    j = 0
    for i in xrange(len(cpy)):
        if cpy[i] == '_':
            cpy[i] = subst[j]
            j += 1
    return cpy

def scheme(rule_spec):
    scm = []
    count = {}
    for term in rule_spec:
        if term[1].islower():
            scm.append('_')
            count[term] = count.get(term,0) + 1
        else:
            scm.append(term)
    for term,cnt in count.items():
        if cnt == 1:
            del count[term]
        else:
            count[term] = 0
    sign = []
    for term in rule_spec:
        if term[1].islower():
            if not count.has_key(term):
                sign.append(term)
            else:
                sign.append("%s_%d" % (term,count[term]))
                count[term] += 1
    return sign,scm

_node_class2schemes = None

def sign2key(sign):
    key = sign[:]
    key.sort()
    return tuple(key)
    
def ast_defs(echo=1,set=0):
    proto_parser = JavaParser()

    name2rules = {}
    for rule,name in proto_parser.rule2name.items():
        if not name: continue
        # !!! some rule names do not correspond to the ast node class name,
        # in those cases the action is decorated with a 'making' with the ast node class name
        making = getattr(getattr(JavaParser,'p_%s' % name),'making',None)
        if making is not None:
            name = making
        name2rules.setdefault(name,[]).append(rule)

    node_class2schemes = {}
    seqs = []

    for name,node_class in java_nodes.__dict__.items():
        if not name.startswith('_'):
            if echo: print name
            schemes = {}
            ambiguous_keys = {}
            if issubclass(node_class,Seq):
                if echo:
                    print "  >Seq<",
                separator = {}
                for rule in name2rules[name]:
                    for term in rule[1]:
                        if term.isupper():
                            separator[term] = 1
                if len(separator) == 1:
                    separator = separator.keys()[0]
                    if echo: print separator
                    schemes['sep'] = separator
                else:
                    if echo: print                            
                seqs.append(name)
            
            for rule in name2rules[name]:
                spec = rule[1]
                sign,scm = scheme(spec)
                key = sign2key(sign)
                amb_key = 0
                if ambiguous_keys.has_key(key):
                    amb_key = 1
                if schemes.has_key(key):
                    amb_key = 1
                    ambiguous_keys[key] = 1
                    other_scm = schemes[key][1]
                    disamb = [ x for x in other_scm if x != '_' ]
                    disamb_key = sign2key(sign+disamb)
                    if disamb_key != key:
                        schemes[disamb_key] = schemes[key]
                        del schemes[key]                    
                if amb_key:
                    disamb = [ x for x in scm if x != '_' ]
                    schemes[sign2key(sign+disamb)] = (sign,scm,spec)
                else:
                    schemes[key] = (sign,scm,spec)

            # !!! harrumph, inelegant way to get there
            fixed_spec = getattr(getattr(JavaParser,'p_%s' % name),'_spec',None)
            if echo:
                for key,scheme_inst in schemes.items():
                    if key == 'sep': continue
                    sign,scm,spec = scheme_inst
                    to_show = fill(scm,sign)
                    print "%c  %s" % (len(sign)==len(key) and ' ' or 'a',' '.join(to_show),)
            if fixed_spec:
                key = list(fixed_spec)
                key.sort()
                key = tuple(key)
                schemes = { key: (list(fixed_spec),['_']*len(fixed_spec),fixed_spec) }
                if echo: print "  >FIXED: %s<" % ' '.join(fixed_spec)

            if not fixed_spec:
                length2keys = {}
                for key in schemes.keys(): 
                    sign = schemes[key][0] # use sign len
                    length2keys.setdefault(len(sign),[]).append(key)
                for length,keys in length2keys.items():
                    if len(keys) == 1:
                        schemes[length] = schemes[keys[0]]
                        if echo: print " ",length,":",keys[0]
            
            node_class2schemes[node_class] = schemes

    if echo:
        print "- Seqs -"
        for name in seqs:
            print name

    if set:
        global _node_class2schemes
        _node_class2schemes = node_class2schemes

def make_id(s):
    return java_nodes.Identifier(('IDENTIFIER',),[Token('IDENTIFIER',s)])

def make_qualid(s):
    return make(java_nodes.QualifiedIdentifier,[make_id(s)])

def make_literal(s):
    x = JavaLexer(s).scan()
    return java_nodes.Literal((x[0].type,),x)

class UnknownScheme(Exception):
    pass

def make(node_class,*args,**kw):
    if _node_class2schemes is None:
        ast_defs(echo=0,set=1)
    schemes = _node_class2schemes[node_class]
    if issubclass(node_class,Seq) and len(kw)==0:
        if len(args) == 1 and type(args[0]) is type([]):
            args = args[0]
        if schemes.has_key('sep'):
           sep = schemes['sep']
           if len(args) >= 2:
               if not (isinstance(args[1],Token) and args[1].type == sep):
                   sep = getattr(java_tokens,sep)
                   new_args = []
                   for arg in args:
                       new_args.append(arg)
                       new_args.append(sep)
                   new_args.pop()
                   args = new_args
        return node_class(None,args)

    if args:
        length = len(args)
        try:
            sign,scm,spec = schemes[length]
        except KeyError:
            raise UnknownScheme
    else:
        key = kw.keys()
        key.sort()
        try:
            sign,scm,spec = schemes[tuple(key)]
        except:
            raise UnknownScheme
        args = []
        for name in sign:
            args.append(kw[name])
    # !!! ~
    children = []
    j = 0
    for el in scm:
        if el == '_':
            children.append(args[j])
            j += 1
        else:
            children.append(getattr(java_tokens,el))
            
    return node_class(spec,children)

def join_seq_nodes(*args):
    kind = None
    for seq in args:
        if not isinstance(seq,Seq):
            raise Exception,"expected seq node"
        if kind is None:
            kind = seq.__class__
        else:
            if kind is not seq.__class__:
                raise Exception,"expected same seq node kind"
    if _node_class2schemes is None:
        ast_defs(echo=0,set=1)
    sep = _node_class2schemes[kind].get('sep',None)
    if sep:
        sep = getattr(java_tokens,sep)
    joined = []
    for seq in args:
        joined.extend(seq.children)
        if seq.children and sep:
            joined.append(sep)
    if joined and sep:
        joined.pop()
    return kind("*",joined)
            
