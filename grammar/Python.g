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
    NameTok;
    ExecTok;
    InTok;
    ClassTok;
    Test;
    Msg;
    Level;
    Body;
    Bases; 
    Arguments;
    Args;
    Arg;
    Keyword;
    StarArgs;
    KWArgs;
    Assign;
    AugAssign;
    Tuple;
    List;
    Dict;
    IfExp;
    TryExcept;
    TryFinally;
    ExceptHandler;
    StrTok;
    NumTok;
    IsNot;
    NotIn;
    Type;
    Inst;
    Tback;
    Globals;
    Locals;
    Ellipsis;
    ListComp;
    Repr;
    Subscript;
    SubscriptList;
    Index;
    Target;
    Value;
    Lower;
    Upper;
    Step;
    UAdd;
    USub;
    Invert;
    Alias;
    Asname;
    Decorators;
    GeneratorExp;
    Ifs;
    Elts;
    Call;
    Dest;
    Values;
    Newline;

    FpList;
    StepOp;
    UpperOp;

    GenFor;
    GenIf;
    ListFor;
    ListIf;
    Parens;
    Brackets;
}

@header {
package org.python.antlr;

import org.antlr.runtime.CommonToken;

import org.python.antlr.ParseException;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.argumentsType;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.Break;
import org.python.antlr.ast.Context;
import org.python.antlr.ast.Continue;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.stmtType;
import org.python.antlr.ast.Str;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PyUnicode;

import java.math.BigInteger;
import java.util.Iterator;
} 

