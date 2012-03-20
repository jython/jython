/*
 [The 'BSD licence']
 Copyright (c) 2004 Terence Parr and Loring Craymer
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/** Python 2.3.3 Grammar
 *
 *  Terence Parr and Loring Craymer
 *  February 2004
 *
 *  Converted to ANTLR v3 November 2005 by Terence Parr.
 *
 *  This grammar was derived automatically from the Python 2.3.3
 *  parser grammar to get a syntactically correct ANTLR grammar
 *  for Python.  Then Terence hand tweaked it to be semantically
 *  correct; i.e., removed lookahead issues etc...  It is LL(1)
 *  except for the (sometimes optional) trailing commas and semi-colons.
 *  It needs two symbols of lookahead in this case.
 *
 *  Starting with Loring's preliminary lexer for Python, I modified it
 *  to do my version of the whole nasty INDENT/DEDENT issue just so I
 *  could understand the problem better.  This grammar requires
 *  PythonTokenStream.java to work.  Also I used some rules from the
 *  semi-formal grammar on the web for Python (automatically
 *  translated to ANTLR format by an ANTLR grammar, naturally <grin>).
 *  The lexical rules for python are particularly nasty and it took me
 *  a long time to get it 'right'; i.e., think about it in the proper
 *  way.  Resist changing the lexer unless you've used ANTLR a lot. ;)
 *
 *  I (Terence) tested this by running it on the jython-2.1/Lib
 *  directory of 40k lines of Python.
 *
 *  REQUIRES ANTLR v3
 *
 *  Updated to Python 2.5 by Frank Wierzbicki.
 *
 */

grammar PythonPartial;
options {
    tokenVocab=Python;
}

@header {
package org.python.antlr;
}

@members {
    private ErrorHandler errorHandler = new FailFastHandler();

    protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
        throws RecognitionException {

        Object o = errorHandler.recoverFromMismatchedToken(this, input, ttype, follow);
        if (o != null) {
            return o;
        }
        return super.recoverFromMismatchedToken(input, ttype, follow);
    }

}

@rulecatch {
catch (RecognitionException e) {
    throw e;
}
}

@lexer::header {
package org.python.antlr;
}

@lexer::members {
/** Handles context-sensitive lexing of implicit line joining such as
 *  the case where newline is ignored in cases like this:
 *  a = [3,
 *       4]
 */

//For use in partial parsing.
public boolean eofWhileNested = false;
public boolean partial = false;

int implicitLineJoiningLevel = 0;
int startPos=-1;

//If you want to use another error recovery mechanism change this
//and the same one in the parser.
private ErrorHandler errorHandler;

    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
    }

    /**
     *  Taken directly from antlr's Lexer.java -- needs to be re-integrated every time
     *  we upgrade from Antlr (need to consider a Lexer subclass, though the issue would
     *  remain).
     */
    public Token nextToken() {
        startPos = getCharPositionInLine();
        while (true) {
            state.token = null;
            state.channel = Token.DEFAULT_CHANNEL;
            state.tokenStartCharIndex = input.index();
            state.tokenStartCharPositionInLine = input.getCharPositionInLine();
            state.tokenStartLine = input.getLine();
            state.text = null;
            if ( input.LA(1)==CharStream.EOF ) {
                if (implicitLineJoiningLevel > 0) {
                    eofWhileNested = true;
                }
                return Token.EOF_TOKEN;
            }
            try {
                mTokens();
                if ( state.token==null ) {
                    emit();
                }
                else if ( state.token==Token.SKIP_TOKEN ) {
                    continue;
                }
                return state.token;
            } catch (NoViableAltException nva) {
                errorHandler.reportError(this, nva);
                errorHandler.recover(this, nva); // throw out current char and try again
            } catch (FailedPredicateException fp) {
                //XXX: added this for failed STRINGPART -- the FailedPredicateException
                //     hides a NoViableAltException.  This should be the only
                //     FailedPredicateException that gets thrown by the lexer.
                errorHandler.reportError(this, fp);
                errorHandler.recover(this, fp); // throw out current char and try again
            } catch (RecognitionException re) {
                errorHandler.reportError(this, re);
                // match() routine has already called recover()
            }
        }
    }
}


