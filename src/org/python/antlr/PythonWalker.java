// $ANTLR 3.0.1 /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g 2008-03-19 16:53:22
 
package org.python.antlr;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.antlr.ast.aliasType;
import org.python.antlr.ast.argumentsType;
import org.python.antlr.ast.boolopType;
import org.python.antlr.ast.comprehensionType;
import org.python.antlr.ast.cmpopType;
import org.python.antlr.ast.excepthandlerType;
import org.python.antlr.ast.exprType;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.ast.keywordType;
import org.python.antlr.ast.modType;
import org.python.antlr.ast.operatorType;
import org.python.antlr.ast.sliceType;
import org.python.antlr.ast.stmtType;
import org.python.antlr.ast.unaryopType;
import org.python.antlr.ast.Assert;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Attribute;
import org.python.antlr.ast.AugAssign;
import org.python.antlr.ast.BinOp;
import org.python.antlr.ast.BoolOp;
import org.python.antlr.ast.Break;
import org.python.antlr.ast.Call;
import org.python.antlr.ast.ClassDef;
import org.python.antlr.ast.Compare;
import org.python.antlr.ast.Continue;
import org.python.antlr.ast.Delete;
import org.python.antlr.ast.Dict;
import org.python.antlr.ast.Ellipsis;
import org.python.antlr.ast.Exec;
import org.python.antlr.ast.Expr;
import org.python.antlr.ast.ExtSlice;
import org.python.antlr.ast.For;
import org.python.antlr.ast.FunctionDef;
import org.python.antlr.ast.GeneratorExp;
import org.python.antlr.ast.Global;
import org.python.antlr.ast.If;
import org.python.antlr.ast.IfExp;
import org.python.antlr.ast.Index;
import org.python.antlr.ast.Import;
import org.python.antlr.ast.ImportFrom;
import org.python.antlr.ast.Lambda;
import org.python.antlr.ast.ListComp;
import org.python.antlr.ast.Module;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Num;
import org.python.antlr.ast.Slice;
import org.python.antlr.ast.Subscript;
import org.python.antlr.ast.TryExcept;
import org.python.antlr.ast.TryFinally;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.Pass;
import org.python.antlr.ast.Print;
import org.python.antlr.ast.Raise;
import org.python.antlr.ast.Repr;
import org.python.antlr.ast.Return;
import org.python.antlr.ast.Str;
import org.python.antlr.ast.UnaryOp;
import org.python.antlr.ast.Unicode;
import org.python.antlr.ast.With;
import org.python.antlr.ast.While;
import org.python.antlr.ast.Yield;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;