@members {
    boolean debugOn = false;

    private ErrorHandler errorHandler;

    private boolean seenSingleOuterSuite = false;

    public void setErrorHandler(ErrorHandler eh) {
        this.errorHandler = eh;
    }

    private void debug(String message) {
        if (debugOn) {
            System.out.println(message);
        }
    }

    private void throwGenExpNotSoleArg(PythonTree t) {
        throw new ParseException("Generator expression must be parenthesized if not sole argument", t);
    }

    private exprType[] makeExprs(List exprs) {
        return makeExprs(exprs, 0);
    }

    private exprType[] makeExprs(List exprs, int start) {
        if (exprs != null) {
            List<exprType> result = new ArrayList<exprType>();
            for (int i=start; i<exprs.size(); i++) {
                exprType e = (exprType)exprs.get(i);
                result.add(e);
            }
            return (exprType[])result.toArray(new exprType[result.size()]);
        }
        return new exprType[0];
    }

    private stmtType[] makeStmts(List stmts) {
        if (stmts != null) {
            List<stmtType> result = new ArrayList<stmtType>();
            for (int i=0; i<stmts.size(); i++) {
                result.add((stmtType)stmts.get(i));
            }
            return (stmtType[])result.toArray(new stmtType[result.size()]);
        }
        return new stmtType[0];
    }

    private exprType makeDottedAttr(Token nameToken, List attrs) {
        exprType current = new Name(nameToken, nameToken.getText(), expr_contextType.Load);
        for (int i=attrs.size() - 1; i > -1; i--) {
            Token t = ((PythonTree)attrs.get(i)).token;
            current = new Attribute(t, current, t.getText(),
                expr_contextType.Load);
        }
        return current;
    }

    private FunctionDef makeFunctionDef(PythonTree t, PythonTree nameToken, argumentsType args, List funcStatements, List decorators) {
        argumentsType a;
        debug("Matched FunctionDef");
        if (args != null) {
            a = args;
        } else {
            a = new argumentsType(t, new exprType[0], null, null, new exprType[0]); 
        }
        stmtType[] s = (stmtType[])funcStatements.toArray(new stmtType[funcStatements.size()]);
        exprType[] d;
        if (decorators != null) {
            d = (exprType[])decorators.toArray(new exprType[decorators.size()]);
        } else {
            d = new exprType[0];
        }
        return new FunctionDef(t, nameToken.getText(), a, s, d);
    }

    private argumentsType makeArgumentsType(Token t, List params, Token snameToken,
        Token knameToken, List defaults) {
        debug("Matched Arguments");

        exprType[] p = (exprType[])params.toArray(new exprType[params.size()]);
        exprType[] d = (exprType[])defaults.toArray(new exprType[defaults.size()]);
        String s;
        String k;
        if (snameToken == null) {
            s = null;
        } else {
            s = snameToken.getText();
        }
        if (knameToken == null) {
            k = null;
        } else {
            k = knameToken.getText();
        }
        return new argumentsType(t, p, s, k, d);
    }



    Object makeFloat(Token t) {
        debug("makeFloat matched " + t.getText());
        return Py.newFloat(Double.valueOf(t.getText()));
    }

    Object makeComplex(Token t) {
        String s = t.getText();
        s = s.substring(0, s.length() - 1);
        return Py.newImaginary(Double.valueOf(s));
    }

    Object makeInt(Token t) {
        debug("Num matched " + t.getText());
        String s = t.getText();
        int radix = 10;
        if (s.startsWith("0x") || s.startsWith("0X")) {
            radix = 16;
            s = s.substring(2, s.length());
        } else if (s.startsWith("0")) {
            radix = 8;
        }
        if (s.endsWith("L") || s.endsWith("l")) {
            s = s.substring(0, s.length()-1);
            return Py.newLong(new BigInteger(s, radix));
        }
        int ndigits = s.length();
        int i=0;
        while (i < ndigits && s.charAt(i) == '0')
            i++;
        if ((ndigits - i) > 11) {
            return Py.newLong(new BigInteger(s, radix));
        }

        long l = Long.valueOf(s, radix).longValue();
        if (l > 0xffffffffl || (l > Integer.MAX_VALUE)) {
            return Py.newLong(new BigInteger(s, radix));
        }
        return Py.newInteger((int) l);
    }

    class StringPair {
        private String s;
        private boolean unicode;

        StringPair(String s, boolean unicode) {
            this.s = s;
            this.unicode = unicode;
        }
        String getString() {
            return s;
        }
        
        boolean isUnicode() {
            return unicode;
        }
    }

    PyString extractStrings(List s) {
        boolean ustring = false;
        Token last = null;
        StringBuffer sb = new StringBuffer();
        Iterator iter = s.iterator();
        while (iter.hasNext()) {
            last = (Token)iter.next();
            StringPair sp = extractString(last);
            if (sp.isUnicode()) {
                ustring = true;
            }
            sb.append(sp.getString());
        }
        if (ustring) {
            return new PyUnicode(sb.toString());
        }
        return new PyString(sb.toString());
    }

    StringPair extractString(Token t) {
        String s = t.getText();
        char quoteChar = s.charAt(0);
        int start=0;
        boolean ustring = false;
        if (quoteChar == 'u' || quoteChar == 'U') {
            ustring = true;
            start++;
        }
        quoteChar = s.charAt(start);
        boolean raw = false;
        if (quoteChar == 'r' || quoteChar == 'R') {
            raw = true;
            start++;
        }
        int quotes = 3;
        if (s.length() - start == 2) {
            quotes = 1;
        }
        if (s.charAt(start) != s.charAt(start+1)) {
            quotes = 1;
        }

        if (raw) {
            return new StringPair(s.substring(quotes+start, s.length()-quotes), ustring);
        } else {
            StringBuffer sb = new StringBuffer(s.length());
            char[] ca = s.toCharArray();
            int n = ca.length-quotes;
            int i=quotes+start;
            int last_i=i;
            return new StringPair(PyString.decode_UnicodeEscape(s, i, n, "strict", ustring), ustring);
            //return decode_UnicodeEscape(s, i, n, "strict", ustring);
        }
    }

    Token extractStringToken(List s) {
        return (Token)s.get(s.size() - 1);
    }

 
    protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
        if (errorHandler.isRecoverable()) {
            super.mismatch(input, ttype, follow);
        } else {
            throw new MismatchedTokenException(ttype, input);
        }
    }

    protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
        throws RecognitionException
    {
        if (errorHandler.isRecoverable()) {
            return super.recoverFromMismatchedToken(input, ttype, follow);
        }
        mismatch(input, ttype, follow);
        return null;
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

//If you want to use another error recovery mechanisms change this
//and the same one in the parser.
private ErrorHandler errorHandler;

int implicitLineJoiningLevel = 0;
int startPos=-1;

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
            } catch (RecognitionException re) {
                errorHandler.reportError(this, re);
                // match() routine has already called recover()
            }
        }
    }
}