//single_input: NEWLINE | simple_stmt | compound_stmt NEWLINE
single_input

    : NEWLINE
    | simple_stmt
    | compound_stmt NEWLINE?
    ;

//eval_input: testlist NEWLINE* ENDMARKER
eval_input
    : LEADING_WS? (NEWLINE)* testlist? (NEWLINE)* EOF
    ;

//not in CPython's Grammar file
dotted_attr
    : NAME
      ( (DOT NAME)+
      |
      )
    ;

//attr is here for Java  compatibility.  A Java foo.getIf() can be called from Jython as foo.if
//     so we need to support any keyword as an attribute.

attr
    : NAME
    | AND
    | AS
    | ASSERT
    | BREAK
    | CLASS
    | CONTINUE
    | DEF
    | DELETE
    | ELIF
    | EXCEPT
    | EXEC
    | FINALLY
    | FROM
    | FOR
    | GLOBAL
    | IF
    | IMPORT
    | IN
    | IS
    | LAMBDA
    | NOT
    | OR
    | ORELSE
    | PASS
    | PRINT
    | RAISE
    | RETURN
    | TRY
    | WHILE
    | WITH
    | YIELD
    ;

//decorator: '@' dotted_name [ '(' [arglist] ')' ] NEWLINE
decorator
    : AT dotted_attr
    ( LPAREN
      ( arglist
      |
      )
      RPAREN
    |
    ) NEWLINE
    ;

//decorators: decorator+
decorators
    : decorator+
    ;

//funcdef: [decorators] 'def' NAME parameters ':' suite
funcdef
    : decorators? DEF NAME parameters COLON suite
    ;

//parameters: '(' [varargslist] ')'
parameters
    : LPAREN
      (varargslist
      |
      )
      RPAREN
    ;

//not in CPython's Grammar file
defparameter
    : fpdef (ASSIGN test)?
    ;

//varargslist: ((fpdef ['=' test] ',')*
//              ('*' NAME [',' '**' NAME] | '**' NAME) |
//              fpdef ['=' test] (',' fpdef ['=' test])* [','])
varargslist
    : defparameter (options {greedy=true;}:COMMA defparameter)*
      (COMMA
          (STAR NAME (COMMA DOUBLESTAR NAME)?
          | DOUBLESTAR NAME
          )?
      )?
    | STAR NAME (COMMA DOUBLESTAR NAME)?
    | DOUBLESTAR NAME
    ;

//fpdef: NAME | '(' fplist ')'
fpdef
    : NAME
    | (LPAREN fpdef COMMA) => LPAREN fplist RPAREN
    | LPAREN fplist RPAREN
    ;

//fplist: fpdef (',' fpdef)* [',']
fplist
    : fpdef
      (options {greedy=true;}:COMMA fpdef)* (COMMA)?
    ;

//stmt: simple_stmt | compound_stmt
stmt
    : simple_stmt
    | compound_stmt
    ;

//simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
simple_stmt
    : small_stmt (options {greedy=true;}:SEMI small_stmt)* (SEMI)? (NEWLINE|EOF)
    ;

//small_stmt: (expr_stmt | print_stmt  | del_stmt | pass_stmt | flow_stmt |
//             import_stmt | global_stmt | exec_stmt | assert_stmt)
small_stmt : expr_stmt
           | print_stmt
           | del_stmt
           | pass_stmt
           | flow_stmt
           | import_stmt
           | global_stmt
           | exec_stmt
           | assert_stmt
           ;

//expr_stmt: testlist (augassign (yield_expr|testlist) |
//                     ('=' (yield_expr|testlist))*)
expr_stmt
    : ((testlist augassign) => testlist
        ( (augassign yield_expr
          )
        | (augassign testlist
          )
        )
    | (testlist ASSIGN) => testlist
        (
        | ((ASSIGN testlist)+
          )
        | ((ASSIGN yield_expr)+
          )
        )
    | testlist
    )
    ;

//augassign: ('+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' |
//            '<<=' | '>>=' | '**=' | '//=')
augassign
    : PLUSEQUAL
    | MINUSEQUAL
    | STAREQUAL
    | SLASHEQUAL
    | PERCENTEQUAL
    | AMPEREQUAL
    | VBAREQUAL
    | CIRCUMFLEXEQUAL
    | LEFTSHIFTEQUAL
    | RIGHTSHIFTEQUAL
    | DOUBLESTAREQUAL
    | DOUBLESLASHEQUAL
    ;

