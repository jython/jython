/*
Licensing - this is based on work originally performed by Parr +
Craymer under a standard BSD license, so make certain we
acknowledge and incorp as desired. I don't see any issues here, and
this was dicussed at PyCon 2007.

Objective: construct generational grammar to represent desired AST
Let's freely using node copying to make visiting super simple

We should emulate existing CPython AST as much as possible as this
gives us a very useful target. It's much more likely that _ast does
the right thing than our work at this stage.

Use a two-step approach:

1. Build a simple AST, that is one that doesn't have to worry about
splitting say keywords from arguments, because this require context
sensitivity, and who wants to deal with that

2. Rewrite that with a tree grammar to conform to existing CPython 2.5
Zephyr; this would includ doing such analysis as store/load, key=value
splits, etc.

We can also do some fixups of legal syntax at this point

May still need to do some semantic analysis, esp with respect to from
__future__ type scenarios. See existing Jython, but please don't do it
the same way.

- Jim Baker

*/

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

/** Beginning of Python 2.5 Grammar, Jim Baker, based on
 *
 *  Python 2.3.3 Grammar
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
 */
 
 /*
  * CHANGELOG
  * 
  * 
  * Version 2.? (? 2007)
  * 	o Add BOLLEAN type
  *	o expr_stmt
  *	o varargslist : correcting rewrite rule to conforming to 2.3 CPython syntax
  *	o correcting various rewrite rule but must to be re-cheched (commented as TODO)
 */
 
 grammar Python;

options {
    ASTLabelType=CommonTree;
    language=Java;
    output=AST;
    backtrack=true;
}


tokens {
    INDENT;
    DEDENT;

    Module;
    Body;
    Import; ImportFrom; Names; Name; Alias; AsName;Level;
    Decorators; Decorator;
    ClassDef; Bases; 
    FunctionDef; Func;
    Params;Param; Args; StarArgs; KWArgs; Keywords;Attribute;Attr;
    Keyword; Arg; Vararg; Arguments;
    Assign; Targets; Id; Ctx; Store; Load;
    Expr; Call; Subscript; Slice;
    Tuple; List; Dict; Set; Elts; Keys; Values; Value; /* TODO: is 'Values' still needed (replaced by 'Value' like Cpython) ? */
    If; Test;
    OrElse; /* follow Python.asdl in naming... */
    Elif; /* is this even necessary? check the CPython AST... */
    With; Context;
    Try; Except; Finally;
    ListComp;
    For; Target; Iter; Gen; GenTarget; GenIf;
    Body;
    While; 
    Pass; Print;
    Return; Yield; 
    Decorator;
    Comparison;

    None;
    BoolOp; BinOp; UnaryOp;Operand;
    Left; Right; Op;
    Mult;Div;Add;UAdd;Sub;USub;FloorDiv;Mod;Pow; 
    LONG_INTEGER; 
    Num; N;
    Str; S;
    Defaults;
}

@header { 
package org.python.antlr;
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
}

module
    : file_input -> ^(Module ^(Body["body"] file_input))
    ;

single_input
    : NEWLINE
	| simple_stmt
	| compound_stmt NEWLINE
	;

file_input
    :   (NEWLINE | stmt)* -> stmt*
	;

eval_input
    :   (NEWLINE)* testlist (NEWLINE)*
	;

funcdef
    :  decorators 'def' NAME parameters COLON suite
    -> ^(FunctionDef ^(Name NAME) ^(Args parameters) ^(Body suite) decorators)
	;

	
/* do decorators require a newline?  one thing is clear - we can't use
   parameters here, because these are formal params! not what a
   decorator takes! 
   ---

  */
decorators
    :   (ATSIGN NAME parameters* NEWLINE)* // TODO: check parameters
    -> ^(Decorators ^(Name ^(Id NAME) ^(Ctx ^(Load)))*)
    ;

parameters
    :   LPAREN (varargslist)? RPAREN 
    ->  (varargslist)?
	;

