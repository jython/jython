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
 *  Updated the original parser for Python 2.5 features. The parser has been
 *  altered to produce an AST - the AST work started from tne newcompiler
 *  grammar from Jim Baker.  The current parsing and compiling strategy looks
 *  like this:
 *
 *  Python source->Python.g->AST (org/python/parser/ast/*)->CodeCompiler(ASM)->.class
 */

grammar Python;
options {
    ASTLabelType=PythonTree;
    output=AST;
}

tokens {
    INDENT;
    DEDENT;
}

@header {
package org.python.antlr;

import org.antlr.runtime.CommonToken;

import org.python.antlr.ParseException;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.aliasType;
import org.python.antlr.ast.argumentsType;
import org.python.antlr.ast.Assert;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.AugAssign;
import org.python.antlr.ast.BinOp;
import org.python.antlr.ast.BoolOp;
import org.python.antlr.ast.boolopType;
import org.python.antlr.ast.Break;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.Compare;
import org.python.antlr.ast.comprehensionType;
import org.python.antlr.ast.Context;
import org.python.antlr.ast.Continue;
import org.python.antlr.ast.Delete;
import org.python.antlr.ast.Dict;
import org.python.antlr.ast.Ellipsis;
import org.python.antlr.ast.excepthandlerType;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.Expression;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.ExtSlice;
import org.python.antlr.ast.For;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.If;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Interactive;
import org.python.antlr.ast.keywordType;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Raise;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.stmtType;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.Subscript;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.unaryopType;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.While;
import org.python.antlr.ast.With;
import org.python.antlr.ast.Yield;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PyUnicode;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
} 

@members {
    private ErrorHandler errorHandler;

    private GrammarActions actions = new GrammarActions();

    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
        actions.setErrorHandler(eh);
    }

    protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
        if (errorHandler.mismatch(this, input, ttype, follow)) {
            super.mismatch(input, ttype, follow);
        }
    }

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
catch (RecognitionException re) {
    errorHandler.reportError(this, re);
    errorHandler.recover(this, input,re);
    retval.tree = (PythonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
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
@init {
    modType mtype = null;
}
@after {
    $single_input.tree = mtype;
}
    : NEWLINE* EOF {
        mtype = new Interactive($single_input.start, new stmtType[0]);
    }
    | simple_stmt NEWLINE* EOF {
        mtype = new Interactive($single_input.start, actions.castStmts($simple_stmt.stypes));
    }
    | compound_stmt NEWLINE+ EOF {
        mtype = new Interactive($single_input.start, actions.castStmts($compound_stmt.tree));
    }
    ;

//file_input: (NEWLINE | stmt)* ENDMARKER
file_input
@init {
    modType mtype = null;
    List stypes = new ArrayList();
}
@after {
    if (!stypes.isEmpty()) {
        //The EOF token messes up the end offsets, so set them manually.
        //XXX: this may no longer be true now that PythonTokenSource is
        //     adjusting EOF offsets -- but needs testing before I remove
        //     this.
        PythonTree stop = (PythonTree)stypes.get(stypes.size() -1);
        mtype.setCharStopIndex(stop.getCharStopIndex());
        mtype.setTokenStopIndex(stop.getTokenStopIndex());
    }

    $file_input.tree = mtype;
}
    : (NEWLINE
      | stmt {stypes.addAll($stmt.stypes);}
      )* EOF {
        mtype = new Module($file_input.start, actions.castStmts(stypes));
    }
    ;

//eval_input: testlist NEWLINE* ENDMARKER
eval_input
@init {
    modType mtype = null;
}
@after {
    $eval_input.tree = mtype;
}
    : LEADING_WS? (NEWLINE)* testlist[expr_contextType.Load] (NEWLINE)* EOF {
        mtype = new Expression($eval_input.start, actions.castExpr($testlist.tree));
    }
    ;

//not in CPython's Grammar file
dotted_attr returns [exprType etype]
    : n1=NAME
      ( (DOT n2+=NAME)+ { $etype = actions.makeDottedAttr($n1, $n2); }
      | { $etype = new Name($n1, $n1.text, expr_contextType.Load); }
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
decorator returns [exprType etype]
@after {
   $decorator.tree = $etype;
}
    : AT dotted_attr 
    ( LPAREN
      ( arglist
        {
            $etype = actions.makeCall($LPAREN, $dotted_attr.etype, $arglist.args, $arglist.keywords,
                     $arglist.starargs, $arglist.kwargs);
        }
      | {
            $etype = actions.makeCall($LPAREN, $dotted_attr.etype);
        }
      )
      RPAREN
    | {
          $etype = $dotted_attr.etype;
      }
    ) NEWLINE
    ;

//decorators: decorator+
decorators returns [List etypes]
    : d+=decorator+
      {
          $etypes = $d;
      }
    ;

//funcdef: [decorators] 'def' NAME parameters ':' suite
funcdef
@init { stmtType stype = null; }
@after { $funcdef.tree = stype; }
    : decorators? DEF NAME parameters COLON suite[false]
    {
        Token t = $DEF;
        if ($decorators.start != null) {
            t = $decorators.start;
        }
        stype = actions.makeFuncdef(t, $NAME, $parameters.args, $suite.stypes, $decorators.etypes);
    }
    ;

//parameters: '(' [varargslist] ')'
parameters returns [argumentsType args]
    : LPAREN 
      (varargslist {$args = $varargslist.args;}
      | { $args = new argumentsType($parameters.start, new exprType[0], null, null, new exprType[0]);
        }
      )
      RPAREN
    ;

//not in CPython's Grammar file
defparameter[List defaults] returns [exprType etype]
@after {
   $defparameter.tree = $etype;
}
    : fpdef[expr_contextType.Param] (ASSIGN test[expr_contextType.Load])?
      {
          $etype = actions.castExpr($fpdef.tree);
          if ($ASSIGN != null) {
              defaults.add($test.tree);
          } else if (!defaults.isEmpty()) {
              throw new ParseException("non-default argument follows default argument", $fpdef.tree);
          }
      }
    ;

//varargslist: ((fpdef ['=' test] ',')*
//              ('*' NAME [',' '**' NAME] | '**' NAME) |
//              fpdef ['=' test] (',' fpdef ['=' test])* [','])
varargslist returns [argumentsType args]
@init {
    List defaults = new ArrayList();
}
    : d+=defparameter[defaults] (options {greedy=true;}:COMMA d+=defparameter[defaults])*
      (COMMA
          (STAR starargs=NAME (COMMA DOUBLESTAR kwargs=NAME)?
          | DOUBLESTAR kwargs=NAME
          )?
      )?
      {
          $args = actions.makeArgumentsType($varargslist.start, $d, $starargs, $kwargs, defaults);
      }
    | STAR starargs=NAME (COMMA DOUBLESTAR kwargs=NAME)?
      {
          $args = actions.makeArgumentsType($varargslist.start, $d, $starargs, $kwargs, defaults);
      }
    | DOUBLESTAR kwargs=NAME
      {
          $args = actions.makeArgumentsType($varargslist.start, $d, null, $kwargs, defaults);
      }
    ;

//fpdef: NAME | '(' fplist ')'
fpdef[expr_contextType ctype]
@after {
    actions.checkAssign(actions.castExpr($fpdef.tree));
}
    : NAME 
   -> ^(NAME<Name>[$NAME, $NAME.text, ctype])
    | (LPAREN fpdef[null] COMMA) => LPAREN fplist RPAREN
   -> ^(LPAREN<Tuple>[$fplist.start, actions.castExprs($fplist.etypes), expr_contextType.Store])
    | LPAREN fplist RPAREN
   -> fplist
    ;

//fplist: fpdef (',' fpdef)* [',']
fplist returns [List etypes]
    : f+=fpdef[expr_contextType.Store]
      (options {greedy=true;}:COMMA f+=fpdef[expr_contextType.Store])* (COMMA)?
      {
          $etypes = $f;
      }
    ;

//stmt: simple_stmt | compound_stmt
stmt returns [List stypes]
    : simple_stmt
      {
          $stypes = $simple_stmt.stypes;
      }
    | compound_stmt
      {
          $stypes = new ArrayList();
          $stypes.add($compound_stmt.tree);
      }
    ;

//simple_stmt: small_stmt (';' small_stmt)* [';'] NEWLINE
simple_stmt returns [List stypes]
    : s+=small_stmt (options {greedy=true;}:SEMI s+=small_stmt)* (SEMI)? NEWLINE
      {
          $stypes = $s;
      }
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
@init {
    stmtType stype = null;
}
@after {
    if (stype != null) {
        $expr_stmt.tree = stype;
    }
}
    : ((testlist[null] augassign) => lhs=testlist[expr_contextType.AugStore]
        ( (aay=augassign y1=yield_expr 
            {
                actions.checkAssign(actions.castExpr($lhs.tree));
                stype = new AugAssign($lhs.tree, actions.castExpr($lhs.tree), $aay.op, actions.castExpr($y1.tree));
            }
          )
        | (aat=augassign rhs=testlist[expr_contextType.Load]
            {
                actions.checkAssign(actions.castExpr($lhs.tree));
                stype = new AugAssign($lhs.tree, actions.castExpr($lhs.tree), $aat.op, actions.castExpr($rhs.tree));
            }
          )
        )
    | (testlist[null] ASSIGN) => lhs=testlist[expr_contextType.Store]
        (
        | ((at=ASSIGN t+=testlist[expr_contextType.Store])+
       -> ^(ASSIGN<Assign>[$lhs.start, actions.makeAssignTargets(actions.castExpr($lhs.tree), $t),
            actions.makeAssignValue($t)])
          )
        | ((ay=ASSIGN y2+=yield_expr)+
       -> ^(ASSIGN<Assign>[$lhs.start, actions.makeAssignTargets(actions.castExpr($lhs.tree), $y2),
            actions.makeAssignValue($y2)])
          )
        )
    | lhs=testlist[expr_contextType.Load]
      {
          stype = new Expr($lhs.start, actions.castExpr($lhs.tree));
      }
    )
    ;

//augassign: ('+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' |
//            '<<=' | '>>=' | '**=' | '//=')
augassign returns [operatorType op]
    : PLUSEQUAL {$op = operatorType.Add;}
    | MINUSEQUAL {$op = operatorType.Sub;}
    | STAREQUAL {$op = operatorType.Mult;}
    | SLASHEQUAL {$op = operatorType.Div;}
    | PERCENTEQUAL {$op = operatorType.Mod;}
    | AMPEREQUAL {$op = operatorType.BitAnd;}
    | VBAREQUAL {$op = operatorType.BitOr;}
    | CIRCUMFLEXEQUAL {$op = operatorType.BitXor;}
    | LEFTSHIFTEQUAL {$op = operatorType.LShift;}
    | RIGHTSHIFTEQUAL {$op = operatorType.RShift;}
    | DOUBLESTAREQUAL {$op = operatorType.Pow;}
    | DOUBLESLASHEQUAL {$op = operatorType.FloorDiv;}
    ;

//print_stmt: 'print' ( [ test (',' test)* [','] ] |
//                      '>>' test [ (',' test)+ [','] ] )
print_stmt
    : PRINT 
      (t1=printlist
     -> ^(PRINT<Print>[$PRINT, null, actions.castExprs($t1.elts), $t1.newline])
      | RIGHTSHIFT t2=printlist2
     -> ^(PRINT<Print>[$PRINT, actions.castExpr($t2.elts.get(0)), actions.castExprs($t2.elts, 1), $t2.newline])
      |
     -> ^(PRINT<Print>[$PRINT, null, new exprType[0\], false])
      )
           ;

//not in CPython's Grammar file
printlist returns [boolean newline, List elts]
    : (test[null] COMMA) =>
       t+=test[expr_contextType.Load] (options {k=2;}: COMMA t+=test[expr_contextType.Load])*
         (trailcomma=COMMA)?
       {
           $elts=$t;
           if ($trailcomma == null) {
               $newline = true;
           } else {
               $newline = false;
           }
       }
    | t+=test[expr_contextType.Load]
      {
          $elts=$t;
          $newline = true;
      }
    ;

//XXX: would be nice if printlist and printlist2 could be merged.
//not in CPython's Grammar file
printlist2 returns [boolean newline, List elts]
    : (test[null] COMMA test[null]) =>
       t+=test[expr_contextType.Load] (options {k=2;}: COMMA t+=test[expr_contextType.Load])*
         (trailcomma=COMMA)?
       { $elts=$t;
           if ($trailcomma == null) {
               $newline = true;
           } else {
               $newline = false;
           }
       }
    | t+=test[expr_contextType.Load]
      {
          $elts=$t;
          $newline = true;
      }
    ;


//del_stmt: 'del' exprlist
del_stmt
    : DELETE del_list
   -> ^(DELETE<Delete>[$DELETE, $del_list.etypes])
    ;

//pass_stmt: 'pass'
pass_stmt
    : PASS
   -> ^(PASS<Pass>[$PASS])
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
   -> ^(BREAK<Break>[$BREAK])
    ;

//continue_stmt: 'continue'
continue_stmt
    : CONTINUE {
        if (!$suite.isEmpty() && $suite::continueIllegal) {
            errorHandler.error("'continue' not supported inside 'finally' clause", new PythonTree($continue_stmt.start));
        }
    }
   -> ^(CONTINUE<Continue>[$CONTINUE])
    ;

//return_stmt: 'return' [testlist]
return_stmt
    : RETURN 
      (testlist[expr_contextType.Load]
     -> ^(RETURN<Return>[$RETURN, actions.castExpr($testlist.tree)])
      |
     -> ^(RETURN<Return>[$RETURN, null])
      )
      ;

//yield_stmt: yield_expr
yield_stmt
    : yield_expr -> ^(YIELD<Expr>[$yield_expr.start, actions.castExpr($yield_expr.tree)])
    ;

//raise_stmt: 'raise' [test [',' test [',' test]]]
raise_stmt
    : RAISE (t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Load]
        (COMMA t3=test[expr_contextType.Load])?)?)?
   -> ^(RAISE<Raise>[$RAISE, actions.castExpr($t1.tree), actions.castExpr($t2.tree), actions.castExpr($t3.tree)])
    ;

//import_stmt: import_name | import_from
import_stmt
    : import_name
    | import_from
    ;

//import_name: 'import' dotted_as_names
import_name
    : IMPORT dotted_as_names
   -> ^(IMPORT<Import>[$IMPORT, $dotted_as_names.atypes])
    ;

//import_from: ('from' ('.'* dotted_name | '.'+)
//              'import' ('*' | '(' import_as_names ')' | import_as_names))
import_from
    : FROM (d+=DOT* dotted_name | d+=DOT+) IMPORT 
        (STAR
       -> ^(FROM<ImportFrom>[$FROM, actions.makeFromText($d, $dotted_name.text),
             actions.makeStarAlias($STAR), actions.makeLevel($d)])
        | i1=import_as_names
       -> ^(FROM<ImportFrom>[$FROM, actions.makeFromText($d, $dotted_name.text),
             actions.makeAliases($i1.atypes), actions.makeLevel($d)])
        | LPAREN i2=import_as_names COMMA? RPAREN
       -> ^(FROM<ImportFrom>[$FROM, actions.makeFromText($d, $dotted_name.text),
             actions.makeAliases($i2.atypes), actions.makeLevel($d)])
        )
    ;

//import_as_names: import_as_name (',' import_as_name)* [',']
import_as_names returns [aliasType[\] atypes]
    : n+=import_as_name (COMMA! n+=import_as_name)*
    {
        $atypes = (aliasType[])$n.toArray(new aliasType[$n.size()]);
    }
    ;

//import_as_name: NAME [('as' | NAME) NAME]
import_as_name returns [aliasType atype]
@after {
    $import_as_name.tree = $atype;
}
    : name=NAME (AS asname=NAME)?
    {
        $atype = new aliasType($name, $name.text, $asname.text);
    }
    ;

//XXX: when does CPython Grammar match "dotted_name NAME NAME"?
//dotted_as_name: dotted_name [('as' | NAME) NAME]
dotted_as_name returns [aliasType atype]
@after {
    $dotted_as_name.tree = $atype;
}

    : dotted_name (AS NAME)?
    {
        $atype = new aliasType($NAME, $dotted_name.text, $NAME.text);
    }
    ;

//dotted_as_names: dotted_as_name (',' dotted_as_name)*
dotted_as_names returns [aliasType[\] atypes]
    : d+=dotted_as_name (COMMA! d+=dotted_as_name)*
    {
        $atypes = (aliasType[])$d.toArray(new aliasType[$d.size()]);
    }
    ;

//dotted_name: NAME ('.' NAME)*
dotted_name
    : NAME (DOT attr)*
    ;

//global_stmt: 'global' NAME (',' NAME)*
global_stmt
    : GLOBAL n+=NAME (COMMA n+=NAME)*
   -> ^(GLOBAL<Global>[$GLOBAL, actions.makeNames($n)])
    ;

//exec_stmt: 'exec' expr ['in' test [',' test]]
exec_stmt
@init {
    stmtType stype = null;
}
@after {
   $exec_stmt.tree = stype;
}
    : EXEC expr[expr_contextType.Load] (IN t1=test[expr_contextType.Load]
        (COMMA t2=test[expr_contextType.Load])?)?
      {
         stype = new Exec($EXEC, actions.castExpr($expr.tree), actions.castExpr($t1.tree), actions.castExpr($t2.tree));
      }
    ;

//assert_stmt: 'assert' test [',' test]
assert_stmt
    : ASSERT t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Load])?
   -> ^(ASSERT<Assert>[$ASSERT, actions.castExpr($t1.tree), actions.castExpr($t2.tree)])
    ;

