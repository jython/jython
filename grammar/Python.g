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
 *
 *  Baby step towards an antlr based Jython parser.
 *  Terence's Lexer is intact pretty much unchanged, the parser has
 *  been altered to produce an AST - the AST work started from tne newcompiler
 *  grammar from Jim Baker minus post-2.3 features.  The current parsing
 *  and compiling strategy looks like this:
 *
 *  Python source->Python.g->simple antlr AST->PythonWalker.g->
 *  decorated AST (org/python/parser/ast/*)->CodeCompiler(ASM)->.class
 *
 *  for a very limited set of functionality.
 */

grammar Python;
options {
    ASTLabelType=PythonTree;
    output=AST;
}

tokens {
    INDENT;
    DEDENT;
    
    Module;
    Interactive;
    Expression;
    Test;
    Msg;
    Import;
    ImportFrom;
    Level;
    Name;
    Body;
    ClassDef;
    Bases; 
    FunctionDef;
    Arguments;
    Args;
    Arg;
    Keyword;
    StarArgs;
    KWArgs;
    Assign;
    AugAssign;
    Compare;
    Expr;
    Tuple;
    List;
    Dict;
    If;
    IfExp;
    OrElse;
    Elif;
    While; 
    Pass;
    Break;
    Continue;
    Print;
    TryExcept;
    TryFinally;
    ExceptHandler;
    For;
    Return;
    Yield;
    Str;
    Num;
    IsNot;
    In;
    NotIn;
    Raise;
    Type;
    Inst;
    Tback;
    Global;
    Exec;
    Globals;
    Locals;
    Assert;
    Ellipsis;
    Comprehension;
    ListComp;
    Lambda;
    Repr;
    BinOp;
    Subscript;
    SubscriptList;
    Index;
    Target;
    Targets;
    Value;
    Lower;
    Upper;
    Step;
    UnaryOp;
    UAdd;
    USub;
    Invert;
    Delete;
    Default;
    Alias;
    Asname;
    Decorator;
    Decorators;
    With;
    GeneratorExp;
    Id;
    Iter;
    Ifs;
    Elts;
    Ctx;
    Attr;
    Call;
    Dest;
    Values;
    Newline;
    //The tokens below are not represented in the 2.5 Python.asdl
    FpList;
    StepOp;
    UpperOp;

    GenFor;
    GenIf;
    ListFor;
    ListIf;
    FinalBody;
    Parens;
    Brackets;
}

@header { 
package org.python.antlr;

import org.python.antlr.ParseException;
import org.python.antlr.PythonTree;
} 

@members {
    boolean debugOn = false;

    private void debug(String message) {
        if (debugOn) {
            System.out.println(message);
        }
    }

    protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
        throw new MismatchedTokenException(ttype, input);
    }

    protected void mismatch(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
        throw e;
    }

    /**
     * A list holding the error message(s) encountered during parse.
     */
    private List<String> _errors = new ArrayList<String>();

    /**
     * @return <code>true</code> if the parser collected one or more error messages,
     *         <code>false</code> otherwise.
     */
    public boolean hasErrors() {
      return getErrors().size() > 0;
    }

    /**
     * @return A list of the error message(s) collected during parse.
     */
    public List<String> getErrors() {
        return _errors;
    }

    /**
     * Overridden to be able to collect error messages.
     * <p>
     * Since we do not want to lose the recovery mechanism and the verbose messages.
     */
    @Override
    public void emitErrorMessage(String msg) {
       super.emitErrorMessage(msg);
       getErrors().add(msg);
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

    public Token nextToken() {
		while (true) {
			state.token = null;
			state.channel = Token.DEFAULT_CHANNEL;
			state.tokenStartCharIndex = input.index();
			state.tokenStartCharPositionInLine = input.getCharPositionInLine();
			state.tokenStartLine = input.getLine();
			state.text = null;
			if ( input.LA(1)==CharStream.EOF ) {
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
                throw new ParseException(getErrorMessage(re, this.getTokenNames()));
            }
        }
    }
}

//single_input: NEWLINE | simple_stmt | compound_stmt NEWLINE
//XXX: I don't know why, but in "compound_stmt NEWLINE"
//     Jython chokes on the NEWLINE every time -- so I made
//     it optional for now.
single_input : NEWLINE
             | simple_stmt -> ^(Interactive simple_stmt)
             | compound_stmt NEWLINE? -> ^(Interactive compound_stmt)
             ;

//file_input: (NEWLINE | stmt)* ENDMARKER
file_input : (NEWLINE | stmt)* {debug("parsed file_input");}
          -> ^(Module stmt*)
           ;

//eval_input: testlist NEWLINE* ENDMARKER
eval_input : (NEWLINE)* testlist (NEWLINE)* -> ^(Expression testlist)
           ;

//decorators: decorator+
decorators: decorator+
          ;

//decorator: '@' dotted_name [ '(' [arglist] ')' ] NEWLINE
decorator: AT dotted_attr 
           ( (LPAREN arglist? RPAREN) -> ^(Decorator dotted_attr ^(Call ^(Args arglist)?))
           | -> ^(Decorator dotted_attr)
           ) NEWLINE
         ;

dotted_attr
    : NAME (DOT^ NAME)*
    ;

//funcdef: [decorators] 'def' NAME parameters ':' suite
funcdef : decorators? 'def' NAME parameters COLON suite
       -> ^(FunctionDef 'def' ^(Name NAME) parameters ^(Body suite) ^(Decorators decorators?))
        ;

//parameters: '(' [varargslist] ')'
parameters : LPAREN 
                 (varargslist -> ^(Arguments varargslist)
                 | -> ^(Arguments)
                 )
             RPAREN
           ;

//varargslist: (fpdef ['=' test] ',')* ('*' NAME [',' '**' NAME] | '**' NAME) | fpdef ['=' test] (',' fpdef ['=' test])* [',']
varargslist : defparameter (options {greedy=true;}:COMMA defparameter)*
              (COMMA
                  ( STAR starargs=NAME (COMMA DOUBLESTAR kwargs=NAME)?
                  | DOUBLESTAR kwargs=NAME
                  )?
              )? {debug("parsed varargslist");}
           -> ^(Args defparameter+) ^(StarArgs $starargs)? ^(KWArgs $kwargs)?
            | STAR starargs=NAME (COMMA DOUBLESTAR kwargs=NAME)?{debug("parsed varargslist STARARGS");}
           -> ^(StarArgs $starargs) ^(KWArgs $kwargs)?
            | DOUBLESTAR kwargs=NAME {debug("parsed varargslist KWS");}
           -> ^(KWArgs $kwargs)
            ;

//not in CPython's Grammar file
defparameter : fpdef (ASSIGN test)? {debug("parsed defparameter");}
             ;

//fpdef: NAME | '(' fplist ')'
fpdef : NAME {debug("parsed fpdef NAME");}
      | LPAREN fplist RPAREN {debug("parsed fpdef:fplist");}
     -> ^(FpList fplist)
      ;

//fplist: fpdef (',' fpdef)* [',']
fplist : fpdef (options {greedy=true;}:COMMA fpdef)* (COMMA)?
       {debug("parsed fplist");}
      -> fpdef+
       ;

//stmt: simple_stmt | compound_stmt
stmt : simple_stmt
     | compound_stmt
     ;

//simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
simple_stmt : small_stmt (options {greedy=true;}:SEMI small_stmt)* (SEMI)? NEWLINE
           -> small_stmt+
            ;

//small_stmt: expr_stmt | print_stmt  | del_stmt | pass_stmt | flow_stmt | import_stmt | global_stmt | exec_stmt | assert_stmt
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

//expr_stmt: testlist (augassign testlist | ('=' testlist)*)
expr_stmt : lhs=testlist
            ( (augassign yield_expr -> ^(augassign $lhs yield_expr))
            | (augassign rhs=testlist -> ^(augassign $lhs $rhs))
            | ((assigns) {debug("matched assigns");} -> ^(Assign ^(Target $lhs) assigns))
            | -> $lhs
            )
          ;

//not in CPython's Grammar file
assigns
    @after {
        PythonTree pt = ((PythonTree)$assigns.tree);
        int children = pt.getChildCount();
        if (children == 1) {
            pt.token = new CommonToken(Value, "Value");
        } else {
            PythonTree c = (PythonTree)pt.getChild(children - 1);
            c.token = new CommonToken(Value, "Value");
        }
    }
    : assign_testlist+
    | assign_yield+
    ;

//not in CPython's Grammar file
assign_testlist
       : ASSIGN testlist -> ^(Target testlist)
       ;

//not in CPython's Grammar file
assign_yield
    : ASSIGN yield_expr -> ^(Value yield_expr)
    ;

//augassign: '+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' | '<<=' | '>>=' | '**=' | '//='
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

//print_stmt: 'print' ( [ test (',' test)* [','] ] | '>>' test [ (',' test)+ [','] ] )
print_stmt : 'print'
             ( t1=printlist -> {$t1.newline}? ^(Print 'print' ^(Values $t1) ^(Newline))
                            -> ^(Print 'print' ^(Values $t1))
             | RIGHTSHIFT t2=printlist2 -> {$t2.newline}? ^(Print 'print' ^(Dest RIGHTSHIFT) ^(Values $t2) ^(Newline))
                                       -> ^(Print 'print' ^(Dest RIGHTSHIFT) ^(Values $t2))
             | -> ^(Print 'print' ^(Newline))
             )
           ;

//not in CPython's Grammar file
printlist returns [boolean newline]
    : (test COMMA) => test (options {k=2;}: COMMA test)* (trailcomma=COMMA)?
    { if ($trailcomma == null) {
          $newline = true;
      } else {
          $newline = false;
      }
    }
   -> ^(Elts test+)
    | test {$newline = true;}
   -> ^(Elts test)
    ;

//not in CPython's Grammar file
printlist2 returns [boolean newline]
    : (test COMMA test) => test (options {k=2;}: COMMA test)* (trailcomma=COMMA)?
    { if ($trailcomma == null) {
          $newline = true;
      } else {
          $newline = false;
      }
    }
   -> ^(Elts test+)
    | test {$newline = true;}
   -> ^(Elts test)
    ;


//del_stmt: 'del' exprlist
del_stmt : 'del' exprlist2
        -> ^(Delete 'del' exprlist2)
         ;

//pass_stmt: 'pass'
pass_stmt : 'pass'
         -> ^(Pass 'pass')
          ;

//flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
flow_stmt : break_stmt
          | continue_stmt
          | return_stmt
          | raise_stmt
          | yield_stmt
          ;

//break_stmt: 'break'
break_stmt : 'break'
          -> ^(Break 'break')
           ;

//continue_stmt: 'continue'
continue_stmt : 'continue'
             -> ^(Continue 'continue')
              ;

//return_stmt: 'return' [testlist]
return_stmt : 'return' (testlist)?
          -> ^(Return 'return' ^(Value testlist)?)
            ;

//yield_stmt: yield_expr
yield_stmt : yield_expr
           ;

//raise_stmt: 'raise' [test [',' test [',' test]]]
raise_stmt: 'raise' (t1=test (COMMA t2=test (COMMA t3=test)?)?)?
          -> ^(Raise 'raise' ^(Type $t1)? ^(Inst $t2)? ^(Tback $t3)?)
          ;

//import_stmt: import_name | import_from
import_stmt : import_name
            | import_from
            ;

//import_name: 'import' dotted_as_names
import_name : 'import' dotted_as_names
           -> ^(Import 'import' dotted_as_names)
            ;

//import_from: ('from' ('.'* dotted_name | '.'+)
//              'import' ('*' | '(' import_as_names ')' | import_as_names))
import_from: 'from' (DOT* dotted_name | DOT+) 'import'
              (STAR
             -> ^(ImportFrom 'from' ^(Level DOT*)? ^(Name dotted_name)? ^(Import STAR))
              | import_as_names
             -> ^(ImportFrom 'from' ^(Level DOT*)? ^(Name dotted_name)? ^(Import import_as_names))
              | LPAREN import_as_names RPAREN
             -> ^(ImportFrom 'from' ^(Level DOT*)? ^(Name dotted_name)? ^(Import import_as_names))
              )
           ;

//import_as_names: import_as_name (',' import_as_name)* [',']
import_as_names : import_as_name (COMMA! import_as_name)* (COMMA!)?
                ;

//import_as_name: NAME [('as' | NAME) NAME]
import_as_name : name=NAME (keyAS asname=NAME)?
              -> ^(Alias $name ^(Asname $asname)?)
               ;

//XXX: when does Grammar match "dotted_name NAME NAME"? This may be a big
//       problem because of the keyAS rule, which matches NAME (needed to allow
//       'as' to be a method name for Java integration).

//dotted_as_name: dotted_name [('as' | NAME) NAME]
dotted_as_name : dotted_name (keyAS asname=NAME)?
              -> ^(Alias dotted_name ^(Asname NAME)?)
               ;

//dotted_as_names: dotted_as_name (',' dotted_as_name)*
dotted_as_names : dotted_as_name (COMMA! dotted_as_name)*
                ;
//dotted_name: NAME ('.' NAME)*
dotted_name : NAME (DOT NAME)*
            ;

//global_stmt: 'global' NAME (',' NAME)*
global_stmt : 'global' NAME (COMMA NAME)*
           -> ^(Global 'global' NAME+)
            ;

//exec_stmt: 'exec' expr ['in' test [',' test]]
exec_stmt : keyEXEC expr ('in' t1=test (COMMA t2=test)?)?
         -> ^(Exec keyEXEC expr ^(Globals $t1)? ^(Locals $t2)?)
          ;

//assert_stmt: 'assert' test [',' test]
assert_stmt : 'assert' t1=test (COMMA t2=test)?
           -> ^(Assert 'assert' ^(Test $t1) ^(Msg $t2)?)
            ;

//compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | funcdef | classdef
compound_stmt : if_stmt
              | while_stmt
              | for_stmt
              | try_stmt
              | with_stmt
              | funcdef
              | classdef
              ;

//if_stmt: 'if' test ':' suite ('elif' test ':' suite)* ['else' ':' suite]
if_stmt: 'if' test COLON ifsuite=suite elif_clause*  ('else' COLON elsesuite=suite)?
      -> ^(If 'if' test $ifsuite elif_clause* ^(OrElse $elsesuite)?)
       ;

//not in CPython's Grammar file
elif_clause : 'elif' test COLON suite
           -> ^(Elif test suite)
            ;

//while_stmt: 'while' test ':' suite ['else' ':' suite]
while_stmt : 'while' test COLON s1=suite ('else' COLON s2=suite)?
          -> ^(While 'while' test ^(Body $s1) ^(OrElse $s2)?)
           ;

//for_stmt: 'for' exprlist 'in' testlist ':' suite ['else' ':' suite]
for_stmt : 'for' exprlist 'in' testlist COLON s1=suite ('else' COLON s2=suite)?
        -> ^(For 'for' ^(Target exprlist) ^(Iter testlist) ^(Body $s1) ^(OrElse $s2)?)
         ;

//try_stmt: ('try' ':' suite
//           ((except_clause ':' suite)+
//	    ['else' ':' suite]
//	    ['finally' ':' suite] |
//	   'finally' ':' suite))
try_stmt : 'try' COLON trysuite=suite
           ( (except_clause+ ('else' COLON elsesuite=suite)? ('finally' COLON finalsuite=suite)?
          -> ^(TryExcept 'try' ^(Body $trysuite) except_clause+ ^(OrElse $elsesuite)? ^(FinalBody 'finally' $finalsuite)?))
           | ('finally' COLON finalsuite=suite
          -> ^(TryFinally 'try' ^(Body $trysuite) ^(FinalBody $finalsuite)))
           )
         ;

//with_stmt: 'with' test [ with_var ] ':' suite
with_stmt: 'with' test (with_var)? COLON suite
        -> ^(With test with_var? ^(Body suite))
         ;

//with_var: ('as' | NAME) expr
with_var: (keyAS | NAME) expr
        ;

//except_clause: 'except' [test [',' test]]
except_clause : 'except' (t1=test (COMMA t2=test)?)? COLON suite
             //Note: passing the 'except' keyword on so we can pass the same offset
             //      as CPython.
             -> ^(ExceptHandler 'except' ^(Type $t1)? ^(Name $t2)? ^(Body suite))
              ;

//suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
suite : simple_stmt
      | NEWLINE! INDENT (stmt)+ DEDENT
      ;

//test: or_test ['if' or_test 'else' test] | lambdef
test: o1=or_test
    ( ('if' or_test 'else') => 'if' o2=or_test 'else' test
      -> ^(IfExp ^(Test $o2) ^(Body $o1) ^(OrElse test))
    | -> or_test
    )
    | lambdef {debug("parsed lambdef");}
    ;

//or_test: and_test ('or' and_test)*
or_test : and_test (OR^ and_test)*
        ;

//and_test: not_test ('and' not_test)*
and_test : not_test (AND^ not_test)*
         ;

//not_test: 'not' not_test | comparison
not_test : NOT^ not_test
         | comparison
         ;

//comparison: expr (comp_op expr)*
comparison: expr (comp_op^ expr)*
	;

//comp_op: '<'|'>'|'=='|'>='|'<='|'<>'|'!='|'in'|'not' 'in'|'is'|'is' 'not'
comp_op : LESS
        | GREATER
        | EQUAL
        | GREATEREQUAL
        | LESSEQUAL
        | ALT_NOTEQUAL
        | NOTEQUAL
        | 'in'
        | NOT 'in' -> NotIn
        | 'is'
        | 'is' NOT -> IsNot
        ;

//expr: xor_expr ('|' xor_expr)*
expr : xor_expr (VBAR^ xor_expr)*
     ;

//xor_expr: and_expr ('^' and_expr)*
xor_expr : and_expr (CIRCUMFLEX^ and_expr)*
         ;

//and_expr: shift_expr ('&' shift_expr)*
and_expr : shift_expr (AMPER^ shift_expr)*
         ;

//shift_expr: arith_expr (('<<'|'>>') arith_expr)*
shift_expr : arith_expr ((LEFTSHIFT^|RIGHTSHIFT^) arith_expr)*
           ;

//arith_expr: term (('+'|'-') term)*
arith_expr: term ((PLUS^|MINUS^) term)*
	;

//term: factor (('*'|'/'|'%'|'//') factor)*
term : factor ((STAR^ | SLASH^ | PERCENT^ | DOUBLESLASH^ ) factor)*
     ;

//factor: ('+'|'-'|'~') factor | power
factor : PLUS factor -> ^(UAdd PLUS factor)
       | MINUS factor -> ^(USub MINUS factor)
       | TILDE factor -> ^(Invert TILDE factor)
       | power
       ;

//power: atom trailer* ['**' factor]
power : atom (trailer^)* (options {greedy=true;}:DOUBLESTAR^ factor)?
      ;

//atom: ('(' [yield_expr|testlist_gexp] ')' |
//       '[' [listmaker] ']' |
//       '{' [dictmaker] '}' |
//       '`' testlist1 '`' |
//       NAME | NUMBER | STRING+)
atom : LPAREN 
       ( yield_expr    -> ^(Parens LPAREN yield_expr)
       | testlist_gexp {debug("parsed testlist_gexp");} -> ^(Parens LPAREN testlist_gexp)
       | -> ^(Tuple)
       )
       RPAREN
     | LBRACK
       (listmaker -> ^(Brackets LBRACK listmaker)
       | -> ^(Brackets LBRACK ^(List))
       )
       RBRACK
     | LCURLY (dictmaker)? RCURLY -> ^(Dict LCURLY ^(Elts dictmaker)?)
     | BACKQUOTE testlist BACKQUOTE -> ^(Repr BACKQUOTE testlist)
     | NAME {debug("parsed NAME");} -> ^(Name NAME)
     | INT -> ^(Num INT)
     | LONGINT -> ^(Num LONGINT)
     | FLOAT -> ^(Num FLOAT)
     | COMPLEX -> ^(Num COMPLEX)
     | (STRING)+ -> ^(Str STRING+)
     ;

//listmaker: test ( list_for | (',' test)* [','] )
listmaker : test 
            ( list_for -> ^(ListComp test list_for)
            | (options {greedy=true;}:COMMA test)* -> ^(List ^(Elts test+))
            ) (COMMA)?
          ;

//testlist_gexp: test ( gen_for | (',' test)* [','] )
testlist_gexp
    : test ( ((options {k=2;}: c1=COMMA test)* (c2=COMMA)? -> { $c1 != null || $c2 != null }? ^(Tuple ^(Elts test+))
                                                           -> test
             )
           | ( gen_for -> ^(GeneratorExp test gen_for))
           )
    ;

//lambdef: 'lambda' [varargslist] ':' test
lambdef: 'lambda' (varargslist)? COLON test {debug("parsed lambda");}
      -> ^(Lambda 'lambda' varargslist? ^(Body test))
       ;

//trailer: '(' [arglist] ')' | '[' subscriptlist ']' | '.' NAME
trailer : LPAREN (arglist)? RPAREN -> ^(Call ^(Args arglist)?)
        | LBRACK subscriptlist RBRACK -> ^(SubscriptList subscriptlist)
        | DOT^ NAME {debug("motched DOT^ NAME");}
        ;

//subscriptlist: subscript (',' subscript)* [',']
subscriptlist : subscript (options {greedy=true;}:COMMA subscript)* (COMMA)?
             -> subscript+
              ;

//subscript: '.' '.' '.' | test | [test] ':' [test] [sliceop]
subscript : DOT DOT DOT -> Ellipsis
          | (test COLON) => t1=test (COLON (t2=test)? (sliceop)?)? -> ^(Subscript ^(Lower $t1) ^(Upper COLON ^(UpperOp $t2)?)? sliceop?)
          | (COLON) => COLON (test)? (sliceop)? -> ^(Subscript ^(Upper COLON ^(UpperOp test)?)? sliceop?)
          | test -> ^(Index test)
          ;

//sliceop: ':' [test]
sliceop : COLON (test)? -> ^(Step COLON ^(StepOp test)?)
        ;

//exprlist: expr (',' expr)* [',']
exprlist : (expr COMMA) => expr (options {k=2;}: COMMA expr)* (COMMA)? -> ^(Tuple ^(Elts expr+))
         | expr
         ;

//XXX: I'm hoping I can get rid of this -- but for now I need an exprlist that does not produce tuples
//     at least for del_stmt
exprlist2 : expr (options {k=2;}: COMMA expr)* (COMMA)?
         -> expr+
          ;

//testlist: test (',' test)* [',']
testlist
    : test (options {k=2;}: c1=COMMA test)* (c2=COMMA)?
     -> { $c1 != null || $c2 != null }? ^(Tuple ^(Elts test+))
     -> test
    ;

//XXX:
//testlist_safe: test [(',' test)+ [',']]

//dictmaker: test ':' test (',' test ':' test)* [',']
dictmaker : test COLON test
            (options {k=2;}:COMMA test COLON test)* (COMMA)?
         -> test+
          ;

//classdef: 'class' NAME ['(' [testlist] ')'] ':' suite
classdef: 'class' NAME (LPAREN testlist? RPAREN)? COLON suite
    -> ^(ClassDef 'class' ^(Name NAME) ^(Bases testlist)? ^(Body suite))
    ;

//arglist: (argument ',')* (argument [',']| '*' test [',' '**' test] | '**' test)
arglist : argument (COMMA argument)*
          ( COMMA
            ( STAR starargs=test (COMMA DOUBLESTAR kwargs=test)?
            | DOUBLESTAR kwargs=test
            )?
          )?
       -> ^(Args argument+) ^(StarArgs $starargs)? ^(KWArgs $kwargs)?
        |   STAR starargs=test (COMMA DOUBLESTAR kwargs=test)?
       -> ^(StarArgs $starargs) ^(KWArgs $kwargs)?
        |   DOUBLESTAR kwargs=test
       -> ^(KWArgs $kwargs)
        ;

//argument: test [gen_for] | test '=' test  # Really [keyword '='] test
argument : t1=test
         ( (ASSIGN t2=test) -> ^(Keyword ^(Arg $t1) ^(Value $t2)?)
         | gen_for -> ^(GenFor $t1 gen_for)
         | -> ^(Arg $t1)
         )
         ;

//list_iter: list_for | list_if
list_iter : list_for
          | list_if
          ;

//list_for: 'for' exprlist 'in' testlist_safe [list_iter]
list_for : 'for' exprlist 'in' testlist (list_iter)?
        -> ^(ListFor ^(Target exprlist) ^(Iter testlist) ^(Ifs list_iter)?)
         ;

//list_if: 'if' test [list_iter]
list_if : 'if' test (list_iter)?
       -> ^(ListIf ^(Target test) (Ifs list_iter)?)
        ;

//gen_iter: gen_for | gen_if
gen_iter: gen_for
        | gen_if
        ;

//gen_for: 'for' exprlist 'in' or_test [gen_iter]
gen_for: 'for' exprlist 'in' or_test gen_iter?
      -> ^(GenFor ^(Target exprlist) ^(Iter or_test) ^(Ifs gen_iter)?)
       ;

//gen_if: 'if' old_test [gen_iter]
gen_if: 'if' test gen_iter?
     -> ^(GenIf ^(Target test) ^(Ifs gen_iter)?)
      ;

//yield_expr: 'yield' [testlist]
yield_expr : 'yield' testlist?
          -> ^(Yield 'yield' ^(Value testlist)?)
           ;

//XXX:
//testlist1: test (',' test)*

//These are all Python keywords that are not Java keywords
//This means that Jython needs to support these as NAMEs
//unlike CPython.  For now I have only done this for 'as'
//and 'exec'.

//keyAND    : {input.LT(1).getText().equals("and")}? NAME ;
keyAS     : {input.LT(1).getText().equals("as")}? NAME ;
//keyDEF    : {input.LT(1).getText().equals("def")}? NAME ;
//keyDEL    : {input.LT(1).getText().equals("del")}? NAME ;
//keyELIF   : {input.LT(1).getText().equals("elif")}? NAME ;
//keyEXCEPT : {input.LT(1).getText().equals("except")}? NAME ;
keyEXEC   : {input.LT(1).getText().equals("exec")}? NAME ;
//keyFROM   : {input.LT(1).getText().equals("from")}? NAME ;
//keyGLOBAL : {input.LT(1).getText().equals("global")}? NAME ;
//keyIN     : {input.LT(1).getText().equals("in")}? NAME ;
//keyIS     : {input.LT(1).getText().equals("is")}? NAME ;
//keyLAMBDA : {input.LT(1).getText().equals("lambda")}? NAME ;
//keyNOT    : {input.LT(1).getText().equals("not")}? NAME ;
//keyOR     : {input.LT(1).getText().equals("or")}? NAME ;
//keyPASS   : {input.LT(1).getText().equals("pass")}? NAME ;
//keyPRINT  : {input.LT(1).getText().equals("print")}? NAME ;
//keyRAISE  : {input.LT(1).getText().equals("raise")}? NAME ;
//keyWITH   : {input.LT(1).getText().equals("with")}? NAME ;
//keyYIELD  : {input.LT(1).getText().equals("yield")}? NAME ;


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

