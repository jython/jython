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
 *  Updated to Python 2.5 by Frank Wierzbicki.
 *
 *  This particular version has some changes to allow "partial" parsing
 *  So that an interactive session can tell if it should wait for more
 *  input or not.  For example, this grammar will allow a String that
 *  starts with """ but has no ending """ and will allow a Suite to have
 *  an indent but no dedent.
 */

grammar PythonPartial;

tokens {
    INDENT;
    DEDENT;
    ENDMARKER;
}

@header { 
package org.python.antlr;
} 

@members {
    boolean debugOn = false;

    private void debug(String message) {
        if (debugOn) {
            System.out.println(message);
        }
    }

    /*
    protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(ttype, input);
    }
    protected void mismatch(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
        throw e;
    }

	protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
		throws RecognitionException
	{
        mismatch(input, ttype, follow);
        return null;
    }
    */

	public void emitErrorMessage(String msg) {
		//System.err.print("[EMITTING] ");
    }

	public void reportError(RecognitionException e) {
		//System.err.print("[REPORTING] ");
		// if we've already reported an error and have not matched a token
		// yet successfully, don't report any errors.
		if ( state.errorRecovery ) {
			System.err.print("[SPURIOUS] ");
			return;
		}
		state.syntaxErrors++; // don't count spurious
		state.errorRecovery = true;

		displayRecognitionError(this.getTokenNames(), e);
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
int implicitLineJoiningLevel = 0;
int startPos=-1;
public boolean eofWhileNested = false;
    public Token nextToken() {
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
			}
            catch (RecognitionException re) {
                throw new ParseException(re);
            }
        }
    }

	public void reportError(RecognitionException e) {
		System.err.print("[LEXER REPORTING] ");
		// if we've already reported an error and have not matched a token
		// yet successfully, don't report any errors.
		if ( state.errorRecovery ) {
			System.err.print("[SPURIOUS] ");
			return;
		}
		state.syntaxErrors++; // don't count spurious
		state.errorRecovery = true;

		displayRecognitionError(this.getTokenNames(), e);
	}


}

single_input : NEWLINE
             | simple_stmt {debug("matched simple_stmt");}
             | compound_stmt NEWLINE?
             ;

decorators: decorator+
          ;

decorator: AT dotted_attr (LPAREN arglist? RPAREN)? NEWLINE
         ;

dotted_attr
    : NAME (DOT NAME)*
    ;

funcdef : decorators? 'def' NAME parameters COLON suite
        ;

parameters : LPAREN (varargslist)? RPAREN
           ;

varargslist : defparameter (options {greedy=true;}:COMMA defparameter)*
              (COMMA
                  ( STAR NAME (COMMA DOUBLESTAR NAME)?
                  | DOUBLESTAR NAME
                  )?
              )?
            | STAR NAME (COMMA DOUBLESTAR NAME)?
            | DOUBLESTAR NAME
            ;

defparameter : fpdef (ASSIGN test)?
             ;

fpdef : NAME
      | LPAREN fplist RPAREN
      ;

fplist : fpdef (options {greedy=true;}:COMMA fpdef)* (COMMA)?
       ;

stmt : simple_stmt
     | compound_stmt
     ;

simple_stmt : small_stmt (options {greedy=true;}:SEMI small_stmt)* (SEMI)? (NEWLINE|ENDMARKER)
            ;

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

expr_stmt : testlist {debug("matched expr_stmt");}
            ( augassign yield_expr
            | augassign testlist
            | assigns
            )?
          ;

assigns
    : assign_testlist+
    | assign_yield+
    ;

assign_testlist
       : ASSIGN testlist
       ;

assign_yield
    : ASSIGN yield_expr
    ;

augassign : PLUSEQUAL
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

print_stmt : 'print' (printlist | RIGHTSHIFT printlist)?
           ;

printlist returns [boolean newline]
    : test (options {k=2;}: COMMA test)* (COMMA)?
    ;

del_stmt : 'del' exprlist
         ;

pass_stmt : 'pass'
          ;

flow_stmt : break_stmt
          | continue_stmt
          | return_stmt
          | raise_stmt
          | yield_stmt
          ;