//compound_stmt: if_stmt | while_stmt | for_stmt | try_stmt | funcdef | classdef
compound_stmt
    : if_stmt
    | while_stmt
    | for_stmt
    | try_stmt
    | with_stmt
    | (decorators? DEF) => funcdef
    | classdef
    ;

//if_stmt: 'if' test ':' suite ('elif' test ':' suite)* ['else' ':' suite]
if_stmt
    : IF test[expr_contextType.Load] COLON ifsuite=suite[false] elif_clause[$test.start]?
   -> ^(IF<If>[$IF, actions.castExpr($test.tree), actions.castStmts($ifsuite.stypes),
         actions.makeElse($elif_clause.stypes, $elif_clause.tree)])
    ;

//not in CPython's Grammar file
elif_clause [Token iftest] returns [List stypes]
    : else_clause {
        $stypes = $else_clause.stypes;
    }
    | ELIF test[expr_contextType.Load] COLON suite[false]
        (e2=elif_clause[$iftest]
       -> ^(ELIF<If>[$iftest, actions.castExpr($test.tree), actions.castStmts($suite.stypes), actions.makeElse($e2.stypes, $e2.tree)])
        |
       -> ^(ELIF<If>[$iftest, actions.castExpr($test.tree), actions.castStmts($suite.stypes), new stmtType[0\]])
        )
    ;