/*
 * 
 * For the 'star' syntax see the pep-3102:  http://www.python.org/dev/peps/pep-3102/
 * TODO: recheck the rewrite rule especially for the None node /!\
*/
varargslist
    :   defparameter (options {greedy=true;}:COMMA defparameter)*
        (COMMA
            ( STAR starargs=NAME (COMMA DOUBLESTAR kwargs=NAME)?
            | DOUBLESTAR kwargs=NAME
            )?
        )? -> ^(Arguments defparameter*) ^(Vararg) ^(StarArgs $starargs)? ^(KWArgs $kwargs?) ^(Defaults)
    |   STAR starargs=NAME (COMMA DOUBLESTAR kwargs=NAME)? -> ^(StarArgs $starargs) ^(KWArgs $kwargs)?
    |   DOUBLESTAR kwargs=NAME -> ^(KWArgs $kwargs)
    ;

/* may need to do a second pass here? */

defparameter
    :   fpdef (ASSIGN expr)? -> ^(Args fpdef)
    ;

fpdef
    :   NAME -> ^(Name ^(Id NAME) ^(Ctx ^(Param))) 
	|   LPAREN fplist RPAREN -> fplist
	;

/* actually this is a tuple! TODO */

fplist
    :   fpdef (options {greedy=true;}:COMMA fpdef)* (COMMA)?
    -> ^(Tuple fpdef*)
	;

stmt: simple_stmt
	| compound_stmt
	;

simple_stmt
    :   small_stmt (options {greedy=true;}:SEMI small_stmt)* (SEMI)? NEWLINE
    -> small_stmt*
	;

small_stmt: expr_stmt
	| print_stmt
	| del_stmt
	| pass_stmt
	| flow_stmt
	| import_stmt
	| global_stmt
	| exec_stmt
	| assert_stmt
	;

/* Need to write this to distinguish assignment from just evaluating
   an expression. Also an Expr needs to be further unpacked, such as
   dotted names, function application, etc.

   augassign is not quite right, it's just one of a possible assignment

   another thing; we might have a side effect expression - often!
   so let's call this a bit better

*/

/*
 * For the assignment and evalutation, a context (ctx) is declared: respectivly Store or Load (see trailer rule)
 * TODO: - rechecking Store/Load
 */
expr_stmt
    : lhs=exprlist
        (augassign rhs=exprlist -> ^(augassign ^(Targets ^({$lhs.tree} ^(Ctx ^(Store)) ) ) ^(Value $rhs) ) 
        |                       -> ^(Expr ^(Value["value"] $lhs)) // Set Contex here? Rechechek when ANTLR bug will be solved
        )
    ;

augassign
    : 	  PLUSEQUAL
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
    	| ASSIGN -> ^(Assign)
	;

/* IMSM: there's some interesting logic here, but for now we're going to keep it simple! */

print_stmt
    :   'print'
    (   testlist
    |   RIGHTSHIFT testlist
    )?
 	-> ^(Print testlist) 
    ;

del_stmt: 'del' exprlist
	;

pass_stmt: 'pass' -> ^(Pass)
	;

flow_stmt: break_stmt
	| continue_stmt
	| return_stmt
	| raise_stmt
	| yield_stmt
	;

break_stmt: 'break'
	;

continue_stmt: 'continue'
	;

return_stmt: 'return' (testlist)?
    -> ^(Return testlist?) // TODO recheck
	;

yield_stmt: 'yield' testlist
    -> ^(Yield testlist)
	;

raise_stmt: 'raise' (test (COMMA test (COMMA test)?)?)?
	;
/*
old overgenerous code
import_stmt
    :   'import' dotted_as_name (COMMA dotted_as_name)*
	|   'from' dotted_name 'import'
        (STAR | import_as_name (COMMA import_as_name)*)
	;

import_as_name
    :   NAME (NAME NAME)?
	;
*/
/* TODO: None and Level*/
import_stmt
    :   'import' import_as_name (COMMA import_as_name)*
    -> ^(Import ^(Names["names"] import_as_name*))

	|   'from' dotted_name 'import'
        (STAR | import_as_name (COMMA import_as_name)*)
        -> ^(ImportFrom ^(Module["module"] dotted_name) ^(Names["names"] import_as_name* ) ^(Level) )
	;