//print_stmt: 'print' ( [ test (',' test)* [','] ] |
//                      '>>' test [ (',' test)+ [','] ] )
print_stmt
    : PRINT
      (printlist
      | RIGHTSHIFT printlist
      |
      )
      ;

//not in CPython's Grammar file
printlist
    : (test COMMA) =>
       test (options {k=2;}: COMMA test)*
         (COMMA)?
    | test
    ;

//del_stmt: 'del' exprlist
del_stmt
    : DELETE exprlist
    ;

//pass_stmt: 'pass'
pass_stmt
    : PASS
    ;

//flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
flow_stmt
    : break_stmt
    | continue_stmt
    | return_stmt
    | raise_stmt
    | yield_stmt
    ;

//break_stmt: 'break'
break_stmt
    : BREAK
    ;

//continue_stmt: 'continue'
continue_stmt
    : CONTINUE
    ;

//return_stmt: 'return' [testlist]
return_stmt
    : RETURN
      (testlist
      |
      )
    ;

//yield_stmt: yield_expr
yield_stmt
    : yield_expr
    ;

//raise_stmt: 'raise' [test [',' test [',' test]]]
raise_stmt
    : RAISE (test (COMMA test
        (COMMA test)?)?)?
    ;

//import_stmt: import_name | import_from
import_stmt
    : import_name
    | import_from
    ;

//import_name: 'import' dotted_as_names
import_name
    : IMPORT dotted_as_names
    ;

//import_from: ('from' ('.'* dotted_name | '.'+)
//              'import' ('*' | '(' import_as_names ')' | import_as_names))
import_from
    : FROM (DOT* dotted_name | DOT+) IMPORT
        (STAR
        | import_as_names
        | LPAREN import_as_names COMMA? RPAREN
        )
    ;

//import_as_names: import_as_name (',' import_as_name)* [',']
import_as_names
    : import_as_name (COMMA import_as_name)*
    ;

//import_as_name: NAME [('as' | NAME) NAME]
import_as_name
    : NAME (AS NAME)?
    ;

//XXX: when does CPython Grammar match "dotted_name NAME NAME"?
//dotted_as_name: dotted_name [('as' | NAME) NAME]
dotted_as_name
    : dotted_name (AS NAME)?
    ;

//dotted_as_names: dotted_as_name (',' dotted_as_name)*
dotted_as_names
    : dotted_as_name (COMMA dotted_as_name)*
    ;

//dotted_name: NAME ('.' NAME)*
dotted_name
    : NAME (DOT attr)*
    ;

//global_stmt: 'global' NAME (',' NAME)*
global_stmt
    : GLOBAL NAME (COMMA NAME)*
    ;

//exec_stmt: 'exec' expr ['in' test [',' test]]
exec_stmt
    : EXEC expr (IN test (COMMA test)?)?
    ;

//assert_stmt: 'assert' test [',' test]
assert_stmt
    : ASSERT test (COMMA test)?
    ;

//compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | funcdef | classdef
compound_stmt
    : if_stmt
    | while_stmt
    | for_stmt
    | try_stmt
    | with_stmt
    | (decorators? DEF) => funcdef
    | (decorators? CLASS) => classdef
    | decorators
    ;

//if_stmt: 'if' test ':' suite ('elif' test ':' suite)* ['else' ':' suite]
if_stmt
    : IF test COLON suite elif_clause?
    ;

//not in CPython's Grammar file
elif_clause
    : else_clause
    | ELIF test COLON suite
        (elif_clause
        |
        )
    ;

//not in CPython's Grammar file
else_clause
    : ORELSE COLON suite
    ;

//while_stmt: 'while' test ':' suite ['else' ':' suite]
while_stmt
    : WHILE test COLON suite (ORELSE COLON suite)?
    ;

//for_stmt: 'for' exprlist 'in' testlist ':' suite ['else' ':' suite]
for_stmt
    : FOR exprlist IN testlist COLON suite
        (ORELSE COLON suite)?
    ;

//try_stmt: ('try' ':' suite
//           ((except_clause ':' suite)+
//           ['else' ':' suite]
//           ['finally' ':' suite] |
//           'finally' ':' suite))
try_stmt
    : TRY COLON suite
      ( except_clause+ (ORELSE COLON suite)? (FINALLY COLON suite)?
      | FINALLY COLON suite
      )?
      ;