//not in CPython's Grammar file
else_clause returns [List stypes]
    : ORELSE COLON elsesuite=suite[false] {
        $stypes = $suite.stypes;
    }
    ;

//while_stmt: 'while' test ':' suite ['else' ':' suite]
while_stmt
@init {
    stmtType stype = null;
}
@after {
   $while_stmt.tree = stype;
}
    : WHILE test[expr_contextType.Load] COLON s1=suite[false] (ORELSE COLON s2=suite[false])?
    {
        stype = actions.makeWhile($WHILE, actions.castExpr($test.tree), $s1.stypes, $s2.stypes);
    }
    ;

//for_stmt: 'for' exprlist 'in' testlist ':' suite ['else' ':' suite]
for_stmt
@init {
    stmtType stype = null;
}
@after {
   $for_stmt.tree = stype;
}
    : FOR exprlist[expr_contextType.Store] IN testlist[expr_contextType.Load] COLON s1=suite[false]
        (ORELSE COLON s2=suite[false])?
      {
          stype = actions.makeFor($FOR, $exprlist.etype, actions.castExpr($testlist.tree), $s1.stypes, $s2.stypes);
      }
    ;

//try_stmt: ('try' ':' suite
//           ((except_clause ':' suite)+
//           ['else' ':' suite]
//           ['finally' ':' suite] |
//           'finally' ':' suite))
try_stmt
@init {
    stmtType stype = null;
}
@after {
   $try_stmt.tree = stype;
}
    : TRY COLON trysuite=suite[!$suite.isEmpty() && $suite::continueIllegal]
      ( e+=except_clause+ (ORELSE COLON elsesuite=suite[!$suite.isEmpty() && $suite::continueIllegal])? (FINALLY COLON finalsuite=suite[true])?
        {
            stype = actions.makeTryExcept($TRY, $trysuite.stypes, $e, $elsesuite.stypes, $finalsuite.stypes);
        }
      | FINALLY COLON finalsuite=suite[true]
        {
            stype = actions.makeTryFinally($TRY, $trysuite.stypes, $finalsuite.stypes);
        }
      )
      ;