import_as_name
    :   alias=dotted_name ('as' asname=NAME)?
    -> ^(Alias["alias"] ^(Name["name"] $alias) ^(AsName["asname"] {$asname} ))
	;

dotted_as_name: dotted_name (NAME NAME)?
	;

dotted_name: NAME (DOT NAME)*
    //-> NAME* //-> ^(Name NAME*)
	;

global_stmt: 'global' NAME (COMMA NAME)*
	;

exec_stmt: 'exec' expr ('in' test (COMMA test)?)?
	;

assert_stmt: 'assert' test (COMMA test)?
	;


compound_stmt:    if_stmt
		| while_stmt
		| for_stmt
	    	| with_stmt
		| try_stmt
		| funcdef
		| classdef
	;

/* exprlist is too broad, since we can't do arbitrary functions here,
   however we can arbitrary tuple unpacking */

for_stmt: 'for' exprlist 'in' testlist COLON body=suite ('else' COLON else_suite=suite)?
    -> ^(For ^(Target exprlist) ^(Iter testlist) ^(Body $body) ^(OrElse $else_suite)?)
	;

while_stmt: 'while' test COLON body=suite ('else' COLON else_suite=suite)?
    -> ^(While ^(Test test) ^(Body $body) ^(OrElse $else_suite)?)
	;

if_stmt: 'if' test COLON body=suite ('elif' test COLON elif_suite=suite)* ('else' COLON else_suite=suite)?
    -> ^(If ^(Test test) ^(Body $body) ^(Elif $elif_suite)* ^(OrElse $else_suite)?)
	;

with_stmt: 'with' context=exprlist ('as' args=exprlist)? COLON body=suite
    -> ^(With ^(Context $context) ^(Target $args) ^(Body $body))
    ;

/*
 * based on : http://www.python.org/doc/2.5/ref/try.html
 * and also described in http://www.python.org/dev/peps/pep-0341/
 * 
 */
try_stmt
    :   'try' COLON try_suite=suite
        (except_clauses
        ('else' COLON else_suite=suite)?
        ('finally' COLON finally_suite=suite)?
        
        | ('finally' COLON finally_suite=suite))
        
    -> ^(Try $try_suite ^(Except except_clauses)? ^(OrElse $else_suite)? ^(Finally $finally_suite)?) // TODO recheck this
	;

/*
 * The final exception clause without test should be the last one:
 *
 * except EOFERROR:
 *	pass
 *  except:	
 *  	pass
 *
 * TODO: be 2.3 compliant
*/

except_clauses
    : ((except_clause COLON except_suite=suite)+ ('except' COLON except_last=suite)?) 
    -> ^(Except $except_suite) ^(Except $except_last)? 
    | ('except' COLON except_notest=suite) -> ^(Except $except_notest)
    ;

except_clause: 'except' (test (COMMA test)*)//? //<- TODO: need a '?' ?
	;

suite: 	  simple_stmt
	| NEWLINE INDENT (stmt)+ DEDENT -> stmt
	;


test: and_test ('or' and_test)*
	| lambdef
	;

and_test
	: not_test ('and' not_test)*
	;

not_test
	: 'not' not_test
	| comparison
	;


comparison
    : expr (comp_op expr)*
	;

/*
comparison
    : expr comp_op expr -> ^(comp_op expr*)
    | expr
	;
*/
comp_op: LESS
	|GREATER
	|EQUAL
	|GREATEREQUAL
	|LESSEQUAL
	|ALT_NOTEQUAL
	|NOTEQUAL
	|'in'
	|'not' 'in'
	|'is'
	|'is' 'not'
	;

   	
/* what's going on here is we're working through our precedences,
   from xor to power to function application */

expr: xor_expr (VBAR^ xor_expr)*
	;

xor_expr: and_expr (CIRCUMFLEX^ and_expr)*
	;

and_expr: shift_expr (AMPER^ shift_expr)*
	;

shift_expr: arith_expr ((LEFTSHIFT|RIGHTSHIFT)^ arith_expr)*
	;

/* TODO: recheck if rewrite rule can be simplified with template (be less redundant) 
 *       /!\ set CONTEXT (e.g. a+1)
 */