break_stmt : 'break'
           ;

continue_stmt : 'continue'
              ;

return_stmt : 'return' (testlist)?
            ;

yield_stmt : yield_expr
           ;

raise_stmt: 'raise' (test (COMMA test (COMMA test)?)?)?
          ;

import_stmt : import_name
            | import_from
            ;

import_name : 'import' dotted_as_names
            ;

import_from: 'from' (DOT* dotted_name | DOT+) 'import'
              (STAR
              | import_as_names
              | LPAREN import_as_names RPAREN
              )
           ;

import_as_names : import_as_name (COMMA import_as_name)* (COMMA)?
                ;

import_as_name : NAME ('as' NAME)?
               ;

dotted_as_name : dotted_name ('as' NAME)?
               ;

dotted_as_names : dotted_as_name (COMMA dotted_as_name)*
                ;
dotted_name : NAME (DOT NAME)*
            ;

global_stmt : 'global' NAME (COMMA NAME)*
            ;

exec_stmt : 'exec' expr ('in' test (COMMA test)?)?
          ;

assert_stmt : 'assert' test (COMMA test)?
            ;

compound_stmt : if_stmt
              | while_stmt
              | for_stmt
              | try_stmt
              | with_stmt
              | funcdef
              | classdef
              ;

if_stmt: 'if' test COLON suite elif_clause*  ('else' COLON suite)?
       ;

elif_clause : 'elif' test COLON suite
            ;

while_stmt : 'while' test COLON suite ('else' COLON suite)?
           ;

for_stmt : 'for' exprlist 'in' testlist COLON suite ('else' COLON suite)?
         ;

try_stmt : 'try' COLON suite
           ( except_clause+ ('else' COLON suite)? ('finally' COLON suite)?
           | 'finally' COLON suite
           )?
         ;

with_stmt: 'with' test (with_var)? COLON suite
         ;

with_var: ('as' | NAME) expr
        ;

except_clause : 'except' (test (COMMA test)?)? COLON suite
              ;

suite : simple_stmt
      | NEWLINE ((INDENT (stmt)+ (DEDENT|ENDMARKER))|ENDMARKER)
      ;

test: or_test {debug("matched test: or_test");} 
    ( ('if' or_test 'else') => 'if' or_test 'else' test)?
    | lambdef
    ;

or_test : and_test (OR and_test)* {debug("matched or_test");} 
        ;

and_test : not_test (AND not_test)*
         ;

not_test : NOT not_test
         | comparison
         ;

comparison: expr (comp_op expr)*
          ;

comp_op : LESS
        | GREATER
        | EQUAL
        | GREATEREQUAL
        | LESSEQUAL
        | ALT_NOTEQUAL
        | NOTEQUAL
        | 'in'
        | NOT 'in'
        | 'is'
        | 'is' NOT
        ;

expr : xor_expr (VBAR xor_expr)* {debug("matched expr");}
     ;

xor_expr : and_expr (CIRCUMFLEX and_expr)*
         ;

and_expr : shift_expr (AMPER shift_expr)*
         ;

shift_expr : arith_expr ((LEFTSHIFT|RIGHTSHIFT) arith_expr)*
           ;

arith_expr: term ((PLUS|MINUS) term)*
          ;

term : factor ((STAR | SLASH | PERCENT | DOUBLESLASH ) factor)*
     ;

factor : PLUS factor
       | MINUS factor
       | TILDE factor
       | power
       ;

power : atom (trailer)* (options {greedy=true;}:DOUBLESTAR factor)?
      ;

atom : LPAREN 
       ( yield_expr
       | testlist_gexp
       )?
       RPAREN
     | LBRACK (listmaker)? RBRACK
     | LCURLY (dictmaker)? RCURLY
     | BACKQUOTE testlist BACKQUOTE
     | NAME
     | INT
     | LONGINT
     | FLOAT
     | COMPLEX
     | (STRING)+ {debug("matched STRING");} 
     | STRINGPART
     ;

listmaker : test 
            ( list_for
            | (options {greedy=true;}:COMMA test)*
            ) (COMMA)?
          ;