//with_stmt: 'with' test [ with_var ] ':' suite
with_stmt
@init {
    stmtType stype = null;
}
@after {
   $with_stmt.tree = stype;
}
    : WITH test[expr_contextType.Load] (with_var)? COLON suite[false]
      {
          stype = new With($WITH, actions.castExpr($test.tree), $with_var.etype,
              actions.castStmts($suite.stypes));
      }
    ;

//with_var: ('as' | NAME) expr
with_var returns [exprType etype]
    : (AS | NAME) expr[expr_contextType.Store]
      {
          $etype = actions.castExpr($expr.tree);
      }
    ;

//except_clause: 'except' [test [',' test]]
except_clause
    : EXCEPT (t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Store])?)? COLON suite[!$suite.isEmpty() && $suite::continueIllegal]
   -> ^(EXCEPT<excepthandlerType>[$EXCEPT, actions.castExpr($t1.tree), actions.castExpr($t2.tree),
          actions.castStmts($suite.stypes), $EXCEPT.getLine(), $EXCEPT.getCharPositionInLine()])
    ;

//suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
suite[boolean fromFinally] returns [List stypes]
scope {
    boolean continueIllegal;
}
@init {
    if ($suite::continueIllegal || fromFinally) {
        $suite::continueIllegal = true;
    } else {
        $suite::continueIllegal = false;
    }
    $stypes = new ArrayList();
}
    : simple_stmt
      {
          $stypes = $simple_stmt.stypes;
      }
    | NEWLINE INDENT
      (stmt
          {
              $stypes.addAll($stmt.stypes);
          }
      )+ DEDENT
    ;