arith_expr: 
	/* Unary operation */
	(    term -> term
	   | PLUS term -> ^(UnaryOp ^(Op["op"] ^(UAdd)) ^(Operand["operand"] term))
	   | MINUS arith_expr -> ^(UnaryOp ^(Op["op"] ^(USub)) ^(Operand["operand"] arith_expr))
	)
	
	/* Binary operation */
	(   PLUS right=term -> ^(BinOp ^(Left["left"] $arith_expr) ^(Op["op"] Add) ^(Right["right"] $right))
	  | MINUS right=term -> ^(BinOp ^(Left["left"] $arith_expr) ^(Op["op"] Sub) ^(Right["right"] $right))
	  
	)*
	;

term: //factor ((STAR | SLASH | PERCENT | DOUBLESLASH )^ factor)*
	(factor -> factor)
	
	( STAR right=factor -> ^(BinOp ^(Left["left"] $term) ^(Op["op"] Mult) ^(Right["right"] $right))
	 |SLASH right=factor -> ^(BinOp ^(Left["left"] $term) ^(Op["op"] Div) ^(Right["right"] $right))
	 |PERCENT right=factor -> ^(BinOp ^(Left["left"] $term) ^(Op["op"] Mod) ^(Right["right"] $right))
	 |DOUBLESLASH right=factor -> ^(BinOp ^(Left["left"] $term) ^(Op["op"] FloorDiv) ^(Right["right"] $right))
	)*
	;

factor
	:  (PLUS|MINUS|TILDE) factor -> factor
	| power -> power
	//power
	;

power
	: (dotted_expr-> dotted_expr) 
	  (options {greedy=true;}:DOUBLESTAR arith_expr -> ^(BinOp ^(Left["left"] dotted_expr) ^(Op["op"] ^(Pow)) ^(Right["right"] arith_expr)) )? 
	;

atom
    : LPAREN (exprlist)? RPAREN -> exprlist?
	| LBRACK (listmaker)? RBRACK -> ^(List listmaker?)
	| LCURLY (dictmaker)? RCURLY -> ^(Dict dictmaker?) // TODO: need a '?'
	| BACKQUOTE testlist BACKQUOTE
	| NAME -> ^(Name ^(Id["id"] NAME))
	| INT -> ^(Num ^(N["n"] INT))
    	| LONGINT
	| FLOAT
    	| COMPLEX
	| (STRING)+ -> ^(Str ^(S["s"] STRING)) //TODO
	;

listmaker
    :   expr (options {k=2;}:COMMA expr)+ (COMMA)? -> expr+
    |   expr COMMA -> expr
    |   listcomp
    |   expr -> expr
	;

listcomp
    :  expr list_for
    -> ^(ListComp ^(Target expr) list_for)
    ;

genexp
    : expr list_for
    -> ^(Gen ^(Target expr) list_for)
    ;    

lambdef: 'lambda' (varargslist)? COLON test
	;

/*
 * The first rule is used for the Function call:
 *  the AST CPython requires a Call and a Func nodes before indicating the function name (i.e atom).
 *  Either it's an atom and the appropriate atom's node is used or it's a function and we use the subtree likes CPython
 *
 * TODO: finding a way to "increase the depth" of the atom root-node in the function call
 *	  but it seems to be an ANTLR bug: see http://www.antlr.org:8888/browse/ANTLR-133
 *	  and http://www.antlr.org:8080/pipermail/antlr-interest/2007-June/021012.html
 */
/*trailer	: (atom -> atom)
	   (( LPAREN arglist RPAREN -> ^(Call ^(Func ^(atom ^(Ctx ^(Load)))) arglist))
	     | LBRACK subscriptlist RBRACK -> ^(Subscript subscriptlist)
	     //| DOT NAME -> NAME
	     //| DOT expr -> ^(expr ^(Attribute) ^(Ctx ^(Load)) ^(Attr atom)) // TODO check the rewrite rule
	   )*	
	
	;*/
/*
 * TODO: - setting Context as a $trailer's child (in [] subrule)
 *	 - rewrite rule with dotted expression
 */