//with_stmt: 'with' test [ with_var ] ':' suite
with_stmt
    : WITH test (with_var)? COLON suite
    ;

//with_var: ('as' | NAME) expr
with_var
    : (AS | NAME) expr
    ;

//except_clause: 'except' [test [',' test]]
except_clause
    : EXCEPT (test (COMMA test)?)? COLON suite
    ;

//suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
suite
    : simple_stmt
    | NEWLINE (EOF
              | (DEDENT)+ EOF
              | INDENT (stmt)+ (DEDENT
                               |EOF
                               )
              )
    ;

//test: or_test ['if' or_test 'else' test] | lambdef
test
    :or_test
      ( (IF or_test ORELSE) => IF or_test ORELSE test
      |
      )
    | lambdef
    ;

//or_test: and_test ('or' and_test)*
or_test
    : and_test
        ( (OR and_test
          )+
        |
        )
    ;

//and_test: not_test ('and' not_test)*
and_test
    : not_test
        ( (AND not_test
          )+
        |
        )
    ;

//not_test: 'not' not_test | comparison
not_test
    : NOT not_test
    | comparison
    ;

//comparison: expr (comp_op expr)*
comparison
    : expr
       ( ( comp_op expr
         )+
       |
       )
    ;

//comp_op: '<'|'>'|'=='|'>='|'<='|'<>'|'!='|'in'|'not' 'in'|'is'|'is' 'not'
comp_op
    : LESS
    | GREATER
    | EQUAL
    | GREATEREQUAL
    | LESSEQUAL
    | ALT_NOTEQUAL
    | NOTEQUAL
    | IN
    | NOT IN
    | IS
    | IS NOT
    ;

//expr: xor_expr ('|' xor_expr)*
expr
    : xor_expr
        ( (VBAR xor_expr
          )+
        |
        )
    ;

//xor_expr: and_expr ('^' and_expr)*
xor_expr
    : and_expr
        ( (CIRCUMFLEX and_expr
          )+
        |
        )
    ;

//and_expr: shift_expr ('&' shift_expr)*
and_expr
    : shift_expr
        ( (AMPER shift_expr
          )+
        |
        )
    ;

//shift_expr: arith_expr (('<<'|'>>') arith_expr)*
shift_expr
    : arith_expr
        ( ( shift_op arith_expr
          )+
        |
        )
    ;

shift_op
    : LEFTSHIFT
    | RIGHTSHIFT
    ;

//arith_expr: term (('+'|'-') term)*
arith_expr
    : term
        ( (arith_op term
          )+
        |
        )
    ;

arith_op
    : PLUS
    | MINUS
    ;

//term: factor (('*'|'/'|'%'|'//') factor)*
term
    : factor
        ( (term_op factor
          )+
        |
        )
    ;

term_op
    :STAR
    |SLASH
    |PERCENT
    |DOUBLESLASH
    ;

//factor: ('+'|'-'|'~') factor | power
factor
    : PLUS factor
    | MINUS factor
    | TILDE factor
    | power
    | TRAILBACKSLASH
    ;

//power: atom trailer* ['**' factor]
power
    : atom (trailer)* (options {greedy=true;}:DOUBLESTAR factor)?
    ;

//atom: ('(' [yield_expr|testlist_gexp] ')' |
//       '[' [listmaker] ']' |
//       '{' [dictmaker] '}' |
//       '`' testlist1 '`' |
//       NAME | NUMBER | STRING+)
atom
    : LPAREN
      ( yield_expr
      | testlist_gexp
      |
      )
      RPAREN
    | LBRACK
      (listmaker
      |
      )
      RBRACK
    | LCURLY
       (dictmaker
       |
       )
       RCURLY
     | BACKQUOTE testlist BACKQUOTE
     | NAME
     | INT
     | LONGINT
     | FLOAT
     | COMPLEX
     | (STRING)+
     | TRISTRINGPART
     | STRINGPART TRAILBACKSLASH
     ;

//listmaker: test ( list_for | (',' test)* [','] )
listmaker
    : test
        (list_for
        | (options {greedy=true;}:COMMA test)*
        ) (COMMA)?
    ;