//test: or_test ['if' or_test 'else' test] | lambdef
test[expr_contextType ctype]
    :o1=or_test[ctype]
      ( (IF or_test[null] ORELSE) => IF o2=or_test[ctype] ORELSE e=test[expr_contextType.Load]
     -> ^(IF<IfExp>[$o1.start, actions.castExpr($o2.tree), actions.castExpr($o1.tree), actions.castExpr($e.tree)])
      |
     -> or_test
      )
    | lambdef
    ;

//or_test: and_test ('or' and_test)*
or_test[expr_contextType ctype]
@after {
    if ($or != null) {
        $or_test.tree = actions.makeBoolOp($left.tree, boolopType.Or, $right);
    }
}
    : left=and_test[ctype]
        ( (or=OR right+=and_test[ctype]
          )+
        |
       -> $left
        )
    ;

//and_test: not_test ('and' not_test)*
and_test[expr_contextType ctype]
@after {
    if ($and != null) {
        $and_test.tree = actions.makeBoolOp($left.tree, boolopType.And, $right); 
    }
}
    : left=not_test[ctype]
        ( (and=AND right+=not_test[ctype]
          )+
        |
       -> $left
        )
    ;

//not_test: 'not' not_test | comparison
not_test[expr_contextType ctype]
    : NOT nt=not_test[ctype]
   -> ^(NOT<UnaryOp>[$NOT, unaryopType.Not, actions.castExpr($nt.tree)])
    | comparison[ctype]
    ;

//comparison: expr (comp_op expr)*
comparison[expr_contextType ctype]
@init {
    List cmps = new ArrayList();
}
@after {
    if (!cmps.isEmpty()) {
        $comparison.tree = new Compare($left.start, actions.castExpr($left.tree), actions.makeCmpOps(cmps),
            actions.castExprs($right));
    }
}
    : left=expr[ctype]
       ( ( comp_op right+=expr[ctype] {cmps.add($comp_op.op);}
         )+
       |
      -> $left
       )
    ;

//comp_op: '<'|'>'|'=='|'>='|'<='|'<>'|'!='|'in'|'not' 'in'|'is'|'is' 'not'
comp_op returns [cmpopType op]
    : LESS {$op = cmpopType.Lt;}
    | GREATER {$op = cmpopType.Gt;}
    | EQUAL {$op = cmpopType.Eq;}
    | GREATEREQUAL {$op = cmpopType.GtE;}
    | LESSEQUAL {$op = cmpopType.LtE;}
    | ALT_NOTEQUAL {$op = cmpopType.NotEq;}
    | NOTEQUAL {$op = cmpopType.NotEq;}
    | IN {$op = cmpopType.In;}
    | NOT IN {$op = cmpopType.NotIn;}
    | IS {$op = cmpopType.Is;}
    | IS NOT {$op = cmpopType.IsNot;}
    ;


//expr: xor_expr ('|' xor_expr)*
expr[expr_contextType ect]
scope {
    expr_contextType ctype;
}
@init {
    $expr::ctype = ect;
}
@after {
    if ($op != null) {
        $expr.tree = actions.makeBinOp($left.tree, operatorType.BitOr, $right); 
    }
}
    : left=xor_expr
        ( (op=VBAR right+=xor_expr
          )+
        |
       -> $left
        )
    ;


//xor_expr: and_expr ('^' and_expr)*
xor_expr
@after {
    if ($op != null) {
        $xor_expr.tree = actions.makeBinOp($left.tree, operatorType.BitXor, $right); 
    }
}
    : left=and_expr
        ( (op=CIRCUMFLEX right+=and_expr
          )+
        |
       -> $left
        )
    ;

//and_expr: shift_expr ('&' shift_expr)*
and_expr
@after {
    if ($op != null) {
        $and_expr.tree = actions.makeBinOp($left.tree, operatorType.BitAnd, $right); 
    }
}
    : left=shift_expr
        ( (op=AMPER right+=shift_expr
          )+
        |
       -> $left
        )
    ;

//shift_expr: arith_expr (('<<'|'>>') arith_expr)*
shift_expr
@init {
    List ops = new ArrayList();
}
@after {
    if (!ops.isEmpty()) {
        $shift_expr.tree = actions.makeBinOp($left.tree, ops, $right); 
    }
}
    : left=arith_expr
        ( ( shift_op right+=arith_expr {ops.add($shift_op.op);}
          )+
        |
       -> $left
        )
    ;

shift_op returns [operatorType op]
    : LEFTSHIFT {$op = operatorType.LShift;}
    | RIGHTSHIFT {$op = operatorType.RShift;}
    ;

