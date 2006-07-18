# copyright 2004-2005 Samuele Pedroni
"""
 Java lexer
"""

import re
import types

class Token:
    def __init__(self,type,value=None):
        self.type = type
        self.value = value
        #
        self.infront_comments = []
        self.attached_comments = []

    def __repr__(self):
        return  "%s(%r)" % (self.type,self.value)

    def __eq__(self,other): # !!! ?
        return self.type == other

    def __ne__(self,other):
        return self.type != other
    
# TokenList stores tokens attaching comments to appropriate tokens

class InfrontComment_State:
    def __init__(self,toklist,tok=None):
        self.toklist = toklist
        if tok is not None:
            self.comments = [tok]
        else:
            self.comments = []

    def comment(self,tok):
        self.comments.append(tok)
        return self

    def significative_token(self,tok):
        tok.infront_comments = self.comments
        return SignificativeToken_State(self.toklist,tok)
        
class SignificativeToken_State:
    def __init__(self,toklist,tok):
        self.toklist = toklist
        self.tok_append = toklist.tokens.append
        self.significative_token(tok)

    def significative_token(self,tok):
        self.tok_append(tok)
        self.last = tok
        return self

    def comment(self,tok):
        last = self.last
        if last.lineno == tok.start:
            return AttachedComment_State(self.toklist,last,tok)
        else:
            return InfrontComment_State(self.toklist,tok)

class AttachedComment_State:
    def __init__(self,toklist,attach_to,tok):
        self.toklist = toklist
        attach_to.attached_comments = self.comments = [tok]
        attach_to.attached_line_delta = 1
        self.attach_to = attach_to
        self.start = tok.start
        self.col = tok.col

    def set_attached_line_delta(self,tok):
        self.attach_to.attached_line_delta = tok.lineno - self.comments[-1].end
        
    def comment(self,tok):
        if tok.start == self.start or tok.col == self.col:
            self.comments.append(tok)
            return self
        else:
            self.set_attached_line_delta(tok)
            return InfrontComment_State(self.toklist,tok)

    def significative_token(self,tok):
        self.set_attached_line_delta(tok)        
        return SignificativeToken_State(self.toklist,tok)


class TokenList:

    def __init__(self):
        self.tokens = []
        self.state = InfrontComment_State(self)
        
    def add(self,tok):
        if tok.type == 'COMMENT':
            self.state = self.state.comment(tok)
        else:
            self.state = self.state.significative_token(tok)

    def aslist(self):
        return self.tokens

# Lexer

# construction

def CHOICE(*regexes):
    return '|'.join([ "(%s)" % regex for regex in regexes])

def collect_simple():
  global _pattern,_actions
  patts = [ x for x in globals().items() if x[0].startswith('t_') ]
  patts.sort(lambda x,y: -cmp(len(x[1]),len(y[1])))
  patterns = []
  for name,patt in patts:
      type = name[2:]
      _actions[name] = (type,None)
      #print name,patt
      patterns.append("(?P<%s>%s)" % (name,patt))
  _pattern = '|'.join(patterns)

def add_w_action(type,action,patt=None):
    global _pattern,_actions

    name = action.__name__
    _actions[name] = (type,action)

    if patt is None:
        patt = action.__doc__
    patt = "(?P<%s>%s)" % (name,patt)
    if _pattern:
        _pattern =  patt + "|" + _pattern
    else:
        _pattern = patt

def RESERVED(spec,resdict):
    for res in re.split(r"(?:,|\s)+",spec): # split on , and whitespace
        if res:
            resdict[res.lower()] = res
    return resdict

def finish_setup():
    global _pattern
    _pattern = re.compile(_pattern,re.VERBOSE)
    groupindex = _pattern.groupindex
    actions = _actions
    for name,action in actions.items():
        del actions[name]
        actions[groupindex[name]] = action

# operators & delims

# delims

t_PLHSTARTPARMS = r'`\(' # for placeholder arguments

t_LPAREN, t_RPAREN = r'\(',r'\)'
t_LBRACK, t_RBRACK = r'\[',r'\]'
t_LBRACE, t_RBRACE = r'\{',r'\}'

t_SEMICOLON = r';'
t_COMMA     = r','
t_COLON     = r':'

# dot

t_DOT = r'\.'

# ellipsis

t_ELLIPSIS=r'\.\.\.'

# operators

t_MULT = r'\*'

t_EQ = r'='

t_PLUSPLUS   = r'\+\+'
t_MINUSMINUS = r'--'

t_PLUS, t_MINUS, t_COMP, t_NOT, t_DIV, t_MOD = r'\+',r'-',r'~',r'!',r'/',r'%'

t_LSHIFT, t_RSHIFT, t_URSHIFT = r'<<',r'>>',r'>>>'

t_LT, t_GT, t_LTEQ, t_GTEQ = r'<',r'>',r'<=',r'>='
t_EQEQ, t_NOTEQ = r'==',r'!='

t_AND    = r'&'
t_XOR    = r'\^'
t_OR     = r'\|'
t_ANDAND = r'&&'
t_OROR   = r'\|\|'

t_QUESTION = r'\?'

t_MULTEQ = r'\*='
t_PLUSEQ, t_MINUSEQ, t_DIVEQ, t_MODEQ = r'\+=',r'-=',r'/=',r'%='

t_LSHIFTEQ, t_RSHIFTEQ, t_URSHIFTEQ = r'<<=',r'>>=',r'>>>='

t_ANDEQ = r'&='
t_XOREQ = r'\^='
t_OREQ  = r'\|='