testlist_gexp
    : test ( (options {k=2;}: COMMA test)* (COMMA)?
           | gen_for
           )
           
    ;

lambdef: 'lambda' (varargslist)? COLON test
       ;

trailer : LPAREN (arglist)? RPAREN
        | LBRACK subscriptlist RBRACK
        | DOT NAME
        ;

subscriptlist : subscript (options {greedy=true;}:COMMA subscript)* (COMMA)?
              ;

subscript : DOT DOT DOT
          | test (COLON (test)? (sliceop)?)?
          | COLON (test)? (sliceop)?
          ;

sliceop : COLON (test)?
        ;

exprlist : expr (options {k=2;}: COMMA expr)* (COMMA)?
         ;

testlist
    : test (options {k=2;}: COMMA test)* (COMMA)? {debug("matched testlist");}
    ;

dictmaker : test COLON test (options {k=2;}:COMMA test COLON test)* (COMMA)?
          ;

classdef: 'class' NAME (LPAREN testlist? RPAREN)? COLON suite
        ;

arglist : argument (COMMA argument)*
          ( COMMA
            ( STAR test (COMMA DOUBLESTAR test)?
            | DOUBLESTAR test
            )?
          )?
        |   STAR test (COMMA DOUBLESTAR test)?
        |   DOUBLESTAR test
        ;

argument : test ( (ASSIGN test) | gen_for)?
         ;

list_iter : list_for
          | list_if
          ;

list_for : 'for' exprlist 'in' testlist (list_iter)?
         ;

list_if : 'if' test (list_iter)?
        ;

gen_iter: gen_for
        | gen_if
        ;

gen_for: 'for' exprlist 'in' or_test gen_iter?
       ;

gen_if: 'if' test gen_iter?
      ;

yield_expr : 'yield' testlist?
           ;

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
        '0' ( '0' .. '7' )*
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
    :   ('r'|'u'|'ur')?
        (   '\'\'\'' (options {greedy=false;}:TRIAPOS)* '\'\'\''
        |   '"""' (options {greedy=false;}:TRIQUOTE)* '"""'
        |   '"' (ESC|~('\\'|'\n'|'"'))* '"'
        |   '\'' (ESC|~('\\'|'\n'|'\''))* '\''
        )
    ;

STRINGPART
    :   ('r'|'u'|'ur')?
        (   '\'\'\'' ~('\'\'\'')*
        |   '"""' ~('"""')*
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
    :    '\\' ('\r')? '\n' (' '|'\t')*  { $channel=HIDDEN; }
         ( nl=NEWLINE {emit(new ClassicToken(NEWLINE,nl.getText()));}
         |
         )
    ;

/** Treat a sequence of blank lines as a single blank line.  If
 *  nested within a (..), {..}, or [..], then ignore newlines.
 *  If the first newline starts in column one, they are to be ignored.
 *
 *  Frank Wierzbicki added: Also ignore FORMFEEDS (\u000C).
 */
NEWLINE
    :   (('\u000C')?('\r')? '\n' )+
        {if ( startPos==0 || implicitLineJoiningLevel>0 )
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
}
    :   {startPos==0}?=>
        (   {implicitLineJoiningLevel>0}? ( ' ' | '\t' )+ {$channel=HIDDEN;}
           |    (     ' '  { spaces++; }
            |    '\t' { spaces += 8; spaces -= (spaces \% 8); }
               )+
            {
            // make a string of n spaces where n is column number - 1
            char[] indentation = new char[spaces];
            for (int i=0; i<spaces; i++) {
                indentation[i] = ' ';
            }
            String s = new String(indentation);
            emit(new ClassicToken(LEADING_WS,new String(indentation)));
            }
            // kill trailing newline if present and then ignore
            ( ('\r')? '\n' {if (state.token!=null) state.token.setChannel(HIDDEN); else $channel=HIDDEN;})*
           // {state.token.setChannel(99); }
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
    |    {startPos>0}?=> '#' (~'\n')* // let NEWLINE handle \n unless char pos==0 for '#'
    ;