trailer[boolean dotted] :
	  (a=atom -> atom)
	   ( ( LPAREN arglist RPAREN -> {!dotted}? ^(Call ^(Func ^({$a.tree} ^(Ctx ^(Load)))) arglist)
	   			     -> ^(Call ^(Func ^({$a.tree} ^(Ctx ^(Load)))) arglist) )
	    |( LBRACK subscriptlist RBRACK -> {!dotted}? ^(Subscript ^(Value $trailer ^(Ctx ^(Load))) ^(Slice /*subscriptlist*/))
	    				   -> ^(Subscript ^(Value ^($trailer ^(Ctx ^(Load)))) ^(Slice /*subscriptlist*/)) )
	   )*	
	 
	;

dotted_expr 
	:
	 (tr=trailer[false] -> $tr) (DOT attr=trailer[true] -> ^(Attribute ^(Value ^($dotted_expr ^(Ctx ^(Load))) ^(Attr $attr) ^(Ctx ^(Load))) ) )*
	 ;
	
subscriptlist
    :   subscript (options {greedy=true;}:COMMA subscript)* (COMMA)?
	;

subscript
	: DOT DOT DOT
    | test (COLON (test)? (sliceop)?)?
    | COLON (test)? (sliceop)?
    ;

sliceop: COLON (test)?
	;

exprlist
/*    : expr (options {k=2;greedy=true;}:'for' exprlist 'in' testlist (list_iter)?)   list_for -> ^(Gen ^(Target expr) list_for) */
    : expr list_for -> ^(Gen ^(Target expr) list_for)
    | expr (options {k=2;}:COMMA expr)+ (COMMA)? -> ^(Tuple expr+)
    | expr COMMA -> ^(Tuple expr) 
    | expr
	;

testlist
    : test (options {k=2;}: COMMA test)* (COMMA)? -> test*
    ;

dictmaker
    : test COLON test
        (options {k=2;}:COMMA test COLON test)* (COMMA)?
    -> test*
    ;

classdef
    : 'class' NAME (LPAREN testlist RPAREN)? COLON suite
    -> ^(ClassDef ^(Name NAME) ^(Bases testlist?) ^(Body suite)) // TODO check the rewriting rule
    	;

/*
 * TODO: None (predicate?)
 */
arglist
    :  argument (COMMA argument)*
        ( COMMA
          ( STAR starargs=test (COMMA DOUBLESTAR kwargs=test)?
          | DOUBLESTAR kwargs=test
          )?
        )? -> ^(Args argument*) ^(Keywords) ^(StarArgs $starargs?) ^(KWArgs $kwargs?) // TODO check the rewriting rule + None+Keywords+Args
    |   STAR starargs=test (COMMA DOUBLESTAR kwargs=test)? -> ^(StarArgs $starargs) ^(KWArgs $kwargs?)
    |   DOUBLESTAR kwargs=test -> ^(KWArgs $kwargs)
    | -> ^(Args) ^(Keywords) ^(StarArgs) ^(KWArgs)
    ;


argument 
    : NAME ASSIGN expr -> ^(Keyword NAME expr)
    | expr -> expr //^(Arg expr)
    ;

list_iter
    : list_for
	| list_if
	;

list_for: 'for' exprlist 'in' testlist (list_iter)?
    -> ^(For exprlist) ^(Iter testlist list_iter?)
	;

list_if: 'if' expr (list_iter)?
    -> ^(If expr list_iter?)
	;

LPAREN	: '(' {implicitLineJoiningLevel++;} ;

RPAREN	: ')' {implicitLineJoiningLevel--;} ;

LBRACK	: '[' {implicitLineJoiningLevel++;} ;

RBRACK	: ']' {implicitLineJoiningLevel--;} ;

ATSIGN  : '@' ;

COLON 	: ':' ;

COMMA	: ',' ;

SEMI	: ';' ;

PLUS	: '+' ;

MINUS	: '-' ;

STAR	: '*' ;

SLASH	: '/' ;

VBAR	: '|' ;

AMPER	: '&' ;

LESS	: '<' ;

GREATER	: '>' ;

ASSIGN	: '=';

PERCENT	: '%' ;

BACKQUOTE	: '`' ;