//arith_expr: term (('+'|'-') term)*
arith_expr
@init {
    List ops = new ArrayList();
}
@after {
    if (!ops.isEmpty()) {
        $arith_expr.tree = actions.makeBinOp($left.tree, ops, $right);
    }
}
    : left=term
        ( (arith_op right+=term {ops.add($arith_op.op);}
          )+
        |
       -> $left
        )
    ;

arith_op returns [operatorType op]
    : PLUS {$op = operatorType.Add;}
    | MINUS {$op = operatorType.Sub;}
    ;

//term: factor (('*'|'/'|'%'|'//') factor)*
term
@init {
    List ops = new ArrayList();
}
@after {
    if (!ops.isEmpty()) {
        $term.tree = actions.makeBinOp($left.tree, ops, $right);
    }
}
    : left=factor
        ( (term_op right+=factor {ops.add($term_op.op);}
          )+
        |
       -> $left
        )
    ;

term_op returns [operatorType op]
    :STAR {$op = operatorType.Mult;}
    |SLASH {$op = operatorType.Div;}
    |PERCENT {$op = operatorType.Mod;}
    |DOUBLESLASH {$op = operatorType.FloorDiv;}
    ;

//factor: ('+'|'-'|'~') factor | power
factor returns [exprType etype]
@after {
    $factor.tree = $etype;
}
    : PLUS p=factor {$etype = new UnaryOp($PLUS, unaryopType.UAdd, $p.etype);}
    | MINUS m=factor {$etype = actions.negate($MINUS, $m.etype);}
    | TILDE t=factor {$etype = new UnaryOp($TILDE, unaryopType.Invert, $t.etype);}
    | power {$etype = actions.castExpr($power.tree);}
    ;

//power: atom trailer* ['**' factor]
power returns [exprType etype]
@after {
    $power.tree = $etype;
}
    : atom (t+=trailer[$atom.start, $atom.tree])* (options {greedy=true;}:d=DOUBLESTAR factor)?
      {
          //XXX: This could be better.
          $etype = actions.castExpr($atom.tree);
          if ($t != null) {
              for(Object o : $t) {
                  if ($etype instanceof Context) {
                      ((Context)$etype).setContext(expr_contextType.Load);
                  }
                  if (o instanceof Call) {
                      Call c = (Call)o;
                      c.func = $etype;
                      $etype = c;
                  } else if (o instanceof Subscript) {
                      Subscript c = (Subscript)o;
                      c.value = $etype;
                      $etype = c;
                  } else if (o instanceof Attribute) {
                      Attribute c = (Attribute)o;
                      c.setCharStartIndex($etype.getCharStartIndex());
                      c.value = $etype;
                      $etype = c;
                  }
              }
          }
          if ($d != null) {
              List right = new ArrayList();
              right.add($factor.tree);
              $etype = actions.makeBinOp($etype, operatorType.Pow, right);
          }
      }
    ;

//atom: ('(' [yield_expr|testlist_gexp] ')' |
//       '[' [listmaker] ']' |
//       '{' [dictmaker] '}' |
//       '`' testlist1 '`' |
//       NAME | NUMBER | STRING+)
atom
    : LPAREN 
      ( yield_expr
     -> yield_expr
      | testlist_gexp
     -> testlist_gexp
      |
     -> ^(LPAREN<Tuple>[$LPAREN, new exprType[0\], $expr::ctype])
      )
      RPAREN
    | LBRACK
      (listmaker[$LBRACK]
     -> listmaker
      |
     -> ^(LBRACK<org.python.antlr.ast.List>[$LBRACK, new exprType[0\], $expr::ctype])
      )
      RBRACK
    | LCURLY 
       (dictmaker
      -> ^(LCURLY<Dict>[$LCURLY, actions.castExprs($dictmaker.keys),
              actions.castExprs($dictmaker.values)])
       |
      -> ^(LCURLY<Dict>[$LCURLY, new exprType[0\], new exprType[0\]])
       )
       RCURLY
     | lb=BACKQUOTE testlist[expr_contextType.Load] rb=BACKQUOTE
    -> ^(BACKQUOTE<Repr>[$lb, actions.castExpr($testlist.tree)])
     | NAME
    -> ^(NAME<Name>[$NAME, $NAME.text, $expr::ctype])
     | INT
    -> ^(INT<Num>[$INT, actions.makeInt($INT)])
     | LONGINT
    -> ^(LONGINT<Num>[$LONGINT, actions.makeInt($LONGINT)])
     | FLOAT
    -> ^(FLOAT<Num>[$FLOAT, actions.makeFloat($FLOAT)])
     | COMPLEX
    -> ^(COMPLEX<Num>[$COMPLEX, actions.makeComplex($COMPLEX)])
     | (S+=STRING)+ 
    -> ^(STRING<Str>[actions.extractStringToken($S), actions.extractStrings($S)])
     ;

//listmaker: test ( list_for | (',' test)* [','] )
listmaker[Token lbrack]
@init {
    List gens = new ArrayList();
    exprType etype = null;
}
@after {
   $listmaker.tree = etype;
}
    : t+=test[$expr::ctype] 
        (list_for[gens]
          {
              Collections.reverse(gens);
              comprehensionType[] c =
                  (comprehensionType[])gens.toArray(new comprehensionType[gens.size()]);
              etype = new ListComp($listmaker.start, actions.castExpr($t.get(0)), c);
          }
        | (options {greedy=true;}:COMMA t+=test[$expr::ctype])*
          {
              etype = new org.python.antlr.ast.List($lbrack, actions.castExprs($t), $expr::ctype);
          }
        ) (COMMA)?
          ;