//single_input: NEWLINE | simple_stmt | compound_stmt NEWLINE
single_input : NEWLINE? -> ^(Interactive)
             | simple_stmt -> ^(Interactive simple_stmt)
             | compound_stmt NEWLINE -> ^(Interactive compound_stmt)
             ;

//file_input: (NEWLINE | stmt)* ENDMARKER
file_input : (NEWLINE | stmt)* {debug("parsed file_input");}
          -> ^(Module stmt*)
           ;

//eval_input: testlist NEWLINE* ENDMARKER
eval_input : (NEWLINE)* testlist[expr_contextType.Load] (NEWLINE)* -> ^(Expression testlist)
           ;

//not in CPython's Grammar file
dotted_attr
    : NAME (DOT^ NAME)*
    ;

//decorator: '@' dotted_name [ '(' [arglist] ')' ] NEWLINE
decorator: AT dotted_attr 
           ( (LPAREN arglist? RPAREN) -> ^(AT dotted_attr ^(Call ^(Args arglist)?))
           | -> ^(AT dotted_attr)
           ) NEWLINE
         ;

//decorators: decorator+
decorators: decorator+
          ;

//funcdef: [decorators] 'def' NAME parameters ':' suite
funcdef : decorators? DEF NAME parameters COLON suite
       -> ^(DEF NAME parameters ^(Body suite) ^(Decorators decorators?))
        ;

//parameters: '(' [varargslist] ')'
parameters : LPAREN 
                 (varargslist -> ^(Arguments varargslist)
                 | -> ^(Arguments)
                 )
             RPAREN
           ;

//not in CPython's Grammar file
defparameter : fpdef (ASSIGN test[expr_contextType.Load])? {debug("parsed defparameter");}
             ;

//varargslist: ((fpdef ['=' test] ',')*
//              ('*' NAME [',' '**' NAME] | '**' NAME) |
//              fpdef ['=' test] (',' fpdef ['=' test])* [','])
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

//fpdef: NAME | '(' fplist ')'
fpdef : NAME {debug("parsed fpdef NAME");}
      | (LPAREN fpdef COMMA) => LPAREN fplist RPAREN
     -> ^(FpList fplist)
      | LPAREN fplist RPAREN
     -> fplist
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
expr_stmt : lhs=testlist[expr_contextType.Store]
            ( (augassign yield_expr -> ^(augassign $lhs yield_expr))
            | (augassign rhs=testlist[expr_contextType.Load] -> ^(augassign $lhs $rhs))
            | ((assigns) {debug("matched assigns");} -> ^(Assign ^(Target $lhs) assigns))
            | -> $lhs
            )
          ;

//not in CPython's Grammar file
assigns
    @after {
        PythonTree pt = ((PythonTree)$assigns.tree);
        int children = pt.getChildCount();
        PythonTree child;
        if (children == 1) {
            child = pt;
            pt.token = new CommonToken(Value, "Value");
        } else {
            child = (PythonTree)pt.getChild(children - 1);
            child.token = new CommonToken(Value, "Value");
        }
        child.token = new CommonToken(Value, "Value");
        PythonTree targ = (PythonTree)child.getChild(0);
        if (targ instanceof Context) {
            ((Context)targ).setContext(expr_contextType.Load);
        }
}
    : assign_testlist+
    | assign_yield+
    ;

//not in CPython's Grammar file
assign_testlist
       : ASSIGN testlist[expr_contextType.Store] -> ^(Target testlist)
       ;