LCURLY	: '{' {implicitLineJoiningLevel++;} ;

RCURLY	: '}' {implicitLineJoiningLevel--;} ;

CIRCUMFLEX	: '^' ;

TILDE	: '~' ;

EQUAL	: '==' ;

NOTEQUAL	: '!=' ;

ALT_NOTEQUAL: '<>' ;

LESSEQUAL	: '<=' ;

LEFTSHIFT	: '<<' ;

GREATEREQUAL	: '>=' ;

RIGHTSHIFT	: '>>' ;

PLUSEQUAL	: '+=' ;

MINUSEQUAL	: '-=' ;

DOUBLESTAR	: '**' ;

STAREQUAL	: '*=' ;

DOUBLESLASH	: '//' ;

SLASHEQUAL	: '/=' ;

VBAREQUAL	: '|=' ;

PERCENTEQUAL	: '%=' ;

AMPEREQUAL	: '&=' ;

CIRCUMFLEXEQUAL	: '^=' ;

LEFTSHIFTEQUAL	: '<<=' ;

RIGHTSHIFTEQUAL	: '>>=' ;

DOUBLESTAREQUAL	: '**=' ;

DOUBLESLASHEQUAL	: '//=' ;

DOT : '.' ;

FLOAT
	:	'.' DIGITS (Exponent)?
    |   DIGITS ('.' (DIGITS (Exponent)?)? | Exponent)
    ;

long_integer
    :   LONGINT -> ^(LONG_INTEGER LONGINT)
    ;

LONGINT
    :   INT ('l'|'L')
    ;



fragment
Exponent
	:	('e' | 'E') ( '+' | '-' )? DIGITS
	;

INT:    //(MINUS)?
	(
	// Hex
        '0' ('x' | 'X') ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
        ('l' | 'L')?
    |   // Octal
        '0' DIGITS*
    |   '1'..'9' DIGITS*)
    ;
	
COMPLEX
    :   INT ('j'|'J')
    |   FLOAT ('j'|'J')
    ;

fragment
DIGITS : ( '0' .. '9' )+ ;

NAME:	( 'a' .. 'z' | 'A' .. 'Z' | '_')
        ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
    ;

/** Match various string types.  Note that greedy=false implies '''
 *  should make us exit loop not continue.
 */
STRING
    :   ('r'|'u'|'ur')?
        (   '\'\'\'' (options {greedy=false;}:.)* '\'\'\''
        |   '"""' (options {greedy=false;}:.)* '"""'
        |   '"' (ESC|~('\\'|'\n'|'"'))* '"'
        |   '\'' (ESC|~('\\'|'\n'|'\''))* '\''
        )
	;

fragment
ESC
	:	'\\' .
	;

/** Consume a newline and any whitespace at start of next line */
CONTINUED_LINE
	:	'\\' ('\r')? '\n' (' '|'\t')* { $channel=HIDDEN; }
	;

/** Treat a sequence of blank lines as a single blank line.  If
 *  nested within a (..), {..}, or [..], then ignore newlines.
 *  If the first newline starts in column one, they are to be ignored.
 */
NEWLINE
    :   (('\r')? '\n' )+
        {if ( startPos==0 || implicitLineJoiningLevel>0 )
            $channel=HIDDEN;
        }
    ;

WS	:	{startPos>0}?=> (' '|'\t')+ {$channel=HIDDEN;}
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
       	|	( 	' '  { spaces++; }
        	|	'\t' { spaces += 8; spaces -= (spaces \% 8); }
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
        	( ('\r')? '\n' {if (token!=null) token.setChannel(99); else $channel=HIDDEN;})*
           // {token.setChannel(99); }
        )

/*
        |   // if comment, then only thing on a line; kill so we
            // ignore totally also wack any following newlines as
            // they cannot be terminating a statement
            '#' (~'\n')* ('\n')+ 
            {if (token!=null) token.setChannel(99); else $channel=HIDDEN;}
        )?
        */
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
    :	{startPos==0}?=> (' '|'\t')* '#' (~'\n')* '\n'+
    |	{startPos>0}?=> '#' (~'\n')* // let NEWLINE handle \n unless char pos==0 for '#'
    ;