# literals

# floating point

t_FLOATING_POINT_LITERAL = CHOICE(
    r'((\d*\.\d+)|(\d+\.\d*))([eE][+-]?\d+)?[fFdD]?',
    r'\d+((([eE][+-]?\d+)[fFdD]?)|(([eE][+-]?\d+)?[fFdD]))' )

# integer

t_INTEGER_LITERAL = CHOICE(
    r'0[0-7]+[lL]?',            # oct
    r'0[xX][0-9a-fA-F]+[lL]?',  # hex
    r'(0|([1-9]\d*))[lL]?'      # dec
    )

# for the moment accept \uXXXX only inside char/string literals
# this is not the spec way of doing things!

# chars
# ''' '\' are invalid

t_CHARACTER_LITERAL = r"'(\ |[^\s'\\])'|'\\([btnfr\"\'\\]|[0-3]?[0-7][0-7]?|u+[0-9a-fA-F]{4})'"

# string

t_STRING_LITERAL = r"\"(\ |[^\s\"\\]|\\([btnfr\"\'\\]|[0-3]?[0-7][0-7]?|u+[0-9a-fA-F]{4}))*\""

# placeholder

t_PLACEHOLDER = r'`[A-Za-z_$][\w_$]*'
       
_ignore = ' \t\x0c' # !!! tabs vs comment.col ?
_pattern = None
_actions = {}

collect_simple()

# comments

# COMMMENT: start,col up to end

def t_comment_c(lexer,tok): # fixed recursion problem at least when using 2.3
    r' /\*[\S\s]*?\*/'

    pos = lexer.pos
    lineno = lexer.lineno

    col = pos-lexer.line_start_pos

    tok.start = lineno # == tok.lineno
    tok.col = col

    value = tok.value
    lexer.lineno += value.count('\n')

    tok.end = lexer.lineno

    nl = value.rfind('\n')
    if nl > -1:
        lexer.line_start_pos = pos + nl + 1

add_w_action('COMMENT',t_comment_c)

def t_comment_cpp(lexer,tok): # \n? correct ?
    r' //.*\n?'

    pos = lexer.pos
    lineno = lexer.lineno

    col = pos-lexer.line_start_pos

    tok.start = lineno # == tok.lineno
    tok.col = col
    tok.end = lineno

    if tok.value[-1] == '\n':
        lexer.lineno += 1
        lexer.line_start_pos = pos + len(tok.value)
        tok.value = tok.value[:-1]

add_w_action('COMMENT',t_comment_cpp)

# identifiers and reserved

_reserved = RESERVED("""
BOOLEAN
BYTE, SHORT, INT, LONG, CHAR
FLOAT, DOUBLE
PACKAGE
IMPORT
PUBLIC, PROTECTED, PRIVATE
STATIC
ABSTRACT, FINAL, NATIVE, SYNCHRONIZED, TRANSIENT, VOLATILE
CLASS
EXTENDS
IMPLEMENTS
VOID
THROWS
THIS, SUPER
INTERFACE
IF, ELSE
SWITCH
CASE, DEFAULT
DO, WHILE
FOR
BREAK
CONTINUE
RETURN
THROW
TRY
CATCH
FINALLY
NEW

INSTANCEOF

CONST, GOTO

STRICTFP

ASSERT
"""
#ENUM
, {
    'null':  'NULL_LITERAL',
    'true':  'BOOLEAN_LITERAL',
    'false': 'BOOLEAN_LITERAL',
    })

def t_identifier(lexer,tok):
    r'[A-Za-z_$][\w_$]*'
    tok.type = _reserved.get(tok.value,'IDENTIFIER')

add_w_action('IDENTIFIER',t_identifier)

finish_setup() # fix _pattern, _actions


class JavaLexer:

    def __init__(self,s):
        self.s = s

    def error(self,ch):
        raise Exception,"Illegal character %s" % repr(ch)

    def scan(self):   
        ignore = _ignore
        pattern = _pattern
        actions = _actions

        s = self.s

        tokens = TokenList()
        pos = 0
        line_start_pos = 0
        lineno = 1
        stop = len(s)
        while pos < stop:
            ch = s[pos]
            if ch == '\n':
                lineno += 1
                pos += 1
                line_start_pos = pos
                continue
            if ch in ignore:
                pos += 1
                continue

            m = _pattern.match(s,pos)
            if m is None:
                self.error(s[pos])

            type,action = _actions[m.lastindex]

            # make token

            value = m.group()

            tok = Token(type,value)
            tok.lineno = lineno

            if action:
                self.lineno, self.pos, self.line_start_pos = lineno, pos, line_start_pos
                action(self,tok)
                lineno, line_start_pos = self.lineno, self.line_start_pos

            pos += len(value)

            tokens.add(tok)

        # !!! ? pending comments

        return tokens.aslist()


class _Bag:
    pass

java_tokens = _Bag()

def concrete_toks():
    toks = java_tokens
    for name,t_patt in globals().items():
        if (name.startswith('t_') and isinstance(t_patt,types.StringType)
            and not name.endswith('LITERAL')):
            name = name[2:]
            val = t_patt.replace('\\','')
            if re.match(t_patt,val):
                setattr(toks,name,Token(name,val))
    for val,name in _reserved.items():
        if not name.endswith('LITERAL'):
            setattr(toks,name,Token(name,val))

concrete_toks()



##if __name__ == '__main__':
##    import sys
##    f = open(sys.argv[1])
##    s = f.read()
##    f.close()
##    for tok in JavaLexer(s).scan(): 
##        print tok