//testlist_gexp: test ( gen_for | (',' test)* [','] )
testlist_gexp
    : test
        ( ((options {k=2;}: COMMA test)* (COMMA)?
          )
        | (gen_for
          )
        )
    ;

//lambdef: 'lambda' [varargslist] ':' test
lambdef
    : LAMBDA (varargslist)? COLON test
    ;

//trailer: '(' [arglist] ')' | '[' subscriptlist ']' | '.' NAME
trailer
    : LPAREN
        (arglist
        |
        )
      RPAREN
    | LBRACK subscriptlist RBRACK
    | DOT attr
    ;

//subscriptlist: subscript (',' subscript)* [',']
subscriptlist
    : subscript (options {greedy=true;}:COMMA subscript)* (COMMA)?
    ;

//subscript: '.' '.' '.' | test | [test] ':' [test] [sliceop]
subscript
    : DOT DOT DOT
    | (test COLON)
   => test (COLON (test)? (sliceop)?)?
    | (COLON)
   => COLON (test)? (sliceop)?
    | test
    ;

//sliceop: ':' [test]
sliceop
    : COLON
     (test
     |
     )
    ;

//exprlist: expr (',' expr)* [',']
exprlist
    : (expr COMMA) => expr (options {k=2;}: COMMA expr)* (COMMA)?
    | expr
    ;

//not in CPython's Grammar file
//Needed as an exprlist that does not produce tuples for del_stmt.
del_list
    : expr (options {k=2;}: COMMA expr)* (COMMA)?
    ;

//testlist: test (',' test)* [',']
testlist
    : (test COMMA)
   => test (options {k=2;}: COMMA test)* (COMMA)?
    | test
    ;

//dictmaker: test ':' test (',' test ':' test)* [',']
dictmaker
    : test COLON test
        (options {k=2;}:COMMA test COLON test)*
        (COMMA)?
    ;

//classdef: 'class' NAME ['(' [testlist] ')'] ':' suite
classdef
    : decorators? CLASS NAME (LPAREN testlist? RPAREN)? COLON suite
    ;

//arglist: (argument ',')* (argument [',']| '*' test [',' '**' test] | '**' test)
arglist
    : argument (COMMA argument)*
          (COMMA
              ( STAR test (COMMA DOUBLESTAR test)?
              | DOUBLESTAR test
              )?
          )?
    | STAR test (COMMA DOUBLESTAR test)?
    | DOUBLESTAR test
    ;

//argument: test [gen_for] | test '=' test  # Really [keyword '='] test
argument
    : test
        ((ASSIGN test)
        | gen_for
        |
        )
    ;

//list_iter: list_for | list_if
list_iter
    : list_for
    | list_if
    ;

//list_for: 'for' exprlist 'in' testlist_safe [list_iter]
list_for
    : FOR exprlist IN testlist (list_iter)?
    ;

//list_if: 'if' test [list_iter]
list_if
    : IF test (list_iter)?
    ;

//gen_iter: gen_for | gen_if
gen_iter
    : gen_for
    | gen_if
    ;

//gen_for: 'for' exprlist 'in' or_test [gen_iter]
gen_for
    : FOR exprlist IN or_test gen_iter?
    ;

//gen_if: 'if' old_test [gen_iter]
gen_if
    : IF test gen_iter?
    ;

//yield_expr: 'yield' [testlist]
yield_expr
    : YIELD testlist?
    ;

AS        : 'as' ;
ASSERT    : 'assert' ;
BREAK     : 'break' ;
CLASS     : 'class' ;
CONTINUE  : 'continue' ;
DEF       : 'def' ;
DELETE    : 'del' ;
ELIF      : 'elif' ;
EXCEPT    : 'except' ;
EXEC      : 'exec' ;
FINALLY   : 'finally' ;
FROM      : 'from' ;
FOR       : 'for' ;
GLOBAL    : 'global' ;
IF        : 'if' ;
IMPORT    : 'import' ;
IN        : 'in' ;
IS        : 'is' ;
LAMBDA    : 'lambda' ;
ORELSE    : 'else' ;
PASS      : 'pass'  ;
PRINT     : 'print' ;
RAISE     : 'raise' ;
RETURN    : 'return' ;
TRY       : 'try' ;
WHILE     : 'while' ;
WITH      : 'with' ;
YIELD     : 'yield' ;