//testlist_gexp: test ( gen_for | (',' test)* [','] )
testlist_gexp
@init {
    exprType etype = null;
    List gens = new ArrayList();
}
@after {
    if (etype != null) {
        $testlist_gexp.tree = etype;
    }
}
    : t+=test[$expr::ctype]
        ( ((options {k=2;}: c1=COMMA t+=test[$expr::ctype])* (c2=COMMA)?
         -> { $c1 != null || $c2 != null }?
                ^(COMMA<Tuple>[$testlist_gexp.start, actions.castExprs($t), $expr::ctype])
         -> test
          )
        | (gen_for[gens]
           {
               Collections.reverse(gens);
               comprehensionType[] c = (comprehensionType[])gens.toArray(new comprehensionType[gens.size()]);
               exprType e = actions.castExpr($t.get(0));
               if (e instanceof Context) {
                   ((Context)e).setContext(expr_contextType.Load);
               }
               etype = new GeneratorExp($testlist_gexp.start, actions.castExpr($t.get(0)), c);
           }
          )
        )
    ;

//lambdef: 'lambda' [varargslist] ':' test
lambdef
@init {
    exprType etype = null;
}
@after {
    $lambdef.tree = etype;
}
    : LAMBDA (varargslist)? COLON test[expr_contextType.Load]
      {
          argumentsType a = $varargslist.args;
          if (a == null) {
              a = new argumentsType($LAMBDA, new exprType[0], null, null, new exprType[0]);
          }
          etype = new Lambda($LAMBDA, a, actions.castExpr($test.tree));
      }
    ;

//trailer: '(' [arglist] ')' | '[' subscriptlist ']' | '.' NAME
trailer [Token begin, PythonTree tree]
    : LPAREN 
        (arglist
       -> ^(LPAREN<Call>[$begin, actions.castExpr($tree), actions.castExprs($arglist.args),
               actions.makeKeywords($arglist.keywords), $arglist.starargs, $arglist.kwargs])
        |
       -> ^(LPAREN<Call>[$begin, actions.castExpr($tree), new exprType[0\], new keywordType[0\], null, null])
        )
      RPAREN
    | LBRACK subscriptlist[$begin] RBRACK
   -> ^(LBRACK<Subscript>[$begin, actions.castExpr($tree), actions.castSlice($subscriptlist.tree), $expr::ctype])
    | DOT attr
   -> ^(DOT<Attribute>[$begin, actions.castExpr($tree), $attr.text, $expr::ctype])
    ;

//subscriptlist: subscript (',' subscript)* [',']
subscriptlist[Token begin]
@init {
    sliceType sltype = null;
}
@after {
   $subscriptlist.tree = sltype;
}
    : sub+=subscript (options {greedy=true;}:c1=COMMA sub+=subscript)* (c2=COMMA)?
      {
          sltype = actions.makeSliceType($begin, $c1, $c2, $sub);
      }
    ;

//subscript: '.' '.' '.' | test | [test] ':' [test] [sliceop]
subscript returns [sliceType sltype]
@after {
    if ($sltype != null) {
        $subscript.tree = $sltype;
    }
}
    : d1=DOT DOT DOT
   -> DOT<Ellipsis>[$d1]
    | (test[null] COLON)
   => lower=test[expr_contextType.Load] (c1=COLON (upper1=test[expr_contextType.Load])? (sliceop)?)?
      {
        $sltype = actions.makeSubscript($lower.tree, $c1, $upper1.tree, $sliceop.tree);
      }
    | (COLON)
   => c2=COLON (upper2=test[expr_contextType.Load])? (sliceop)?
      {
          $sltype = actions.makeSubscript(null, $c2, $upper2.tree, $sliceop.tree);
      }
    | test[expr_contextType.Load]
   -> ^(LPAREN<Index>[$test.start, actions.castExpr($test.tree)])
    ;

//sliceop: ':' [test]
sliceop
    : COLON
      (test[expr_contextType.Load]
     -> test
      )?
    ;

//exprlist: expr (',' expr)* [',']
exprlist[expr_contextType ctype] returns [exprType etype]
    : (expr[null] COMMA) => e+=expr[ctype] (options {k=2;}: COMMA e+=expr[ctype])* (COMMA)?
      {
          $etype = new Tuple($exprlist.start, actions.castExprs($e), ctype);
      }
    | expr[ctype]
      {
        $etype = actions.castExpr($expr.tree);
      }
    ;

//not in CPython's Grammar file
//Needed as an exprlist that does not produce tuples for del_stmt.
del_list returns [exprType[\] etypes]
    : e+=expr[expr_contextType.Del] (options {k=2;}: COMMA e+=expr[expr_contextType.Del])* (COMMA)?
      {
          $etypes = actions.makeDeleteList($e);
      }
    ;

//testlist: test (',' test)* [',']
testlist[expr_contextType ctype]
    : (test[null] COMMA)
   => t+=test[ctype] (options {k=2;}: COMMA t+=test[ctype])* (COMMA)?
   -> ^(COMMA<Tuple>[$testlist.start, actions.castExprs($t), ctype])
    | test[ctype]
    ;

//dictmaker: test ':' test (',' test ':' test)* [',']
dictmaker returns [List keys, List values]
    : k+=test[expr_contextType.Load] COLON v+=test[expr_contextType.Load]
        (options {k=2;}:COMMA k+=test[expr_contextType.Load] COLON v+=test[expr_contextType.Load])*
        (COMMA)?
      {
          $keys = $k;
          $values= $v;
      }
    ;