//not in CPython's Grammar file
assign_yield
    : ASSIGN yield_expr -> ^(Value yield_expr)
    ;

//augassign: ('+=' | '-=' | '*=' | '/=' | '%=' | '&=' | '|=' | '^=' |
//            '<<=' | '>>=' | '**=' | '//=')
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

//print_stmt: 'print' ( [ test (',' test)* [','] ] |
//                      '>>' test [ (',' test)+ [','] ] )
print_stmt : PRINT
             ( t1=printlist -> {$t1.newline}? ^(PRINT ^(Values $t1) ^(Newline))
                            -> ^(PRINT ^(Values $t1))
             | RIGHTSHIFT t2=printlist2 -> {$t2.newline}? ^(PRINT ^(Dest RIGHTSHIFT) ^(Values $t2) ^(Newline))
                                       -> ^(PRINT ^(Dest RIGHTSHIFT) ^(Values $t2))
             | -> ^(PRINT ^(Newline))
             )
           ;

//not in CPython's Grammar file
printlist returns [boolean newline]
    : (test[expr_contextType.Load] COMMA) => test[expr_contextType.Load] (options {k=2;}: COMMA test[expr_contextType.Load])* (trailcomma=COMMA)?
    { if ($trailcomma == null) {
          $newline = true;
      } else {
          $newline = false;
      }
    }
   -> ^(Elts test+)
    | test[expr_contextType.Load] {$newline = true;}
   -> ^(Elts test)
    ;

//not in CPython's Grammar file
printlist2 returns [boolean newline]
    : (test[expr_contextType.Load] COMMA test[expr_contextType.Load]) => test[expr_contextType.Load] (options {k=2;}: COMMA test[expr_contextType.Load])* (trailcomma=COMMA)?
    { if ($trailcomma == null) {
          $newline = true;
      } else {
          $newline = false;
      }
    }
   -> ^(Elts test+)
    | test[expr_contextType.Load] {$newline = true;}
   -> ^(Elts test)
    ;


//del_stmt: 'del' exprlist
del_stmt : DELETE exprlist2
        -> ^(DELETE exprlist2)
         ;

//pass_stmt: 'pass'
pass_stmt : PASS 
         -> ^(PASS)
          ;

//flow_stmt: break_stmt | continue_stmt | return_stmt | raise_stmt | yield_stmt
flow_stmt : break_stmt
          | continue_stmt
          | return_stmt
          | raise_stmt
          | yield_stmt
          ;

//break_stmt: 'break'
break_stmt : BREAK
          -> ^(BREAK<Break>[$BREAK])
           ;

//continue_stmt: 'continue'
continue_stmt : CONTINUE
             -> ^(CONTINUE<Continue>[$CONTINUE])
              ;

//return_stmt: 'return' [testlist]
return_stmt : RETURN (testlist[expr_contextType.Load])?
          -> ^(RETURN ^(Value testlist)?)
            ;

//yield_stmt: yield_expr
yield_stmt : yield_expr
           ;

//raise_stmt: 'raise' [test [',' test [',' test]]]
raise_stmt: RAISE (t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Load] (COMMA t3=test[expr_contextType.Load])?)?)?
          -> ^(RAISE ^(Type $t1)? ^(Inst $t2)? ^(Tback $t3)?)
          ;

//import_stmt: import_name | import_from
import_stmt : import_name
            | import_from
            ;

//import_name: 'import' dotted_as_names
import_name : IMPORT dotted_as_names
           -> ^(IMPORT dotted_as_names)
            ;

//import_from: ('from' ('.'* dotted_name | '.'+)
//              'import' ('*' | '(' import_as_names ')' | import_as_names))
import_from: FROM (DOT* dotted_name | DOT+) IMPORT 
              (STAR
             -> ^(FROM ^(Level DOT*)? ^(Value dotted_name)? ^(IMPORT STAR))
              | import_as_names
             -> ^(FROM ^(Level DOT*)? ^(Value dotted_name)? ^(IMPORT import_as_names))
              | LPAREN import_as_names RPAREN
             -> ^(FROM ^(Level DOT*)? ^(Value dotted_name)? ^(IMPORT import_as_names))
              )
           ;