LPAREN    : '(' {implicitLineJoiningLevel++;} ;

RPAREN    : ')' {implicitLineJoiningLevel--;} ;

LBRACK    : '[' {implicitLineJoiningLevel++;} ;

RBRACK    : ']' {implicitLineJoiningLevel--;} ;

COLON     : ':' ;

COMMA    : ',' ;

SEMI    : ';' ;

PLUS    : '+' ;

MINUS    : '-' ;

STAR    : '*' ;

SLASH    : '/' ;

VBAR    : '|' ;

AMPER    : '&' ;

LESS    : '<' ;

GREATER    : '>' ;

ASSIGN    : '=' ;

PERCENT    : '%' ;

BACKQUOTE    : '`' ;

LCURLY    : '{' {implicitLineJoiningLevel++;} ;

RCURLY    : '}' {implicitLineJoiningLevel--;} ;

CIRCUMFLEX    : '^' ;

TILDE    : '~' ;

EQUAL    : '==' ;

NOTEQUAL    : '!=' ;

ALT_NOTEQUAL: '<>' ;

LESSEQUAL    : '<=' ;

LEFTSHIFT    : '<<' ;

GREATEREQUAL    : '>=' ;

RIGHTSHIFT    : '>>' ;

PLUSEQUAL    : '+=' ;

MINUSEQUAL    : '-=' ;

DOUBLESTAR    : '**' ;

STAREQUAL    : '*=' ;

DOUBLESLASH    : '//' ;

SLASHEQUAL    : '/=' ;

VBAREQUAL    : '|=' ;

PERCENTEQUAL    : '%=' ;

AMPEREQUAL    : '&=' ;

CIRCUMFLEXEQUAL    : '^=' ;

LEFTSHIFTEQUAL    : '<<=' ;

RIGHTSHIFTEQUAL    : '>>=' ;

DOUBLESTAREQUAL    : '**=' ;

DOUBLESLASHEQUAL    : '//=' ;

DOT : '.' ;

AT : '@' ;

AND : 'and' ;

OR : 'or' ;

NOT : 'not' ;

FLOAT
    :   '.' DIGITS (Exponent)?
    |   DIGITS '.' Exponent
    |   DIGITS ('.' (DIGITS (Exponent)?)? | Exponent)
    ;

LONGINT
    :   INT ('l'|'L')
    ;

fragment
Exponent
    :    ('e' | 'E') ( '+' | '-' )? DIGITS
    ;

INT :   // Hex
        '0' ('x' | 'X') ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
    |   // Octal
        '0'  ( '0' .. '7' )*
    |   '1'..'9' DIGITS*
    ;

COMPLEX
    :   DIGITS+ ('j'|'J')
    |   FLOAT ('j'|'J')
    ;

fragment
DIGITS : ( '0' .. '9' )+ ;

NAME:    ( 'a' .. 'z' | 'A' .. 'Z' | '_')
        ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
    ;

/** Match various string types.  Note that greedy=false implies '''
 *  should make us exit loop not continue.
 */
STRING
    :   ('r'|'u'|'ur'|'R'|'U'|'UR'|'uR'|'Ur')?
        (   '\'\'\'' (options {greedy=false;}:TRIAPOS)* '\'\'\''
        |   '"""' (options {greedy=false;}:TRIQUOTE)* '"""'
        |   '"' (ESC|~('\\'|'\n'|'"'))* '"'
        |   '\'' (ESC|~('\\'|'\n'|'\''))* '\''
        ) {
           if (state.tokenStartLine != input.getLine()) {
               state.tokenStartLine = input.getLine();
               state.tokenStartCharPositionInLine = -2;
           }
        }
    ;

TRISTRINGPART
    : ('r'|'u'|'ur'|'R'|'U'|'UR'|'uR'|'Ur')?
        (   '\'\'\'' ~('\'\'\'')*
        |   '"""' ~('"""')*
        )
    ;

STRINGPART
    : ('r'|'u'|'ur'|'R'|'U'|'UR'|'uR'|'Ur')?
        (   '"' (ESC|~('\\'|'\n'|'"'))* CONTINUED_LINE
        |   '\'' (ESC|~('\\'|'\n'|'\''))* CONTINUED_LINE
        )
    ;