import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class PythonWalker extends TreeParser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "INDENT", "DEDENT", "Module", "Test", "Msg", "Import", "ImportFrom", "Level", "Name", "Body", "ClassDef", "Bases", "FunctionDef", "Arguments", "Args", "Arg", "Keyword", "StarArgs", "KWArgs", "Assign", "AugAssign", "Compare", "Expr", "Tuple", "List", "Dict", "If", "IfExp", "OrElse", "Elif", "While", "Pass", "Break", "Continue", "Print", "TryExcept", "TryFinally", "ExceptHandler", "For", "Return", "Yield", "Str", "Num", "IsNot", "In", "NotIn", "Raise", "Type", "Inst", "Tback", "Global", "Exec", "Globals", "Locals", "Assert", "Ellipsis", "Comprehension", "ListComp", "Lambda", "Repr", "BinOp", "Subscript", "SubscriptList", "Index", "Target", "Targets", "Value", "Lower", "Upper", "Step", "UnaryOp", "UAdd", "USub", "Invert", "Delete", "Default", "Alias", "Asname", "Decorator", "Decorators", "With", "GeneratorExp", "Id", "Iter", "Ifs", "Elts", "Ctx", "Attr", "Call", "Dest", "Values", "Newline", "FpList", "StepOp", "UpperOp", "GenFor", "GenIf", "ListFor", "ListIf", "FinalBody", "Parens", "NEWLINE", "AT", "LPAREN", "RPAREN", "NAME", "DOT", "COLON", "COMMA", "STAR", "DOUBLESTAR", "ASSIGN", "SEMI", "PLUSEQUAL", "MINUSEQUAL", "STAREQUAL", "SLASHEQUAL", "PERCENTEQUAL", "AMPEREQUAL", "VBAREQUAL", "CIRCUMFLEXEQUAL", "LEFTSHIFTEQUAL", "RIGHTSHIFTEQUAL", "DOUBLESTAREQUAL", "DOUBLESLASHEQUAL", "RIGHTSHIFT", "OR", "AND", "NOT", "LESS", "GREATER", "EQUAL", "GREATEREQUAL", "LESSEQUAL", "ALT_NOTEQUAL", "NOTEQUAL", "VBAR", "CIRCUMFLEX", "AMPER", "LEFTSHIFT", "PLUS", "MINUS", "SLASH", "PERCENT", "DOUBLESLASH", "TILDE", "LBRACK", "RBRACK", "LCURLY", "RCURLY", "BACKQUOTE", "INT", "LONGINT", "FLOAT", "COMPLEX", "STRING", "DIGITS", "Exponent", "TRIAPOS", "TRIQUOTE", "ESC", "CONTINUED_LINE", "WS", "LEADING_WS", "COMMENT", "'def'", "'print'", "'del'", "'pass'", "'break'", "'continue'", "'return'", "'raise'", "'import'", "'from'", "'as'", "'global'", "'exec'", "'in'", "'assert'", "'if'", "'else'", "'elif'", "'while'", "'for'", "'try'", "'finally'", "'with'", "'except'", "'is'", "'lambda'", "'class'", "'yield'"
    };
    public static final int Str=45;
    public static final int Dict=29;
    public static final int COMMA=112;
    public static final int MINUS=145;
    public static final int Args=18;
    public static final int DEDENT=5;
    public static final int Targets=69;
    public static final int Ctx=90;
    public static final int TRIQUOTE=163;
    public static final int Delete=78;
    public static final int Tback=53;
    public static final int COMPLEX=158;
    public static final int Ellipsis=59;
    public static final int Elts=89;
    public static final int ListFor=101;
    public static final int DOUBLESLASHEQUAL=128;
    public static final int TILDE=149;
    public static final int Default=79;
    public static final int Locals=57;
    public static final int NEWLINE=105;
    public static final int DOT=110;
    public static final int Index=67;
    public static final int Invert=77;
    public static final int UpperOp=98;
    public static final int PLUSEQUAL=117;
    public static final int AND=131;
    public static final int Compare=25;
    public static final int RIGHTSHIFTEQUAL=126;
    public static final int LCURLY=152;
    public static final int Exec=55;
    public static final int StarArgs=21;
    public static final int UnaryOp=74;
    public static final int RPAREN=108;
    public static final int TryExcept=39;
    public static final int IsNot=47;
    public static final int FinalBody=103;
    public static final int PLUS=144;
    public static final int Expr=26;
    public static final int ClassDef=14;
    public static final int Print=38;
    public static final int Decorator=82;
    public static final int AT=106;
    public static final int Subscript=65;
    public static final int Type=51;
    public static final int Continue=37;
    public static final int Raise=50;
    public static final int Pass=35;
    public static final int List=28;
    public static final int Assert=58;
    public static final int Import=9;
    public static final int WS=166;
    public static final int STRING=159;
    public static final int Msg=8;
    public static final int Upper=72;
    public static final int In=48;
    public static final int Name=12;
    public static final int LBRACK=150;
    public static final int SEMI=116;
    public static final int Call=92;
    public static final int Keyword=20;
    public static final int EQUAL=135;
    public static final int Values=94;
    public static final int LESSEQUAL=137;
    public static final int USub=76;
    public static final int Test=7;
    public static final int Asname=81;
    public static final int Newline=95;
    public static final int Body=13;
    public static final int Step=73;
    public static final int ALT_NOTEQUAL=138;
    public static final int COLON=111;
    public static final int AMPER=142;
    public static final int Dest=93;
    public static final int NAME=109;
    public static final int Lambda=62;
    public static final int DOUBLESTAREQUAL=127;
    public static final int For=42;
    public static final int Level=11;
    public static final int Assign=23;
    public static final int NotIn=49;
    public static final int PERCENT=147;
    public static final int Iter=87;
    public static final int ListIf=102;
    public static final int FpList=96;
    public static final int If=30;
    public static final int Inst=52;
    public static final int BinOp=64;
    public static final int Break=36;
    public static final int DOUBLESTAR=114;
    public static final int FLOAT=157;
    public static final int GenFor=99;
    public static final int Attr=91;
    public static final int SLASHEQUAL=120;
    public static final int Arg=19;
    public static final int KWArgs=22;
    public static final int Num=46;
    public static final int OR=130;
    public static final int NOTEQUAL=139;
    public static final int Decorators=83;
    public static final int Ifs=88;
    public static final int CIRCUMFLEX=141;
    public static final int Module=6;
    public static final int RCURLY=153;
    public static final int LESS=133;
    public static final int GeneratorExp=85;
    public static final int TryFinally=40;
    public static final int LONGINT=156;
    public static final int ExceptHandler=41;
    public static final int INT=155;
    public static final int UAdd=75;
    public static final int OrElse=32;
    public static final int LEADING_WS=167;
    public static final int ASSIGN=115;
    public static final int Arguments=17;
    public static final int LPAREN=107;
    public static final int GREATER=134;
    public static final int VBAR=140;
    public static final int SubscriptList=66;
    public static final int BACKQUOTE=154;
    public static final int Yield=44;
    public static final int AugAssign=24;
    public static final int CONTINUED_LINE=165;
    public static final int Alias=80;
    public static final int IfExp=31;
    public static final int Parens=104;
    public static final int Exponent=161;
    public static final int DIGITS=160;
    public static final int SLASH=146;
    public static final int Bases=15;
    public static final int Tuple=27;
    public static final int Repr=63;
    public static final int ImportFrom=10;
    public static final int COMMENT=168;
    public static final int TRIAPOS=162;
    public static final int AMPEREQUAL=122;
    public static final int ESC=164;
    public static final int GenIf=100;
    public static final int Elif=33;
    public static final int With=84;
    public static final int Target=68;
    public static final int Global=54;
    public static final int RIGHTSHIFT=129;
    public static final int StepOp=97;
    public static final int MINUSEQUAL=118;
    public static final int PERCENTEQUAL=121;
    public static final int LEFTSHIFTEQUAL=125;
    public static final int While=34;
    public static final int EOF=-1;
    public static final int CIRCUMFLEXEQUAL=124;
    public static final int INDENT=4;
    public static final int Value=70;
    public static final int RBRACK=151;
    public static final int ListComp=61;
    public static final int Comprehension=60;
    public static final int FunctionDef=16;
    public static final int GREATEREQUAL=136;
    public static final int DOUBLESLASH=148;
    public static final int Globals=56;
    public static final int STAR=113;
    public static final int STAREQUAL=119;
    public static final int VBAREQUAL=123;
    public static final int NOT=132;
    public static final int Lower=71;
    public static final int LEFTSHIFT=143;
    public static final int Id=86;
    public static final int Return=43;

        public PythonWalker(TreeNodeStream input) {
            super(input);
        }
        

    public String[] getTokenNames() { return tokenNames; }
    public String getGrammarFileName() { return "/Users/frank/tmp/trunk/jython/grammar/PythonWalker.g"; }


        boolean debugOn = false;

        public void debug(String message) {
            if (debugOn) {
                System.out.println(message);
            }
        }

        String name = "Test";

        //XXX: Not sure I need any below...
        String filename = "test.py";
        boolean linenumbers = true;
        boolean setFile = true;
        boolean printResults = false;
        //CompilerFlags cflags = Py.getCompilerFlags();

        private modType makeMod(PythonTree t, List stmts) {
            stmtType[] s;
            if (stmts != null) {
                s = (stmtType[])stmts.toArray(new stmtType[stmts.size()]);
            } else {
                s = new stmtType[0];
            }
            return new Module(t, s);
        }

        private ClassDef makeClassDef(PythonTree t, PythonTree nameToken, List bases, List body) {
            exprType[] b = (exprType[])bases.toArray(new exprType[bases.size()]);
            stmtType[] s = (stmtType[])body.toArray(new stmtType[body.size()]);
            return new ClassDef(t, nameToken.getText(), b, s);
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

        private argumentsType makeArgumentsType(PythonTree t, List params, PythonTree snameToken,
            PythonTree knameToken, List defaults) {
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

        StringPair extractStrings(List s) {
            boolean ustring = false;
            StringBuffer sb = new StringBuffer();
            Iterator iter = s.iterator();
            while (iter.hasNext()) {
                StringPair sp = extractString((String)iter.next());
                if (sp.isUnicode()) {
                    ustring = true;
                }
                sb.append(sp.getString());
            }
            return new StringPair(sb.toString(), ustring);
        }

        StringPair extractString(String s) {
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

        Num makeFloat(PythonTree t) {
            debug("makeFloat matched " + t.getText());
            return new Num(t, Py.newFloat(Double.valueOf(t.getText())));
        }

        Num makeComplex(PythonTree t) {
            String s = t.getText();
            s = s.substring(0, s.length() - 1);
            return new Num(t, Py.newImaginary(Double.valueOf(s)));
        }

        Num makeInt(PythonTree t) {
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
                return new Num(t, Py.newLong(new BigInteger(s, radix)));
            }
            int ndigits = s.length();
            int i=0;
            while (i < ndigits && s.charAt(i) == '0')
                i++;
            if ((ndigits - i) > 11) {
                return new Num(t, Py.newLong(new BigInteger(s, radix)));
            }

            long l = Long.valueOf(s, radix).longValue();
            if (l > 0xffffffffl || (radix == 10 && l > Integer.MAX_VALUE)) {
                return new Num(t, Py.newLong(new BigInteger(s, radix)));
            }
            return new Num(t, Py.newInteger((int) l));
        }

        private stmtType makeTryExcept(PythonTree t, List body, List handlers, List orelse, List finBody) {
            stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
            excepthandlerType[] e = (excepthandlerType[])handlers.toArray(new excepthandlerType[handlers.size()]);
            stmtType[] o;
            if (orelse != null) {
                o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
            } else {
                o = new stmtType[0];
            }
     
            stmtType te = new TryExcept(t, b, e, o);
            if (finBody == null) {
                return te;
            }
            stmtType[] f = (stmtType[])finBody.toArray(new stmtType[finBody.size()]);
            stmtType[] mainBody = new stmtType[]{te};
            return new TryFinally(t, mainBody, f);
        }

        private TryFinally makeTryFinally(PythonTree t,  List body, List finBody) {
            stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
            stmtType[] f = (stmtType[])finBody.toArray(new stmtType[finBody.size()]);
            return new TryFinally(t, b, f);
        }

        private If makeIf(PythonTree t, exprType test, List body, List orelse) {
            stmtType[] o;
            if (orelse != null) {
                o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
            } else {
                o = new stmtType[0];
            }
            stmtType[] b;
            if (body != null) {
                b = (stmtType[])body.toArray(new stmtType[body.size()]);
            } else {
                b = new stmtType[0];
            }
            return new If(t, test, b, o);
        }


        private While makeWhile(PythonTree t, exprType test, List body, List orelse) {
            stmtType[] o;
            if (orelse != null) {
                o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
            } else {
                o = new stmtType[0];
            }
            stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
            return new While(t, test, b, o);
        }

        private For makeFor(PythonTree t, exprType target, exprType iter, List body, List orelse) {
            stmtType[] o;
            if (orelse != null) {
                o = (stmtType[])orelse.toArray(new stmtType[orelse.size()]);
            } else {
                o = new stmtType[0];
            }
            stmtType[] b = (stmtType[])body.toArray(new stmtType[body.size()]);
            return new For(t, target, iter, b, o);
        }
        
        //FIXME: just calling __neg__ for now - can be better.  Also does not parse expressions like
        //       --2 correctly (should give ^(USub -2) but gives 2).
        private exprType negate(PythonTree t, exprType o) {
            if (o instanceof Num) {
                Num num = (Num)o;
                if (num.n instanceof PyObject) {
                    num.n = ((PyObject)num.n).__neg__();
                }
                return num;
            }
            return new UnaryOp(t, unaryopType.USub, o);
        }



    // $ANTLR start module
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:342:1: module returns [modType mod] : ^( Module ( stmts | ) ) ;
    public final modType module() throws RecognitionException {
        modType mod = null;

        PythonTree Module1=null;
        stmts_return stmts2 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:343:5: ( ^( Module ( stmts | ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:343:7: ^( Module ( stmts | ) )
            {
            Module1=(PythonTree)input.LT(1);
            match(input,Module,FOLLOW_Module_in_module56); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:344:9: ( stmts | )
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==INDENT||(LA1_0>=Import && LA1_0<=ImportFrom)||LA1_0==Name||LA1_0==ClassDef||LA1_0==FunctionDef||LA1_0==Assign||(LA1_0>=Tuple && LA1_0<=IfExp)||(LA1_0>=While && LA1_0<=TryFinally)||(LA1_0>=For && LA1_0<=IsNot)||(LA1_0>=NotIn && LA1_0<=Raise)||(LA1_0>=Global && LA1_0<=Exec)||LA1_0==Assert||(LA1_0>=ListComp && LA1_0<=Repr)||LA1_0==SubscriptList||(LA1_0>=UAdd && LA1_0<=Delete)||(LA1_0>=With && LA1_0<=GeneratorExp)||LA1_0==Call||LA1_0==Parens||LA1_0==DOT||(LA1_0>=STAR && LA1_0<=DOUBLESTAR)||(LA1_0>=PLUSEQUAL && LA1_0<=DOUBLESLASH)||LA1_0==182||LA1_0==193) ) {
                    alt1=1;
                }
                else if ( (LA1_0==UP) ) {
                    alt1=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("344:9: ( stmts | )", 1, 0, input);

                    throw nvae;
                }
                switch (alt1) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:344:11: stmts
                        {
                        pushFollow(FOLLOW_stmts_in_module68);
                        stmts2=stmts();
                        _fsp--;

                        mod = makeMod(Module1, stmts2.stypes); 

                        }
                        break;
                    case 2 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:345:11: 
                        {
                        mod = makeMod(Module1, null);

                        }
                        break;

                }


                match(input, Token.UP, null); 
            }

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return mod;
    }
    // $ANTLR end module


    // $ANTLR start funcdef
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:350:1: funcdef : ^( FunctionDef ^( Name NAME ) ^( Arguments ( varargslist )? ) ^( Body stmts ) ^( Decorators ( decorators )? ) ) ;
    public final void funcdef() throws RecognitionException {
        PythonTree FunctionDef3=null;
        PythonTree NAME4=null;
        argumentsType varargslist5 = null;

        stmts_return stmts6 = null;

        List decorators7 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:351:5: ( ^( FunctionDef ^( Name NAME ) ^( Arguments ( varargslist )? ) ^( Body stmts ) ^( Decorators ( decorators )? ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:351:7: ^( FunctionDef ^( Name NAME ) ^( Arguments ( varargslist )? ) ^( Body stmts ) ^( Decorators ( decorators )? ) )
            {
            FunctionDef3=(PythonTree)input.LT(1);
            match(input,FunctionDef,FOLLOW_FunctionDef_in_funcdef116); 

            match(input, Token.DOWN, null); 
            match(input,Name,FOLLOW_Name_in_funcdef119); 

            match(input, Token.DOWN, null); 
            NAME4=(PythonTree)input.LT(1);
            match(input,NAME,FOLLOW_NAME_in_funcdef121); 

            match(input, Token.UP, null); 
            match(input,Arguments,FOLLOW_Arguments_in_funcdef125); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:351:46: ( varargslist )?
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==Args||(LA2_0>=StarArgs && LA2_0<=KWArgs)) ) {
                    alt2=1;
                }
                switch (alt2) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:351:46: varargslist
                        {
                        pushFollow(FOLLOW_varargslist_in_funcdef127);
                        varargslist5=varargslist();
                        _fsp--;


                        }
                        break;

                }


                match(input, Token.UP, null); 
            }
            match(input,Body,FOLLOW_Body_in_funcdef132); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_stmts_in_funcdef134);
            stmts6=stmts();
            _fsp--;


            match(input, Token.UP, null); 
            match(input,Decorators,FOLLOW_Decorators_in_funcdef138); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:351:87: ( decorators )?
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( (LA3_0==Decorator) ) {
                    alt3=1;
                }
                switch (alt3) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:351:87: decorators
                        {
                        pushFollow(FOLLOW_decorators_in_funcdef140);
                        decorators7=decorators();
                        _fsp--;


                        }
                        break;

                }


                match(input, Token.UP, null); 
            }

            match(input, Token.UP, null); 

                    ((stmts_scope)stmts_stack.peek()).statements.add(makeFunctionDef(FunctionDef3, NAME4, varargslist5, stmts6.stypes, decorators7));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end funcdef


    // $ANTLR start varargslist
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:356:1: varargslist returns [argumentsType args] : ( ^( Args ( defparameter[params, defaults] )+ ) ( ^( StarArgs sname= NAME ) )? ( ^( KWArgs kname= NAME ) )? | ^( StarArgs sname= NAME ) ( ^( KWArgs kname= NAME ) )? | ^( KWArgs NAME ) );
    public final argumentsType varargslist() throws RecognitionException {
        argumentsType args = null;

        PythonTree sname=null;
        PythonTree kname=null;
        PythonTree Args8=null;
        PythonTree StarArgs9=null;
        PythonTree KWArgs10=null;
        PythonTree NAME11=null;


            List params = new ArrayList();
            List defaults = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:5: ( ^( Args ( defparameter[params, defaults] )+ ) ( ^( StarArgs sname= NAME ) )? ( ^( KWArgs kname= NAME ) )? | ^( StarArgs sname= NAME ) ( ^( KWArgs kname= NAME ) )? | ^( KWArgs NAME ) )
            int alt8=3;
            switch ( input.LA(1) ) {
            case Args:
                {
                alt8=1;
                }
                break;
            case StarArgs:
                {
                alt8=2;
                }
                break;
            case KWArgs:
                {
                alt8=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("356:1: varargslist returns [argumentsType args] : ( ^( Args ( defparameter[params, defaults] )+ ) ( ^( StarArgs sname= NAME ) )? ( ^( KWArgs kname= NAME ) )? | ^( StarArgs sname= NAME ) ( ^( KWArgs kname= NAME ) )? | ^( KWArgs NAME ) );", 8, 0, input);

                throw nvae;
            }

            switch (alt8) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:7: ^( Args ( defparameter[params, defaults] )+ ) ( ^( StarArgs sname= NAME ) )? ( ^( KWArgs kname= NAME ) )?
                    {
                    Args8=(PythonTree)input.LT(1);
                    match(input,Args,FOLLOW_Args_in_varargslist172); 

                    match(input, Token.DOWN, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:14: ( defparameter[params, defaults] )+
                    int cnt4=0;
                    loop4:
                    do {
                        int alt4=2;
                        int LA4_0 = input.LA(1);

                        if ( (LA4_0==FpList||LA4_0==NAME) ) {
                            alt4=1;
                        }


                        switch (alt4) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:14: defparameter[params, defaults]
                    	    {
                    	    pushFollow(FOLLOW_defparameter_in_varargslist174);
                    	    defparameter(params,  defaults);
                    	    _fsp--;


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt4 >= 1 ) break loop4;
                                EarlyExitException eee =
                                    new EarlyExitException(4, input);
                                throw eee;
                        }
                        cnt4++;
                    } while (true);


                    match(input, Token.UP, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:47: ( ^( StarArgs sname= NAME ) )?
                    int alt5=2;
                    int LA5_0 = input.LA(1);

                    if ( (LA5_0==StarArgs) ) {
                        alt5=1;
                    }
                    switch (alt5) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:48: ^( StarArgs sname= NAME )
                            {
                            match(input,StarArgs,FOLLOW_StarArgs_in_varargslist181); 

                            match(input, Token.DOWN, null); 
                            sname=(PythonTree)input.LT(1);
                            match(input,NAME,FOLLOW_NAME_in_varargslist185); 

                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:73: ( ^( KWArgs kname= NAME ) )?
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0==KWArgs) ) {
                        alt6=1;
                    }
                    switch (alt6) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:361:74: ^( KWArgs kname= NAME )
                            {
                            match(input,KWArgs,FOLLOW_KWArgs_in_varargslist192); 

                            match(input, Token.DOWN, null); 
                            kname=(PythonTree)input.LT(1);
                            match(input,NAME,FOLLOW_NAME_in_varargslist196); 

                            match(input, Token.UP, null); 

                            }
                            break;

                    }


                            args = makeArgumentsType(Args8, params, sname, kname, defaults);
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:364:7: ^( StarArgs sname= NAME ) ( ^( KWArgs kname= NAME ) )?
                    {
                    StarArgs9=(PythonTree)input.LT(1);
                    match(input,StarArgs,FOLLOW_StarArgs_in_varargslist210); 

                    match(input, Token.DOWN, null); 
                    sname=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_varargslist214); 

                    match(input, Token.UP, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:364:30: ( ^( KWArgs kname= NAME ) )?
                    int alt7=2;
                    int LA7_0 = input.LA(1);

                    if ( (LA7_0==KWArgs) ) {
                        alt7=1;
                    }
                    switch (alt7) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:364:31: ^( KWArgs kname= NAME )
                            {
                            match(input,KWArgs,FOLLOW_KWArgs_in_varargslist219); 

                            match(input, Token.DOWN, null); 
                            kname=(PythonTree)input.LT(1);
                            match(input,NAME,FOLLOW_NAME_in_varargslist223); 

                            match(input, Token.UP, null); 

                            }
                            break;

                    }


                            args = makeArgumentsType(StarArgs9,params, sname, kname, defaults);
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:367:7: ^( KWArgs NAME )
                    {
                    KWArgs10=(PythonTree)input.LT(1);
                    match(input,KWArgs,FOLLOW_KWArgs_in_varargslist237); 

                    match(input, Token.DOWN, null); 
                    NAME11=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_varargslist239); 

                    match(input, Token.UP, null); 

                            args = makeArgumentsType(KWArgs10, params, null, NAME11, defaults);
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return args;
    }
    // $ANTLR end varargslist


    // $ANTLR start defparameter
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:372:1: defparameter[List params, List defaults] : fpdef[expr_contextType.Param, null] ( ASSIGN test[expr_contextType.Load] )? ;
    public final void defparameter(List params, List defaults) throws RecognitionException {
        PythonTree ASSIGN13=null;
        exprType fpdef12 = null;

        test_return test14 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:373:5: ( fpdef[expr_contextType.Param, null] ( ASSIGN test[expr_contextType.Load] )? )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:373:7: fpdef[expr_contextType.Param, null] ( ASSIGN test[expr_contextType.Load] )?
            {
            pushFollow(FOLLOW_fpdef_in_defparameter260);
            fpdef12=fpdef(expr_contextType.Param,  null);
            _fsp--;

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:373:43: ( ASSIGN test[expr_contextType.Load] )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==ASSIGN) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:373:44: ASSIGN test[expr_contextType.Load]
                    {
                    ASSIGN13=(PythonTree)input.LT(1);
                    match(input,ASSIGN,FOLLOW_ASSIGN_in_defparameter264); 
                    pushFollow(FOLLOW_test_in_defparameter266);
                    test14=test(expr_contextType.Load);
                    _fsp--;


                    }
                    break;

            }


                    params.add(fpdef12);
                    if (ASSIGN13 != null) {
                        defaults.add(test14.etype);
                    }
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end defparameter


    // $ANTLR start fpdef
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:381:1: fpdef[expr_contextType ctype, List nms] returns [exprType etype] : ( NAME | ^( FpList fplist ) );
    public final exprType fpdef(expr_contextType ctype, List nms) throws RecognitionException {
        exprType etype = null;

        PythonTree NAME15=null;
        fplist_return fplist16 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:382:5: ( NAME | ^( FpList fplist ) )
            int alt10=2;
            int LA10_0 = input.LA(1);

            if ( (LA10_0==NAME) ) {
                alt10=1;
            }
            else if ( (LA10_0==FpList) ) {
                alt10=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("381:1: fpdef[expr_contextType ctype, List nms] returns [exprType etype] : ( NAME | ^( FpList fplist ) );", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:382:7: NAME
                    {
                    NAME15=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_fpdef294); 

                            exprType e = new Name(NAME15, NAME15.getText(), ctype);
                            if (nms == null) {
                                etype = e;
                            } else {
                                nms.add(e);
                            }
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:390:7: ^( FpList fplist )
                    {
                    match(input,FpList,FOLLOW_FpList_in_fpdef305); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_fplist_in_fpdef307);
                    fplist16=fplist();
                    _fsp--;


                    match(input, Token.UP, null); 

                            exprType[] e = (exprType[])fplist16.etypes.toArray(new exprType[fplist16.etypes.size()]);
                            Tuple t = new Tuple(((PythonTree)fplist16.start), e, expr_contextType.Store);
                            if (nms == null) {
                                etype = t;
                            } else {
                                nms.add(t);
                            }
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end fpdef

    public static class fplist_return extends TreeRuleReturnScope {
        public List etypes;
    };

    // $ANTLR start fplist
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:401:1: fplist returns [List etypes] : ( fpdef[expr_contextType.Store, nms] )+ ;
    public final fplist_return fplist() throws RecognitionException {
        fplist_return retval = new fplist_return();
        retval.start = input.LT(1);


            List nms = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:405:5: ( ( fpdef[expr_contextType.Store, nms] )+ )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:405:7: ( fpdef[expr_contextType.Store, nms] )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:405:7: ( fpdef[expr_contextType.Store, nms] )+
            int cnt11=0;
            loop11:
            do {
                int alt11=2;
                int LA11_0 = input.LA(1);

                if ( (LA11_0==FpList||LA11_0==NAME) ) {
                    alt11=1;
                }


                switch (alt11) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:405:7: fpdef[expr_contextType.Store, nms]
            	    {
            	    pushFollow(FOLLOW_fpdef_in_fplist336);
            	    fpdef(expr_contextType.Store,  nms);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt11 >= 1 ) break loop11;
                        EarlyExitException eee =
                            new EarlyExitException(11, input);
                        throw eee;
                }
                cnt11++;
            } while (true);


                    retval.etypes = nms;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end fplist


    // $ANTLR start decorators
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:410:1: decorators returns [List etypes] : ( decorator[decs] )+ ;
    public final List decorators() throws RecognitionException {
        List etypes = null;


            List decs = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:414:5: ( ( decorator[decs] )+ )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:414:7: ( decorator[decs] )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:414:7: ( decorator[decs] )+
            int cnt12=0;
            loop12:
            do {
                int alt12=2;
                int LA12_0 = input.LA(1);

                if ( (LA12_0==Decorator) ) {
                    alt12=1;
                }


                switch (alt12) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:414:7: decorator[decs]
            	    {
            	    pushFollow(FOLLOW_decorator_in_decorators366);
            	    decorator(decs);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt12 >= 1 ) break loop12;
                        EarlyExitException eee =
                            new EarlyExitException(12, input);
                        throw eee;
                }
                cnt12++;
            } while (true);


                    etypes = decs;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etypes;
    }
    // $ANTLR end decorators


    // $ANTLR start decorator
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:419:1: decorator[List decs] : ^( Decorator dotted_attr ( ^( Call ( ^( Args arglist ) )? ) )? ) ;
    public final void decorator(List decs) throws RecognitionException {
        PythonTree Call17=null;
        PythonTree Args19=null;
        dotted_attr_return dotted_attr18 = null;

        arglist_return arglist20 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:420:5: ( ^( Decorator dotted_attr ( ^( Call ( ^( Args arglist ) )? ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:420:7: ^( Decorator dotted_attr ( ^( Call ( ^( Args arglist ) )? ) )? )
            {
            match(input,Decorator,FOLLOW_Decorator_in_decorator390); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_dotted_attr_in_decorator392);
            dotted_attr18=dotted_attr();
            _fsp--;

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:420:31: ( ^( Call ( ^( Args arglist ) )? ) )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==Call) ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:420:32: ^( Call ( ^( Args arglist ) )? )
                    {
                    Call17=(PythonTree)input.LT(1);
                    match(input,Call,FOLLOW_Call_in_decorator396); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:420:39: ( ^( Args arglist ) )?
                        int alt13=2;
                        int LA13_0 = input.LA(1);

                        if ( (LA13_0==Args) ) {
                            alt13=1;
                        }
                        switch (alt13) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:420:40: ^( Args arglist )
                                {
                                Args19=(PythonTree)input.LT(1);
                                match(input,Args,FOLLOW_Args_in_decorator400); 

                                match(input, Token.DOWN, null); 
                                pushFollow(FOLLOW_arglist_in_decorator402);
                                arglist20=arglist();
                                _fsp--;


                                match(input, Token.UP, null); 

                                }
                                break;

                        }


                        match(input, Token.UP, null); 
                    }

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    if (Call17 == null) {
                        decs.add(dotted_attr18.etype);
                    } else {
                        exprType[] args;
                        keywordType[] keywords;
                        exprType starargs = null;
                        exprType kwargs = null;
                        if (Args19 != null) {
                            args = (exprType[])arglist20.args.toArray(new exprType[arglist20.args.size()]);
                            keywords = (keywordType[])arglist20.keywords.toArray(new keywordType[arglist20.keywords.size()]);
                            starargs = arglist20.starargs;
                            kwargs = arglist20.kwargs;
                        } else {
                            args = new exprType[0];
                            keywords = new keywordType[0];
                        }
                        Call c = new Call(Call17, dotted_attr18.etype, args, keywords, starargs, kwargs);
                        decs.add(c);
                    }
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end decorator

    public static class dotted_attr_return extends TreeRuleReturnScope {
        public exprType etype;
    };

    // $ANTLR start dotted_attr
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:443:1: dotted_attr returns [exprType etype] : ( NAME | ^( DOT n1= dotted_attr n2= dotted_attr ) );
    public final dotted_attr_return dotted_attr() throws RecognitionException {
        dotted_attr_return retval = new dotted_attr_return();
        retval.start = input.LT(1);

        PythonTree NAME21=null;
        PythonTree DOT22=null;
        dotted_attr_return n1 = null;

        dotted_attr_return n2 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:444:5: ( NAME | ^( DOT n1= dotted_attr n2= dotted_attr ) )
            int alt15=2;
            int LA15_0 = input.LA(1);

            if ( (LA15_0==NAME) ) {
                alt15=1;
            }
            else if ( (LA15_0==DOT) ) {
                alt15=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("443:1: dotted_attr returns [exprType etype] : ( NAME | ^( DOT n1= dotted_attr n2= dotted_attr ) );", 15, 0, input);

                throw nvae;
            }
            switch (alt15) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:444:7: NAME
                    {
                    NAME21=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_dotted_attr432); 
                    retval.etype = new Name(NAME21, NAME21.getText(), expr_contextType.Load); debug("matched NAME in dotted_attr");

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:445:7: ^( DOT n1= dotted_attr n2= dotted_attr )
                    {
                    DOT22=(PythonTree)input.LT(1);
                    match(input,DOT,FOLLOW_DOT_in_dotted_attr443); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_dotted_attr_in_dotted_attr447);
                    n1=dotted_attr();
                    _fsp--;

                    pushFollow(FOLLOW_dotted_attr_in_dotted_attr451);
                    n2=dotted_attr();
                    _fsp--;


                    match(input, Token.UP, null); 

                            retval.etype = new Attribute(DOT22, n1.etype, input.getTokenStream().toString(
                      input.getTreeAdaptor().getTokenStartIndex(n2.start),
                      input.getTreeAdaptor().getTokenStopIndex(n2.start)), expr_contextType.Load);
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end dotted_attr

    protected static class stmts_scope {
        List statements;
    }
    protected Stack stmts_stack = new Stack();

    public static class stmts_return extends TreeRuleReturnScope {
        public List stypes;
    };

    // $ANTLR start stmts
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:450:1: stmts returns [List stypes] : ( ( stmt )+ | INDENT ( stmt )+ DEDENT );
    public final stmts_return stmts() throws RecognitionException {
        stmts_stack.push(new stmts_scope());
        stmts_return retval = new stmts_return();
        retval.start = input.LT(1);


            ((stmts_scope)stmts_stack.peek()).statements = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:457:5: ( ( stmt )+ | INDENT ( stmt )+ DEDENT )
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( ((LA18_0>=Import && LA18_0<=ImportFrom)||LA18_0==Name||LA18_0==ClassDef||LA18_0==FunctionDef||LA18_0==Assign||(LA18_0>=Tuple && LA18_0<=IfExp)||(LA18_0>=While && LA18_0<=TryFinally)||(LA18_0>=For && LA18_0<=IsNot)||(LA18_0>=NotIn && LA18_0<=Raise)||(LA18_0>=Global && LA18_0<=Exec)||LA18_0==Assert||(LA18_0>=ListComp && LA18_0<=Repr)||LA18_0==SubscriptList||(LA18_0>=UAdd && LA18_0<=Delete)||(LA18_0>=With && LA18_0<=GeneratorExp)||LA18_0==Call||LA18_0==Parens||LA18_0==DOT||(LA18_0>=STAR && LA18_0<=DOUBLESTAR)||(LA18_0>=PLUSEQUAL && LA18_0<=DOUBLESLASH)||LA18_0==182||LA18_0==193) ) {
                alt18=1;
            }
            else if ( (LA18_0==INDENT) ) {
                alt18=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("450:1: stmts returns [List stypes] : ( ( stmt )+ | INDENT ( stmt )+ DEDENT );", 18, 0, input);

                throw nvae;
            }
            switch (alt18) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:457:7: ( stmt )+
                    {
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:457:7: ( stmt )+
                    int cnt16=0;
                    loop16:
                    do {
                        int alt16=2;
                        int LA16_0 = input.LA(1);

                        if ( ((LA16_0>=Import && LA16_0<=ImportFrom)||LA16_0==Name||LA16_0==ClassDef||LA16_0==FunctionDef||LA16_0==Assign||(LA16_0>=Tuple && LA16_0<=IfExp)||(LA16_0>=While && LA16_0<=TryFinally)||(LA16_0>=For && LA16_0<=IsNot)||(LA16_0>=NotIn && LA16_0<=Raise)||(LA16_0>=Global && LA16_0<=Exec)||LA16_0==Assert||(LA16_0>=ListComp && LA16_0<=Repr)||LA16_0==SubscriptList||(LA16_0>=UAdd && LA16_0<=Delete)||(LA16_0>=With && LA16_0<=GeneratorExp)||LA16_0==Call||LA16_0==Parens||LA16_0==DOT||(LA16_0>=STAR && LA16_0<=DOUBLESTAR)||(LA16_0>=PLUSEQUAL && LA16_0<=DOUBLESLASH)||LA16_0==182||LA16_0==193) ) {
                            alt16=1;
                        }


                        switch (alt16) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:457:7: stmt
                    	    {
                    	    pushFollow(FOLLOW_stmt_in_stmts484);
                    	    stmt();
                    	    _fsp--;


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt16 >= 1 ) break loop16;
                                EarlyExitException eee =
                                    new EarlyExitException(16, input);
                                throw eee;
                        }
                        cnt16++;
                    } while (true);


                            debug("Matched stmts");
                            retval.stypes = ((stmts_scope)stmts_stack.peek()).statements;
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:461:7: INDENT ( stmt )+ DEDENT
                    {
                    match(input,INDENT,FOLLOW_INDENT_in_stmts495); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:461:14: ( stmt )+
                    int cnt17=0;
                    loop17:
                    do {
                        int alt17=2;
                        int LA17_0 = input.LA(1);

                        if ( ((LA17_0>=Import && LA17_0<=ImportFrom)||LA17_0==Name||LA17_0==ClassDef||LA17_0==FunctionDef||LA17_0==Assign||(LA17_0>=Tuple && LA17_0<=IfExp)||(LA17_0>=While && LA17_0<=TryFinally)||(LA17_0>=For && LA17_0<=IsNot)||(LA17_0>=NotIn && LA17_0<=Raise)||(LA17_0>=Global && LA17_0<=Exec)||LA17_0==Assert||(LA17_0>=ListComp && LA17_0<=Repr)||LA17_0==SubscriptList||(LA17_0>=UAdd && LA17_0<=Delete)||(LA17_0>=With && LA17_0<=GeneratorExp)||LA17_0==Call||LA17_0==Parens||LA17_0==DOT||(LA17_0>=STAR && LA17_0<=DOUBLESTAR)||(LA17_0>=PLUSEQUAL && LA17_0<=DOUBLESLASH)||LA17_0==182||LA17_0==193) ) {
                            alt17=1;
                        }


                        switch (alt17) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:461:14: stmt
                    	    {
                    	    pushFollow(FOLLOW_stmt_in_stmts497);
                    	    stmt();
                    	    _fsp--;


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt17 >= 1 ) break loop17;
                                EarlyExitException eee =
                                    new EarlyExitException(17, input);
                                throw eee;
                        }
                        cnt17++;
                    } while (true);

                    match(input,DEDENT,FOLLOW_DEDENT_in_stmts500); 

                            debug("Matched stmts");
                            retval.stypes = ((stmts_scope)stmts_stack.peek()).statements;
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            stmts_stack.pop();
        }
        return retval;
    }
    // $ANTLR end stmts


    // $ANTLR start stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:467:1: stmt : ( expr_stmt | print_stmt | del_stmt | pass_stmt | flow_stmt | import_stmt | global_stmt | exec_stmt | assert_stmt | if_stmt | while_stmt | for_stmt | try_stmt | with_stmt | funcdef | classdef );
    public final void stmt() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:468:5: ( expr_stmt | print_stmt | del_stmt | pass_stmt | flow_stmt | import_stmt | global_stmt | exec_stmt | assert_stmt | if_stmt | while_stmt | for_stmt | try_stmt | with_stmt | funcdef | classdef )
            int alt19=16;
            switch ( input.LA(1) ) {
            case Name:
            case Assign:
            case Tuple:
            case List:
            case Dict:
            case IfExp:
            case Yield:
            case Str:
            case Num:
            case IsNot:
            case NotIn:
            case ListComp:
            case Lambda:
            case Repr:
            case SubscriptList:
            case UAdd:
            case USub:
            case Invert:
            case GeneratorExp:
            case Call:
            case Parens:
            case DOT:
            case STAR:
            case DOUBLESTAR:
            case PLUSEQUAL:
            case MINUSEQUAL:
            case STAREQUAL:
            case SLASHEQUAL:
            case PERCENTEQUAL:
            case AMPEREQUAL:
            case VBAREQUAL:
            case CIRCUMFLEXEQUAL:
            case LEFTSHIFTEQUAL:
            case RIGHTSHIFTEQUAL:
            case DOUBLESTAREQUAL:
            case DOUBLESLASHEQUAL:
            case RIGHTSHIFT:
            case OR:
            case AND:
            case NOT:
            case LESS:
            case GREATER:
            case EQUAL:
            case GREATEREQUAL:
            case LESSEQUAL:
            case ALT_NOTEQUAL:
            case NOTEQUAL:
            case VBAR:
            case CIRCUMFLEX:
            case AMPER:
            case LEFTSHIFT:
            case PLUS:
            case MINUS:
            case SLASH:
            case PERCENT:
            case DOUBLESLASH:
            case 182:
            case 193:
                {
                alt19=1;
                }
                break;
            case Print:
                {
                alt19=2;
                }
                break;
            case Delete:
                {
                alt19=3;
                }
                break;
            case Pass:
                {
                alt19=4;
                }
                break;
            case Break:
            case Continue:
            case Return:
            case Raise:
                {
                alt19=5;
                }
                break;
            case Import:
            case ImportFrom:
                {
                alt19=6;
                }
                break;
            case Global:
                {
                alt19=7;
                }
                break;
            case Exec:
                {
                alt19=8;
                }
                break;
            case Assert:
                {
                alt19=9;
                }
                break;
            case If:
                {
                alt19=10;
                }
                break;
            case While:
                {
                alt19=11;
                }
                break;
            case For:
                {
                alt19=12;
                }
                break;
            case TryExcept:
            case TryFinally:
                {
                alt19=13;
                }
                break;
            case With:
                {
                alt19=14;
                }
                break;
            case FunctionDef:
                {
                alt19=15;
                }
                break;
            case ClassDef:
                {
                alt19=16;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("467:1: stmt : ( expr_stmt | print_stmt | del_stmt | pass_stmt | flow_stmt | import_stmt | global_stmt | exec_stmt | assert_stmt | if_stmt | while_stmt | for_stmt | try_stmt | with_stmt | funcdef | classdef );", 19, 0, input);

                throw nvae;
            }

            switch (alt19) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:468:7: expr_stmt
                    {
                    pushFollow(FOLLOW_expr_stmt_in_stmt520);
                    expr_stmt();
                    _fsp--;


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:469:7: print_stmt
                    {
                    pushFollow(FOLLOW_print_stmt_in_stmt528);
                    print_stmt();
                    _fsp--;


                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:470:7: del_stmt
                    {
                    pushFollow(FOLLOW_del_stmt_in_stmt536);
                    del_stmt();
                    _fsp--;


                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:471:7: pass_stmt
                    {
                    pushFollow(FOLLOW_pass_stmt_in_stmt544);
                    pass_stmt();
                    _fsp--;


                    }
                    break;
                case 5 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:472:7: flow_stmt
                    {
                    pushFollow(FOLLOW_flow_stmt_in_stmt552);
                    flow_stmt();
                    _fsp--;


                    }
                    break;
                case 6 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:473:7: import_stmt
                    {
                    pushFollow(FOLLOW_import_stmt_in_stmt560);
                    import_stmt();
                    _fsp--;


                    }
                    break;
                case 7 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:474:7: global_stmt
                    {
                    pushFollow(FOLLOW_global_stmt_in_stmt568);
                    global_stmt();
                    _fsp--;


                    }
                    break;
                case 8 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:475:7: exec_stmt
                    {
                    pushFollow(FOLLOW_exec_stmt_in_stmt576);
                    exec_stmt();
                    _fsp--;


                    }
                    break;
                case 9 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:476:7: assert_stmt
                    {
                    pushFollow(FOLLOW_assert_stmt_in_stmt584);
                    assert_stmt();
                    _fsp--;


                    }
                    break;
                case 10 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:477:7: if_stmt
                    {
                    pushFollow(FOLLOW_if_stmt_in_stmt592);
                    if_stmt();
                    _fsp--;


                    }
                    break;
                case 11 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:478:7: while_stmt
                    {
                    pushFollow(FOLLOW_while_stmt_in_stmt600);
                    while_stmt();
                    _fsp--;


                    }
                    break;
                case 12 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:479:7: for_stmt
                    {
                    pushFollow(FOLLOW_for_stmt_in_stmt608);
                    for_stmt();
                    _fsp--;


                    }
                    break;
                case 13 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:480:7: try_stmt
                    {
                    pushFollow(FOLLOW_try_stmt_in_stmt616);
                    try_stmt();
                    _fsp--;


                    }
                    break;
                case 14 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:481:7: with_stmt
                    {
                    pushFollow(FOLLOW_with_stmt_in_stmt624);
                    with_stmt();
                    _fsp--;


                    }
                    break;
                case 15 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:482:7: funcdef
                    {
                    pushFollow(FOLLOW_funcdef_in_stmt632);
                    funcdef();
                    _fsp--;


                    }
                    break;
                case 16 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:483:7: classdef
                    {
                    pushFollow(FOLLOW_classdef_in_stmt640);
                    classdef();
                    _fsp--;


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end stmt


    // $ANTLR start expr_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:486:1: expr_stmt : ( test[expr_contextType.Load] | ^( augassign targ= test[expr_contextType.Store] value= test[expr_contextType.Load] ) | ^( Assign targets ^( Value value= test[expr_contextType.Load] ) ) );
    public final void expr_stmt() throws RecognitionException {
        PythonTree Assign26=null;
        test_return targ = null;

        test_return value = null;

        test_return test23 = null;

        augassign_return augassign24 = null;

        List targets25 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:487:5: ( test[expr_contextType.Load] | ^( augassign targ= test[expr_contextType.Store] value= test[expr_contextType.Load] ) | ^( Assign targets ^( Value value= test[expr_contextType.Load] ) ) )
            int alt20=3;
            switch ( input.LA(1) ) {
            case Name:
            case Tuple:
            case List:
            case Dict:
            case IfExp:
            case Yield:
            case Str:
            case Num:
            case IsNot:
            case NotIn:
            case ListComp:
            case Lambda:
            case Repr:
            case SubscriptList:
            case UAdd:
            case USub:
            case Invert:
            case GeneratorExp:
            case Call:
            case Parens:
            case DOT:
            case STAR:
            case DOUBLESTAR:
            case RIGHTSHIFT:
            case OR:
            case AND:
            case NOT:
            case LESS:
            case GREATER:
            case EQUAL:
            case GREATEREQUAL:
            case LESSEQUAL:
            case ALT_NOTEQUAL:
            case NOTEQUAL:
            case VBAR:
            case CIRCUMFLEX:
            case AMPER:
            case LEFTSHIFT:
            case PLUS:
            case MINUS:
            case SLASH:
            case PERCENT:
            case DOUBLESLASH:
            case 182:
            case 193:
                {
                alt20=1;
                }
                break;
            case PLUSEQUAL:
            case MINUSEQUAL:
            case STAREQUAL:
            case SLASHEQUAL:
            case PERCENTEQUAL:
            case AMPEREQUAL:
            case VBAREQUAL:
            case CIRCUMFLEXEQUAL:
            case LEFTSHIFTEQUAL:
            case RIGHTSHIFTEQUAL:
            case DOUBLESTAREQUAL:
            case DOUBLESLASHEQUAL:
                {
                alt20=2;
                }
                break;
            case Assign:
                {
                alt20=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("486:1: expr_stmt : ( test[expr_contextType.Load] | ^( augassign targ= test[expr_contextType.Store] value= test[expr_contextType.Load] ) | ^( Assign targets ^( Value value= test[expr_contextType.Load] ) ) );", 20, 0, input);

                throw nvae;
            }

            switch (alt20) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:487:7: test[expr_contextType.Load]
                    {
                    pushFollow(FOLLOW_test_in_expr_stmt657);
                    test23=test(expr_contextType.Load);
                    _fsp--;


                            debug("matched expr_stmt:test " + test23.etype);
                            ((stmts_scope)stmts_stack.peek()).statements.add(new Expr(((PythonTree)test23.start), test23.etype));
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:491:7: ^( augassign targ= test[expr_contextType.Store] value= test[expr_contextType.Load] )
                    {
                    pushFollow(FOLLOW_augassign_in_expr_stmt669);
                    augassign24=augassign();
                    _fsp--;


                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_expr_stmt673);
                    targ=test(expr_contextType.Store);
                    _fsp--;

                    pushFollow(FOLLOW_test_in_expr_stmt678);
                    value=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                            AugAssign a = new AugAssign(((PythonTree)augassign24.start), targ.etype, augassign24.op, value.etype);
                            ((stmts_scope)stmts_stack.peek()).statements.add(a);
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:495:7: ^( Assign targets ^( Value value= test[expr_contextType.Load] ) )
                    {
                    Assign26=(PythonTree)input.LT(1);
                    match(input,Assign,FOLLOW_Assign_in_expr_stmt691); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_targets_in_expr_stmt693);
                    targets25=targets();
                    _fsp--;

                    match(input,Value,FOLLOW_Value_in_expr_stmt696); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_expr_stmt700);
                    value=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                    match(input, Token.UP, null); 

                            debug("Matched Assign");
                            exprType[] e = new exprType[targets25.size()];
                            for(int i=0;i<targets25.size();i++) {
                                e[i] = (exprType)targets25.get(i);
                            }
                            debug("exprs: " + e.length);
                            Assign a = new Assign(Assign26, e, value.etype);
                            ((stmts_scope)stmts_stack.peek()).statements.add(a);
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end expr_stmt


    // $ANTLR start call_expr
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:507:1: call_expr returns [exprType etype] : ^( Call ( ^( Args arglist ) )? test[expr_contextType.Load] ) ;
    public final exprType call_expr() throws RecognitionException {
        exprType etype = null;

        PythonTree Args27=null;
        PythonTree Call28=null;
        test_return test29 = null;

        arglist_return arglist30 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:508:5: ( ^( Call ( ^( Args arglist ) )? test[expr_contextType.Load] ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:508:7: ^( Call ( ^( Args arglist ) )? test[expr_contextType.Load] )
            {
            Call28=(PythonTree)input.LT(1);
            match(input,Call,FOLLOW_Call_in_call_expr727); 

            match(input, Token.DOWN, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:508:14: ( ^( Args arglist ) )?
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0==Args) ) {
                alt21=1;
            }
            switch (alt21) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:508:15: ^( Args arglist )
                    {
                    Args27=(PythonTree)input.LT(1);
                    match(input,Args,FOLLOW_Args_in_call_expr731); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_arglist_in_call_expr733);
                    arglist30=arglist();
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }

            pushFollow(FOLLOW_test_in_call_expr738);
            test29=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 

                    Call c;
                    if (Args27 == null) {
                        c = new Call(Call28, test29.etype, new exprType[0], new keywordType[0], null, null);
                        debug("Matched Call site no args");
                    } else {
                        debug("Matched Call w/ args");
                        exprType[] args = (exprType[])arglist30.args.toArray(new exprType[arglist30.args.size()]);
                        keywordType[] keywords = (keywordType[])arglist30.keywords.toArray(new keywordType[arglist30.keywords.size()]);
                        c = new Call(Call28, test29.etype, args, keywords, arglist30.starargs, arglist30.kwargs);
                    }
                    etype = c;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end call_expr


    // $ANTLR start targets
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:523:1: targets returns [List etypes] : ( target[targs] )+ ;
    public final List targets() throws RecognitionException {
        List etypes = null;


            List targs = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:527:5: ( ( target[targs] )+ )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:527:7: ( target[targs] )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:527:7: ( target[targs] )+
            int cnt22=0;
            loop22:
            do {
                int alt22=2;
                int LA22_0 = input.LA(1);

                if ( (LA22_0==Target) ) {
                    alt22=1;
                }


                switch (alt22) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:527:7: target[targs]
            	    {
            	    pushFollow(FOLLOW_target_in_targets768);
            	    target(targs);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt22 >= 1 ) break loop22;
                        EarlyExitException eee =
                            new EarlyExitException(22, input);
                        throw eee;
                }
                cnt22++;
            } while (true);


                    etypes = targs;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etypes;
    }
    // $ANTLR end targets


    // $ANTLR start target
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:532:1: target[List etypes] : ^( Target test[expr_contextType.Store] ) ;
    public final void target(List etypes) throws RecognitionException {
        test_return test31 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:533:5: ( ^( Target test[expr_contextType.Store] ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:533:7: ^( Target test[expr_contextType.Store] )
            {
            match(input,Target,FOLLOW_Target_in_target791); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_target793);
            test31=test(expr_contextType.Store);
            _fsp--;


            match(input, Token.UP, null); 

                    etypes.add(test31.etype);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end target

    public static class augassign_return extends TreeRuleReturnScope {
        public operatorType op;
    };

    // $ANTLR start augassign
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:538:1: augassign returns [operatorType op] : ( PLUSEQUAL | MINUSEQUAL | STAREQUAL | SLASHEQUAL | PERCENTEQUAL | AMPEREQUAL | VBAREQUAL | CIRCUMFLEXEQUAL | LEFTSHIFTEQUAL | RIGHTSHIFTEQUAL | DOUBLESTAREQUAL | DOUBLESLASHEQUAL );
    public final augassign_return augassign() throws RecognitionException {
        augassign_return retval = new augassign_return();
        retval.start = input.LT(1);

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:539:5: ( PLUSEQUAL | MINUSEQUAL | STAREQUAL | SLASHEQUAL | PERCENTEQUAL | AMPEREQUAL | VBAREQUAL | CIRCUMFLEXEQUAL | LEFTSHIFTEQUAL | RIGHTSHIFTEQUAL | DOUBLESTAREQUAL | DOUBLESLASHEQUAL )
            int alt23=12;
            switch ( input.LA(1) ) {
            case PLUSEQUAL:
                {
                alt23=1;
                }
                break;
            case MINUSEQUAL:
                {
                alt23=2;
                }
                break;
            case STAREQUAL:
                {
                alt23=3;
                }
                break;
            case SLASHEQUAL:
                {
                alt23=4;
                }
                break;
            case PERCENTEQUAL:
                {
                alt23=5;
                }
                break;
            case AMPEREQUAL:
                {
                alt23=6;
                }
                break;
            case VBAREQUAL:
                {
                alt23=7;
                }
                break;
            case CIRCUMFLEXEQUAL:
                {
                alt23=8;
                }
                break;
            case LEFTSHIFTEQUAL:
                {
                alt23=9;
                }
                break;
            case RIGHTSHIFTEQUAL:
                {
                alt23=10;
                }
                break;
            case DOUBLESTAREQUAL:
                {
                alt23=11;
                }
                break;
            case DOUBLESLASHEQUAL:
                {
                alt23=12;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("538:1: augassign returns [operatorType op] : ( PLUSEQUAL | MINUSEQUAL | STAREQUAL | SLASHEQUAL | PERCENTEQUAL | AMPEREQUAL | VBAREQUAL | CIRCUMFLEXEQUAL | LEFTSHIFTEQUAL | RIGHTSHIFTEQUAL | DOUBLESTAREQUAL | DOUBLESLASHEQUAL );", 23, 0, input);

                throw nvae;
            }

            switch (alt23) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:539:7: PLUSEQUAL
                    {
                    match(input,PLUSEQUAL,FOLLOW_PLUSEQUAL_in_augassign818); 
                    retval.op = operatorType.Add;

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:540:7: MINUSEQUAL
                    {
                    match(input,MINUSEQUAL,FOLLOW_MINUSEQUAL_in_augassign828); 
                    retval.op = operatorType.Sub;

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:541:7: STAREQUAL
                    {
                    match(input,STAREQUAL,FOLLOW_STAREQUAL_in_augassign838); 
                    retval.op = operatorType.Mult;

                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:542:7: SLASHEQUAL
                    {
                    match(input,SLASHEQUAL,FOLLOW_SLASHEQUAL_in_augassign848); 
                    retval.op = operatorType.Div;

                    }
                    break;
                case 5 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:543:7: PERCENTEQUAL
                    {
                    match(input,PERCENTEQUAL,FOLLOW_PERCENTEQUAL_in_augassign858); 
                    retval.op = operatorType.Mod;

                    }
                    break;
                case 6 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:544:7: AMPEREQUAL
                    {
                    match(input,AMPEREQUAL,FOLLOW_AMPEREQUAL_in_augassign868); 
                    retval.op = operatorType.BitAnd;

                    }
                    break;
                case 7 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:545:7: VBAREQUAL
                    {
                    match(input,VBAREQUAL,FOLLOW_VBAREQUAL_in_augassign878); 
                    retval.op = operatorType.BitOr;

                    }
                    break;
                case 8 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:546:7: CIRCUMFLEXEQUAL
                    {
                    match(input,CIRCUMFLEXEQUAL,FOLLOW_CIRCUMFLEXEQUAL_in_augassign888); 
                    retval.op = operatorType.BitXor;

                    }
                    break;
                case 9 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:547:7: LEFTSHIFTEQUAL
                    {
                    match(input,LEFTSHIFTEQUAL,FOLLOW_LEFTSHIFTEQUAL_in_augassign898); 
                    retval.op = operatorType.LShift;

                    }
                    break;
                case 10 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:548:7: RIGHTSHIFTEQUAL
                    {
                    match(input,RIGHTSHIFTEQUAL,FOLLOW_RIGHTSHIFTEQUAL_in_augassign908); 
                    retval.op = operatorType.RShift;

                    }
                    break;
                case 11 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:549:7: DOUBLESTAREQUAL
                    {
                    match(input,DOUBLESTAREQUAL,FOLLOW_DOUBLESTAREQUAL_in_augassign918); 
                    retval.op = operatorType.Pow;

                    }
                    break;
                case 12 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:550:7: DOUBLESLASHEQUAL
                    {
                    match(input,DOUBLESLASHEQUAL,FOLLOW_DOUBLESLASHEQUAL_in_augassign928); 
                    retval.op = operatorType.FloorDiv;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end augassign


    // $ANTLR start binop
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:553:1: binop returns [operatorType op] : ( PLUS | MINUS | STAR | SLASH | PERCENT | AMPER | VBAR | CIRCUMFLEX | LEFTSHIFT | RIGHTSHIFT | DOUBLESTAR | DOUBLESLASH );
    public final operatorType binop() throws RecognitionException {
        operatorType op = null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:554:5: ( PLUS | MINUS | STAR | SLASH | PERCENT | AMPER | VBAR | CIRCUMFLEX | LEFTSHIFT | RIGHTSHIFT | DOUBLESTAR | DOUBLESLASH )
            int alt24=12;
            switch ( input.LA(1) ) {
            case PLUS:
                {
                alt24=1;
                }
                break;
            case MINUS:
                {
                alt24=2;
                }
                break;
            case STAR:
                {
                alt24=3;
                }
                break;
            case SLASH:
                {
                alt24=4;
                }
                break;
            case PERCENT:
                {
                alt24=5;
                }
                break;
            case AMPER:
                {
                alt24=6;
                }
                break;
            case VBAR:
                {
                alt24=7;
                }
                break;
            case CIRCUMFLEX:
                {
                alt24=8;
                }
                break;
            case LEFTSHIFT:
                {
                alt24=9;
                }
                break;
            case RIGHTSHIFT:
                {
                alt24=10;
                }
                break;
            case DOUBLESTAR:
                {
                alt24=11;
                }
                break;
            case DOUBLESLASH:
                {
                alt24=12;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("553:1: binop returns [operatorType op] : ( PLUS | MINUS | STAR | SLASH | PERCENT | AMPER | VBAR | CIRCUMFLEX | LEFTSHIFT | RIGHTSHIFT | DOUBLESTAR | DOUBLESLASH );", 24, 0, input);

                throw nvae;
            }

            switch (alt24) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:554:7: PLUS
                    {
                    match(input,PLUS,FOLLOW_PLUS_in_binop951); 
                    op = operatorType.Add;

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:555:7: MINUS
                    {
                    match(input,MINUS,FOLLOW_MINUS_in_binop961); 
                    op = operatorType.Sub;

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:556:7: STAR
                    {
                    match(input,STAR,FOLLOW_STAR_in_binop971); 
                    op = operatorType.Mult;

                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:557:7: SLASH
                    {
                    match(input,SLASH,FOLLOW_SLASH_in_binop981); 
                    op = operatorType.Div;

                    }
                    break;
                case 5 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:558:7: PERCENT
                    {
                    match(input,PERCENT,FOLLOW_PERCENT_in_binop991); 
                    op = operatorType.Mod;

                    }
                    break;
                case 6 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:559:7: AMPER
                    {
                    match(input,AMPER,FOLLOW_AMPER_in_binop1001); 
                    op = operatorType.BitAnd;

                    }
                    break;
                case 7 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:560:7: VBAR
                    {
                    match(input,VBAR,FOLLOW_VBAR_in_binop1011); 
                    op = operatorType.BitOr;

                    }
                    break;
                case 8 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:561:7: CIRCUMFLEX
                    {
                    match(input,CIRCUMFLEX,FOLLOW_CIRCUMFLEX_in_binop1021); 
                    op = operatorType.BitXor;

                    }
                    break;
                case 9 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:562:7: LEFTSHIFT
                    {
                    match(input,LEFTSHIFT,FOLLOW_LEFTSHIFT_in_binop1031); 
                    op = operatorType.LShift;

                    }
                    break;
                case 10 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:563:7: RIGHTSHIFT
                    {
                    match(input,RIGHTSHIFT,FOLLOW_RIGHTSHIFT_in_binop1041); 
                    op = operatorType.RShift;

                    }
                    break;
                case 11 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:564:7: DOUBLESTAR
                    {
                    match(input,DOUBLESTAR,FOLLOW_DOUBLESTAR_in_binop1051); 
                    op = operatorType.Pow;

                    }
                    break;
                case 12 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:565:7: DOUBLESLASH
                    {
                    match(input,DOUBLESLASH,FOLLOW_DOUBLESLASH_in_binop1061); 
                    op = operatorType.FloorDiv;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return op;
    }
    // $ANTLR end binop


    // $ANTLR start print_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:569:1: print_stmt : ^( Print ( ^( Dest RIGHTSHIFT ) )? ( ^( Values ^( Elts elts[expr_contextType.Load] ) ) )? ( Newline )? ) ;
    public final void print_stmt() throws RecognitionException {
        PythonTree Newline32=null;
        PythonTree Dest33=null;
        PythonTree Values34=null;
        PythonTree Print36=null;
        List elts35 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:5: ( ^( Print ( ^( Dest RIGHTSHIFT ) )? ( ^( Values ^( Elts elts[expr_contextType.Load] ) ) )? ( Newline )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:7: ^( Print ( ^( Dest RIGHTSHIFT ) )? ( ^( Values ^( Elts elts[expr_contextType.Load] ) ) )? ( Newline )? )
            {
            Print36=(PythonTree)input.LT(1);
            match(input,Print,FOLLOW_Print_in_print_stmt1082); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:15: ( ^( Dest RIGHTSHIFT ) )?
                int alt25=2;
                int LA25_0 = input.LA(1);

                if ( (LA25_0==Dest) ) {
                    alt25=1;
                }
                switch (alt25) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:16: ^( Dest RIGHTSHIFT )
                        {
                        Dest33=(PythonTree)input.LT(1);
                        match(input,Dest,FOLLOW_Dest_in_print_stmt1086); 

                        match(input, Token.DOWN, null); 
                        match(input,RIGHTSHIFT,FOLLOW_RIGHTSHIFT_in_print_stmt1088); 

                        match(input, Token.UP, null); 

                        }
                        break;

                }

                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:37: ( ^( Values ^( Elts elts[expr_contextType.Load] ) ) )?
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0==Values) ) {
                    alt26=1;
                }
                switch (alt26) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:38: ^( Values ^( Elts elts[expr_contextType.Load] ) )
                        {
                        Values34=(PythonTree)input.LT(1);
                        match(input,Values,FOLLOW_Values_in_print_stmt1095); 

                        match(input, Token.DOWN, null); 
                        match(input,Elts,FOLLOW_Elts_in_print_stmt1098); 

                        match(input, Token.DOWN, null); 
                        pushFollow(FOLLOW_elts_in_print_stmt1100);
                        elts35=elts(expr_contextType.Load);
                        _fsp--;


                        match(input, Token.UP, null); 

                        match(input, Token.UP, null); 

                        }
                        break;

                }

                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:86: ( Newline )?
                int alt27=2;
                int LA27_0 = input.LA(1);

                if ( (LA27_0==Newline) ) {
                    alt27=1;
                }
                switch (alt27) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:570:87: Newline
                        {
                        Newline32=(PythonTree)input.LT(1);
                        match(input,Newline,FOLLOW_Newline_in_print_stmt1108); 

                        }
                        break;

                }


                match(input, Token.UP, null); 
            }

                    Print p;
                    exprType[] values;

                    exprType dest = null;
                    boolean hasdest = false;

                    boolean newline = false;
                    if (Newline32 != null) {
                        newline = true;
                    }

                    if (Dest33 != null) {
                        hasdest = true;
                    }
                    if (Values34 != null) {
                        exprType[] t = (exprType[])elts35.toArray(new exprType[elts35.size()]);
                        if (hasdest) {
                            dest = t[0];
                            values = new exprType[t.length - 1];
                            System.arraycopy(t, 1, values, 0, values.length);
                        } else {
                            values = t;
                        }
                    } else {
                        values = new exprType[0];
                    }
                    p = new Print(Print36, dest, values, newline);
                    ((stmts_scope)stmts_stack.peek()).statements.add(p);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end print_stmt


    // $ANTLR start del_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:602:1: del_stmt : ^( Delete elts[expr_contextType.Del] ) ;
    public final void del_stmt() throws RecognitionException {
        PythonTree Delete38=null;
        List elts37 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:603:5: ( ^( Delete elts[expr_contextType.Del] ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:603:7: ^( Delete elts[expr_contextType.Del] )
            {
            Delete38=(PythonTree)input.LT(1);
            match(input,Delete,FOLLOW_Delete_in_del_stmt1131); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_elts_in_del_stmt1133);
            elts37=elts(expr_contextType.Del);
            _fsp--;


            match(input, Token.UP, null); 

                    exprType[] t = (exprType[])elts37.toArray(new exprType[elts37.size()]);
                    ((stmts_scope)stmts_stack.peek()).statements.add(new Delete(Delete38, t));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end del_stmt


    // $ANTLR start pass_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:609:1: pass_stmt : Pass ;
    public final void pass_stmt() throws RecognitionException {
        PythonTree Pass39=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:610:5: ( Pass )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:610:7: Pass
            {
            Pass39=(PythonTree)input.LT(1);
            match(input,Pass,FOLLOW_Pass_in_pass_stmt1154); 

                    debug("Matched Pass");
                    ((stmts_scope)stmts_stack.peek()).statements.add(new Pass(Pass39));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end pass_stmt


    // $ANTLR start flow_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:616:1: flow_stmt : ( break_stmt | continue_stmt | return_stmt | raise_stmt );
    public final void flow_stmt() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:617:5: ( break_stmt | continue_stmt | return_stmt | raise_stmt )
            int alt28=4;
            switch ( input.LA(1) ) {
            case Break:
                {
                alt28=1;
                }
                break;
            case Continue:
                {
                alt28=2;
                }
                break;
            case Return:
                {
                alt28=3;
                }
                break;
            case Raise:
                {
                alt28=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("616:1: flow_stmt : ( break_stmt | continue_stmt | return_stmt | raise_stmt );", 28, 0, input);

                throw nvae;
            }

            switch (alt28) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:617:7: break_stmt
                    {
                    pushFollow(FOLLOW_break_stmt_in_flow_stmt1173);
                    break_stmt();
                    _fsp--;


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:618:7: continue_stmt
                    {
                    pushFollow(FOLLOW_continue_stmt_in_flow_stmt1181);
                    continue_stmt();
                    _fsp--;


                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:619:7: return_stmt
                    {
                    pushFollow(FOLLOW_return_stmt_in_flow_stmt1189);
                    return_stmt();
                    _fsp--;


                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:620:7: raise_stmt
                    {
                    pushFollow(FOLLOW_raise_stmt_in_flow_stmt1197);
                    raise_stmt();
                    _fsp--;


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end flow_stmt


    // $ANTLR start break_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:623:1: break_stmt : Break ;
    public final void break_stmt() throws RecognitionException {
        PythonTree Break40=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:624:5: ( Break )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:624:7: Break
            {
            Break40=(PythonTree)input.LT(1);
            match(input,Break,FOLLOW_Break_in_break_stmt1214); 

                    ((stmts_scope)stmts_stack.peek()).statements.add(new Break(Break40));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end break_stmt


    // $ANTLR start continue_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:629:1: continue_stmt : Continue ;
    public final void continue_stmt() throws RecognitionException {
        PythonTree Continue41=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:630:5: ( Continue )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:630:7: Continue
            {
            Continue41=(PythonTree)input.LT(1);
            match(input,Continue,FOLLOW_Continue_in_continue_stmt1233); 

                    ((stmts_scope)stmts_stack.peek()).statements.add(new Continue(Continue41));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end continue_stmt


    // $ANTLR start return_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:635:1: return_stmt : ^( Return ( ^( Value test[expr_contextType.Load] ) )? ) ;
    public final void return_stmt() throws RecognitionException {
        PythonTree Value42=null;
        PythonTree Return44=null;
        test_return test43 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:636:5: ( ^( Return ( ^( Value test[expr_contextType.Load] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:636:7: ^( Return ( ^( Value test[expr_contextType.Load] ) )? )
            {
            Return44=(PythonTree)input.LT(1);
            match(input,Return,FOLLOW_Return_in_return_stmt1253); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:636:16: ( ^( Value test[expr_contextType.Load] ) )?
                int alt29=2;
                int LA29_0 = input.LA(1);

                if ( (LA29_0==Value) ) {
                    alt29=1;
                }
                switch (alt29) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:636:17: ^( Value test[expr_contextType.Load] )
                        {
                        Value42=(PythonTree)input.LT(1);
                        match(input,Value,FOLLOW_Value_in_return_stmt1257); 

                        match(input, Token.DOWN, null); 
                        pushFollow(FOLLOW_test_in_return_stmt1259);
                        test43=test(expr_contextType.Load);
                        _fsp--;


                        match(input, Token.UP, null); 

                        }
                        break;

                }


                match(input, Token.UP, null); 
            }

                    exprType v = null;
                    if (Value42 != null) {
                        v = test43.etype;
                    }
                    ((stmts_scope)stmts_stack.peek()).statements.add(new Return(Return44, v));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end return_stmt


    // $ANTLR start yield_expr
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:645:1: yield_expr returns [exprType etype] : ^( Yield ( ^( Value test[expr_contextType.Load] ) )? ) ;
    public final exprType yield_expr() throws RecognitionException {
        exprType etype = null;

        PythonTree Value45=null;
        PythonTree Yield47=null;
        test_return test46 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:646:5: ( ^( Yield ( ^( Value test[expr_contextType.Load] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:646:7: ^( Yield ( ^( Value test[expr_contextType.Load] ) )? )
            {
            Yield47=(PythonTree)input.LT(1);
            match(input,Yield,FOLLOW_Yield_in_yield_expr1288); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:646:15: ( ^( Value test[expr_contextType.Load] ) )?
                int alt30=2;
                int LA30_0 = input.LA(1);

                if ( (LA30_0==Value) ) {
                    alt30=1;
                }
                switch (alt30) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:646:16: ^( Value test[expr_contextType.Load] )
                        {
                        Value45=(PythonTree)input.LT(1);
                        match(input,Value,FOLLOW_Value_in_yield_expr1292); 

                        match(input, Token.DOWN, null); 
                        pushFollow(FOLLOW_test_in_yield_expr1294);
                        test46=test(expr_contextType.Load);
                        _fsp--;


                        match(input, Token.UP, null); 

                        }
                        break;

                }


                match(input, Token.UP, null); 
            }

                    exprType v = null;
                    if (Value45 != null) {
                        v = test46.etype; 
                    }
                    etype = new Yield(Yield47, v);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end yield_expr


    // $ANTLR start raise_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:655:1: raise_stmt : ^( Raise ( ^( Type type= test[expr_contextType.Load] ) )? ( ^( Inst inst= test[expr_contextType.Load] ) )? ( ^( Tback tback= test[expr_contextType.Load] ) )? ) ;
    public final void raise_stmt() throws RecognitionException {
        PythonTree Type48=null;
        PythonTree Inst49=null;
        PythonTree Tback50=null;
        PythonTree Raise51=null;
        test_return type = null;

        test_return inst = null;

        test_return tback = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:5: ( ^( Raise ( ^( Type type= test[expr_contextType.Load] ) )? ( ^( Inst inst= test[expr_contextType.Load] ) )? ( ^( Tback tback= test[expr_contextType.Load] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:7: ^( Raise ( ^( Type type= test[expr_contextType.Load] ) )? ( ^( Inst inst= test[expr_contextType.Load] ) )? ( ^( Tback tback= test[expr_contextType.Load] ) )? )
            {
            Raise51=(PythonTree)input.LT(1);
            match(input,Raise,FOLLOW_Raise_in_raise_stmt1319); 

            if ( input.LA(1)==Token.DOWN ) {
                match(input, Token.DOWN, null); 
                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:15: ( ^( Type type= test[expr_contextType.Load] ) )?
                int alt31=2;
                int LA31_0 = input.LA(1);

                if ( (LA31_0==Type) ) {
                    alt31=1;
                }
                switch (alt31) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:16: ^( Type type= test[expr_contextType.Load] )
                        {
                        Type48=(PythonTree)input.LT(1);
                        match(input,Type,FOLLOW_Type_in_raise_stmt1323); 

                        match(input, Token.DOWN, null); 
                        pushFollow(FOLLOW_test_in_raise_stmt1327);
                        type=test(expr_contextType.Load);
                        _fsp--;


                        match(input, Token.UP, null); 

                        }
                        break;

                }

                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:59: ( ^( Inst inst= test[expr_contextType.Load] ) )?
                int alt32=2;
                int LA32_0 = input.LA(1);

                if ( (LA32_0==Inst) ) {
                    alt32=1;
                }
                switch (alt32) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:60: ^( Inst inst= test[expr_contextType.Load] )
                        {
                        Inst49=(PythonTree)input.LT(1);
                        match(input,Inst,FOLLOW_Inst_in_raise_stmt1335); 

                        match(input, Token.DOWN, null); 
                        pushFollow(FOLLOW_test_in_raise_stmt1339);
                        inst=test(expr_contextType.Load);
                        _fsp--;


                        match(input, Token.UP, null); 

                        }
                        break;

                }

                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:103: ( ^( Tback tback= test[expr_contextType.Load] ) )?
                int alt33=2;
                int LA33_0 = input.LA(1);

                if ( (LA33_0==Tback) ) {
                    alt33=1;
                }
                switch (alt33) {
                    case 1 :
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:656:104: ^( Tback tback= test[expr_contextType.Load] )
                        {
                        Tback50=(PythonTree)input.LT(1);
                        match(input,Tback,FOLLOW_Tback_in_raise_stmt1347); 

                        match(input, Token.DOWN, null); 
                        pushFollow(FOLLOW_test_in_raise_stmt1351);
                        tback=test(expr_contextType.Load);
                        _fsp--;


                        match(input, Token.UP, null); 

                        }
                        break;

                }


                match(input, Token.UP, null); 
            }

                    exprType t = null;
                    if (Type48 != null) {
                        t = type.etype;
                    }
                    exprType i = null;
                    if (Inst49 != null) {
                        i = inst.etype;
                    }
                    exprType b = null;
                    if (Tback50 != null) {
                        b = tback.etype;
                    }

                    ((stmts_scope)stmts_stack.peek()).statements.add(new Raise(Raise51, t, i, b));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end raise_stmt


    // $ANTLR start import_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:674:1: import_stmt : ( ^( Import ( dotted_as_name[nms] )+ ) | ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import STAR ) ) | ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import ( import_as_name[nms] )+ ) ) );
    public final void import_stmt() throws RecognitionException {
        PythonTree Import52=null;
        PythonTree Name53=null;
        PythonTree Level55=null;
        PythonTree Import57=null;
        PythonTree STAR58=null;
        PythonTree Name59=null;
        PythonTree Level61=null;
        PythonTree Import63=null;
        String dotted_name54 = null;

        int dots56 = 0;

        String dotted_name60 = null;

        int dots62 = 0;



            List nms = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:678:5: ( ^( Import ( dotted_as_name[nms] )+ ) | ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import STAR ) ) | ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import ( import_as_name[nms] )+ ) ) )
            int alt40=3;
            alt40 = dfa40.predict(input);
            switch (alt40) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:678:7: ^( Import ( dotted_as_name[nms] )+ )
                    {
                    Import52=(PythonTree)input.LT(1);
                    match(input,Import,FOLLOW_Import_in_import_stmt1381); 

                    match(input, Token.DOWN, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:678:16: ( dotted_as_name[nms] )+
                    int cnt34=0;
                    loop34:
                    do {
                        int alt34=2;
                        int LA34_0 = input.LA(1);

                        if ( (LA34_0==Alias) ) {
                            alt34=1;
                        }


                        switch (alt34) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:678:16: dotted_as_name[nms]
                    	    {
                    	    pushFollow(FOLLOW_dotted_as_name_in_import_stmt1383);
                    	    dotted_as_name(nms);
                    	    _fsp--;


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt34 >= 1 ) break loop34;
                                EarlyExitException eee =
                                    new EarlyExitException(34, input);
                                throw eee;
                        }
                        cnt34++;
                    } while (true);


                    match(input, Token.UP, null); 

                            aliasType[] n = (aliasType[])nms.toArray(new aliasType[nms.size()]);
                            ((stmts_scope)stmts_stack.peek()).statements.add(new Import(Import52, n));
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:682:7: ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import STAR ) )
                    {
                    match(input,ImportFrom,FOLLOW_ImportFrom_in_import_stmt1397); 

                    match(input, Token.DOWN, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:682:20: ( ^( Level dots ) )?
                    int alt35=2;
                    int LA35_0 = input.LA(1);

                    if ( (LA35_0==Level) ) {
                        alt35=1;
                    }
                    switch (alt35) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:682:21: ^( Level dots )
                            {
                            Level55=(PythonTree)input.LT(1);
                            match(input,Level,FOLLOW_Level_in_import_stmt1401); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_dots_in_import_stmt1403);
                            dots56=dots();
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:682:37: ( ^( Name dotted_name ) )?
                    int alt36=2;
                    int LA36_0 = input.LA(1);

                    if ( (LA36_0==Name) ) {
                        alt36=1;
                    }
                    switch (alt36) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:682:38: ^( Name dotted_name )
                            {
                            Name53=(PythonTree)input.LT(1);
                            match(input,Name,FOLLOW_Name_in_import_stmt1410); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_dotted_name_in_import_stmt1412);
                            dotted_name54=dotted_name();
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    Import57=(PythonTree)input.LT(1);
                    match(input,Import,FOLLOW_Import_in_import_stmt1418); 

                    match(input, Token.DOWN, null); 
                    STAR58=(PythonTree)input.LT(1);
                    match(input,STAR,FOLLOW_STAR_in_import_stmt1420); 

                    match(input, Token.UP, null); 

                    match(input, Token.UP, null); 

                            String name = "";
                            if (Name53 != null) {
                                name = dotted_name54;
                            }
                            int level = 0;
                            if (Level55 != null) {
                                level = dots56;
                            }
                            aliasType[] n = (aliasType[])nms.toArray(new aliasType[nms.size()]);
                            ((stmts_scope)stmts_stack.peek()).statements.add(new ImportFrom(Import57, name, new aliasType[]{new aliasType(STAR58, "*", null)}, level));
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:7: ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import ( import_as_name[nms] )+ ) )
                    {
                    match(input,ImportFrom,FOLLOW_ImportFrom_in_import_stmt1433); 

                    match(input, Token.DOWN, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:20: ( ^( Level dots ) )?
                    int alt37=2;
                    int LA37_0 = input.LA(1);

                    if ( (LA37_0==Level) ) {
                        alt37=1;
                    }
                    switch (alt37) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:21: ^( Level dots )
                            {
                            Level61=(PythonTree)input.LT(1);
                            match(input,Level,FOLLOW_Level_in_import_stmt1437); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_dots_in_import_stmt1439);
                            dots62=dots();
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:37: ( ^( Name dotted_name ) )?
                    int alt38=2;
                    int LA38_0 = input.LA(1);

                    if ( (LA38_0==Name) ) {
                        alt38=1;
                    }
                    switch (alt38) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:38: ^( Name dotted_name )
                            {
                            Name59=(PythonTree)input.LT(1);
                            match(input,Name,FOLLOW_Name_in_import_stmt1446); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_dotted_name_in_import_stmt1448);
                            dotted_name60=dotted_name();
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    Import63=(PythonTree)input.LT(1);
                    match(input,Import,FOLLOW_Import_in_import_stmt1454); 

                    match(input, Token.DOWN, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:69: ( import_as_name[nms] )+
                    int cnt39=0;
                    loop39:
                    do {
                        int alt39=2;
                        int LA39_0 = input.LA(1);

                        if ( (LA39_0==Alias) ) {
                            alt39=1;
                        }


                        switch (alt39) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:694:69: import_as_name[nms]
                    	    {
                    	    pushFollow(FOLLOW_import_as_name_in_import_stmt1456);
                    	    import_as_name(nms);
                    	    _fsp--;


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt39 >= 1 ) break loop39;
                                EarlyExitException eee =
                                    new EarlyExitException(39, input);
                                throw eee;
                        }
                        cnt39++;
                    } while (true);


                    match(input, Token.UP, null); 

                    match(input, Token.UP, null); 

                            String name = "";
                            if (Name59 != null) {
                                name = dotted_name60;
                            }
                            int level = 0;
                            if (Level61 != null) {
                                level = dots62;
                            }
                            aliasType[] n = (aliasType[])nms.toArray(new aliasType[nms.size()]);
                            ((stmts_scope)stmts_stack.peek()).statements.add(new ImportFrom(Import63, name, n, level));
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end import_stmt


    // $ANTLR start dots
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:710:1: dots returns [int level] : ( dot[buf] )+ ;
    public final int dots() throws RecognitionException {
        int level = 0;


            StringBuffer buf = new StringBuffer();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:714:5: ( ( dot[buf] )+ )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:714:7: ( dot[buf] )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:714:7: ( dot[buf] )+
            int cnt41=0;
            loop41:
            do {
                int alt41=2;
                int LA41_0 = input.LA(1);

                if ( (LA41_0==DOT) ) {
                    alt41=1;
                }


                switch (alt41) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:714:7: dot[buf]
            	    {
            	    pushFollow(FOLLOW_dot_in_dots1490);
            	    dot(buf);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt41 >= 1 ) break loop41;
                        EarlyExitException eee =
                            new EarlyExitException(41, input);
                        throw eee;
                }
                cnt41++;
            } while (true);


                    level = buf.length();
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return level;
    }
    // $ANTLR end dots


    // $ANTLR start dot
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:719:1: dot[StringBuffer buf] : DOT ;
    public final void dot(StringBuffer buf) throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:720:5: ( DOT )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:720:7: DOT
            {
            match(input,DOT,FOLLOW_DOT_in_dot1512); 
            buf.append(".");

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end dot


    // $ANTLR start import_as_name
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:723:1: import_as_name[List nms] : ^( Alias name= NAME ( ^( Asname asname= NAME ) )? ) ;
    public final void import_as_name(List nms) throws RecognitionException {
        PythonTree name=null;
        PythonTree asname=null;
        PythonTree Asname64=null;
        PythonTree Alias65=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:724:5: ( ^( Alias name= NAME ( ^( Asname asname= NAME ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:724:7: ^( Alias name= NAME ( ^( Asname asname= NAME ) )? )
            {
            Alias65=(PythonTree)input.LT(1);
            match(input,Alias,FOLLOW_Alias_in_import_as_name1532); 

            match(input, Token.DOWN, null); 
            name=(PythonTree)input.LT(1);
            match(input,NAME,FOLLOW_NAME_in_import_as_name1536); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:724:25: ( ^( Asname asname= NAME ) )?
            int alt42=2;
            int LA42_0 = input.LA(1);

            if ( (LA42_0==Asname) ) {
                alt42=1;
            }
            switch (alt42) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:724:26: ^( Asname asname= NAME )
                    {
                    Asname64=(PythonTree)input.LT(1);
                    match(input,Asname,FOLLOW_Asname_in_import_as_name1540); 

                    match(input, Token.DOWN, null); 
                    asname=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_import_as_name1544); 

                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    String as = null;
                    if (Asname64 != null) {
                        as = asname.getText();
                    }
                    aliasType a = new aliasType(Alias65, name.getText(), as);
                    nms.add(a);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end import_as_name


    // $ANTLR start dotted_as_name
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:734:1: dotted_as_name[List nms] : ^( Alias dotted_name ( ^( Asname NAME ) )? ) ;
    public final void dotted_as_name(List nms) throws RecognitionException {
        PythonTree Asname66=null;
        PythonTree NAME67=null;
        PythonTree Alias68=null;
        String dotted_name69 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:735:5: ( ^( Alias dotted_name ( ^( Asname NAME ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:735:7: ^( Alias dotted_name ( ^( Asname NAME ) )? )
            {
            Alias68=(PythonTree)input.LT(1);
            match(input,Alias,FOLLOW_Alias_in_dotted_as_name1570); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_dotted_name_in_dotted_as_name1572);
            dotted_name69=dotted_name();
            _fsp--;

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:735:27: ( ^( Asname NAME ) )?
            int alt43=2;
            int LA43_0 = input.LA(1);

            if ( (LA43_0==Asname) ) {
                alt43=1;
            }
            switch (alt43) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:735:28: ^( Asname NAME )
                    {
                    Asname66=(PythonTree)input.LT(1);
                    match(input,Asname,FOLLOW_Asname_in_dotted_as_name1576); 

                    match(input, Token.DOWN, null); 
                    NAME67=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_dotted_as_name1578); 

                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    String as = null;
                    if (Asname66 != null) {
                        as = NAME67.getText();
                    }
                    aliasType a = new aliasType(Alias68, dotted_name69, as);
                    nms.add(a);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end dotted_as_name


    // $ANTLR start dotted_name
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:745:1: dotted_name returns [String result] : NAME ( dot_name[buf] )* ;
    public final String dotted_name() throws RecognitionException {
        String result = null;

        PythonTree NAME70=null;


            StringBuffer buf = new StringBuffer();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:749:5: ( NAME ( dot_name[buf] )* )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:749:7: NAME ( dot_name[buf] )*
            {
            NAME70=(PythonTree)input.LT(1);
            match(input,NAME,FOLLOW_NAME_in_dotted_name1610); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:749:12: ( dot_name[buf] )*
            loop44:
            do {
                int alt44=2;
                int LA44_0 = input.LA(1);

                if ( (LA44_0==DOT) ) {
                    alt44=1;
                }


                switch (alt44) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:749:12: dot_name[buf]
            	    {
            	    pushFollow(FOLLOW_dot_name_in_dotted_name1612);
            	    dot_name(buf);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    break loop44;
                }
            } while (true);


                    result = NAME70.getText() + buf.toString();
                    debug("matched dotted_name " + result);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return result;
    }
    // $ANTLR end dotted_name


    // $ANTLR start dot_name
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:755:1: dot_name[StringBuffer buf] : DOT NAME ;
    public final void dot_name(StringBuffer buf) throws RecognitionException {
        PythonTree NAME71=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:756:5: ( DOT NAME )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:756:7: DOT NAME
            {
            match(input,DOT,FOLLOW_DOT_in_dot_name1635); 
            NAME71=(PythonTree)input.LT(1);
            match(input,NAME,FOLLOW_NAME_in_dot_name1637); 

                    buf.append(".");
                    buf.append(NAME71.getText());
                    debug("matched dot_name " + buf);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end dot_name


    // $ANTLR start global_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:763:1: global_stmt : ^( Global ( name_expr[nms] )+ ) ;
    public final void global_stmt() throws RecognitionException {
        PythonTree Global72=null;


            List nms = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:767:5: ( ^( Global ( name_expr[nms] )+ ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:767:7: ^( Global ( name_expr[nms] )+ )
            {
            Global72=(PythonTree)input.LT(1);
            match(input,Global,FOLLOW_Global_in_global_stmt1662); 

            match(input, Token.DOWN, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:767:16: ( name_expr[nms] )+
            int cnt45=0;
            loop45:
            do {
                int alt45=2;
                int LA45_0 = input.LA(1);

                if ( (LA45_0==NAME) ) {
                    alt45=1;
                }


                switch (alt45) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:767:16: name_expr[nms]
            	    {
            	    pushFollow(FOLLOW_name_expr_in_global_stmt1664);
            	    name_expr(nms);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt45 >= 1 ) break loop45;
                        EarlyExitException eee =
                            new EarlyExitException(45, input);
                        throw eee;
                }
                cnt45++;
            } while (true);


            match(input, Token.UP, null); 

                    String[] n = (String[])nms.toArray(new String[nms.size()]);
                    ((stmts_scope)stmts_stack.peek()).statements.add(new Global(Global72, n));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end global_stmt


    // $ANTLR start name_expr
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:773:1: name_expr[List nms] : NAME ;
    public final void name_expr(List nms) throws RecognitionException {
        PythonTree NAME73=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:774:5: ( NAME )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:774:7: NAME
            {
            NAME73=(PythonTree)input.LT(1);
            match(input,NAME,FOLLOW_NAME_in_name_expr1687); 

                    nms.add(NAME73.getText());
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end name_expr


    // $ANTLR start exec_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:779:1: exec_stmt : ^( Exec exec= test[expr_contextType.Load] ( ^( Globals globals= test[expr_contextType.Load] ) )? ( ^( Locals locals= test[expr_contextType.Load] ) )? ) ;
    public final void exec_stmt() throws RecognitionException {
        PythonTree Globals74=null;
        PythonTree Locals75=null;
        PythonTree Exec76=null;
        test_return exec = null;

        test_return globals = null;

        test_return locals = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:780:5: ( ^( Exec exec= test[expr_contextType.Load] ( ^( Globals globals= test[expr_contextType.Load] ) )? ( ^( Locals locals= test[expr_contextType.Load] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:780:7: ^( Exec exec= test[expr_contextType.Load] ( ^( Globals globals= test[expr_contextType.Load] ) )? ( ^( Locals locals= test[expr_contextType.Load] ) )? )
            {
            Exec76=(PythonTree)input.LT(1);
            match(input,Exec,FOLLOW_Exec_in_exec_stmt1707); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_exec_stmt1711);
            exec=test(expr_contextType.Load);
            _fsp--;

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:780:47: ( ^( Globals globals= test[expr_contextType.Load] ) )?
            int alt46=2;
            int LA46_0 = input.LA(1);

            if ( (LA46_0==Globals) ) {
                alt46=1;
            }
            switch (alt46) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:780:48: ^( Globals globals= test[expr_contextType.Load] )
                    {
                    Globals74=(PythonTree)input.LT(1);
                    match(input,Globals,FOLLOW_Globals_in_exec_stmt1716); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_exec_stmt1720);
                    globals=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:780:97: ( ^( Locals locals= test[expr_contextType.Load] ) )?
            int alt47=2;
            int LA47_0 = input.LA(1);

            if ( (LA47_0==Locals) ) {
                alt47=1;
            }
            switch (alt47) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:780:98: ^( Locals locals= test[expr_contextType.Load] )
                    {
                    Locals75=(PythonTree)input.LT(1);
                    match(input,Locals,FOLLOW_Locals_in_exec_stmt1728); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_exec_stmt1732);
                    locals=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    exprType g = null;
                    if (Globals74 != null) {
                        g = globals.etype;
                    }
                    exprType loc = null;
                    if (Locals75 != null) {
                        loc = locals.etype;
                    }
                    ((stmts_scope)stmts_stack.peek()).statements.add(new Exec(Exec76, exec.etype, g, loc));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end exec_stmt


    // $ANTLR start assert_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:793:1: assert_stmt : ^( Assert ^( Test tst= test[expr_contextType.Load] ) ( ^( Msg msg= test[expr_contextType.Load] ) )? ) ;
    public final void assert_stmt() throws RecognitionException {
        PythonTree Msg77=null;
        PythonTree Assert78=null;
        test_return tst = null;

        test_return msg = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:794:5: ( ^( Assert ^( Test tst= test[expr_contextType.Load] ) ( ^( Msg msg= test[expr_contextType.Load] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:794:7: ^( Assert ^( Test tst= test[expr_contextType.Load] ) ( ^( Msg msg= test[expr_contextType.Load] ) )? )
            {
            Assert78=(PythonTree)input.LT(1);
            match(input,Assert,FOLLOW_Assert_in_assert_stmt1757); 

            match(input, Token.DOWN, null); 
            match(input,Test,FOLLOW_Test_in_assert_stmt1760); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_assert_stmt1764);
            tst=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:794:56: ( ^( Msg msg= test[expr_contextType.Load] ) )?
            int alt48=2;
            int LA48_0 = input.LA(1);

            if ( (LA48_0==Msg) ) {
                alt48=1;
            }
            switch (alt48) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:794:57: ^( Msg msg= test[expr_contextType.Load] )
                    {
                    Msg77=(PythonTree)input.LT(1);
                    match(input,Msg,FOLLOW_Msg_in_assert_stmt1770); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_assert_stmt1774);
                    msg=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    exprType m = null;
                    if (Msg77 != null) {
                        m = msg.etype;
                    }
                    ((stmts_scope)stmts_stack.peek()).statements.add(new Assert(Assert78, tst.etype, m));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end assert_stmt


    // $ANTLR start if_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:803:1: if_stmt : ^( If test[expr_contextType.Load] body= stmts ( elif_clause[elifs] )* ( ^( OrElse orelse= stmts ) )? ) ;
    public final void if_stmt() throws RecognitionException {
        PythonTree OrElse79=null;
        PythonTree If80=null;
        stmts_return body = null;

        stmts_return orelse = null;

        test_return test81 = null;



            List elifs = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:808:5: ( ^( If test[expr_contextType.Load] body= stmts ( elif_clause[elifs] )* ( ^( OrElse orelse= stmts ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:808:7: ^( If test[expr_contextType.Load] body= stmts ( elif_clause[elifs] )* ( ^( OrElse orelse= stmts ) )? )
            {
            If80=(PythonTree)input.LT(1);
            match(input,If,FOLLOW_If_in_if_stmt1805); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_if_stmt1807);
            test81=test(expr_contextType.Load);
            _fsp--;

            pushFollow(FOLLOW_stmts_in_if_stmt1812);
            body=stmts();
            _fsp--;

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:808:51: ( elif_clause[elifs] )*
            loop49:
            do {
                int alt49=2;
                int LA49_0 = input.LA(1);

                if ( (LA49_0==Elif) ) {
                    alt49=1;
                }


                switch (alt49) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:808:51: elif_clause[elifs]
            	    {
            	    pushFollow(FOLLOW_elif_clause_in_if_stmt1814);
            	    elif_clause(elifs);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    break loop49;
                }
            } while (true);

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:808:71: ( ^( OrElse orelse= stmts ) )?
            int alt50=2;
            int LA50_0 = input.LA(1);

            if ( (LA50_0==OrElse) ) {
                alt50=1;
            }
            switch (alt50) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:808:72: ^( OrElse orelse= stmts )
                    {
                    OrElse79=(PythonTree)input.LT(1);
                    match(input,OrElse,FOLLOW_OrElse_in_if_stmt1820); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_stmts_in_if_stmt1824);
                    orelse=stmts();
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    stmtType[] o;
                    if (OrElse79 != null) {
                        o = (stmtType[])orelse.stypes.toArray(new stmtType[orelse.stypes.size()]);
                    } else {
                        o = new stmtType[0];
                    }
                    stmtType[] b = (stmtType[])body.stypes.toArray(new stmtType[body.stypes.size()]);
                    ListIterator iter = elifs.listIterator(elifs.size());
                    while (iter.hasPrevious()) {
                        If elif = (If)iter.previous();
                        elif.orelse = o;
                        o = new stmtType[]{elif};
                    }
                    If i = new If(If80, test81.etype, b, o);
                    ((stmts_scope)stmts_stack.peek()).statements.add(i);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end if_stmt


    // $ANTLR start elif_clause
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:827:1: elif_clause[List elifs] : ^( Elif test[expr_contextType.Load] stmts ) ;
    public final void elif_clause(List elifs) throws RecognitionException {
        PythonTree Elif83=null;
        stmts_return stmts82 = null;

        test_return test84 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:828:5: ( ^( Elif test[expr_contextType.Load] stmts ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:828:7: ^( Elif test[expr_contextType.Load] stmts )
            {
            Elif83=(PythonTree)input.LT(1);
            match(input,Elif,FOLLOW_Elif_in_elif_clause1849); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_elif_clause1851);
            test84=test(expr_contextType.Load);
            _fsp--;

            pushFollow(FOLLOW_stmts_in_elif_clause1854);
            stmts82=stmts();
            _fsp--;


            match(input, Token.UP, null); 

                    debug("matched elif");
                    stmtType[] b = (stmtType[])stmts82.stypes.toArray(new stmtType[stmts82.stypes.size()]);
                    //the stmtType[0] is intended to be replaced in the iterator of the if_stmt rule.
                    //there is probably a better way to do this.
                    elifs.add(new If(Elif83, test84.etype, b, new stmtType[0]));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end elif_clause


    // $ANTLR start while_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:837:1: while_stmt : ^( While test[expr_contextType.Load] ^( Body body= stmts ) ( ^( OrElse orelse= stmts ) )? ) ;
    public final void while_stmt() throws RecognitionException {
        PythonTree OrElse85=null;
        PythonTree While86=null;
        stmts_return body = null;

        stmts_return orelse = null;

        test_return test87 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:838:5: ( ^( While test[expr_contextType.Load] ^( Body body= stmts ) ( ^( OrElse orelse= stmts ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:838:7: ^( While test[expr_contextType.Load] ^( Body body= stmts ) ( ^( OrElse orelse= stmts ) )? )
            {
            While86=(PythonTree)input.LT(1);
            match(input,While,FOLLOW_While_in_while_stmt1875); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_while_stmt1877);
            test87=test(expr_contextType.Load);
            _fsp--;

            match(input,Body,FOLLOW_Body_in_while_stmt1881); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_stmts_in_while_stmt1885);
            body=stmts();
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:838:62: ( ^( OrElse orelse= stmts ) )?
            int alt51=2;
            int LA51_0 = input.LA(1);

            if ( (LA51_0==OrElse) ) {
                alt51=1;
            }
            switch (alt51) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:838:63: ^( OrElse orelse= stmts )
                    {
                    OrElse85=(PythonTree)input.LT(1);
                    match(input,OrElse,FOLLOW_OrElse_in_while_stmt1890); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_stmts_in_while_stmt1894);
                    orelse=stmts();
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    List o = null;
                    if (OrElse85 != null) {
                        o = orelse.stypes;
                    }
                    While w = makeWhile(While86, test87.etype, body.stypes, o);
                    ((stmts_scope)stmts_stack.peek()).statements.add(w);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end while_stmt


    // $ANTLR start for_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:848:1: for_stmt : ^( For ^( Target targ= test[expr_contextType.Store] ) ^( Iter iter= test[expr_contextType.Load] ) ^( Body body= stmts ) ( ^( OrElse orelse= stmts ) )? ) ;
    public final void for_stmt() throws RecognitionException {
        PythonTree OrElse88=null;
        PythonTree For89=null;
        test_return targ = null;

        test_return iter = null;

        stmts_return body = null;

        stmts_return orelse = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:849:5: ( ^( For ^( Target targ= test[expr_contextType.Store] ) ^( Iter iter= test[expr_contextType.Load] ) ^( Body body= stmts ) ( ^( OrElse orelse= stmts ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:849:7: ^( For ^( Target targ= test[expr_contextType.Store] ) ^( Iter iter= test[expr_contextType.Load] ) ^( Body body= stmts ) ( ^( OrElse orelse= stmts ) )? )
            {
            For89=(PythonTree)input.LT(1);
            match(input,For,FOLLOW_For_in_for_stmt1918); 

            match(input, Token.DOWN, null); 
            match(input,Target,FOLLOW_Target_in_for_stmt1921); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_for_stmt1925);
            targ=test(expr_contextType.Store);
            _fsp--;


            match(input, Token.UP, null); 
            match(input,Iter,FOLLOW_Iter_in_for_stmt1930); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_for_stmt1934);
            iter=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            match(input,Body,FOLLOW_Body_in_for_stmt1939); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_stmts_in_for_stmt1943);
            body=stmts();
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:849:117: ( ^( OrElse orelse= stmts ) )?
            int alt52=2;
            int LA52_0 = input.LA(1);

            if ( (LA52_0==OrElse) ) {
                alt52=1;
            }
            switch (alt52) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:849:118: ^( OrElse orelse= stmts )
                    {
                    OrElse88=(PythonTree)input.LT(1);
                    match(input,OrElse,FOLLOW_OrElse_in_for_stmt1948); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_stmts_in_for_stmt1952);
                    orelse=stmts();
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    List o = null;
                    if (OrElse88 != null) {
                        o = orelse.stypes;
                    }
                    For f = makeFor(For89, targ.etype, iter.etype, body.stypes, o);
                    ((stmts_scope)stmts_stack.peek()).statements.add(f);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end for_stmt


    // $ANTLR start try_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:859:1: try_stmt : ( ^( TryExcept ^( Body body= stmts ) ( except_clause[handlers] )+ ( ^( OrElse orelse= stmts ) )? ( ^( FinalBody 'finally' fin= stmts ) )? ) | ^( TryFinally ^( Body body= stmts ) ^( FinalBody fin= stmts ) ) );
    public final void try_stmt() throws RecognitionException {
        PythonTree OrElse90=null;
        PythonTree FinalBody91=null;
        PythonTree TryExcept92=null;
        PythonTree TryFinally93=null;
        stmts_return body = null;

        stmts_return orelse = null;

        stmts_return fin = null;



            List handlers = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:5: ( ^( TryExcept ^( Body body= stmts ) ( except_clause[handlers] )+ ( ^( OrElse orelse= stmts ) )? ( ^( FinalBody 'finally' fin= stmts ) )? ) | ^( TryFinally ^( Body body= stmts ) ^( FinalBody fin= stmts ) ) )
            int alt56=2;
            int LA56_0 = input.LA(1);

            if ( (LA56_0==TryExcept) ) {
                alt56=1;
            }
            else if ( (LA56_0==TryFinally) ) {
                alt56=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("859:1: try_stmt : ( ^( TryExcept ^( Body body= stmts ) ( except_clause[handlers] )+ ( ^( OrElse orelse= stmts ) )? ( ^( FinalBody 'finally' fin= stmts ) )? ) | ^( TryFinally ^( Body body= stmts ) ^( FinalBody fin= stmts ) ) );", 56, 0, input);

                throw nvae;
            }
            switch (alt56) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:7: ^( TryExcept ^( Body body= stmts ) ( except_clause[handlers] )+ ( ^( OrElse orelse= stmts ) )? ( ^( FinalBody 'finally' fin= stmts ) )? )
                    {
                    TryExcept92=(PythonTree)input.LT(1);
                    match(input,TryExcept,FOLLOW_TryExcept_in_try_stmt1981); 

                    match(input, Token.DOWN, null); 
                    match(input,Body,FOLLOW_Body_in_try_stmt1984); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_stmts_in_try_stmt1988);
                    body=stmts();
                    _fsp--;


                    match(input, Token.UP, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:38: ( except_clause[handlers] )+
                    int cnt53=0;
                    loop53:
                    do {
                        int alt53=2;
                        int LA53_0 = input.LA(1);

                        if ( (LA53_0==ExceptHandler) ) {
                            alt53=1;
                        }


                        switch (alt53) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:38: except_clause[handlers]
                    	    {
                    	    pushFollow(FOLLOW_except_clause_in_try_stmt1991);
                    	    except_clause(handlers);
                    	    _fsp--;


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt53 >= 1 ) break loop53;
                                EarlyExitException eee =
                                    new EarlyExitException(53, input);
                                throw eee;
                        }
                        cnt53++;
                    } while (true);

                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:63: ( ^( OrElse orelse= stmts ) )?
                    int alt54=2;
                    int LA54_0 = input.LA(1);

                    if ( (LA54_0==OrElse) ) {
                        alt54=1;
                    }
                    switch (alt54) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:64: ^( OrElse orelse= stmts )
                            {
                            OrElse90=(PythonTree)input.LT(1);
                            match(input,OrElse,FOLLOW_OrElse_in_try_stmt1997); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_stmts_in_try_stmt2001);
                            orelse=stmts();
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:89: ( ^( FinalBody 'finally' fin= stmts ) )?
                    int alt55=2;
                    int LA55_0 = input.LA(1);

                    if ( (LA55_0==FinalBody) ) {
                        alt55=1;
                    }
                    switch (alt55) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:863:90: ^( FinalBody 'finally' fin= stmts )
                            {
                            FinalBody91=(PythonTree)input.LT(1);
                            match(input,FinalBody,FOLLOW_FinalBody_in_try_stmt2008); 

                            match(input, Token.DOWN, null); 
                            match(input,190,FOLLOW_190_in_try_stmt2010); 
                            pushFollow(FOLLOW_stmts_in_try_stmt2014);
                            fin=stmts();
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }


                    match(input, Token.UP, null); 

                            List o = null;
                            List f = null;
                            if (OrElse90 != null) {
                                o = orelse.stypes;
                            }
                            if (FinalBody91 != null) {
                                f = fin.stypes;
                            }
                            stmtType te = makeTryExcept(TryExcept92, body.stypes, handlers, o, f);
                            ((stmts_scope)stmts_stack.peek()).statements.add(te);
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:875:7: ^( TryFinally ^( Body body= stmts ) ^( FinalBody fin= stmts ) )
                    {
                    TryFinally93=(PythonTree)input.LT(1);
                    match(input,TryFinally,FOLLOW_TryFinally_in_try_stmt2029); 

                    match(input, Token.DOWN, null); 
                    match(input,Body,FOLLOW_Body_in_try_stmt2032); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_stmts_in_try_stmt2036);
                    body=stmts();
                    _fsp--;


                    match(input, Token.UP, null); 
                    match(input,FinalBody,FOLLOW_FinalBody_in_try_stmt2040); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_stmts_in_try_stmt2044);
                    fin=stmts();
                    _fsp--;


                    match(input, Token.UP, null); 

                    match(input, Token.UP, null); 

                            TryFinally tf = makeTryFinally(TryFinally93, body.stypes, fin.stypes);
                            ((stmts_scope)stmts_stack.peek()).statements.add(tf);
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end try_stmt


    // $ANTLR start except_clause
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:881:1: except_clause[List handlers] : ^( ExceptHandler 'except' ( ^( Type type= test[expr_contextType.Load] ) )? ( ^( Name name= test[expr_contextType.Store] ) )? ^( Body stmts ) ) ;
    public final void except_clause(List handlers) throws RecognitionException {
        PythonTree Type95=null;
        PythonTree Name96=null;
        PythonTree ExceptHandler97=null;
        test_return type = null;

        test_return name = null;

        stmts_return stmts94 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:882:5: ( ^( ExceptHandler 'except' ( ^( Type type= test[expr_contextType.Load] ) )? ( ^( Name name= test[expr_contextType.Store] ) )? ^( Body stmts ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:882:7: ^( ExceptHandler 'except' ( ^( Type type= test[expr_contextType.Load] ) )? ( ^( Name name= test[expr_contextType.Store] ) )? ^( Body stmts ) )
            {
            ExceptHandler97=(PythonTree)input.LT(1);
            match(input,ExceptHandler,FOLLOW_ExceptHandler_in_except_clause2067); 

            match(input, Token.DOWN, null); 
            match(input,192,FOLLOW_192_in_except_clause2069); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:882:32: ( ^( Type type= test[expr_contextType.Load] ) )?
            int alt57=2;
            int LA57_0 = input.LA(1);

            if ( (LA57_0==Type) ) {
                alt57=1;
            }
            switch (alt57) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:882:33: ^( Type type= test[expr_contextType.Load] )
                    {
                    Type95=(PythonTree)input.LT(1);
                    match(input,Type,FOLLOW_Type_in_except_clause2073); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_except_clause2077);
                    type=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:882:76: ( ^( Name name= test[expr_contextType.Store] ) )?
            int alt58=2;
            int LA58_0 = input.LA(1);

            if ( (LA58_0==Name) ) {
                alt58=1;
            }
            switch (alt58) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:882:77: ^( Name name= test[expr_contextType.Store] )
                    {
                    Name96=(PythonTree)input.LT(1);
                    match(input,Name,FOLLOW_Name_in_except_clause2085); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_except_clause2089);
                    name=test(expr_contextType.Store);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }

            match(input,Body,FOLLOW_Body_in_except_clause2096); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_stmts_in_except_clause2098);
            stmts94=stmts();
            _fsp--;


            match(input, Token.UP, null); 

            match(input, Token.UP, null); 

                    stmtType[] b;
                    if (((PythonTree)stmts94.start) != null) {
                        b = (stmtType[])stmts94.stypes.toArray(new stmtType[stmts94.stypes.size()]);
                    } else b = new stmtType[0];
                    exprType t = null;
                    if (Type95 != null) {
                        t = type.etype;
                    }
                    exprType n = null;
                    if (Name96 != null) {
                        n = name.etype;
                    }
                    //XXX: getCharPositionInLine() -7 is only accurate in the simplist cases -- need to
                    //     look harder at CPython to figure out what is really needed here.
                    handlers.add(new excepthandlerType(ExceptHandler97, t, n, b, ExceptHandler97.getLine(), ExceptHandler97.getCharPositionInLine()));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end except_clause


    // $ANTLR start with_stmt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:901:1: with_stmt : ^( With test[expr_contextType.Load] ( with_var )? ^( Body stmts ) ) ;
    public final void with_stmt() throws RecognitionException {
        PythonTree With99=null;
        stmts_return stmts98 = null;

        test_return test100 = null;

        exprType with_var101 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:902:5: ( ^( With test[expr_contextType.Load] ( with_var )? ^( Body stmts ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:902:7: ^( With test[expr_contextType.Load] ( with_var )? ^( Body stmts ) )
            {
            With99=(PythonTree)input.LT(1);
            match(input,With,FOLLOW_With_in_with_stmt2120); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_with_stmt2122);
            test100=test(expr_contextType.Load);
            _fsp--;

            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:902:42: ( with_var )?
            int alt59=2;
            int LA59_0 = input.LA(1);

            if ( (LA59_0==NAME||LA59_0==179) ) {
                alt59=1;
            }
            switch (alt59) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:902:42: with_var
                    {
                    pushFollow(FOLLOW_with_var_in_with_stmt2125);
                    with_var101=with_var();
                    _fsp--;


                    }
                    break;

            }

            match(input,Body,FOLLOW_Body_in_with_stmt2129); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_stmts_in_with_stmt2131);
            stmts98=stmts();
            _fsp--;


            match(input, Token.UP, null); 

            match(input, Token.UP, null); 

                    stmtType[] b = (stmtType[])stmts98.stypes.toArray(new stmtType[stmts98.stypes.size()]);
                    ((stmts_scope)stmts_stack.peek()).statements.add(new With(With99, test100.etype, with_var101, b));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end with_stmt


    // $ANTLR start with_var
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:910:1: with_var returns [exprType etype] : ( 'as' | NAME ) test[expr_contextType.Store] ;
    public final exprType with_var() throws RecognitionException {
        exprType etype = null;

        test_return test102 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:911:5: ( ( 'as' | NAME ) test[expr_contextType.Store] )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:911:7: ( 'as' | NAME ) test[expr_contextType.Store]
            {
            if ( input.LA(1)==NAME||input.LA(1)==179 ) {
                input.consume();
                errorRecovery=false;
            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recoverFromMismatchedSet(input,mse,FOLLOW_set_in_with_var2158);    throw mse;
            }

            pushFollow(FOLLOW_test_in_with_var2166);
            test102=test(expr_contextType.Store);
            _fsp--;


                    etype = test102.etype;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end with_var

    public static class test_return extends TreeRuleReturnScope {
        public exprType etype;
        public boolean parens;
    };

    // $ANTLR start test
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:917:1: test[expr_contextType ctype] returns [exprType etype, boolean parens] : ( ^( AND left= test[ctype] right= test[ctype] ) | ^( OR left= test[ctype] right= test[ctype] ) | ^( comp_op left= test[ctype] targs= test[ctype] ) | atom[ctype] | ^( binop left= test[ctype] right= test[ctype] ) | call_expr | lambdef | ^( IfExp ^( Test t1= test[ctype] ) ^( Body t2= test[ctype] ) ^( OrElse t3= test[ctype] ) ) | yield_expr );
    public final test_return test(expr_contextType ctype) throws RecognitionException {
        test_return retval = new test_return();
        retval.start = input.LT(1);

        PythonTree AND103=null;
        PythonTree OR104=null;
        PythonTree IfExp110=null;
        test_return left = null;

        test_return right = null;

        test_return targs = null;

        test_return t1 = null;

        test_return t2 = null;

        test_return t3 = null;

        comp_op_return comp_op105 = null;

        atom_return atom106 = null;

        operatorType binop107 = null;

        exprType call_expr108 = null;

        exprType lambdef109 = null;

        exprType yield_expr111 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:918:5: ( ^( AND left= test[ctype] right= test[ctype] ) | ^( OR left= test[ctype] right= test[ctype] ) | ^( comp_op left= test[ctype] targs= test[ctype] ) | atom[ctype] | ^( binop left= test[ctype] right= test[ctype] ) | call_expr | lambdef | ^( IfExp ^( Test t1= test[ctype] ) ^( Body t2= test[ctype] ) ^( OrElse t3= test[ctype] ) ) | yield_expr )
            int alt60=9;
            switch ( input.LA(1) ) {
            case AND:
                {
                alt60=1;
                }
                break;
            case OR:
                {
                alt60=2;
                }
                break;
            case IsNot:
            case NotIn:
            case LESS:
            case GREATER:
            case EQUAL:
            case GREATEREQUAL:
            case LESSEQUAL:
            case ALT_NOTEQUAL:
            case NOTEQUAL:
            case 182:
            case 193:
                {
                alt60=3;
                }
                break;
            case Name:
            case Tuple:
            case List:
            case Dict:
            case Str:
            case Num:
            case ListComp:
            case Repr:
            case SubscriptList:
            case UAdd:
            case USub:
            case Invert:
            case GeneratorExp:
            case Parens:
            case DOT:
            case NOT:
                {
                alt60=4;
                }
                break;
            case STAR:
            case DOUBLESTAR:
            case RIGHTSHIFT:
            case VBAR:
            case CIRCUMFLEX:
            case AMPER:
            case LEFTSHIFT:
            case PLUS:
            case MINUS:
            case SLASH:
            case PERCENT:
            case DOUBLESLASH:
                {
                alt60=5;
                }
                break;
            case Call:
                {
                alt60=6;
                }
                break;
            case Lambda:
                {
                alt60=7;
                }
                break;
            case IfExp:
                {
                alt60=8;
                }
                break;
            case Yield:
                {
                alt60=9;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("917:1: test[expr_contextType ctype] returns [exprType etype, boolean parens] : ( ^( AND left= test[ctype] right= test[ctype] ) | ^( OR left= test[ctype] right= test[ctype] ) | ^( comp_op left= test[ctype] targs= test[ctype] ) | atom[ctype] | ^( binop left= test[ctype] right= test[ctype] ) | call_expr | lambdef | ^( IfExp ^( Test t1= test[ctype] ) ^( Body t2= test[ctype] ) ^( OrElse t3= test[ctype] ) ) | yield_expr );", 60, 0, input);

                throw nvae;
            }

            switch (alt60) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:918:7: ^( AND left= test[ctype] right= test[ctype] )
                    {
                    AND103=(PythonTree)input.LT(1);
                    match(input,AND,FOLLOW_AND_in_test2193); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2197);
                    left=test(ctype);
                    _fsp--;

                    pushFollow(FOLLOW_test_in_test2202);
                    right=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            List values = new ArrayList();
                            boolean leftIsAnd = false;
                            boolean rightIsAnd = false;
                            BoolOp leftB = null;
                            BoolOp rightB = null;
                            if (! left.parens && ((PythonTree)left.start).getType() == AND) {
                                leftIsAnd = true;
                                leftB = (BoolOp)left.etype;
                            }
                            if (! right.parens && ((PythonTree)right.start).getType() == AND) {
                                rightIsAnd = true;
                                rightB = (BoolOp)right.etype;
                            }
                            exprType[] e;
                            if (leftIsAnd && rightIsAnd) {
                                debug("matched And + L + R");
                                int lenL = leftB.values.length;
                                int lenR = rightB.values.length;
                                e = new exprType[lenL + lenR];
                                System.arraycopy(leftB.values, 0, e, 0, lenL - 1);
                                System.arraycopy(rightB.values, 0, e, lenL - 1, lenL + lenR);
                            } else if (leftIsAnd) {
                                debug("matched And + L");
                                e = new exprType[leftB.values.length + 1];
                                System.arraycopy(leftB.values, 0, e, 0, leftB.values.length);
                                e[e.length - 1] = right.etype;
                            } else if (rightIsAnd) {
                                debug("matched And + R");
                                e = new exprType[rightB.values.length + 1];
                                System.arraycopy(rightB.values, 0, e, 0, rightB.values.length);
                                e[e.length - 1] = left.etype;
                            } else {
                                debug("matched And");
                                e = new exprType[2];
                                e[0] = left.etype;
                                e[1] = right.etype;
                            }
                            //XXX: could re-use BoolOps discarded above in many cases.
                            retval.etype = new BoolOp(AND103, boolopType.And, e);
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:959:7: ^( OR left= test[ctype] right= test[ctype] )
                    {
                    OR104=(PythonTree)input.LT(1);
                    match(input,OR,FOLLOW_OR_in_test2215); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2219);
                    left=test(ctype);
                    _fsp--;

                    pushFollow(FOLLOW_test_in_test2224);
                    right=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            //XXX: AND and OR could be factored into one method.
                            List values = new ArrayList();
                            boolean leftIsOr = false;
                            boolean rightIsOr = false;
                            BoolOp leftB = null;
                            BoolOp rightB = null;
                            if (((PythonTree)left.start).getType() == OR) {
                                leftIsOr = true;
                                leftB = (BoolOp)left.etype;
                            }
                            if (((PythonTree)right.start).getType() == OR) {
                                rightIsOr = true;
                                rightB = (BoolOp)right.etype;
                            }
                            exprType[] e;
                            if (leftIsOr && rightIsOr) {
                                debug("matched Or + L + R");
                                int lenL = leftB.values.length;
                                int lenR = rightB.values.length;
                                e = new exprType[lenL + lenR];
                                System.arraycopy(leftB.values, 0, e, 0, lenL - 1);
                                System.arraycopy(rightB.values, 0, e, lenL - 1, lenL + lenR);
                            } else if (leftIsOr) {
                                debug("matched Or + L");
                                e = new exprType[leftB.values.length + 1];
                                System.arraycopy(leftB.values, 0, e, 0, leftB.values.length);
                                e[e.length - 1] = right.etype;
                            } else if (rightIsOr) {
                                debug("matched Or + R");
                                e = new exprType[rightB.values.length + 1];
                                System.arraycopy(rightB.values, 0, e, 0, rightB.values.length);
                                e[e.length - 1] = left.etype;
                            } else {
                                debug("matched Or");
                                e = new exprType[2];
                                e[0] = left.etype;
                                e[1] = right.etype;
                            }
                            //XXX: could re-use BoolOps discarded above in many cases.
                            retval.etype = new BoolOp(OR104, boolopType.Or, e);
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1001:7: ^( comp_op left= test[ctype] targs= test[ctype] )
                    {
                    pushFollow(FOLLOW_comp_op_in_test2237);
                    comp_op105=comp_op();
                    _fsp--;


                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2241);
                    left=test(ctype);
                    _fsp--;

                    pushFollow(FOLLOW_test_in_test2246);
                    targs=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            exprType[] comparators;
                            cmpopType[] ops;
                            exprType val;
                            //XXX: does right need to be checked for Compare?
                            if (! left.parens && left.etype instanceof Compare) {
                                Compare c = (Compare)left.etype;
                                comparators = new exprType[c.comparators.length + 1];
                                ops = new cmpopType[c.ops.length + 1];
                                System.arraycopy(c.ops, 0, ops, 0, c.ops.length);
                                System.arraycopy(c.comparators, 0, comparators, 0, c.comparators.length);
                                comparators[c.comparators.length] = targs.etype;
                                ops[c.ops.length] = comp_op105.op;
                                val = c.left;
                            } else {
                                comparators = new exprType[1];
                                ops = new cmpopType[1];
                                ops[0] = comp_op105.op;
                                comparators[0] = targs.etype;
                                val = left.etype;
                            }
                            retval.etype = new Compare(((PythonTree)comp_op105.start), val, ops, comparators);
                            debug("COMP_OP: " + ((PythonTree)comp_op105.start) + ":::" + retval.etype + ":::" + retval.parens);
                        

                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1025:7: atom[ctype]
                    {
                    pushFollow(FOLLOW_atom_in_test2258);
                    atom106=atom(ctype);
                    _fsp--;


                            debug("matched atom");
                            debug("***" + atom106.etype);
                            retval.parens = atom106.parens;
                            retval.etype = atom106.etype;
                        

                    }
                    break;
                case 5 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1031:7: ^( binop left= test[ctype] right= test[ctype] )
                    {
                    pushFollow(FOLLOW_binop_in_test2270);
                    binop107=binop();
                    _fsp--;


                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2274);
                    left=test(ctype);
                    _fsp--;

                    pushFollow(FOLLOW_test_in_test2279);
                    right=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            debug("BinOp matched");
                            retval.etype = new BinOp(((PythonTree)left.start), left.etype, binop107, right.etype);
                        

                    }
                    break;
                case 6 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1035:7: call_expr
                    {
                    pushFollow(FOLLOW_call_expr_in_test2291);
                    call_expr108=call_expr();
                    _fsp--;


                            retval.etype = call_expr108;
                        

                    }
                    break;
                case 7 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1038:7: lambdef
                    {
                    pushFollow(FOLLOW_lambdef_in_test2301);
                    lambdef109=lambdef();
                    _fsp--;


                            retval.etype = lambdef109;
                        

                    }
                    break;
                case 8 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1041:7: ^( IfExp ^( Test t1= test[ctype] ) ^( Body t2= test[ctype] ) ^( OrElse t3= test[ctype] ) )
                    {
                    IfExp110=(PythonTree)input.LT(1);
                    match(input,IfExp,FOLLOW_IfExp_in_test2312); 

                    match(input, Token.DOWN, null); 
                    match(input,Test,FOLLOW_Test_in_test2315); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2319);
                    t1=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 
                    match(input,Body,FOLLOW_Body_in_test2324); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2328);
                    t2=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 
                    match(input,OrElse,FOLLOW_OrElse_in_test2333); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_test2337);
                    t3=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                    match(input, Token.UP, null); 

                            retval.etype = new IfExp(IfExp110, t1.etype, t2.etype, t3.etype);
                        

                    }
                    break;
                case 9 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1044:7: yield_expr
                    {
                    pushFollow(FOLLOW_yield_expr_in_test2350);
                    yield_expr111=yield_expr();
                    _fsp--;


                            retval.etype = yield_expr111;
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end test

    public static class comp_op_return extends TreeRuleReturnScope {
        public cmpopType op;
    };

    // $ANTLR start comp_op
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1049:1: comp_op returns [cmpopType op] : ( LESS | GREATER | EQUAL | GREATEREQUAL | LESSEQUAL | ALT_NOTEQUAL | NOTEQUAL | 'in' | NotIn | 'is' | IsNot );
    public final comp_op_return comp_op() throws RecognitionException {
        comp_op_return retval = new comp_op_return();
        retval.start = input.LT(1);

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1050:5: ( LESS | GREATER | EQUAL | GREATEREQUAL | LESSEQUAL | ALT_NOTEQUAL | NOTEQUAL | 'in' | NotIn | 'is' | IsNot )
            int alt61=11;
            switch ( input.LA(1) ) {
            case LESS:
                {
                alt61=1;
                }
                break;
            case GREATER:
                {
                alt61=2;
                }
                break;
            case EQUAL:
                {
                alt61=3;
                }
                break;
            case GREATEREQUAL:
                {
                alt61=4;
                }
                break;
            case LESSEQUAL:
                {
                alt61=5;
                }
                break;
            case ALT_NOTEQUAL:
                {
                alt61=6;
                }
                break;
            case NOTEQUAL:
                {
                alt61=7;
                }
                break;
            case 182:
                {
                alt61=8;
                }
                break;
            case NotIn:
                {
                alt61=9;
                }
                break;
            case 193:
                {
                alt61=10;
                }
                break;
            case IsNot:
                {
                alt61=11;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1049:1: comp_op returns [cmpopType op] : ( LESS | GREATER | EQUAL | GREATEREQUAL | LESSEQUAL | ALT_NOTEQUAL | NOTEQUAL | 'in' | NotIn | 'is' | IsNot );", 61, 0, input);

                throw nvae;
            }

            switch (alt61) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1050:7: LESS
                    {
                    match(input,LESS,FOLLOW_LESS_in_comp_op2373); 
                    retval.op = cmpopType.Lt;

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1051:7: GREATER
                    {
                    match(input,GREATER,FOLLOW_GREATER_in_comp_op2383); 
                    retval.op = cmpopType.Gt;

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1052:7: EQUAL
                    {
                    match(input,EQUAL,FOLLOW_EQUAL_in_comp_op2393); 
                    retval.op = cmpopType.Eq;

                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1053:7: GREATEREQUAL
                    {
                    match(input,GREATEREQUAL,FOLLOW_GREATEREQUAL_in_comp_op2403); 
                    retval.op = cmpopType.GtE;

                    }
                    break;
                case 5 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1054:7: LESSEQUAL
                    {
                    match(input,LESSEQUAL,FOLLOW_LESSEQUAL_in_comp_op2413); 
                    retval.op = cmpopType.LtE;

                    }
                    break;
                case 6 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1055:7: ALT_NOTEQUAL
                    {
                    match(input,ALT_NOTEQUAL,FOLLOW_ALT_NOTEQUAL_in_comp_op2423); 
                    retval.op = cmpopType.NotEq;

                    }
                    break;
                case 7 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1056:7: NOTEQUAL
                    {
                    match(input,NOTEQUAL,FOLLOW_NOTEQUAL_in_comp_op2433); 
                    retval.op = cmpopType.NotEq;

                    }
                    break;
                case 8 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1057:7: 'in'
                    {
                    match(input,182,FOLLOW_182_in_comp_op2443); 
                    retval.op = cmpopType.In;

                    }
                    break;
                case 9 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1058:7: NotIn
                    {
                    match(input,NotIn,FOLLOW_NotIn_in_comp_op2453); 
                    retval.op = cmpopType.NotIn;

                    }
                    break;
                case 10 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1059:7: 'is'
                    {
                    match(input,193,FOLLOW_193_in_comp_op2463); 
                    retval.op = cmpopType.Is;

                    }
                    break;
                case 11 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1060:7: IsNot
                    {
                    match(input,IsNot,FOLLOW_IsNot_in_comp_op2473); 
                    retval.op = cmpopType.IsNot;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end comp_op

    protected static class elts_scope {
        List elements;
    }
    protected Stack elts_stack = new Stack();


    // $ANTLR start elts
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1065:1: elts[expr_contextType ctype] returns [List etypes] : ( elt[ctype] )+ ;
    public final List elts(expr_contextType ctype) throws RecognitionException {
        elts_stack.push(new elts_scope());
        List etypes = null;


            ((elts_scope)elts_stack.peek()).elements = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1073:5: ( ( elt[ctype] )+ )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1073:7: ( elt[ctype] )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1073:7: ( elt[ctype] )+
            int cnt62=0;
            loop62:
            do {
                int alt62=2;
                int LA62_0 = input.LA(1);

                if ( (LA62_0==Name||(LA62_0>=Tuple && LA62_0<=Dict)||LA62_0==IfExp||(LA62_0>=Yield && LA62_0<=IsNot)||LA62_0==NotIn||(LA62_0>=ListComp && LA62_0<=Repr)||LA62_0==SubscriptList||(LA62_0>=UAdd && LA62_0<=Invert)||LA62_0==GeneratorExp||LA62_0==Call||LA62_0==Parens||LA62_0==DOT||(LA62_0>=STAR && LA62_0<=DOUBLESTAR)||(LA62_0>=RIGHTSHIFT && LA62_0<=DOUBLESLASH)||LA62_0==182||LA62_0==193) ) {
                    alt62=1;
                }


                switch (alt62) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1073:7: elt[ctype]
            	    {
            	    pushFollow(FOLLOW_elt_in_elts2509);
            	    elt(ctype);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt62 >= 1 ) break loop62;
                        EarlyExitException eee =
                            new EarlyExitException(62, input);
                        throw eee;
                }
                cnt62++;
            } while (true);


                    etypes = ((elts_scope)elts_stack.peek()).elements;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
            elts_stack.pop();
        }
        return etypes;
    }
    // $ANTLR end elts


    // $ANTLR start elt
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1078:1: elt[expr_contextType ctype] : test[ctype] ;
    public final void elt(expr_contextType ctype) throws RecognitionException {
        test_return test112 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1079:5: ( test[ctype] )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1079:7: test[ctype]
            {
            pushFollow(FOLLOW_test_in_elt2531);
            test112=test(ctype);
            _fsp--;


                    ((elts_scope)elts_stack.peek()).elements.add(test112.etype);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end elt

    public static class atom_return extends TreeRuleReturnScope {
        public exprType etype;
        public boolean parens;
    };

    // $ANTLR start atom
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1085:1: atom[expr_contextType ctype] returns [exprType etype, boolean parens] : ( ^( Tuple ( ^( Elts elts[ctype] ) )? ) | ^( List ( ^( Elts elts[ctype] ) )? ) | comprehension[ctype] | ^( Dict ( ^( Elts elts[ctype] ) )? ) | ^( Repr ( test[ctype] )* ) | ^( Name NAME ) | ^( DOT NAME test[expr_contextType.Load] ) | ^( SubscriptList subscriptlist test[expr_contextType.Load] ) | ^( Num INT ) | ^( Num LONGINT ) | ^( Num FLOAT ) | ^( Num COMPLEX ) | stringlist | ^( USub test[ctype] ) | ^( UAdd test[ctype] ) | ^( Invert test[ctype] ) | ^( NOT test[ctype] ) | ^( Parens test[ctype] ) );
    public final atom_return atom(expr_contextType ctype) throws RecognitionException {
        atom_return retval = new atom_return();
        retval.start = input.LT(1);

        PythonTree Elts113=null;
        PythonTree Tuple115=null;
        PythonTree Elts116=null;
        PythonTree List118=null;
        PythonTree Elts120=null;
        PythonTree Dict122=null;
        PythonTree Repr123=null;
        PythonTree NAME125=null;
        PythonTree NAME127=null;
        PythonTree DOT128=null;
        PythonTree SubscriptList130=null;
        PythonTree INT132=null;
        PythonTree LONGINT133=null;
        PythonTree FLOAT134=null;
        PythonTree COMPLEX135=null;
        PythonTree USub138=null;
        PythonTree UAdd139=null;
        PythonTree Invert141=null;
        PythonTree NOT143=null;
        List elts114 = null;

        List elts117 = null;

        exprType comprehension119 = null;

        List elts121 = null;

        test_return test124 = null;

        test_return test126 = null;

        List subscriptlist129 = null;

        test_return test131 = null;

        stringlist_return stringlist136 = null;

        test_return test137 = null;

        test_return test140 = null;

        test_return test142 = null;

        test_return test144 = null;

        test_return test145 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1086:5: ( ^( Tuple ( ^( Elts elts[ctype] ) )? ) | ^( List ( ^( Elts elts[ctype] ) )? ) | comprehension[ctype] | ^( Dict ( ^( Elts elts[ctype] ) )? ) | ^( Repr ( test[ctype] )* ) | ^( Name NAME ) | ^( DOT NAME test[expr_contextType.Load] ) | ^( SubscriptList subscriptlist test[expr_contextType.Load] ) | ^( Num INT ) | ^( Num LONGINT ) | ^( Num FLOAT ) | ^( Num COMPLEX ) | stringlist | ^( USub test[ctype] ) | ^( UAdd test[ctype] ) | ^( Invert test[ctype] ) | ^( NOT test[ctype] ) | ^( Parens test[ctype] ) )
            int alt67=18;
            switch ( input.LA(1) ) {
            case Tuple:
                {
                alt67=1;
                }
                break;
            case List:
                {
                alt67=2;
                }
                break;
            case ListComp:
            case GeneratorExp:
                {
                alt67=3;
                }
                break;
            case Dict:
                {
                alt67=4;
                }
                break;
            case Repr:
                {
                alt67=5;
                }
                break;
            case Name:
                {
                alt67=6;
                }
                break;
            case DOT:
                {
                alt67=7;
                }
                break;
            case SubscriptList:
                {
                alt67=8;
                }
                break;
            case Num:
                {
                int LA67_9 = input.LA(2);

                if ( (LA67_9==DOWN) ) {
                    switch ( input.LA(3) ) {
                    case INT:
                        {
                        alt67=9;
                        }
                        break;
                    case LONGINT:
                        {
                        alt67=10;
                        }
                        break;
                    case FLOAT:
                        {
                        alt67=11;
                        }
                        break;
                    case COMPLEX:
                        {
                        alt67=12;
                        }
                        break;
                    default:
                        NoViableAltException nvae =
                            new NoViableAltException("1085:1: atom[expr_contextType ctype] returns [exprType etype, boolean parens] : ( ^( Tuple ( ^( Elts elts[ctype] ) )? ) | ^( List ( ^( Elts elts[ctype] ) )? ) | comprehension[ctype] | ^( Dict ( ^( Elts elts[ctype] ) )? ) | ^( Repr ( test[ctype] )* ) | ^( Name NAME ) | ^( DOT NAME test[expr_contextType.Load] ) | ^( SubscriptList subscriptlist test[expr_contextType.Load] ) | ^( Num INT ) | ^( Num LONGINT ) | ^( Num FLOAT ) | ^( Num COMPLEX ) | stringlist | ^( USub test[ctype] ) | ^( UAdd test[ctype] ) | ^( Invert test[ctype] ) | ^( NOT test[ctype] ) | ^( Parens test[ctype] ) );", 67, 16, input);

                        throw nvae;
                    }

                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("1085:1: atom[expr_contextType ctype] returns [exprType etype, boolean parens] : ( ^( Tuple ( ^( Elts elts[ctype] ) )? ) | ^( List ( ^( Elts elts[ctype] ) )? ) | comprehension[ctype] | ^( Dict ( ^( Elts elts[ctype] ) )? ) | ^( Repr ( test[ctype] )* ) | ^( Name NAME ) | ^( DOT NAME test[expr_contextType.Load] ) | ^( SubscriptList subscriptlist test[expr_contextType.Load] ) | ^( Num INT ) | ^( Num LONGINT ) | ^( Num FLOAT ) | ^( Num COMPLEX ) | stringlist | ^( USub test[ctype] ) | ^( UAdd test[ctype] ) | ^( Invert test[ctype] ) | ^( NOT test[ctype] ) | ^( Parens test[ctype] ) );", 67, 9, input);

                    throw nvae;
                }
                }
                break;
            case Str:
                {
                alt67=13;
                }
                break;
            case USub:
                {
                alt67=14;
                }
                break;
            case UAdd:
                {
                alt67=15;
                }
                break;
            case Invert:
                {
                alt67=16;
                }
                break;
            case NOT:
                {
                alt67=17;
                }
                break;
            case Parens:
                {
                alt67=18;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1085:1: atom[expr_contextType ctype] returns [exprType etype, boolean parens] : ( ^( Tuple ( ^( Elts elts[ctype] ) )? ) | ^( List ( ^( Elts elts[ctype] ) )? ) | comprehension[ctype] | ^( Dict ( ^( Elts elts[ctype] ) )? ) | ^( Repr ( test[ctype] )* ) | ^( Name NAME ) | ^( DOT NAME test[expr_contextType.Load] ) | ^( SubscriptList subscriptlist test[expr_contextType.Load] ) | ^( Num INT ) | ^( Num LONGINT ) | ^( Num FLOAT ) | ^( Num COMPLEX ) | stringlist | ^( USub test[ctype] ) | ^( UAdd test[ctype] ) | ^( Invert test[ctype] ) | ^( NOT test[ctype] ) | ^( Parens test[ctype] ) );", 67, 0, input);

                throw nvae;
            }

            switch (alt67) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1086:7: ^( Tuple ( ^( Elts elts[ctype] ) )? )
                    {
                    Tuple115=(PythonTree)input.LT(1);
                    match(input,Tuple,FOLLOW_Tuple_in_atom2558); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1086:15: ( ^( Elts elts[ctype] ) )?
                        int alt63=2;
                        int LA63_0 = input.LA(1);

                        if ( (LA63_0==Elts) ) {
                            alt63=1;
                        }
                        switch (alt63) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1086:16: ^( Elts elts[ctype] )
                                {
                                Elts113=(PythonTree)input.LT(1);
                                match(input,Elts,FOLLOW_Elts_in_atom2562); 

                                match(input, Token.DOWN, null); 
                                pushFollow(FOLLOW_elts_in_atom2564);
                                elts114=elts(ctype);
                                _fsp--;


                                match(input, Token.UP, null); 

                                }
                                break;

                        }


                        match(input, Token.UP, null); 
                    }

                            debug("matched Tuple");
                            exprType[] e;
                            if (Elts113 != null) {
                                e = (exprType[])elts114.toArray(new exprType[elts114.size()]);
                            } else {
                                e = new exprType[0];
                            }
                            retval.etype = new Tuple(Tuple115, e, ctype);
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1096:7: ^( List ( ^( Elts elts[ctype] ) )? )
                    {
                    List118=(PythonTree)input.LT(1);
                    match(input,List,FOLLOW_List_in_atom2580); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1096:14: ( ^( Elts elts[ctype] ) )?
                        int alt64=2;
                        int LA64_0 = input.LA(1);

                        if ( (LA64_0==Elts) ) {
                            alt64=1;
                        }
                        switch (alt64) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1096:15: ^( Elts elts[ctype] )
                                {
                                Elts116=(PythonTree)input.LT(1);
                                match(input,Elts,FOLLOW_Elts_in_atom2584); 

                                match(input, Token.DOWN, null); 
                                pushFollow(FOLLOW_elts_in_atom2586);
                                elts117=elts(ctype);
                                _fsp--;


                                match(input, Token.UP, null); 

                                }
                                break;

                        }


                        match(input, Token.UP, null); 
                    }

                            debug("matched List");
                            exprType[] e;
                            if (Elts116 != null) {
                                e = (exprType[])elts117.toArray(new exprType[elts117.size()]);
                            } else {
                                e = new exprType[0];
                            }
                            retval.etype = new org.python.antlr.ast.List(List118, e, ctype);
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1106:7: comprehension[ctype]
                    {
                    pushFollow(FOLLOW_comprehension_in_atom2601);
                    comprehension119=comprehension(ctype);
                    _fsp--;

                    retval.etype = comprehension119;

                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1107:7: ^( Dict ( ^( Elts elts[ctype] ) )? )
                    {
                    Dict122=(PythonTree)input.LT(1);
                    match(input,Dict,FOLLOW_Dict_in_atom2613); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1107:14: ( ^( Elts elts[ctype] ) )?
                        int alt65=2;
                        int LA65_0 = input.LA(1);

                        if ( (LA65_0==Elts) ) {
                            alt65=1;
                        }
                        switch (alt65) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1107:15: ^( Elts elts[ctype] )
                                {
                                Elts120=(PythonTree)input.LT(1);
                                match(input,Elts,FOLLOW_Elts_in_atom2617); 

                                match(input, Token.DOWN, null); 
                                pushFollow(FOLLOW_elts_in_atom2619);
                                elts121=elts(ctype);
                                _fsp--;


                                match(input, Token.UP, null); 

                                }
                                break;

                        }


                        match(input, Token.UP, null); 
                    }

                            exprType[] keys;
                            exprType[] values;
                            if (Elts120 != null) {
                                int size = elts121.size() / 2;
                                keys = new exprType[size];
                                values = new exprType[size];
                                for(int i=0;i<size;i++) {
                                    keys[i] = (exprType)elts121.get(i*2);
                                    values[i] = (exprType)elts121.get(i*2+1);
                                }
                            } else {
                                keys = new exprType[0];
                                values = new exprType[0];
                            }
                            retval.etype = new Dict(Dict122, keys, values);
                     
                        

                    }
                    break;
                case 5 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1125:7: ^( Repr ( test[ctype] )* )
                    {
                    Repr123=(PythonTree)input.LT(1);
                    match(input,Repr,FOLLOW_Repr_in_atom2635); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1125:14: ( test[ctype] )*
                        loop66:
                        do {
                            int alt66=2;
                            int LA66_0 = input.LA(1);

                            if ( (LA66_0==Name||(LA66_0>=Tuple && LA66_0<=Dict)||LA66_0==IfExp||(LA66_0>=Yield && LA66_0<=IsNot)||LA66_0==NotIn||(LA66_0>=ListComp && LA66_0<=Repr)||LA66_0==SubscriptList||(LA66_0>=UAdd && LA66_0<=Invert)||LA66_0==GeneratorExp||LA66_0==Call||LA66_0==Parens||LA66_0==DOT||(LA66_0>=STAR && LA66_0<=DOUBLESTAR)||(LA66_0>=RIGHTSHIFT && LA66_0<=DOUBLESLASH)||LA66_0==182||LA66_0==193) ) {
                                alt66=1;
                            }


                            switch (alt66) {
                        	case 1 :
                        	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1125:14: test[ctype]
                        	    {
                        	    pushFollow(FOLLOW_test_in_atom2637);
                        	    test124=test(ctype);
                        	    _fsp--;


                        	    }
                        	    break;

                        	default :
                        	    break loop66;
                            }
                        } while (true);


                        match(input, Token.UP, null); 
                    }

                            retval.etype = new Repr(Repr123, test124.etype);
                        

                    }
                    break;
                case 6 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1128:7: ^( Name NAME )
                    {
                    match(input,Name,FOLLOW_Name_in_atom2651); 

                    match(input, Token.DOWN, null); 
                    NAME125=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_atom2653); 

                    match(input, Token.UP, null); 

                            debug("matched Name " + NAME125.getText());
                            retval.etype = new Name(NAME125, NAME125.getText(), ctype);
                        

                    }
                    break;
                case 7 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1132:7: ^( DOT NAME test[expr_contextType.Load] )
                    {
                    DOT128=(PythonTree)input.LT(1);
                    match(input,DOT,FOLLOW_DOT_in_atom2665); 

                    match(input, Token.DOWN, null); 
                    NAME127=(PythonTree)input.LT(1);
                    match(input,NAME,FOLLOW_NAME_in_atom2667); 
                    pushFollow(FOLLOW_test_in_atom2669);
                    test126=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                            debug("matched DOT in atom: " + test126.etype + "###" + NAME127.getText());
                            retval.etype = new Attribute(DOT128, test126.etype, NAME127.getText(), ctype);
                        

                    }
                    break;
                case 8 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1136:7: ^( SubscriptList subscriptlist test[expr_contextType.Load] )
                    {
                    SubscriptList130=(PythonTree)input.LT(1);
                    match(input,SubscriptList,FOLLOW_SubscriptList_in_atom2682); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_subscriptlist_in_atom2684);
                    subscriptlist129=subscriptlist();
                    _fsp--;

                    pushFollow(FOLLOW_test_in_atom2686);
                    test131=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                            //XXX: only handling one subscript for now.
                            sliceType s;
                            List sltypes = subscriptlist129;
                            if (sltypes.size() == 0) {
                                s = null;
                            } else if (sltypes.size() == 1){
                                s = (sliceType)sltypes.get(0);
                            } else {
                                sliceType[] st;
                                //FIXME: here I am using ClassCastException to decide if sltypes is populated with Index
                                //       only.  Clearly this is not the best way to do this but it's late. Somebody do
                                //       something better please :) -- (hopefully a note to self)
                                try {
                                    Iterator iter = sltypes.iterator();
                                    List etypes = new ArrayList();
                                    while (iter.hasNext()) {
                                        Index i = (Index)iter.next();
                                        etypes.add(i.value);
                                    }
                                    exprType[] es = (exprType[])etypes.toArray(new exprType[etypes.size()]);
                                    exprType t = new Tuple(SubscriptList130, es, expr_contextType.Load);
                                    s = new Index(SubscriptList130, t);
                                } catch (ClassCastException cc) {
                                    st = (sliceType[])sltypes.toArray(new sliceType[sltypes.size()]);
                                    s = new ExtSlice(SubscriptList130, st);
                                }
                            }
                            retval.etype = new Subscript(SubscriptList130, test131.etype, s, ctype);
                        

                    }
                    break;
                case 9 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1166:7: ^( Num INT )
                    {
                    match(input,Num,FOLLOW_Num_in_atom2699); 

                    match(input, Token.DOWN, null); 
                    INT132=(PythonTree)input.LT(1);
                    match(input,INT,FOLLOW_INT_in_atom2701); 

                    match(input, Token.UP, null); 

                            retval.etype = makeInt(INT132);
                            debug("makeInt output: " + retval.etype);
                        

                    }
                    break;
                case 10 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1170:7: ^( Num LONGINT )
                    {
                    match(input,Num,FOLLOW_Num_in_atom2713); 

                    match(input, Token.DOWN, null); 
                    LONGINT133=(PythonTree)input.LT(1);
                    match(input,LONGINT,FOLLOW_LONGINT_in_atom2715); 

                    match(input, Token.UP, null); 
                    retval.etype = makeInt(LONGINT133);

                    }
                    break;
                case 11 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1171:7: ^( Num FLOAT )
                    {
                    match(input,Num,FOLLOW_Num_in_atom2727); 

                    match(input, Token.DOWN, null); 
                    FLOAT134=(PythonTree)input.LT(1);
                    match(input,FLOAT,FOLLOW_FLOAT_in_atom2729); 

                    match(input, Token.UP, null); 

                            retval.etype = makeFloat(FLOAT134);
                            debug("float matched" + retval.etype);
                        

                    }
                    break;
                case 12 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1175:7: ^( Num COMPLEX )
                    {
                    match(input,Num,FOLLOW_Num_in_atom2741); 

                    match(input, Token.DOWN, null); 
                    COMPLEX135=(PythonTree)input.LT(1);
                    match(input,COMPLEX,FOLLOW_COMPLEX_in_atom2743); 

                    match(input, Token.UP, null); 
                    retval.etype = makeComplex(COMPLEX135);

                    }
                    break;
                case 13 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1176:7: stringlist
                    {
                    pushFollow(FOLLOW_stringlist_in_atom2754);
                    stringlist136=stringlist();
                    _fsp--;


                            StringPair sp = extractStrings(stringlist136.strings);
                            if (sp.isUnicode()) {
                                retval.etype = new Unicode(((PythonTree)stringlist136.start), sp.getString());
                            } else {
                                retval.etype = new Str(((PythonTree)stringlist136.start), sp.getString());
                            }
                        

                    }
                    break;
                case 14 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1184:7: ^( USub test[ctype] )
                    {
                    USub138=(PythonTree)input.LT(1);
                    match(input,USub,FOLLOW_USub_in_atom2765); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_atom2767);
                    test137=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            debug("USub matched " + test137.etype);
                            retval.etype = negate(USub138, test137.etype);
                        

                    }
                    break;
                case 15 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1188:7: ^( UAdd test[ctype] )
                    {
                    UAdd139=(PythonTree)input.LT(1);
                    match(input,UAdd,FOLLOW_UAdd_in_atom2780); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_atom2782);
                    test140=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            retval.etype = new UnaryOp(UAdd139, unaryopType.UAdd, test140.etype);
                        

                    }
                    break;
                case 16 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1191:7: ^( Invert test[ctype] )
                    {
                    Invert141=(PythonTree)input.LT(1);
                    match(input,Invert,FOLLOW_Invert_in_atom2795); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_atom2797);
                    test142=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            retval.etype = new UnaryOp(Invert141, unaryopType.Invert, test142.etype);
                        

                    }
                    break;
                case 17 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1194:7: ^( NOT test[ctype] )
                    {
                    NOT143=(PythonTree)input.LT(1);
                    match(input,NOT,FOLLOW_NOT_in_atom2810); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_atom2812);
                    test144=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            retval.etype = new UnaryOp(NOT143, unaryopType.Not, test144.etype);
                        

                    }
                    break;
                case 18 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1197:7: ^( Parens test[ctype] )
                    {
                    match(input,Parens,FOLLOW_Parens_in_atom2825); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_atom2827);
                    test145=test(ctype);
                    _fsp--;


                    match(input, Token.UP, null); 

                            debug("PARENS! " + test145.etype);
                            retval.parens = true;
                            retval.etype = test145.etype;
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end atom


    // $ANTLR start comprehension
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1204:1: comprehension[expr_contextType ctype] returns [exprType etype] : ( ^( ListComp test[ctype] list_for[gens] ) | ^( GeneratorExp test[ctype] gen_for[gens] ) );
    public final exprType comprehension(expr_contextType ctype) throws RecognitionException {
        exprType etype = null;

        PythonTree ListComp146=null;
        PythonTree GeneratorExp148=null;
        test_return test147 = null;

        test_return test149 = null;



            List gens = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1208:5: ( ^( ListComp test[ctype] list_for[gens] ) | ^( GeneratorExp test[ctype] gen_for[gens] ) )
            int alt68=2;
            int LA68_0 = input.LA(1);

            if ( (LA68_0==ListComp) ) {
                alt68=1;
            }
            else if ( (LA68_0==GeneratorExp) ) {
                alt68=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1204:1: comprehension[expr_contextType ctype] returns [exprType etype] : ( ^( ListComp test[ctype] list_for[gens] ) | ^( GeneratorExp test[ctype] gen_for[gens] ) );", 68, 0, input);

                throw nvae;
            }
            switch (alt68) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1208:7: ^( ListComp test[ctype] list_for[gens] )
                    {
                    ListComp146=(PythonTree)input.LT(1);
                    match(input,ListComp,FOLLOW_ListComp_in_comprehension2859); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_comprehension2861);
                    test147=test(ctype);
                    _fsp--;

                    pushFollow(FOLLOW_list_for_in_comprehension2864);
                    list_for(gens);
                    _fsp--;


                    match(input, Token.UP, null); 

                            debug("matched ListComp");
                            Collections.reverse(gens);
                            comprehensionType[] c = (comprehensionType[])gens.toArray(new comprehensionType[gens.size()]);
                            etype = new ListComp(ListComp146, test147.etype, c);
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1214:7: ^( GeneratorExp test[ctype] gen_for[gens] )
                    {
                    GeneratorExp148=(PythonTree)input.LT(1);
                    match(input,GeneratorExp,FOLLOW_GeneratorExp_in_comprehension2877); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_comprehension2879);
                    test149=test(ctype);
                    _fsp--;

                    pushFollow(FOLLOW_gen_for_in_comprehension2882);
                    gen_for(gens);
                    _fsp--;


                    match(input, Token.UP, null); 

                            debug("matched GeneratorExp");
                            Collections.reverse(gens);
                            comprehensionType[] c = (comprehensionType[])gens.toArray(new comprehensionType[gens.size()]);
                            etype = new GeneratorExp(GeneratorExp148, test149.etype, c);
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end comprehension

    public static class stringlist_return extends TreeRuleReturnScope {
        public List strings;
    };

    // $ANTLR start stringlist
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1222:1: stringlist returns [List strings] : ^( Str ( string[strs] )+ ) ;
    public final stringlist_return stringlist() throws RecognitionException {
        stringlist_return retval = new stringlist_return();
        retval.start = input.LT(1);


            List strs = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1226:5: ( ^( Str ( string[strs] )+ ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1226:7: ^( Str ( string[strs] )+ )
            {
            match(input,Str,FOLLOW_Str_in_stringlist2913); 

            match(input, Token.DOWN, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1226:13: ( string[strs] )+
            int cnt69=0;
            loop69:
            do {
                int alt69=2;
                int LA69_0 = input.LA(1);

                if ( (LA69_0==STRING) ) {
                    alt69=1;
                }


                switch (alt69) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1226:13: string[strs]
            	    {
            	    pushFollow(FOLLOW_string_in_stringlist2915);
            	    string(strs);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt69 >= 1 ) break loop69;
                        EarlyExitException eee =
                            new EarlyExitException(69, input);
                        throw eee;
                }
                cnt69++;
            } while (true);


            match(input, Token.UP, null); 
            retval.strings = strs;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end stringlist


    // $ANTLR start string
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1229:1: string[List strs] : STRING ;
    public final void string(List strs) throws RecognitionException {
        PythonTree STRING150=null;

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1230:5: ( STRING )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1230:7: STRING
            {
            STRING150=(PythonTree)input.LT(1);
            match(input,STRING,FOLLOW_STRING_in_string2938); 
            strs.add(STRING150.getText());

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end string


    // $ANTLR start lambdef
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1233:1: lambdef returns [exprType etype] : ^( Lambda ( varargslist )? ^( Body test[expr_contextType.Load] ) ) ;
    public final exprType lambdef() throws RecognitionException {
        exprType etype = null;

        PythonTree Lambda152=null;
        argumentsType varargslist151 = null;

        test_return test153 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1234:5: ( ^( Lambda ( varargslist )? ^( Body test[expr_contextType.Load] ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1234:7: ^( Lambda ( varargslist )? ^( Body test[expr_contextType.Load] ) )
            {
            Lambda152=(PythonTree)input.LT(1);
            match(input,Lambda,FOLLOW_Lambda_in_lambdef2962); 

            match(input, Token.DOWN, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1234:16: ( varargslist )?
            int alt70=2;
            int LA70_0 = input.LA(1);

            if ( (LA70_0==Args||(LA70_0>=StarArgs && LA70_0<=KWArgs)) ) {
                alt70=1;
            }
            switch (alt70) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1234:16: varargslist
                    {
                    pushFollow(FOLLOW_varargslist_in_lambdef2964);
                    varargslist151=varargslist();
                    _fsp--;


                    }
                    break;

            }

            match(input,Body,FOLLOW_Body_in_lambdef2968); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_lambdef2970);
            test153=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 

            match(input, Token.UP, null); 

                    argumentsType a = varargslist151;
                    if (a == null) {
                        a = new argumentsType(Lambda152, new exprType[0], null, null, new exprType[0]);
                    }
                    etype = new Lambda(Lambda152, a, test153.etype);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end lambdef


    // $ANTLR start subscriptlist
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1243:1: subscriptlist returns [List sltypes] : ( subscript[subs] )+ ;
    public final List subscriptlist() throws RecognitionException {
        List sltypes = null;


            List subs = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1247:5: ( ( subscript[subs] )+ )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1247:9: ( subscript[subs] )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1247:9: ( subscript[subs] )+
            int cnt71=0;
            loop71:
            do {
                int alt71=2;
                int LA71_0 = input.LA(1);

                if ( (LA71_0==Ellipsis||LA71_0==Subscript||LA71_0==Index) ) {
                    alt71=1;
                }


                switch (alt71) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1247:9: subscript[subs]
            	    {
            	    pushFollow(FOLLOW_subscript_in_subscriptlist3003);
            	    subscript(subs);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt71 >= 1 ) break loop71;
                        EarlyExitException eee =
                            new EarlyExitException(71, input);
                        throw eee;
                }
                cnt71++;
            } while (true);


                    sltypes = subs;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return sltypes;
    }
    // $ANTLR end subscriptlist


    // $ANTLR start subscript
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1252:1: subscript[List subs] : ( Ellipsis | ^( Index test[expr_contextType.Load] ) | ^( Subscript ( ^( Lower start= test[expr_contextType.Load] ) )? ( ^( Upper COLON ( ^( UpperOp end= test[expr_contextType.Load] ) )? ) )? ( ^( Step COLON ( ^( StepOp op= test[expr_contextType.Load] ) )? ) )? ) );
    public final void subscript(List subs) throws RecognitionException {
        PythonTree Ellipsis154=null;
        PythonTree Index155=null;
        PythonTree Lower157=null;
        PythonTree Upper158=null;
        PythonTree UpperOp159=null;
        PythonTree Step160=null;
        PythonTree StepOp161=null;
        PythonTree Subscript162=null;
        test_return start = null;

        test_return end = null;

        test_return op = null;

        test_return test156 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1253:5: ( Ellipsis | ^( Index test[expr_contextType.Load] ) | ^( Subscript ( ^( Lower start= test[expr_contextType.Load] ) )? ( ^( Upper COLON ( ^( UpperOp end= test[expr_contextType.Load] ) )? ) )? ( ^( Step COLON ( ^( StepOp op= test[expr_contextType.Load] ) )? ) )? ) )
            int alt77=3;
            switch ( input.LA(1) ) {
            case Ellipsis:
                {
                alt77=1;
                }
                break;
            case Index:
                {
                alt77=2;
                }
                break;
            case Subscript:
                {
                alt77=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1252:1: subscript[List subs] : ( Ellipsis | ^( Index test[expr_contextType.Load] ) | ^( Subscript ( ^( Lower start= test[expr_contextType.Load] ) )? ( ^( Upper COLON ( ^( UpperOp end= test[expr_contextType.Load] ) )? ) )? ( ^( Step COLON ( ^( StepOp op= test[expr_contextType.Load] ) )? ) )? ) );", 77, 0, input);

                throw nvae;
            }

            switch (alt77) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1253:7: Ellipsis
                    {
                    Ellipsis154=(PythonTree)input.LT(1);
                    match(input,Ellipsis,FOLLOW_Ellipsis_in_subscript3026); 

                            subs.add(new Ellipsis(Ellipsis154));
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1256:7: ^( Index test[expr_contextType.Load] )
                    {
                    Index155=(PythonTree)input.LT(1);
                    match(input,Index,FOLLOW_Index_in_subscript3037); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_subscript3039);
                    test156=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                            subs.add(new Index(Index155, test156.etype));
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1259:7: ^( Subscript ( ^( Lower start= test[expr_contextType.Load] ) )? ( ^( Upper COLON ( ^( UpperOp end= test[expr_contextType.Load] ) )? ) )? ( ^( Step COLON ( ^( StepOp op= test[expr_contextType.Load] ) )? ) )? )
                    {
                    Subscript162=(PythonTree)input.LT(1);
                    match(input,Subscript,FOLLOW_Subscript_in_subscript3052); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1259:19: ( ^( Lower start= test[expr_contextType.Load] ) )?
                        int alt72=2;
                        int LA72_0 = input.LA(1);

                        if ( (LA72_0==Lower) ) {
                            alt72=1;
                        }
                        switch (alt72) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1259:20: ^( Lower start= test[expr_contextType.Load] )
                                {
                                Lower157=(PythonTree)input.LT(1);
                                match(input,Lower,FOLLOW_Lower_in_subscript3056); 

                                match(input, Token.DOWN, null); 
                                pushFollow(FOLLOW_test_in_subscript3060);
                                start=test(expr_contextType.Load);
                                _fsp--;


                                match(input, Token.UP, null); 

                                }
                                break;

                        }

                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:11: ( ^( Upper COLON ( ^( UpperOp end= test[expr_contextType.Load] ) )? ) )?
                        int alt74=2;
                        int LA74_0 = input.LA(1);

                        if ( (LA74_0==Upper) ) {
                            alt74=1;
                        }
                        switch (alt74) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:12: ^( Upper COLON ( ^( UpperOp end= test[expr_contextType.Load] ) )? )
                                {
                                Upper158=(PythonTree)input.LT(1);
                                match(input,Upper,FOLLOW_Upper_in_subscript3078); 

                                match(input, Token.DOWN, null); 
                                match(input,COLON,FOLLOW_COLON_in_subscript3080); 
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:26: ( ^( UpperOp end= test[expr_contextType.Load] ) )?
                                int alt73=2;
                                int LA73_0 = input.LA(1);

                                if ( (LA73_0==UpperOp) ) {
                                    alt73=1;
                                }
                                switch (alt73) {
                                    case 1 :
                                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:27: ^( UpperOp end= test[expr_contextType.Load] )
                                        {
                                        UpperOp159=(PythonTree)input.LT(1);
                                        match(input,UpperOp,FOLLOW_UpperOp_in_subscript3084); 

                                        match(input, Token.DOWN, null); 
                                        pushFollow(FOLLOW_test_in_subscript3088);
                                        end=test(expr_contextType.Load);
                                        _fsp--;


                                        match(input, Token.UP, null); 

                                        }
                                        break;

                                }


                                match(input, Token.UP, null); 

                                }
                                break;

                        }

                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:75: ( ^( Step COLON ( ^( StepOp op= test[expr_contextType.Load] ) )? ) )?
                        int alt76=2;
                        int LA76_0 = input.LA(1);

                        if ( (LA76_0==Step) ) {
                            alt76=1;
                        }
                        switch (alt76) {
                            case 1 :
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:76: ^( Step COLON ( ^( StepOp op= test[expr_contextType.Load] ) )? )
                                {
                                Step160=(PythonTree)input.LT(1);
                                match(input,Step,FOLLOW_Step_in_subscript3099); 

                                match(input, Token.DOWN, null); 
                                match(input,COLON,FOLLOW_COLON_in_subscript3101); 
                                // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:89: ( ^( StepOp op= test[expr_contextType.Load] ) )?
                                int alt75=2;
                                int LA75_0 = input.LA(1);

                                if ( (LA75_0==StepOp) ) {
                                    alt75=1;
                                }
                                switch (alt75) {
                                    case 1 :
                                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1260:90: ^( StepOp op= test[expr_contextType.Load] )
                                        {
                                        StepOp161=(PythonTree)input.LT(1);
                                        match(input,StepOp,FOLLOW_StepOp_in_subscript3105); 

                                        match(input, Token.DOWN, null); 
                                        pushFollow(FOLLOW_test_in_subscript3109);
                                        op=test(expr_contextType.Load);
                                        _fsp--;


                                        match(input, Token.UP, null); 

                                        }
                                        break;

                                }


                                match(input, Token.UP, null); 

                                }
                                break;

                        }


                        match(input, Token.UP, null); 
                    }

                                  boolean isSlice = false;
                                  exprType s = null;
                                  exprType e = null;
                                  exprType o = null;
                                  if (Lower157 != null) {
                                      s = start.etype;
                                  }
                                  if (Upper158 != null) {
                                      isSlice = true;
                                      if (UpperOp159 != null) {
                                          e = end.etype;
                                      }
                                  }
                                  if (Step160 != null) {
                                      isSlice = true;
                                      if (StepOp161 != null) {
                                          o = op.etype;
                                      } else {
                                          o = new Name(Step160, "None", expr_contextType.Load);
                                      }
                                  }

                                  if (isSlice) {
                                     subs.add(new Slice(Subscript162, s, e, o));
                                  }
                                  else {
                                     subs.add(new Index(Subscript162, s));
                                  }
                              

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end subscript


    // $ANTLR start classdef
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1292:1: classdef : ^( ClassDef ^( Name classname= NAME ) ( ^( Bases bases ) )? ^( Body stmts ) ) ;
    public final void classdef() throws RecognitionException {
        PythonTree classname=null;
        PythonTree Bases163=null;
        PythonTree ClassDef165=null;
        List bases164 = null;

        stmts_return stmts166 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1293:5: ( ^( ClassDef ^( Name classname= NAME ) ( ^( Bases bases ) )? ^( Body stmts ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1293:7: ^( ClassDef ^( Name classname= NAME ) ( ^( Bases bases ) )? ^( Body stmts ) )
            {
            ClassDef165=(PythonTree)input.LT(1);
            match(input,ClassDef,FOLLOW_ClassDef_in_classdef3143); 

            match(input, Token.DOWN, null); 
            match(input,Name,FOLLOW_Name_in_classdef3146); 

            match(input, Token.DOWN, null); 
            classname=(PythonTree)input.LT(1);
            match(input,NAME,FOLLOW_NAME_in_classdef3150); 

            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1293:41: ( ^( Bases bases ) )?
            int alt78=2;
            int LA78_0 = input.LA(1);

            if ( (LA78_0==Bases) ) {
                alt78=1;
            }
            switch (alt78) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1293:42: ^( Bases bases )
                    {
                    Bases163=(PythonTree)input.LT(1);
                    match(input,Bases,FOLLOW_Bases_in_classdef3155); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_bases_in_classdef3157);
                    bases164=bases();
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }

            match(input,Body,FOLLOW_Body_in_classdef3163); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_stmts_in_classdef3165);
            stmts166=stmts();
            _fsp--;


            match(input, Token.UP, null); 

            match(input, Token.UP, null); 

                    List b;
                    if (Bases163 != null) {
                        b = bases164;
                    } else {
                        b = new ArrayList();
                    }
                    ((stmts_scope)stmts_stack.peek()).statements.add(makeClassDef(ClassDef165, classname, b, stmts166.stypes));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end classdef


    // $ANTLR start bases
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1304:1: bases returns [List names] : base[nms] ;
    public final List bases() throws RecognitionException {
        List names = null;


            List nms = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1308:5: ( base[nms] )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1308:7: base[nms]
            {
            pushFollow(FOLLOW_base_in_bases3195);
            base(nms);
            _fsp--;


                    //The instanceof and tuple unpack here is gross.  I *should* be able to detect
                    //"Tuple or Tuple DOWN or some such in a syntactic predicate in the "base" rule
                    //instead, but I haven't been able to get it to work.
                    if (nms.get(0) instanceof Tuple) {
                        debug("TUPLE");
                        Tuple t = (Tuple)nms.get(0);
                        names = Arrays.asList(t.elts);
                    } else {
                        debug("NOT TUPLE");
                        names = nms;
                    }
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return names;
    }
    // $ANTLR end bases


    // $ANTLR start base
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1327:1: base[List names] : test[expr_contextType.Load] ;
    public final void base(List names) throws RecognitionException {
        test_return test167 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1328:5: ( test[expr_contextType.Load] )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1328:7: test[expr_contextType.Load]
            {
            pushFollow(FOLLOW_test_in_base3220);
            test167=test(expr_contextType.Load);
            _fsp--;


                    names.add(test167.etype);
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end base

    public static class arglist_return extends TreeRuleReturnScope {
        public List args;
        public List keywords;
        public exprType starargs;
        public exprType kwargs;
    };

    // $ANTLR start arglist
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1333:1: arglist returns [List args, List keywords, exprType starargs, exprType kwargs] : ( ^( Args ( argument[arguments] )* ( keyword[kws] )* ) ( ^( StarArgs stest= test[expr_contextType.Load] ) )? ( ^( KWArgs ktest= test[expr_contextType.Load] ) )? | ^( StarArgs stest= test[expr_contextType.Load] ) ( ^( KWArgs ktest= test[expr_contextType.Load] ) )? | ^( KWArgs test[expr_contextType.Load] ) );
    public final arglist_return arglist() throws RecognitionException {
        arglist_return retval = new arglist_return();
        retval.start = input.LT(1);

        PythonTree StarArgs168=null;
        PythonTree KWArgs169=null;
        PythonTree KWArgs170=null;
        test_return stest = null;

        test_return ktest = null;

        test_return test171 = null;



            List arguments = new ArrayList();
            List kws = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:5: ( ^( Args ( argument[arguments] )* ( keyword[kws] )* ) ( ^( StarArgs stest= test[expr_contextType.Load] ) )? ( ^( KWArgs ktest= test[expr_contextType.Load] ) )? | ^( StarArgs stest= test[expr_contextType.Load] ) ( ^( KWArgs ktest= test[expr_contextType.Load] ) )? | ^( KWArgs test[expr_contextType.Load] ) )
            int alt84=3;
            switch ( input.LA(1) ) {
            case Args:
                {
                alt84=1;
                }
                break;
            case StarArgs:
                {
                alt84=2;
                }
                break;
            case KWArgs:
                {
                alt84=3;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("1333:1: arglist returns [List args, List keywords, exprType starargs, exprType kwargs] : ( ^( Args ( argument[arguments] )* ( keyword[kws] )* ) ( ^( StarArgs stest= test[expr_contextType.Load] ) )? ( ^( KWArgs ktest= test[expr_contextType.Load] ) )? | ^( StarArgs stest= test[expr_contextType.Load] ) ( ^( KWArgs ktest= test[expr_contextType.Load] ) )? | ^( KWArgs test[expr_contextType.Load] ) );", 84, 0, input);

                throw nvae;
            }

            switch (alt84) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:7: ^( Args ( argument[arguments] )* ( keyword[kws] )* ) ( ^( StarArgs stest= test[expr_contextType.Load] ) )? ( ^( KWArgs ktest= test[expr_contextType.Load] ) )?
                    {
                    match(input,Args,FOLLOW_Args_in_arglist3250); 

                    if ( input.LA(1)==Token.DOWN ) {
                        match(input, Token.DOWN, null); 
                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:14: ( argument[arguments] )*
                        loop79:
                        do {
                            int alt79=2;
                            int LA79_0 = input.LA(1);

                            if ( (LA79_0==Arg||LA79_0==GenFor) ) {
                                alt79=1;
                            }


                            switch (alt79) {
                        	case 1 :
                        	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:14: argument[arguments]
                        	    {
                        	    pushFollow(FOLLOW_argument_in_arglist3252);
                        	    argument(arguments);
                        	    _fsp--;


                        	    }
                        	    break;

                        	default :
                        	    break loop79;
                            }
                        } while (true);

                        // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:35: ( keyword[kws] )*
                        loop80:
                        do {
                            int alt80=2;
                            int LA80_0 = input.LA(1);

                            if ( (LA80_0==Keyword) ) {
                                alt80=1;
                            }


                            switch (alt80) {
                        	case 1 :
                        	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:35: keyword[kws]
                        	    {
                        	    pushFollow(FOLLOW_keyword_in_arglist3256);
                        	    keyword(kws);
                        	    _fsp--;


                        	    }
                        	    break;

                        	default :
                        	    break loop80;
                            }
                        } while (true);


                        match(input, Token.UP, null); 
                    }
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:50: ( ^( StarArgs stest= test[expr_contextType.Load] ) )?
                    int alt81=2;
                    int LA81_0 = input.LA(1);

                    if ( (LA81_0==StarArgs) ) {
                        alt81=1;
                    }
                    switch (alt81) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:51: ^( StarArgs stest= test[expr_contextType.Load] )
                            {
                            StarArgs168=(PythonTree)input.LT(1);
                            match(input,StarArgs,FOLLOW_StarArgs_in_arglist3263); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_test_in_arglist3267);
                            stest=test(expr_contextType.Load);
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }

                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:99: ( ^( KWArgs ktest= test[expr_contextType.Load] ) )?
                    int alt82=2;
                    int LA82_0 = input.LA(1);

                    if ( (LA82_0==KWArgs) ) {
                        alt82=1;
                    }
                    switch (alt82) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1338:100: ^( KWArgs ktest= test[expr_contextType.Load] )
                            {
                            KWArgs169=(PythonTree)input.LT(1);
                            match(input,KWArgs,FOLLOW_KWArgs_in_arglist3275); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_test_in_arglist3279);
                            ktest=test(expr_contextType.Load);
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }


                            retval.args =arguments;
                            retval.keywords =kws;
                            if (StarArgs168 != null) {
                                retval.starargs =stest.etype;
                            }
                            if (KWArgs169 != null) {
                                retval.kwargs =ktest.etype;
                            }
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1348:7: ^( StarArgs stest= test[expr_contextType.Load] ) ( ^( KWArgs ktest= test[expr_contextType.Load] ) )?
                    {
                    match(input,StarArgs,FOLLOW_StarArgs_in_arglist3294); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_arglist3298);
                    stest=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1348:53: ( ^( KWArgs ktest= test[expr_contextType.Load] ) )?
                    int alt83=2;
                    int LA83_0 = input.LA(1);

                    if ( (LA83_0==KWArgs) ) {
                        alt83=1;
                    }
                    switch (alt83) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1348:54: ^( KWArgs ktest= test[expr_contextType.Load] )
                            {
                            KWArgs170=(PythonTree)input.LT(1);
                            match(input,KWArgs,FOLLOW_KWArgs_in_arglist3304); 

                            match(input, Token.DOWN, null); 
                            pushFollow(FOLLOW_test_in_arglist3308);
                            ktest=test(expr_contextType.Load);
                            _fsp--;


                            match(input, Token.UP, null); 

                            }
                            break;

                    }


                            retval.args =arguments;
                            retval.keywords =kws;
                            retval.starargs =stest.etype;
                            if (KWArgs170 != null) {
                                retval.kwargs =ktest.etype;
                            }
                        

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1356:7: ^( KWArgs test[expr_contextType.Load] )
                    {
                    match(input,KWArgs,FOLLOW_KWArgs_in_arglist3323); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_arglist3325);
                    test171=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                            retval.args =arguments;
                            retval.keywords =kws;
                            retval.kwargs =test171.etype;
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return retval;
    }
    // $ANTLR end arglist


    // $ANTLR start argument
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1363:1: argument[List arguments] : ( ^( Arg test[expr_contextType.Load] ) | ^( GenFor test[expr_contextType.Load] gen_for[gens] ) );
    public final void argument(List arguments) throws RecognitionException {
        PythonTree GenFor173=null;
        test_return test172 = null;

        test_return test174 = null;



            List gens = new ArrayList();

        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1367:5: ( ^( Arg test[expr_contextType.Load] ) | ^( GenFor test[expr_contextType.Load] gen_for[gens] ) )
            int alt85=2;
            int LA85_0 = input.LA(1);

            if ( (LA85_0==Arg) ) {
                alt85=1;
            }
            else if ( (LA85_0==GenFor) ) {
                alt85=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1363:1: argument[List arguments] : ( ^( Arg test[expr_contextType.Load] ) | ^( GenFor test[expr_contextType.Load] gen_for[gens] ) );", 85, 0, input);

                throw nvae;
            }
            switch (alt85) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1367:7: ^( Arg test[expr_contextType.Load] )
                    {
                    match(input,Arg,FOLLOW_Arg_in_argument3353); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_argument3355);
                    test172=test(expr_contextType.Load);
                    _fsp--;


                    match(input, Token.UP, null); 

                            arguments.add(test172.etype);
                        

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1370:7: ^( GenFor test[expr_contextType.Load] gen_for[gens] )
                    {
                    GenFor173=(PythonTree)input.LT(1);
                    match(input,GenFor,FOLLOW_GenFor_in_argument3368); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_test_in_argument3370);
                    test174=test(expr_contextType.Load);
                    _fsp--;

                    pushFollow(FOLLOW_gen_for_in_argument3373);
                    gen_for(gens);
                    _fsp--;


                    match(input, Token.UP, null); 

                            Collections.reverse(gens);
                            comprehensionType[] c = (comprehensionType[])gens.toArray(new comprehensionType[gens.size()]);
                            arguments.add(new GeneratorExp(GenFor173, test174.etype, c));
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end argument


    // $ANTLR start keyword
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1377:1: keyword[List kws] : ^( Keyword ^( Arg arg= test[expr_contextType.Load] ) ^( Value val= test[expr_contextType.Load] ) ) ;
    public final void keyword(List kws) throws RecognitionException {
        PythonTree Keyword175=null;
        test_return arg = null;

        test_return val = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1378:5: ( ^( Keyword ^( Arg arg= test[expr_contextType.Load] ) ^( Value val= test[expr_contextType.Load] ) ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1378:7: ^( Keyword ^( Arg arg= test[expr_contextType.Load] ) ^( Value val= test[expr_contextType.Load] ) )
            {
            Keyword175=(PythonTree)input.LT(1);
            match(input,Keyword,FOLLOW_Keyword_in_keyword3396); 

            match(input, Token.DOWN, null); 
            match(input,Arg,FOLLOW_Arg_in_keyword3399); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_keyword3403);
            arg=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            match(input,Value,FOLLOW_Value_in_keyword3408); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_keyword3412);
            val=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 

            match(input, Token.UP, null); 

                    kws.add(new keywordType(Keyword175, input.getTokenStream().toString(
              input.getTreeAdaptor().getTokenStartIndex(arg.start),
              input.getTreeAdaptor().getTokenStopIndex(arg.start)), val.etype));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end keyword


    // $ANTLR start list_iter
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1383:1: list_iter[List gens] returns [exprType etype] : ( list_for[gens] | list_if[gens] );
    public final exprType list_iter(List gens) throws RecognitionException {
        exprType etype = null;

        exprType list_if176 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1384:5: ( list_for[gens] | list_if[gens] )
            int alt86=2;
            int LA86_0 = input.LA(1);

            if ( (LA86_0==ListFor) ) {
                alt86=1;
            }
            else if ( (LA86_0==ListIf) ) {
                alt86=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1383:1: list_iter[List gens] returns [exprType etype] : ( list_for[gens] | list_if[gens] );", 86, 0, input);

                throw nvae;
            }
            switch (alt86) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1384:7: list_for[gens]
                    {
                    pushFollow(FOLLOW_list_for_in_list_iter3440);
                    list_for(gens);
                    _fsp--;


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1385:7: list_if[gens]
                    {
                    pushFollow(FOLLOW_list_if_in_list_iter3449);
                    list_if176=list_if(gens);
                    _fsp--;


                            etype = list_if176;
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end list_iter


    // $ANTLR start list_for
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1390:1: list_for[List gens] : ^( ListFor ^( Target targ= test[expr_contextType.Store] ) ^( Iter iter= test[expr_contextType.Load] ) ( ^( Ifs list_iter[gens] ) )? ) ;
    public final void list_for(List gens) throws RecognitionException {
        PythonTree Ifs177=null;
        PythonTree ListFor179=null;
        test_return targ = null;

        test_return iter = null;

        exprType list_iter178 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1391:5: ( ^( ListFor ^( Target targ= test[expr_contextType.Store] ) ^( Iter iter= test[expr_contextType.Load] ) ( ^( Ifs list_iter[gens] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1392:5: ^( ListFor ^( Target targ= test[expr_contextType.Store] ) ^( Iter iter= test[expr_contextType.Load] ) ( ^( Ifs list_iter[gens] ) )? )
            {
            ListFor179=(PythonTree)input.LT(1);
            match(input,ListFor,FOLLOW_ListFor_in_list_for3476); 

            match(input, Token.DOWN, null); 
            match(input,Target,FOLLOW_Target_in_list_for3479); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_list_for3483);
            targ=test(expr_contextType.Store);
            _fsp--;


            match(input, Token.UP, null); 
            match(input,Iter,FOLLOW_Iter_in_list_for3488); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_list_for3492);
            iter=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1392:100: ( ^( Ifs list_iter[gens] ) )?
            int alt87=2;
            int LA87_0 = input.LA(1);

            if ( (LA87_0==Ifs) ) {
                alt87=1;
            }
            switch (alt87) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1392:101: ^( Ifs list_iter[gens] )
                    {
                    Ifs177=(PythonTree)input.LT(1);
                    match(input,Ifs,FOLLOW_Ifs_in_list_for3498); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_list_iter_in_list_for3500);
                    list_iter178=list_iter(gens);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    debug("matched list_for");
                    exprType[] e;
                    if (Ifs177 != null && list_iter178 != null) {
                        e = new exprType[]{list_iter178};
                    } else {
                        e = new exprType[0];
                    }
                    gens.add(new comprehensionType(ListFor179, targ.etype, iter.etype, e));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end list_for


    // $ANTLR start list_if
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1404:1: list_if[List gens] returns [exprType etype] : ^( ListIf ^( Target test[expr_contextType.Load] ) ( Ifs list_iter[gens] )? ) ;
    public final exprType list_if(List gens) throws RecognitionException {
        exprType etype = null;

        test_return test180 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1405:5: ( ^( ListIf ^( Target test[expr_contextType.Load] ) ( Ifs list_iter[gens] )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1405:7: ^( ListIf ^( Target test[expr_contextType.Load] ) ( Ifs list_iter[gens] )? )
            {
            match(input,ListIf,FOLLOW_ListIf_in_list_if3530); 

            match(input, Token.DOWN, null); 
            match(input,Target,FOLLOW_Target_in_list_if3533); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_list_if3535);
            test180=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1405:54: ( Ifs list_iter[gens] )?
            int alt88=2;
            int LA88_0 = input.LA(1);

            if ( (LA88_0==Ifs) ) {
                alt88=1;
            }
            switch (alt88) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1405:55: Ifs list_iter[gens]
                    {
                    match(input,Ifs,FOLLOW_Ifs_in_list_if3540); 
                    pushFollow(FOLLOW_list_iter_in_list_if3542);
                    list_iter(gens);
                    _fsp--;


                    }
                    break;

            }


            match(input, Token.UP, null); 

                    etype = test180.etype;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end list_if


    // $ANTLR start gen_iter
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1410:1: gen_iter[List gens] returns [exprType etype] : ( gen_for[gens] | gen_if[gens] );
    public final exprType gen_iter(List gens) throws RecognitionException {
        exprType etype = null;

        exprType gen_if181 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1411:5: ( gen_for[gens] | gen_if[gens] )
            int alt89=2;
            int LA89_0 = input.LA(1);

            if ( (LA89_0==GenFor) ) {
                alt89=1;
            }
            else if ( (LA89_0==GenIf) ) {
                alt89=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1410:1: gen_iter[List gens] returns [exprType etype] : ( gen_for[gens] | gen_if[gens] );", 89, 0, input);

                throw nvae;
            }
            switch (alt89) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1411:7: gen_for[gens]
                    {
                    pushFollow(FOLLOW_gen_for_in_gen_iter3571);
                    gen_for(gens);
                    _fsp--;


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1412:7: gen_if[gens]
                    {
                    pushFollow(FOLLOW_gen_if_in_gen_iter3580);
                    gen_if181=gen_if(gens);
                    _fsp--;


                            etype = gen_if181;
                        

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end gen_iter


    // $ANTLR start gen_for
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1417:1: gen_for[List gens] : ^( GenFor ^( Target (targ= test[expr_contextType.Store] )+ ) ^( Iter iter= test[expr_contextType.Load] ) ( ^( Ifs gen_iter[gens] ) )? ) ;
    public final void gen_for(List gens) throws RecognitionException {
        PythonTree Ifs182=null;
        PythonTree GenFor184=null;
        test_return targ = null;

        test_return iter = null;

        exprType gen_iter183 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1418:5: ( ^( GenFor ^( Target (targ= test[expr_contextType.Store] )+ ) ^( Iter iter= test[expr_contextType.Load] ) ( ^( Ifs gen_iter[gens] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1418:7: ^( GenFor ^( Target (targ= test[expr_contextType.Store] )+ ) ^( Iter iter= test[expr_contextType.Load] ) ( ^( Ifs gen_iter[gens] ) )? )
            {
            GenFor184=(PythonTree)input.LT(1);
            match(input,GenFor,FOLLOW_GenFor_in_gen_for3603); 

            match(input, Token.DOWN, null); 
            match(input,Target,FOLLOW_Target_in_gen_for3606); 

            match(input, Token.DOWN, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1418:29: (targ= test[expr_contextType.Store] )+
            int cnt90=0;
            loop90:
            do {
                int alt90=2;
                int LA90_0 = input.LA(1);

                if ( (LA90_0==Name||(LA90_0>=Tuple && LA90_0<=Dict)||LA90_0==IfExp||(LA90_0>=Yield && LA90_0<=IsNot)||LA90_0==NotIn||(LA90_0>=ListComp && LA90_0<=Repr)||LA90_0==SubscriptList||(LA90_0>=UAdd && LA90_0<=Invert)||LA90_0==GeneratorExp||LA90_0==Call||LA90_0==Parens||LA90_0==DOT||(LA90_0>=STAR && LA90_0<=DOUBLESTAR)||(LA90_0>=RIGHTSHIFT && LA90_0<=DOUBLESLASH)||LA90_0==182||LA90_0==193) ) {
                    alt90=1;
                }


                switch (alt90) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1418:29: targ= test[expr_contextType.Store]
            	    {
            	    pushFollow(FOLLOW_test_in_gen_for3610);
            	    targ=test(expr_contextType.Store);
            	    _fsp--;


            	    }
            	    break;

            	default :
            	    if ( cnt90 >= 1 ) break loop90;
                        EarlyExitException eee =
                            new EarlyExitException(90, input);
                        throw eee;
                }
                cnt90++;
            } while (true);


            match(input, Token.UP, null); 
            match(input,Iter,FOLLOW_Iter_in_gen_for3616); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_gen_for3620);
            iter=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1418:102: ( ^( Ifs gen_iter[gens] ) )?
            int alt91=2;
            int LA91_0 = input.LA(1);

            if ( (LA91_0==Ifs) ) {
                alt91=1;
            }
            switch (alt91) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1418:103: ^( Ifs gen_iter[gens] )
                    {
                    Ifs182=(PythonTree)input.LT(1);
                    match(input,Ifs,FOLLOW_Ifs_in_gen_for3626); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_gen_iter_in_gen_for3628);
                    gen_iter183=gen_iter(gens);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    debug("matched gen_for");
                    exprType[] e;
                    if (Ifs182 != null && gen_iter183 != null) {
                        e = new exprType[]{gen_iter183};
                    } else {
                        e = new exprType[0];
                    }
                    gens.add(new comprehensionType(GenFor184, targ.etype, iter.etype, e));
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end gen_for


    // $ANTLR start gen_if
    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1430:1: gen_if[List gens] returns [exprType etype] : ^( GenIf ^( Target test[expr_contextType.Load] ) ( ^( Ifs gen_iter[gens] ) )? ) ;
    public final exprType gen_if(List gens) throws RecognitionException {
        exprType etype = null;

        test_return test185 = null;


        try {
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1431:5: ( ^( GenIf ^( Target test[expr_contextType.Load] ) ( ^( Ifs gen_iter[gens] ) )? ) )
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1431:7: ^( GenIf ^( Target test[expr_contextType.Load] ) ( ^( Ifs gen_iter[gens] ) )? )
            {
            match(input,GenIf,FOLLOW_GenIf_in_gen_if3658); 

            match(input, Token.DOWN, null); 
            match(input,Target,FOLLOW_Target_in_gen_if3661); 

            match(input, Token.DOWN, null); 
            pushFollow(FOLLOW_test_in_gen_if3663);
            test185=test(expr_contextType.Load);
            _fsp--;


            match(input, Token.UP, null); 
            // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1431:53: ( ^( Ifs gen_iter[gens] ) )?
            int alt92=2;
            int LA92_0 = input.LA(1);

            if ( (LA92_0==Ifs) ) {
                alt92=1;
            }
            switch (alt92) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/PythonWalker.g:1431:54: ^( Ifs gen_iter[gens] )
                    {
                    match(input,Ifs,FOLLOW_Ifs_in_gen_if3669); 

                    match(input, Token.DOWN, null); 
                    pushFollow(FOLLOW_gen_iter_in_gen_if3671);
                    gen_iter(gens);
                    _fsp--;


                    match(input, Token.UP, null); 

                    }
                    break;

            }


            match(input, Token.UP, null); 

                    etype = test185.etype;
                

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return etype;
    }
    // $ANTLR end gen_if


    protected DFA40 dfa40 = new DFA40(this);
    static final String DFA40_eotS =
        "\22\uffff";
    static final String DFA40_eofS =
        "\22\uffff";
    static final String DFA40_minS =
        "\1\11\1\uffff\1\2\1\11\3\2\1\156\1\155\1\120\2\3\2\uffff\1\11\1"+
        "\155\1\11\1\3";
    static final String DFA40_maxS =
        "\1\12\1\uffff\1\2\1\14\3\2\1\156\1\155\1\161\2\156\2\uffff\1\14"+
        "\1\155\1\11\1\156";
    static final String DFA40_acceptS =
        "\1\uffff\1\1\12\uffff\1\2\1\3\4\uffff";
    static final String DFA40_specialS =
        "\22\uffff}>";
    static final String[] DFA40_transitionS = {
            "\1\1\1\2",
            "",
            "\1\3",
            "\1\6\1\uffff\1\4\1\5",
            "\1\7",
            "\1\10",
            "\1\11",
            "\1\12",
            "\1\13",
            "\1\15\40\uffff\1\14",
            "\1\16\152\uffff\1\12",
            "\1\20\152\uffff\1\17",
            "",
            "",
            "\1\6\2\uffff\1\5",
            "\1\21",
            "\1\6",
            "\1\20\152\uffff\1\17"
    };

    static final short[] DFA40_eot = DFA.unpackEncodedString(DFA40_eotS);
    static final short[] DFA40_eof = DFA.unpackEncodedString(DFA40_eofS);
    static final char[] DFA40_min = DFA.unpackEncodedStringToUnsignedChars(DFA40_minS);
    static final char[] DFA40_max = DFA.unpackEncodedStringToUnsignedChars(DFA40_maxS);
    static final short[] DFA40_accept = DFA.unpackEncodedString(DFA40_acceptS);
    static final short[] DFA40_special = DFA.unpackEncodedString(DFA40_specialS);
    static final short[][] DFA40_transition;

    static {
        int numStates = DFA40_transitionS.length;
        DFA40_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA40_transition[i] = DFA.unpackEncodedString(DFA40_transitionS[i]);
        }
    }

    class DFA40 extends DFA {

        public DFA40(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 40;
            this.eot = DFA40_eot;
            this.eof = DFA40_eof;
            this.min = DFA40_min;
            this.max = DFA40_max;
            this.accept = DFA40_accept;
            this.special = DFA40_special;
            this.transition = DFA40_transition;
        }
        public String getDescription() {
            return "674:1: import_stmt : ( ^( Import ( dotted_as_name[nms] )+ ) | ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import STAR ) ) | ^( ImportFrom ( ^( Level dots ) )? ( ^( Name dotted_name ) )? ^( Import ( import_as_name[nms] )+ ) ) );";
        }
    }
 

    public static final BitSet FOLLOW_Module_in_module56 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_module68 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_FunctionDef_in_funcdef116 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Name_in_funcdef119 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_funcdef121 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Arguments_in_funcdef125 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_varargslist_in_funcdef127 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Body_in_funcdef132 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_funcdef134 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Decorators_in_funcdef138 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_decorators_in_funcdef140 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Args_in_varargslist172 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_defparameter_in_varargslist174 = new BitSet(new long[]{0x0000000000000008L,0x0000200100000000L});
    public static final BitSet FOLLOW_StarArgs_in_varargslist181 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_varargslist185 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_KWArgs_in_varargslist192 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_varargslist196 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_StarArgs_in_varargslist210 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_varargslist214 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_KWArgs_in_varargslist219 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_varargslist223 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_KWArgs_in_varargslist237 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_varargslist239 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_fpdef_in_defparameter260 = new BitSet(new long[]{0x0000000000000002L,0x0008000000000000L});
    public static final BitSet FOLLOW_ASSIGN_in_defparameter264 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_defparameter266 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NAME_in_fpdef294 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_FpList_in_fpdef305 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_fplist_in_fpdef307 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_fpdef_in_fplist336 = new BitSet(new long[]{0x0000000000000002L,0x0000200100000000L});
    public static final BitSet FOLLOW_decorator_in_decorators366 = new BitSet(new long[]{0x0000000000000002L,0x0000000000040000L});
    public static final BitSet FOLLOW_Decorator_in_decorator390 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dotted_attr_in_decorator392 = new BitSet(new long[]{0x0000000000000008L,0x0000000010000000L});
    public static final BitSet FOLLOW_Call_in_decorator396 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Args_in_decorator400 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_arglist_in_decorator402 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_NAME_in_dotted_attr432 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOT_in_dotted_attr443 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dotted_attr_in_dotted_attr447 = new BitSet(new long[]{0x0000000000000000L,0x0000600000000000L});
    public static final BitSet FOLLOW_dotted_attr_in_dotted_attr451 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_stmt_in_stmts484 = new BitSet(new long[]{0xE4C6FDFCF8815602L,0xFFE6410010307804L,0x00400000001FFFFFL,0x0000000000000002L});
    public static final BitSet FOLLOW_INDENT_in_stmts495 = new BitSet(new long[]{0xE4C6FDFCF8815600L,0xFFE6410010307804L,0x00400000001FFFFFL,0x0000000000000002L});
    public static final BitSet FOLLOW_stmt_in_stmts497 = new BitSet(new long[]{0xE4C6FDFCF8815620L,0xFFE6410010307804L,0x00400000001FFFFFL,0x0000000000000002L});
    public static final BitSet FOLLOW_DEDENT_in_stmts500 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_expr_stmt_in_stmt520 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_print_stmt_in_stmt528 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_del_stmt_in_stmt536 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_pass_stmt_in_stmt544 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_flow_stmt_in_stmt552 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_import_stmt_in_stmt560 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_global_stmt_in_stmt568 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_exec_stmt_in_stmt576 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_assert_stmt_in_stmt584 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_if_stmt_in_stmt592 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_while_stmt_in_stmt600 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_for_stmt_in_stmt608 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_try_stmt_in_stmt616 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_with_stmt_in_stmt624 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_funcdef_in_stmt632 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_classdef_in_stmt640 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_expr_stmt657 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_augassign_in_expr_stmt669 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_expr_stmt673 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_expr_stmt678 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Assign_in_expr_stmt691 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_targets_in_expr_stmt693 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000040L});
    public static final BitSet FOLLOW_Value_in_expr_stmt696 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_expr_stmt700 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Call_in_call_expr727 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Args_in_call_expr731 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_arglist_in_call_expr733 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_test_in_call_expr738 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_target_in_targets768 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000010L});
    public static final BitSet FOLLOW_Target_in_target791 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_target793 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_PLUSEQUAL_in_augassign818 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MINUSEQUAL_in_augassign828 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STAREQUAL_in_augassign838 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SLASHEQUAL_in_augassign848 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PERCENTEQUAL_in_augassign858 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AMPEREQUAL_in_augassign868 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_VBAREQUAL_in_augassign878 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CIRCUMFLEXEQUAL_in_augassign888 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LEFTSHIFTEQUAL_in_augassign898 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RIGHTSHIFTEQUAL_in_augassign908 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOUBLESTAREQUAL_in_augassign918 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOUBLESLASHEQUAL_in_augassign928 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PLUS_in_binop951 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_MINUS_in_binop961 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STAR_in_binop971 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SLASH_in_binop981 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_PERCENT_in_binop991 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AMPER_in_binop1001 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_VBAR_in_binop1011 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_CIRCUMFLEX_in_binop1021 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LEFTSHIFT_in_binop1031 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RIGHTSHIFT_in_binop1041 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOUBLESTAR_in_binop1051 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DOUBLESLASH_in_binop1061 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Print_in_print_stmt1082 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Dest_in_print_stmt1086 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_RIGHTSHIFT_in_print_stmt1088 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Values_in_print_stmt1095 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Elts_in_print_stmt1098 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_elts_in_print_stmt1100 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Newline_in_print_stmt1108 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Delete_in_del_stmt1131 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_elts_in_del_stmt1133 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Pass_in_pass_stmt1154 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_break_stmt_in_flow_stmt1173 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_continue_stmt_in_flow_stmt1181 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_return_stmt_in_flow_stmt1189 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_raise_stmt_in_flow_stmt1197 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Break_in_break_stmt1214 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Continue_in_continue_stmt1233 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Return_in_return_stmt1253 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Value_in_return_stmt1257 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_return_stmt1259 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Yield_in_yield_expr1288 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Value_in_yield_expr1292 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_yield_expr1294 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Raise_in_raise_stmt1319 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Type_in_raise_stmt1323 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_raise_stmt1327 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Inst_in_raise_stmt1335 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_raise_stmt1339 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Tback_in_raise_stmt1347 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_raise_stmt1351 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Import_in_import_stmt1381 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dotted_as_name_in_import_stmt1383 = new BitSet(new long[]{0x0000000000000008L,0x0000000000010000L});
    public static final BitSet FOLLOW_ImportFrom_in_import_stmt1397 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Level_in_import_stmt1401 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dots_in_import_stmt1403 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Name_in_import_stmt1410 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dotted_name_in_import_stmt1412 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Import_in_import_stmt1418 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_STAR_in_import_stmt1420 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ImportFrom_in_import_stmt1433 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Level_in_import_stmt1437 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dots_in_import_stmt1439 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Name_in_import_stmt1446 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dotted_name_in_import_stmt1448 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Import_in_import_stmt1454 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_import_as_name_in_import_stmt1456 = new BitSet(new long[]{0x0000000000000008L,0x0000000000010000L});
    public static final BitSet FOLLOW_dot_in_dots1490 = new BitSet(new long[]{0x0000000000000002L,0x0000400000000000L});
    public static final BitSet FOLLOW_DOT_in_dot1512 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Alias_in_import_as_name1532 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_import_as_name1536 = new BitSet(new long[]{0x0000000000000008L,0x0000000000020000L});
    public static final BitSet FOLLOW_Asname_in_import_as_name1540 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_import_as_name1544 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Alias_in_dotted_as_name1570 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_dotted_name_in_dotted_as_name1572 = new BitSet(new long[]{0x0000000000000008L,0x0000000000020000L});
    public static final BitSet FOLLOW_Asname_in_dotted_as_name1576 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_dotted_as_name1578 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_NAME_in_dotted_name1610 = new BitSet(new long[]{0x0000000000000002L,0x0000400000000000L});
    public static final BitSet FOLLOW_dot_name_in_dotted_name1612 = new BitSet(new long[]{0x0000000000000002L,0x0000400000000000L});
    public static final BitSet FOLLOW_DOT_in_dot_name1635 = new BitSet(new long[]{0x0000000000000000L,0x0000200000000000L});
    public static final BitSet FOLLOW_NAME_in_dot_name1637 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Global_in_global_stmt1662 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_name_expr_in_global_stmt1664 = new BitSet(new long[]{0x0000000000000008L,0x0000200000000000L});
    public static final BitSet FOLLOW_NAME_in_name_expr1687 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Exec_in_exec_stmt1707 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_exec_stmt1711 = new BitSet(new long[]{0x0300000000000008L});
    public static final BitSet FOLLOW_Globals_in_exec_stmt1716 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_exec_stmt1720 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Locals_in_exec_stmt1728 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_exec_stmt1732 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Assert_in_assert_stmt1757 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Test_in_assert_stmt1760 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_assert_stmt1764 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Msg_in_assert_stmt1770 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_assert_stmt1774 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_If_in_if_stmt1805 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_if_stmt1807 = new BitSet(new long[]{0xE4C6FDFCF8815610L,0xFFE6410010307804L,0x00400000001FFFFFL,0x0000000000000002L});
    public static final BitSet FOLLOW_stmts_in_if_stmt1812 = new BitSet(new long[]{0x0000000300000008L});
    public static final BitSet FOLLOW_elif_clause_in_if_stmt1814 = new BitSet(new long[]{0x0000000300000008L});
    public static final BitSet FOLLOW_OrElse_in_if_stmt1820 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_if_stmt1824 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Elif_in_elif_clause1849 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_elif_clause1851 = new BitSet(new long[]{0xE4C6FDFCF8815610L,0xFFE6410010307804L,0x00400000001FFFFFL,0x0000000000000002L});
    public static final BitSet FOLLOW_stmts_in_elif_clause1854 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_While_in_while_stmt1875 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_while_stmt1877 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_Body_in_while_stmt1881 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_while_stmt1885 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_OrElse_in_while_stmt1890 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_while_stmt1894 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_For_in_for_stmt1918 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Target_in_for_stmt1921 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_for_stmt1925 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Iter_in_for_stmt1930 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_for_stmt1934 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Body_in_for_stmt1939 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_for_stmt1943 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_OrElse_in_for_stmt1948 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_for_stmt1952 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_TryExcept_in_try_stmt1981 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Body_in_try_stmt1984 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_try_stmt1988 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_except_clause_in_try_stmt1991 = new BitSet(new long[]{0x0000020100000008L,0x0000008000000000L});
    public static final BitSet FOLLOW_OrElse_in_try_stmt1997 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_try_stmt2001 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_FinalBody_in_try_stmt2008 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_190_in_try_stmt2010 = new BitSet(new long[]{0xE4C6FDFCF8815610L,0xFFE6410010307804L,0x00400000001FFFFFL,0x0000000000000002L});
    public static final BitSet FOLLOW_stmts_in_try_stmt2014 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_TryFinally_in_try_stmt2029 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Body_in_try_stmt2032 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_try_stmt2036 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_FinalBody_in_try_stmt2040 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_try_stmt2044 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ExceptHandler_in_except_clause2067 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_192_in_except_clause2069 = new BitSet(new long[]{0x0008000000003000L});
    public static final BitSet FOLLOW_Type_in_except_clause2073 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_except_clause2077 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Name_in_except_clause2085 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_except_clause2089 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Body_in_except_clause2096 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_except_clause2098 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_With_in_with_stmt2120 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_with_stmt2122 = new BitSet(new long[]{0x0000000000002000L,0x0000200000000000L,0x0008000000000000L});
    public static final BitSet FOLLOW_with_var_in_with_stmt2125 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_Body_in_with_stmt2129 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_with_stmt2131 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_set_in_with_var2158 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_with_var2166 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AND_in_test2193 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2197 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_test2202 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_OR_in_test2215 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2219 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_test2224 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_comp_op_in_test2237 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2241 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_test2246 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_atom_in_test2258 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_binop_in_test2270 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2274 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_test2279 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_call_expr_in_test2291 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_lambdef_in_test2301 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IfExp_in_test2312 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Test_in_test2315 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2319 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Body_in_test2324 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2328 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_OrElse_in_test2333 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_test2337 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_yield_expr_in_test2350 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LESS_in_comp_op2373 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GREATER_in_comp_op2383 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_EQUAL_in_comp_op2393 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GREATEREQUAL_in_comp_op2403 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LESSEQUAL_in_comp_op2413 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ALT_NOTEQUAL_in_comp_op2423 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NOTEQUAL_in_comp_op2433 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_182_in_comp_op2443 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NotIn_in_comp_op2453 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_193_in_comp_op2463 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_IsNot_in_comp_op2473 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_elt_in_elts2509 = new BitSet(new long[]{0xE002F000B8001002L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_elt2531 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Tuple_in_atom2558 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Elts_in_atom2562 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_elts_in_atom2564 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_List_in_atom2580 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Elts_in_atom2584 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_elts_in_atom2586 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_comprehension_in_atom2601 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Dict_in_atom2613 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Elts_in_atom2617 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_elts_in_atom2619 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Repr_in_atom2635 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_atom2637 = new BitSet(new long[]{0xE002F000B8001008L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_Name_in_atom2651 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_atom2653 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_DOT_in_atom2665 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_atom2667 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_atom2669 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_SubscriptList_in_atom2682 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_subscriptlist_in_atom2684 = new BitSet(new long[]{0xE002F000B8001000L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_atom2686 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Num_in_atom2699 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_INT_in_atom2701 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Num_in_atom2713 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_LONGINT_in_atom2715 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Num_in_atom2727 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_FLOAT_in_atom2729 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Num_in_atom2741 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_COMPLEX_in_atom2743 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_stringlist_in_atom2754 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_USub_in_atom2765 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_atom2767 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_UAdd_in_atom2780 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_atom2782 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Invert_in_atom2795 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_atom2797 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_NOT_in_atom2810 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_atom2812 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Parens_in_atom2825 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_atom2827 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ListComp_in_comprehension2859 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_comprehension2861 = new BitSet(new long[]{0x0000000000000000L,0x0000002000000000L});
    public static final BitSet FOLLOW_list_for_in_comprehension2864 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_GeneratorExp_in_comprehension2877 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_comprehension2879 = new BitSet(new long[]{0x0000000000000000L,0x0000000800000000L});
    public static final BitSet FOLLOW_gen_for_in_comprehension2882 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Str_in_stringlist2913 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_string_in_stringlist2915 = new BitSet(new long[]{0x0000000000000008L,0x0000000000000000L,0x0000000080000000L});
    public static final BitSet FOLLOW_STRING_in_string2938 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Lambda_in_lambdef2962 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_varargslist_in_lambdef2964 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_Body_in_lambdef2968 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_lambdef2970 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_subscript_in_subscriptlist3003 = new BitSet(new long[]{0x0800000000000002L,0x000000000000000AL});
    public static final BitSet FOLLOW_Ellipsis_in_subscript3026 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Index_in_subscript3037 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_subscript3039 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Subscript_in_subscript3052 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Lower_in_subscript3056 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_subscript3060 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Upper_in_subscript3078 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_COLON_in_subscript3080 = new BitSet(new long[]{0x0000000000000008L,0x0000000400000000L});
    public static final BitSet FOLLOW_UpperOp_in_subscript3084 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_subscript3088 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Step_in_subscript3099 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_COLON_in_subscript3101 = new BitSet(new long[]{0x0000000000000008L,0x0000000200000000L});
    public static final BitSet FOLLOW_StepOp_in_subscript3105 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_subscript3109 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ClassDef_in_classdef3143 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Name_in_classdef3146 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_NAME_in_classdef3150 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Bases_in_classdef3155 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_bases_in_classdef3157 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Body_in_classdef3163 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_stmts_in_classdef3165 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_base_in_bases3195 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_test_in_base3220 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_Args_in_arglist3250 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_argument_in_arglist3252 = new BitSet(new long[]{0x0000000000180008L,0x0000000800000000L});
    public static final BitSet FOLLOW_keyword_in_arglist3256 = new BitSet(new long[]{0x0000000000100008L});
    public static final BitSet FOLLOW_StarArgs_in_arglist3263 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_arglist3267 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_KWArgs_in_arglist3275 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_arglist3279 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_StarArgs_in_arglist3294 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_arglist3298 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_KWArgs_in_arglist3304 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_arglist3308 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_KWArgs_in_arglist3323 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_arglist3325 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Arg_in_argument3353 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_argument3355 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_GenFor_in_argument3368 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_argument3370 = new BitSet(new long[]{0x0000000000000000L,0x0000000800000000L});
    public static final BitSet FOLLOW_gen_for_in_argument3373 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Keyword_in_keyword3396 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Arg_in_keyword3399 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_keyword3403 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Value_in_keyword3408 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_keyword3412 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_list_for_in_list_iter3440 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_list_if_in_list_iter3449 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ListFor_in_list_for3476 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Target_in_list_for3479 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_list_for3483 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Iter_in_list_for3488 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_list_for3492 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Ifs_in_list_for3498 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_list_iter_in_list_for3500 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_ListIf_in_list_if3530 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Target_in_list_if3533 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_list_if3535 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Ifs_in_list_if3540 = new BitSet(new long[]{0x0000000000000000L,0x0000006000000000L});
    public static final BitSet FOLLOW_list_iter_in_list_if3542 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_gen_for_in_gen_iter3571 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_gen_if_in_gen_iter3580 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_GenFor_in_gen_for3603 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Target_in_gen_for3606 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_gen_for3610 = new BitSet(new long[]{0xE002F000B8001008L,0x0006410010203804L,0x00400000001FFFFEL,0x0000000000000002L});
    public static final BitSet FOLLOW_Iter_in_gen_for3616 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_gen_for3620 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Ifs_in_gen_for3626 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_gen_iter_in_gen_for3628 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_GenIf_in_gen_if3658 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_Target_in_gen_if3661 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_test_in_gen_if3663 = new BitSet(new long[]{0x0000000000000008L});
    public static final BitSet FOLLOW_Ifs_in_gen_if3669 = new BitSet(new long[]{0x0000000000000004L});
    public static final BitSet FOLLOW_gen_iter_in_gen_if3671 = new BitSet(new long[]{0x0000000000000008L});

}