//import_as_names: import_as_name (',' import_as_name)* [',']
import_as_names : import_as_name (COMMA! import_as_name)* (COMMA!)?
                ;

//import_as_name: NAME [('as' | NAME) NAME]
import_as_name : name=NAME (keyAS asname=NAME)?
              -> ^(Alias $name ^(Asname $asname)?)
               ;

//XXX: when does CPython Grammar match "dotted_name NAME NAME"? This may be a big
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
global_stmt : GLOBAL NAME (COMMA NAME)*
           -> ^(GLOBAL NAME+)
            ;

//exec_stmt: 'exec' expr ['in' test [',' test]]
exec_stmt : keyEXEC expr[expr_contextType.Load] (keyIN t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Load])?)?
         -> ^(ExecTok[$keyEXEC.start] expr ^(Globals $t1)? ^(Locals $t2)?)
          ;

//assert_stmt: 'assert' test [',' test]
assert_stmt : ASSERT t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Load])?
           -> ^(ASSERT ^(Test $t1) ^(Msg $t2)?)
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
if_stmt: IF test[expr_contextType.Load] COLON ifsuite=suite elif_clause*  (ORELSE COLON elsesuite=suite)?
      -> ^(IF test $ifsuite elif_clause* ^(ORELSE $elsesuite)?)
       ;

//not in CPython's Grammar file
elif_clause : ELIF test[expr_contextType.Load] COLON suite
           -> ^(ELIF test suite)
            ;

//while_stmt: 'while' test ':' suite ['else' ':' suite]
while_stmt : WHILE test[expr_contextType.Load] COLON s1=suite (ORELSE COLON s2=suite)?
          -> ^(WHILE test ^(Body $s1) ^(ORELSE $s2)?)
           ;

//for_stmt: 'for' exprlist 'in' testlist ':' suite ['else' ':' suite]
for_stmt : FOR exprlist[expr_contextType.Store] keyIN testlist[expr_contextType.Load] COLON s1=suite (ORELSE COLON s2=suite)?
        -> ^(FOR ^(Target exprlist) ^(InTok[$keyIN.start] testlist) ^(Body $s1) ^(ORELSE $s2)?)
         ;

//try_stmt: ('try' ':' suite
//           ((except_clause ':' suite)+
//           ['else' ':' suite]
//           ['finally' ':' suite] |
//           'finally' ':' suite))
try_stmt : TRY COLON trysuite=suite
           ( (except_clause+ (ORELSE COLON elsesuite=suite)? (FINALLY COLON finalsuite=suite)?
          -> ^(TryExcept[$TRY] ^(Body $trysuite) except_clause+ ^(ORELSE $elsesuite)? ^(FINALLY $finalsuite)?))
           | (FINALLY COLON finalsuite=suite
          -> ^(TryFinally[$TRY] ^(Body $trysuite) ^(FINALLY $finalsuite)))
           )
         ;

//with_stmt: 'with' test [ with_var ] ':' suite
with_stmt: WITH test[expr_contextType.Load] (with_var)? COLON suite
        -> ^(WITH test with_var? ^(Body suite))
         ;

//with_var: ('as' | NAME) expr
with_var: (keyAS | NAME) expr[expr_contextType.Load]
        ;

//except_clause: 'except' [test [',' test]]
except_clause : 'except' (t1=test[expr_contextType.Load] (COMMA t2=test[expr_contextType.Load])?)? COLON suite
             //Note: passing the 'except' keyword on so we can pass the same offset
             //      as CPython.
             -> ^(ExceptHandler 'except' ^(Type $t1)? ^(Value $t2)? ^(Body suite))
              ;

//suite: simple_stmt | NEWLINE INDENT stmt+ DEDENT
suite
    : simple_stmt
    | NEWLINE! INDENT (stmt)+ DEDENT
    ;