/** the two '"'? cause a warning -- is there a way to avoid that? */
fragment
TRIQUOTE
    : '"'? '"'? (ESC|~('\\'|'"'))+
    ;

/** the two '\''? cause a warning -- is there a way to avoid that? */
fragment
TRIAPOS
    : '\''? '\''? (ESC|~('\\'|'\''))+
    ;

fragment
ESC
    :    '\\' .
    ;

/** Consume a newline and any whitespace at start of next line
 *  unless the next line contains only white space, in that case
 *  emit a newline.
 */
CONTINUED_LINE
@init {
    boolean extraNewlines = false;
}
    :    '\\' ('\r')? '\n' (' '|'\t')*  { $channel=HIDDEN; }
         ( COMMENT
         | nl=NEWLINE
           {
               extraNewlines = true;
           }        
         |
         ) {
               if (input.LA(1) == -1) {
                   if (extraNewlines) {
                       throw new ParseException("invalid syntax");
                   }
                   emit(new CommonToken(TRAILBACKSLASH,"\\"));
               }
           }
    ;

/** Treat a sequence of blank lines as a single blank line.  If
 *  nested within a (..), {..}, or [..], then ignore newlines.
 *  If the first newline starts in column one, they are to be ignored.
 *
 *  Frank Wierzbicki added: Also ignore FORMFEEDS (\u000C).
 */
NEWLINE
    :   (('\u000C')?('\r')? '\n' )+ {
         if ( startPos==0 || implicitLineJoiningLevel>0 )
            $channel=HIDDEN;
        }
    ;

WS  :    {startPos>0}?=> (' '|'\t'|'\u000C')+ {$channel=HIDDEN;}
    ;

/** Grab everything before a real symbol.  Then if newline, kill it
 *  as this is a blank line.  If whitespace followed by comment, kill it
 *  as it's a comment on a line by itself.
 *
 *  Ignore leading whitespace when nested in [..], (..), {..}.
 */
LEADING_WS
@init {
    int spaces = 0;
    int newlines = 0;
}
    :   {startPos==0}?=>
        (   {implicitLineJoiningLevel>0}? ( ' ' | '\t' )+ {$channel=HIDDEN;}
        |    (     ' '  { spaces++; }
             |    '\t' { spaces += 8; spaces -= (spaces \% 8); }
             )+
             ( ('\r')? '\n' {newlines++; }
             )* {
                   if (input.LA(1) != -1 || newlines == 0) {
                       // make a string of n spaces where n is column number - 1
                       char[] indentation = new char[spaces];
                       for (int i=0; i<spaces; i++) {
                           indentation[i] = ' ';
                       }
                       CommonToken c = new CommonToken(LEADING_WS,new String(indentation));
                       c.setLine(input.getLine());
                       c.setCharPositionInLine(input.getCharPositionInLine());
                       c.setStartIndex(input.index() - 1);
                       c.setStopIndex(input.index() - 1);
                       emit(c);
                       // kill trailing newline if present and then ignore
                       if (newlines != 0) {
                           if (state.token!=null) {
                               state.token.setChannel(HIDDEN);
                           } else {
                               $channel=HIDDEN;
                           }
                       }
                   } else {
                       // make a string of n newlines
                       char[] nls = new char[newlines];
                       for (int i=0; i<newlines; i++) {
                           nls[i] = '\n';
                       }
                       CommonToken c = new CommonToken(NEWLINE,new String(nls));
                       c.setLine(input.getLine());
                       c.setCharPositionInLine(input.getCharPositionInLine());
                       c.setStartIndex(input.index() - 1);
                       c.setStopIndex(input.index() - 1);
                       emit(c);
                   }
                }
        )
    ;

/** Comments not on line by themselves are turned into newlines.

    b = a # end of line comment

    or

    a = [1, # weird
         2]

    This rule is invoked directly by nextToken when the comment is in
    first column or when comment is on end of nonwhitespace line.

    Only match \n here if we didn't start on left edge; let NEWLINE return that.
    Kill if newlines if we live on a line by ourselves

    Consume any leading whitespace if it starts on left edge.
 */
COMMENT
@init {
    $channel=HIDDEN;
}
    :    {startPos==0}?=> (' '|'\t')* '#' (~'\n')* '\n'+
    |    '#' (~'\n')* // let NEWLINE handle \n unless char pos==0 for '#'
    ;