//classdef: 'class' NAME ['(' [testlist] ')'] ':' suite
classdef
@init {
    stmtType stype = null;
}
@after {
   $classdef.tree = stype;
}
    : decorators? CLASS NAME (LPAREN testlist[expr_contextType.Load]? RPAREN)? COLON suite[false]
      {
          Token t = $CLASS;
          if ($decorators.start != null) {
              t = $decorators.start;
          }
          stype = new ClassDef(t, actions.cantBeNone($NAME),
              actions.makeBases(actions.castExpr($testlist.tree)),
              actions.castStmts($suite.stypes),
              actions.castExprs($decorators.etypes));
      }
    ;

//arglist: (argument ',')* (argument [',']| '*' test [',' '**' test] | '**' test)
arglist returns [List args, List keywords, exprType starargs, exprType kwargs]
@init {
    List arguments = new ArrayList();
    List kws = new ArrayList();
    List gens = new ArrayList();
}
    : argument[arguments, kws, gens, true] (COMMA argument[arguments, kws, gens, false])*
          (COMMA
              ( STAR s=test[expr_contextType.Load] (COMMA DOUBLESTAR k=test[expr_contextType.Load])?
              | DOUBLESTAR k=test[expr_contextType.Load]
              )?
          )?
      {
          if (arguments.size() > 1 && gens.size() > 0) {
              actions.errorGenExpNotSoleArg(new PythonTree($arglist.start));
          }
          $args=arguments;
          $keywords=kws;
          $starargs=actions.castExpr($s.tree);
          $kwargs=actions.castExpr($k.tree);
      }
    | STAR s=test[expr_contextType.Load] (COMMA DOUBLESTAR k=test[expr_contextType.Load])?
      {
          $starargs=actions.castExpr($s.tree);
            $kwargs=actions.castExpr($k.tree);
      }
    | DOUBLESTAR k=test[expr_contextType.Load]
      {
            $kwargs=actions.castExpr($k.tree);
      }
    ;

//argument: test [gen_for] | test '=' test  # Really [keyword '='] test
argument[List arguments, List kws, List gens, boolean first] returns [boolean genarg]
    : t1=test[expr_contextType.Load]
        ((ASSIGN t2=test[expr_contextType.Load])
          {
              $kws.add(new exprType[]{actions.castExpr($t1.tree), actions.castExpr($t2.tree)});
          }
        | gen_for[$gens]
          {
              if (!first) {
                  actions.errorGenExpNotSoleArg($gen_for.tree);
              }
              $genarg = true;
              Collections.reverse($gens);
              comprehensionType[] c = (comprehensionType[])$gens.toArray(new comprehensionType[$gens.size()]);
              arguments.add(new GeneratorExp($t1.start, actions.castExpr($t1.tree), c));
          }
        | {
              if (kws.size() > 0) {
                  errorHandler.error("non-keyword arg after keyword arg", $t1.tree);
              }
              $arguments.add($t1.tree);
          }
        )
    ;

//list_iter: list_for | list_if
list_iter [List gens] returns [exprType etype]
    : list_for[gens]
    | list_if[gens] {
        $etype = $list_if.etype;
    }
    ;

//list_for: 'for' exprlist 'in' testlist_safe [list_iter]
list_for [List gens]
    : FOR exprlist[expr_contextType.Store] IN testlist[expr_contextType.Load] (list_iter[gens])?
      {
          exprType[] e;
          if ($list_iter.etype != null) {
              e = new exprType[]{$list_iter.etype};
          } else {
              e = new exprType[0];
          }
          gens.add(new comprehensionType($FOR, $exprlist.etype, actions.castExpr($testlist.tree), e));
      }
    ;

//list_if: 'if' test [list_iter]
list_if[List gens] returns [exprType etype]
    : IF test[expr_contextType.Load] (list_iter[gens])?
    {
        $etype = actions.castExpr($test.tree);
    }
    ;

//gen_iter: gen_for | gen_if
gen_iter [List gens] returns [exprType etype]
    : gen_for[gens]
    | gen_if[gens]
      {
          $etype = $gen_if.etype;
      }
    ;

//gen_for: 'for' exprlist 'in' or_test [gen_iter]
gen_for [List gens]
    : FOR exprlist[expr_contextType.Store] IN or_test[expr_contextType.Load] gen_iter[gens]?
      {
          exprType[] e;
          if ($gen_iter.etype != null) {
              e = new exprType[]{$gen_iter.etype};
          } else {
              e = new exprType[0];
          }
          gens.add(new comprehensionType($FOR, $exprlist.etype, actions.castExpr($or_test.tree), e));
      }
    ;

//gen_if: 'if' old_test [gen_iter]
gen_if[List gens] returns [exprType etype]
    : IF test[expr_contextType.Load] gen_iter[gens]?
      {
          $etype = actions.castExpr($test.tree);
      }
    ;

//yield_expr: 'yield' [testlist]
yield_expr
    : YIELD testlist[expr_contextType.Load]?
   -> ^(YIELD<Yield>[$YIELD, actions.castExpr($testlist.tree)])
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

STRINGPART
    : {partial}?=> ('r'|'u'|'ur'|'R'|'U'|'UR'|'uR'|'Ur')?
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
         ( nl=NEWLINE {emit(new CommonToken(NEWLINE,nl.getText()));}
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
@init {
    int newlines = 0;
}
    :   (('\u000C')?('\r')? '\n' {newlines++; } )+ {
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