//test: or_test ['if' or_test 'else' test] | lambdef
test[expr_contextType ctype]
    :o1=or_test[ctype]
    ( (IF or_test[expr_contextType.Load] ORELSE) => IF o2=or_test[ctype] ORELSE test[expr_contextType.Load]
      -> ^(IfExp ^(Test $o2) ^(Body $o1) ^(ORELSE test))
    | -> or_test
    )
    | lambdef {debug("parsed lambdef");}
    ;

//or_test: and_test ('or' and_test)*
or_test[expr_contextType ctype] : and_test[ctype] (OR^ and_test[ctype])*
        ;

//and_test: not_test ('and' not_test)*
and_test[expr_contextType ctype] : not_test[ctype] (AND^ not_test[ctype])*
         ;

//not_test: 'not' not_test | comparison
not_test[expr_contextType ctype] : NOT^ not_test[ctype]
         | comparison[ctype]
         ;

//comparison: expr (comp_op expr)*
comparison[expr_contextType ctype]: expr[ctype] (comp_op^ expr[ctype])*
    ;

//comp_op: '<'|'>'|'=='|'>='|'<='|'<>'|'!='|'in'|'not' 'in'|'is'|'is' 'not'
comp_op : LESS
        | GREATER
        | EQUAL
        | GREATEREQUAL
        | LESSEQUAL
        | ALT_NOTEQUAL
        | NOTEQUAL
        | keyIN -> InTok[$keyIN.start]
        | NOT keyIN -> NotIn
        | 'is'
        | 'is' NOT -> IsNot
        ;

//expr: xor_expr ('|' xor_expr)*
expr[expr_contextType ect]
scope {
    expr_contextType ctype;
}
@init {
    $expr::ctype = ect;
}

    : xor_expr (VBAR^ xor_expr)*
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
     | BACKQUOTE testlist[expr_contextType.Load] BACKQUOTE -> ^(Repr BACKQUOTE testlist)
     | NAME -> ^(NameTok NAME)
     | INT -> ^(NumTok<Num>[$INT, makeInt($INT)])
     | LONGINT -> ^(NumTok<Num>[$LONGINT, makeInt($LONGINT)])
     | FLOAT -> ^(NumTok<Num>[$FLOAT, makeFloat($FLOAT)])
     | COMPLEX -> ^(NumTok<Num>[$COMPLEX, makeComplex($COMPLEX)])
     | (S+=STRING)+ 
    -> ^(StrTok<Str>[extractStringToken($S), extractStrings($S)])
     ;

//listmaker: test ( list_for | (',' test)* [','] )
listmaker : test[expr_contextType.Load] 
            ( list_for -> ^(ListComp test list_for)
            | (options {greedy=true;}:COMMA test[expr_contextType.Load])* -> ^(List ^(Elts test+))
            ) (COMMA)?
          ;

//testlist_gexp: test ( gen_for | (',' test)* [','] )
testlist_gexp
    : test[expr_contextType.Load] ( ((options {k=2;}: c1=COMMA test[expr_contextType.Load])* (c2=COMMA)? -> { $c1 != null || $c2 != null }? ^(Tuple ^(Elts test+))
                                                           -> test
             )
           | ( gen_for -> ^(GeneratorExp test gen_for))
           )
    ;

//lambdef: 'lambda' [varargslist] ':' test
lambdef: LAMBDA (varargslist)? COLON test[expr_contextType.Load] {debug("parsed lambda");}
      -> ^(LAMBDA varargslist? ^(Body test))
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
          | (test[expr_contextType.Load] COLON) => t1=test[expr_contextType.Load] (COLON (t2=test[expr_contextType.Load])? (sliceop)?)? -> ^(Subscript ^(Lower $t1) ^(Upper COLON ^(UpperOp $t2)?)? sliceop?)
          | (COLON) => COLON (test[expr_contextType.Load])? (sliceop)? -> ^(Subscript ^(Upper COLON ^(UpperOp test)?)? sliceop?)
          | test[expr_contextType.Load] -> ^(Index test)
          ;

//sliceop: ':' [test]
sliceop : COLON (test[expr_contextType.Load])? -> ^(Step COLON ^(StepOp test)?)
        ;

//exprlist: expr (',' expr)* [',']
exprlist[expr_contextType ctype]: (expr[expr_contextType.Load] COMMA) => expr[ctype] (options {k=2;}: COMMA expr[ctype])* (COMMA)? -> ^(Tuple ^(Elts expr+))
         | expr[ctype]
         ;

//XXX: I'm hoping I can get rid of this -- but for now I need an exprlist that does not produce tuples
//     at least for del_stmt
exprlist2 : expr[expr_contextType.Load] (options {k=2;}: COMMA expr[expr_contextType.Load])* (COMMA)?
         -> expr+
          ;

//testlist: test (',' test)* [',']
testlist[expr_contextType ctype]
    : test[ctype] (options {k=2;}: c1=COMMA test[ctype])* (c2=COMMA)?
     -> { $c1 != null || $c2 != null }? ^(Tuple ^(Elts test+))
     -> test
    ;

//XXX:
//testlist_safe: test [(',' test)+ [',']]

//dictmaker: test ':' test (',' test ':' test)* [',']
dictmaker : test[expr_contextType.Load] COLON test[expr_contextType.Load]
            (options {k=2;}:COMMA test[expr_contextType.Load] COLON test[expr_contextType.Load])* (COMMA)?
         -> test+
          ;

//classdef: 'class' NAME ['(' [testlist] ')'] ':' suite
classdef: keyCLASS NAME (LPAREN testlist[expr_contextType.Load]? RPAREN)? COLON suite
    -> ^(ClassTok[$keyCLASS.start] NAME ^(Bases testlist)? ^(Body suite))
    ;

//arglist: (argument ',')* (argument [',']| '*' test [',' '**' test] | '**' test)
arglist : a1=argument[true] (COMMA a2+=argument[false])*
          ( COMMA
            ( STAR starargs=test[expr_contextType.Load] (COMMA DOUBLESTAR kwargs=test[expr_contextType.Load])?
            | DOUBLESTAR kwargs=test[expr_contextType.Load]
            )?
          )? { if ($a2 != null) {
                   if ($a1.tree.getType() == GenFor) {
                       throwGenExpNotSoleArg($a1.tree);
                   }
                   for (int i=0;i<$a2.size();i++) {
                       if (((PythonTree)$a2.get(i)).getType() == GenFor) {
                           throwGenExpNotSoleArg(((argument_return)$a2.get(i)).tree);
                       }
                   }
               }
             }
       -> ^(Args argument+) ^(StarArgs $starargs)? ^(KWArgs $kwargs)?
        |   STAR starargs=test[expr_contextType.Load] (COMMA DOUBLESTAR kwargs=test[expr_contextType.Load])?
       -> ^(StarArgs $starargs) ^(KWArgs $kwargs)?
        |   DOUBLESTAR kwargs=test[expr_contextType.Load]
       -> ^(KWArgs $kwargs)
        ;

//argument: test [gen_for] | test '=' test  # Really [keyword '='] test
argument[boolean first]
    : t1=test[expr_contextType.Load]
         ( (ASSIGN t2=test[expr_contextType.Load]) -> ^(Keyword ^(Arg $t1) ^(Value $t2)?)
         | gen_for { if (!first) {
                           throwGenExpNotSoleArg($gen_for.tree);
                     }
                   }
        -> ^(GenFor $t1 gen_for)
         | -> ^(Arg $t1)
         )
         ;

//list_iter: list_for | list_if
list_iter : list_for
          | list_if
          ;

//list_for: 'for' exprlist 'in' testlist_safe [list_iter]
list_for : FOR exprlist[expr_contextType.Load] keyIN testlist[expr_contextType.Load] (list_iter)?
        -> ^(ListFor ^(Target exprlist) ^(InTok[$keyIN.start] testlist) ^(Ifs list_iter)?)
         ;

//list_if: 'if' test [list_iter]
list_if : IF test[expr_contextType.Load] (list_iter)?
       -> ^(ListIf ^(Target test) (Ifs list_iter)?)
        ;

//gen_iter: gen_for | gen_if
gen_iter: gen_for
        | gen_if
        ;

//gen_for: 'for' exprlist 'in' or_test [gen_iter]
gen_for: FOR exprlist[expr_contextType.Load] keyIN or_test[expr_contextType.Load] gen_iter?
      -> ^(GenFor ^(Target exprlist) ^(InTok[$keyIN.start] or_test) ^(Ifs gen_iter)?)
       ;

//gen_if: 'if' old_test [gen_iter]
gen_if: IF test[expr_contextType.Load] gen_iter?
     -> ^(GenIf ^(Target test) ^(Ifs gen_iter)?)
      ;

//yield_expr: 'yield' [testlist]
yield_expr : YIELD testlist[expr_contextType.Load]?
          -> ^(YIELD ^(Value testlist)?)
           ;

//XXX:
//testlist1: test (',' test)*

//These are all Python keywords that are not Java keywords
//This means that Jython needs to support these as NAMEs
//unlike CPython.  For now I have only done this for 'as'
//and 'exec'.

//keyAND    : {input.LT(1).getText().equals("and")}? NAME ;
keyAS       : {input.LT(1).getText().equals("as")}? NAME ;
//keyDEF    : {input.LT(1).getText().equals("def")}? NAME ;
//keyDEL    : {input.LT(1).getText().equals("del")}? NAME ;
//keyELIF   : {input.LT(1).getText().equals("elif")}? NAME ;
//keyEXCEPT : {input.LT(1).getText().equals("except")}? NAME ;
keyEXEC   : {input.LT(1).getText().equals("exec")}? NAME ;
//keyFROM   : {input.LT(1).getText().equals("from")}? NAME ;
//keyGLOBAL : {input.LT(1).getText().equals("global")}? NAME ;
keyIN       : {input.LT(1).getText().equals("in")}? NAME ;
keyCLASS    : {input.LT(1).getText().equals("class")}? NAME ;
//keyIS     : {input.LT(1).getText().equals("is")}? NAME ;
//keyLAMBDA : {input.LT(1).getText().equals("lambda")}? NAME ;
//keyNOT    : {input.LT(1).getText().equals("not")}? NAME ;
//keyOR     : {input.LT(1).getText().equals("or")}? NAME ;
//keyPASS   : {input.LT(1).getText().equals("pass")}? NAME ;
//keyPRINT  : {input.LT(1).getText().equals("print")}? NAME ;
//keyRAISE  : {input.LT(1).getText().equals("raise")}? NAME ;
//keyWITH   : {input.LT(1).getText().equals("with")}? NAME ;
//keyYIELD  : {input.LT(1).getText().equals("yield")}? NAME ;

DEF       : 'def' ;
PRINT     : 'print' ;
BREAK     : 'break' ;
CONTINUE  : 'continue' ;
RETURN    : 'return' ;
RAISE     : 'raise' ;
PASS      : 'pass'  ;
IMPORT    : 'import' ;
FROM      : 'from' ;
FOR       : 'for' ;
ORELSE    : 'else' ;
ELIF      : 'elif' ;
IF        : 'if' ;
WHILE     : 'while' ;
WITH      : 'with' ;
LAMBDA    : 'lambda' ;
GLOBAL    : 'global' ;
YIELD     : 'yield' ;
ASSERT    : 'assert' ;
FINALLY   : 'finally' ;
DELETE    : 'del' ;
TRY       : 'try' ;

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
                   if (input.LA(1) != -1) {
                       // make a string of n spaces where n is column number - 1
                       char[] indentation = new char[spaces];
                       for (int i=0; i<spaces; i++) {
                           indentation[i] = ' ';
                       }
                       CommonToken c = new CommonToken(LEADING_WS,new String(indentation));
                       c.setLine(input.getLine());
                       c.setCharPositionInLine(input.getCharPositionInLine());
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
                       emit(new CommonToken(NEWLINE,new String(nls)));
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

