// $ANTLR 3.0.1 /Users/frank/tmp/trunk/jython/grammar/Python.g 2008-03-19 16:53:22
 
package org.python.antlr;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class PythonLexer extends Lexer {
    public static final int Dict=29;
    public static final int Args=18;
    public static final int MINUS=145;
    public static final int T170=170;
    public static final int DEDENT=5;
    public static final int T190=190;
    public static final int Targets=69;
    public static final int T194=194;
    public static final int TRIQUOTE=163;
    public static final int Delete=78;
    public static final int Tback=53;
    public static final int COMPLEX=158;
    public static final int T193=193;
    public static final int TILDE=149;
    public static final int Locals=57;
    public static final int DOT=110;
    public static final int NEWLINE=105;
    public static final int Index=67;
    public static final int Invert=77;
    public static final int PLUSEQUAL=117;
    public static final int RIGHTSHIFTEQUAL=126;
    public static final int Compare=25;
    public static final int LCURLY=152;
    public static final int StarArgs=21;
    public static final int T186=186;
    public static final int RPAREN=108;
    public static final int TryExcept=39;
    public static final int ClassDef=14;
    public static final int Assert=58;
    public static final int T191=191;
    public static final int WS=166;
    public static final int STRING=159;
    public static final int In=48;
    public static final int T184=184;
    public static final int SEMI=116;
    public static final int Values=94;
    public static final int Test=7;
    public static final int Asname=81;
    public static final int Newline=95;
    public static final int Body=13;
    public static final int COLON=111;
    public static final int AMPER=142;
    public static final int T185=185;
    public static final int DOUBLESTAREQUAL=127;
    public static final int NotIn=49;
    public static final int PERCENT=147;
    public static final int Iter=87;
    public static final int Inst=52;
    public static final int FLOAT=157;
    public static final int DOUBLESTAR=114;
    public static final int GenFor=99;
    public static final int Attr=91;
    public static final int Arg=19;
    public static final int Num=46;
    public static final int OR=130;
    public static final int Ifs=88;
    public static final int CIRCUMFLEX=141;
    public static final int Module=6;
    public static final int LESS=133;
    public static final int TryFinally=40;
    public static final int LONGINT=156;
    public static final int INT=155;
    public static final int LPAREN=107;
    public static final int AugAssign=24;
    public static final int Alias=80;
    public static final int IfExp=31;
    public static final int T174=174;
    public static final int Bases=15;
    public static final int Tuple=27;
    public static final int T181=181;
    public static final int Repr=63;
    public static final int ImportFrom=10;
    public static final int COMMENT=168;
    public static final int T182=182;
    public static final int Elif=33;
    public static final int T195=195;
    public static final int Global=54;
    public static final int T180=180;
    public static final int StepOp=97;
    public static final int PERCENTEQUAL=121;
    public static final int While=34;
    public static final int Tokens=197;
    public static final int RBRACK=151;
    public static final int GREATEREQUAL=136;
    public static final int FunctionDef=16;
    public static final int DOUBLESLASH=148;
    public static final int Globals=56;
    public static final int NOT=132;
    public static final int Lower=71;
    public static final int Return=43;
    public static final int COMMA=112;
    public static final int Str=45;
    public static final int T183=183;
    public static final int Ctx=90;
    public static final int T175=175;
    public static final int T196=196;
    public static final int Elts=89;
    public static final int Ellipsis=59;
    public static final int ListFor=101;
    public static final int DOUBLESLASHEQUAL=128;
    public static final int Default=79;
    public static final int AND=131;
    public static final int UpperOp=98;
    public static final int T173=173;
    public static final int Exec=55;
    public static final int UnaryOp=74;
    public static final int T176=176;
    public static final int FinalBody=103;
    public static final int IsNot=47;
    public static final int PLUS=144;
    public static final int Expr=26;
    public static final int T178=178;
    public static final int Print=38;
    public static final int Decorator=82;
    public static final int AT=106;
    public static final int Subscript=65;
    public static final int Type=51;
    public static final int Continue=37;
    public static final int Raise=50;
    public static final int T177=177;
    public static final int Pass=35;
    public static final int List=28;
    public static final int Import=9;
    public static final int Msg=8;
    public static final int T189=189;
    public static final int Upper=72;
    public static final int Name=12;
    public static final int LBRACK=150;
    public static final int Call=92;
    public static final int Keyword=20;
    public static final int EQUAL=135;
    public static final int USub=76;
    public static final int LESSEQUAL=137;
    public static final int Step=73;
    public static final int ALT_NOTEQUAL=138;
    public static final int Dest=93;
    public static final int NAME=109;
    public static final int Lambda=62;
    public static final int T188=188;
    public static final int For=42;
    public static final int Level=11;
    public static final int T179=179;
    public static final int Assign=23;
    public static final int T172=172;
    public static final int FpList=96;
    public static final int ListIf=102;
    public static final int If=30;
    public static final int BinOp=64;
    public static final int Break=36;
    public static final int SLASHEQUAL=120;
    public static final int KWArgs=22;
    public static final int NOTEQUAL=139;
    public static final int Decorators=83;
    public static final int RCURLY=153;
    public static final int T187=187;
    public static final int GeneratorExp=85;
    public static final int ExceptHandler=41;
    public static final int OrElse=32;
    public static final int UAdd=75;
    public static final int LEADING_WS=167;
    public static final int ASSIGN=115;
    public static final int Arguments=17;
    public static final int VBAR=140;
    public static final int GREATER=134;
    public static final int SubscriptList=66;
    public static final int BACKQUOTE=154;
    public static final int Yield=44;
    public static final int T171=171;
    public static final int CONTINUED_LINE=165;
    public static final int Parens=104;
    public static final int Exponent=161;
    public static final int DIGITS=160;
    public static final int SLASH=146;
    public static final int T192=192;
    public static final int T169=169;
    public static final int TRIAPOS=162;
    public static final int AMPEREQUAL=122;
    public static final int ESC=164;
    public static final int GenIf=100;
    public static final int Target=68;
    public static final int With=84;
    public static final int RIGHTSHIFT=129;
    public static final int MINUSEQUAL=118;
    public static final int LEFTSHIFTEQUAL=125;
    public static final int EOF=-1;
    public static final int CIRCUMFLEXEQUAL=124;
    public static final int INDENT=4;
    public static final int Value=70;
    public static final int Comprehension=60;
    public static final int ListComp=61;
    public static final int VBAREQUAL=123;
    public static final int STAREQUAL=119;
    public static final int STAR=113;
    public static final int LEFTSHIFT=143;
    public static final int Id=86;

    /** Handles context-sensitive lexing of implicit line joining such as
     *  the case where newline is ignored in cases like this:
     *  a = [3,
     *       4]
     */
    int implicitLineJoiningLevel = 0;
    int startPos=-1;

    public PythonLexer() {;} 
    public PythonLexer(CharStream input) {
        super(input);
    }
    public String getGrammarFileName() { return "/Users/frank/tmp/trunk/jython/grammar/Python.g"; }

    // $ANTLR start T169
    public final void mT169() throws RecognitionException {
        try {
            int _type = T169;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:15:6: ( 'def' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:15:8: 'def'
            {
            match("def"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T169

    // $ANTLR start T170
    public final void mT170() throws RecognitionException {
        try {
            int _type = T170;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:16:6: ( 'print' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:16:8: 'print'
            {
            match("print"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T170

    // $ANTLR start T171
    public final void mT171() throws RecognitionException {
        try {
            int _type = T171;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:17:6: ( 'del' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:17:8: 'del'
            {
            match("del"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T171

    // $ANTLR start T172
    public final void mT172() throws RecognitionException {
        try {
            int _type = T172;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:18:6: ( 'pass' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:18:8: 'pass'
            {
            match("pass"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T172

    // $ANTLR start T173
    public final void mT173() throws RecognitionException {
        try {
            int _type = T173;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:19:6: ( 'break' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:19:8: 'break'
            {
            match("break"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T173

    // $ANTLR start T174
    public final void mT174() throws RecognitionException {
        try {
            int _type = T174;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:20:6: ( 'continue' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:20:8: 'continue'
            {
            match("continue"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T174

    // $ANTLR start T175
    public final void mT175() throws RecognitionException {
        try {
            int _type = T175;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:21:6: ( 'return' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:21:8: 'return'
            {
            match("return"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T175

    // $ANTLR start T176
    public final void mT176() throws RecognitionException {
        try {
            int _type = T176;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:22:6: ( 'raise' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:22:8: 'raise'
            {
            match("raise"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T176

    // $ANTLR start T177
    public final void mT177() throws RecognitionException {
        try {
            int _type = T177;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:23:6: ( 'import' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:23:8: 'import'
            {
            match("import"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T177

    // $ANTLR start T178
    public final void mT178() throws RecognitionException {
        try {
            int _type = T178;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:24:6: ( 'from' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:24:8: 'from'
            {
            match("from"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T178

    // $ANTLR start T179
    public final void mT179() throws RecognitionException {
        try {
            int _type = T179;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:25:6: ( 'as' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:25:8: 'as'
            {
            match("as"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T179

    // $ANTLR start T180
    public final void mT180() throws RecognitionException {
        try {
            int _type = T180;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:26:6: ( 'global' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:26:8: 'global'
            {
            match("global"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T180

    // $ANTLR start T181
    public final void mT181() throws RecognitionException {
        try {
            int _type = T181;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:27:6: ( 'exec' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:27:8: 'exec'
            {
            match("exec"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T181

    // $ANTLR start T182
    public final void mT182() throws RecognitionException {
        try {
            int _type = T182;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:28:6: ( 'in' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:28:8: 'in'
            {
            match("in"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T182

    // $ANTLR start T183
    public final void mT183() throws RecognitionException {
        try {
            int _type = T183;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:29:6: ( 'assert' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:29:8: 'assert'
            {
            match("assert"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T183

    // $ANTLR start T184
    public final void mT184() throws RecognitionException {
        try {
            int _type = T184;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:30:6: ( 'if' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:30:8: 'if'
            {
            match("if"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T184

    // $ANTLR start T185
    public final void mT185() throws RecognitionException {
        try {
            int _type = T185;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:31:6: ( 'else' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:31:8: 'else'
            {
            match("else"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T185

    // $ANTLR start T186
    public final void mT186() throws RecognitionException {
        try {
            int _type = T186;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:32:6: ( 'elif' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:32:8: 'elif'
            {
            match("elif"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T186

    // $ANTLR start T187
    public final void mT187() throws RecognitionException {
        try {
            int _type = T187;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:33:6: ( 'while' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:33:8: 'while'
            {
            match("while"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T187

    // $ANTLR start T188
    public final void mT188() throws RecognitionException {
        try {
            int _type = T188;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:34:6: ( 'for' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:34:8: 'for'
            {
            match("for"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T188

    // $ANTLR start T189
    public final void mT189() throws RecognitionException {
        try {
            int _type = T189;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:35:6: ( 'try' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:35:8: 'try'
            {
            match("try"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T189

    // $ANTLR start T190
    public final void mT190() throws RecognitionException {
        try {
            int _type = T190;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:36:6: ( 'finally' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:36:8: 'finally'
            {
            match("finally"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T190

    // $ANTLR start T191
    public final void mT191() throws RecognitionException {
        try {
            int _type = T191;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:37:6: ( 'with' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:37:8: 'with'
            {
            match("with"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T191

    // $ANTLR start T192
    public final void mT192() throws RecognitionException {
        try {
            int _type = T192;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:38:6: ( 'except' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:38:8: 'except'
            {
            match("except"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T192

    // $ANTLR start T193
    public final void mT193() throws RecognitionException {
        try {
            int _type = T193;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:39:6: ( 'is' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:39:8: 'is'
            {
            match("is"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T193

    // $ANTLR start T194
    public final void mT194() throws RecognitionException {
        try {
            int _type = T194;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:40:6: ( 'lambda' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:40:8: 'lambda'
            {
            match("lambda"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T194

    // $ANTLR start T195
    public final void mT195() throws RecognitionException {
        try {
            int _type = T195;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:41:6: ( 'class' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:41:8: 'class'
            {
            match("class"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T195

    // $ANTLR start T196
    public final void mT196() throws RecognitionException {
        try {
            int _type = T196;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:42:6: ( 'yield' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:42:8: 'yield'
            {
            match("yield"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end T196

    // $ANTLR start LPAREN
    public final void mLPAREN() throws RecognitionException {
        try {
            int _type = LPAREN;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:820:11: ( '(' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:820:13: '('
            {
            match('('); 
            implicitLineJoiningLevel++;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LPAREN

    // $ANTLR start RPAREN
    public final void mRPAREN() throws RecognitionException {
        try {
            int _type = RPAREN;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:822:11: ( ')' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:822:13: ')'
            {
            match(')'); 
            implicitLineJoiningLevel--;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RPAREN

    // $ANTLR start LBRACK
    public final void mLBRACK() throws RecognitionException {
        try {
            int _type = LBRACK;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:824:11: ( '[' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:824:13: '['
            {
            match('['); 
            implicitLineJoiningLevel++;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LBRACK

    // $ANTLR start RBRACK
    public final void mRBRACK() throws RecognitionException {
        try {
            int _type = RBRACK;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:826:11: ( ']' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:826:13: ']'
            {
            match(']'); 
            implicitLineJoiningLevel--;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RBRACK

    // $ANTLR start COLON
    public final void mCOLON() throws RecognitionException {
        try {
            int _type = COLON;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:828:11: ( ':' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:828:13: ':'
            {
            match(':'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COLON

    // $ANTLR start COMMA
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:830:10: ( ',' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:830:12: ','
            {
            match(','); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COMMA

    // $ANTLR start SEMI
    public final void mSEMI() throws RecognitionException {
        try {
            int _type = SEMI;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:832:9: ( ';' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:832:11: ';'
            {
            match(';'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SEMI

    // $ANTLR start PLUS
    public final void mPLUS() throws RecognitionException {
        try {
            int _type = PLUS;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:834:9: ( '+' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:834:11: '+'
            {
            match('+'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PLUS

    // $ANTLR start MINUS
    public final void mMINUS() throws RecognitionException {
        try {
            int _type = MINUS;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:836:10: ( '-' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:836:12: '-'
            {
            match('-'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end MINUS

    // $ANTLR start STAR
    public final void mSTAR() throws RecognitionException {
        try {
            int _type = STAR;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:838:9: ( '*' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:838:11: '*'
            {
            match('*'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end STAR

    // $ANTLR start SLASH
    public final void mSLASH() throws RecognitionException {
        try {
            int _type = SLASH;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:840:10: ( '/' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:840:12: '/'
            {
            match('/'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SLASH

    // $ANTLR start VBAR
    public final void mVBAR() throws RecognitionException {
        try {
            int _type = VBAR;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:842:9: ( '|' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:842:11: '|'
            {
            match('|'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end VBAR

    // $ANTLR start AMPER
    public final void mAMPER() throws RecognitionException {
        try {
            int _type = AMPER;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:844:10: ( '&' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:844:12: '&'
            {
            match('&'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end AMPER

    // $ANTLR start LESS
    public final void mLESS() throws RecognitionException {
        try {
            int _type = LESS;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:846:9: ( '<' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:846:11: '<'
            {
            match('<'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LESS

    // $ANTLR start GREATER
    public final void mGREATER() throws RecognitionException {
        try {
            int _type = GREATER;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:848:12: ( '>' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:848:14: '>'
            {
            match('>'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end GREATER

    // $ANTLR start ASSIGN
    public final void mASSIGN() throws RecognitionException {
        try {
            int _type = ASSIGN;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:850:11: ( '=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:850:13: '='
            {
            match('='); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ASSIGN

    // $ANTLR start PERCENT
    public final void mPERCENT() throws RecognitionException {
        try {
            int _type = PERCENT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:852:12: ( '%' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:852:14: '%'
            {
            match('%'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PERCENT

    // $ANTLR start BACKQUOTE
    public final void mBACKQUOTE() throws RecognitionException {
        try {
            int _type = BACKQUOTE;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:854:14: ( '`' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:854:16: '`'
            {
            match('`'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end BACKQUOTE

    // $ANTLR start LCURLY
    public final void mLCURLY() throws RecognitionException {
        try {
            int _type = LCURLY;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:856:11: ( '{' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:856:13: '{'
            {
            match('{'); 
            implicitLineJoiningLevel++;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LCURLY

    // $ANTLR start RCURLY
    public final void mRCURLY() throws RecognitionException {
        try {
            int _type = RCURLY;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:858:11: ( '}' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:858:13: '}'
            {
            match('}'); 
            implicitLineJoiningLevel--;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RCURLY

    // $ANTLR start CIRCUMFLEX
    public final void mCIRCUMFLEX() throws RecognitionException {
        try {
            int _type = CIRCUMFLEX;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:860:15: ( '^' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:860:17: '^'
            {
            match('^'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end CIRCUMFLEX

    // $ANTLR start TILDE
    public final void mTILDE() throws RecognitionException {
        try {
            int _type = TILDE;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:862:10: ( '~' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:862:12: '~'
            {
            match('~'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end TILDE

    // $ANTLR start EQUAL
    public final void mEQUAL() throws RecognitionException {
        try {
            int _type = EQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:864:10: ( '==' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:864:12: '=='
            {
            match("=="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end EQUAL

    // $ANTLR start NOTEQUAL
    public final void mNOTEQUAL() throws RecognitionException {
        try {
            int _type = NOTEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:866:13: ( '!=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:866:15: '!='
            {
            match("!="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NOTEQUAL

    // $ANTLR start ALT_NOTEQUAL
    public final void mALT_NOTEQUAL() throws RecognitionException {
        try {
            int _type = ALT_NOTEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:868:13: ( '<>' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:868:15: '<>'
            {
            match("<>"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end ALT_NOTEQUAL

    // $ANTLR start LESSEQUAL
    public final void mLESSEQUAL() throws RecognitionException {
        try {
            int _type = LESSEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:870:14: ( '<=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:870:16: '<='
            {
            match("<="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LESSEQUAL

    // $ANTLR start LEFTSHIFT
    public final void mLEFTSHIFT() throws RecognitionException {
        try {
            int _type = LEFTSHIFT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:872:14: ( '<<' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:872:16: '<<'
            {
            match("<<"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LEFTSHIFT

    // $ANTLR start GREATEREQUAL
    public final void mGREATEREQUAL() throws RecognitionException {
        try {
            int _type = GREATEREQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:874:17: ( '>=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:874:19: '>='
            {
            match(">="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end GREATEREQUAL

    // $ANTLR start RIGHTSHIFT
    public final void mRIGHTSHIFT() throws RecognitionException {
        try {
            int _type = RIGHTSHIFT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:876:15: ( '>>' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:876:17: '>>'
            {
            match(">>"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RIGHTSHIFT

    // $ANTLR start PLUSEQUAL
    public final void mPLUSEQUAL() throws RecognitionException {
        try {
            int _type = PLUSEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:878:14: ( '+=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:878:16: '+='
            {
            match("+="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PLUSEQUAL

    // $ANTLR start MINUSEQUAL
    public final void mMINUSEQUAL() throws RecognitionException {
        try {
            int _type = MINUSEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:880:15: ( '-=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:880:17: '-='
            {
            match("-="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end MINUSEQUAL

    // $ANTLR start DOUBLESTAR
    public final void mDOUBLESTAR() throws RecognitionException {
        try {
            int _type = DOUBLESTAR;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:882:15: ( '**' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:882:17: '**'
            {
            match("**"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOUBLESTAR

    // $ANTLR start STAREQUAL
    public final void mSTAREQUAL() throws RecognitionException {
        try {
            int _type = STAREQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:884:14: ( '*=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:884:16: '*='
            {
            match("*="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end STAREQUAL

    // $ANTLR start DOUBLESLASH
    public final void mDOUBLESLASH() throws RecognitionException {
        try {
            int _type = DOUBLESLASH;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:886:16: ( '//' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:886:18: '//'
            {
            match("//"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOUBLESLASH

    // $ANTLR start SLASHEQUAL
    public final void mSLASHEQUAL() throws RecognitionException {
        try {
            int _type = SLASHEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:888:15: ( '/=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:888:17: '/='
            {
            match("/="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end SLASHEQUAL

    // $ANTLR start VBAREQUAL
    public final void mVBAREQUAL() throws RecognitionException {
        try {
            int _type = VBAREQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:890:14: ( '|=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:890:16: '|='
            {
            match("|="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end VBAREQUAL

    // $ANTLR start PERCENTEQUAL
    public final void mPERCENTEQUAL() throws RecognitionException {
        try {
            int _type = PERCENTEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:892:17: ( '%=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:892:19: '%='
            {
            match("%="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end PERCENTEQUAL

    // $ANTLR start AMPEREQUAL
    public final void mAMPEREQUAL() throws RecognitionException {
        try {
            int _type = AMPEREQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:894:15: ( '&=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:894:17: '&='
            {
            match("&="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end AMPEREQUAL

    // $ANTLR start CIRCUMFLEXEQUAL
    public final void mCIRCUMFLEXEQUAL() throws RecognitionException {
        try {
            int _type = CIRCUMFLEXEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:896:20: ( '^=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:896:22: '^='
            {
            match("^="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end CIRCUMFLEXEQUAL

    // $ANTLR start LEFTSHIFTEQUAL
    public final void mLEFTSHIFTEQUAL() throws RecognitionException {
        try {
            int _type = LEFTSHIFTEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:898:19: ( '<<=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:898:21: '<<='
            {
            match("<<="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LEFTSHIFTEQUAL

    // $ANTLR start RIGHTSHIFTEQUAL
    public final void mRIGHTSHIFTEQUAL() throws RecognitionException {
        try {
            int _type = RIGHTSHIFTEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:900:20: ( '>>=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:900:22: '>>='
            {
            match(">>="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end RIGHTSHIFTEQUAL

    // $ANTLR start DOUBLESTAREQUAL
    public final void mDOUBLESTAREQUAL() throws RecognitionException {
        try {
            int _type = DOUBLESTAREQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:902:20: ( '**=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:902:22: '**='
            {
            match("**="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOUBLESTAREQUAL

    // $ANTLR start DOUBLESLASHEQUAL
    public final void mDOUBLESLASHEQUAL() throws RecognitionException {
        try {
            int _type = DOUBLESLASHEQUAL;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:904:21: ( '//=' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:904:23: '//='
            {
            match("//="); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOUBLESLASHEQUAL

    // $ANTLR start DOT
    public final void mDOT() throws RecognitionException {
        try {
            int _type = DOT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:906:5: ( '.' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:906:7: '.'
            {
            match('.'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end DOT

    // $ANTLR start AT
    public final void mAT() throws RecognitionException {
        try {
            int _type = AT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:908:4: ( '@' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:908:6: '@'
            {
            match('@'); 

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end AT

    // $ANTLR start AND
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:910:5: ( 'and' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:910:7: 'and'
            {
            match("and"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end AND

    // $ANTLR start OR
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:912:4: ( 'or' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:912:6: 'or'
            {
            match("or"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end OR

    // $ANTLR start NOT
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:914:5: ( 'not' )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:914:7: 'not'
            {
            match("not"); 


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NOT

    // $ANTLR start FLOAT
    public final void mFLOAT() throws RecognitionException {
        try {
            int _type = FLOAT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:917:5: ( '.' DIGITS ( Exponent )? | DIGITS '.' Exponent | DIGITS ( '.' ( DIGITS ( Exponent )? )? | Exponent ) )
            int alt5=3;
            alt5 = dfa5.predict(input);
            switch (alt5) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:917:9: '.' DIGITS ( Exponent )?
                    {
                    match('.'); 
                    mDIGITS(); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:917:20: ( Exponent )?
                    int alt1=2;
                    int LA1_0 = input.LA(1);

                    if ( (LA1_0=='E'||LA1_0=='e') ) {
                        alt1=1;
                    }
                    switch (alt1) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/Python.g:917:21: Exponent
                            {
                            mExponent(); 

                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:918:9: DIGITS '.' Exponent
                    {
                    mDIGITS(); 
                    match('.'); 
                    mExponent(); 

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:9: DIGITS ( '.' ( DIGITS ( Exponent )? )? | Exponent )
                    {
                    mDIGITS(); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:16: ( '.' ( DIGITS ( Exponent )? )? | Exponent )
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0=='.') ) {
                        alt4=1;
                    }
                    else if ( (LA4_0=='E'||LA4_0=='e') ) {
                        alt4=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("919:16: ( '.' ( DIGITS ( Exponent )? )? | Exponent )", 4, 0, input);

                        throw nvae;
                    }
                    switch (alt4) {
                        case 1 :
                            // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:17: '.' ( DIGITS ( Exponent )? )?
                            {
                            match('.'); 
                            // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:21: ( DIGITS ( Exponent )? )?
                            int alt3=2;
                            int LA3_0 = input.LA(1);

                            if ( ((LA3_0>='0' && LA3_0<='9')) ) {
                                alt3=1;
                            }
                            switch (alt3) {
                                case 1 :
                                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:22: DIGITS ( Exponent )?
                                    {
                                    mDIGITS(); 
                                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:29: ( Exponent )?
                                    int alt2=2;
                                    int LA2_0 = input.LA(1);

                                    if ( (LA2_0=='E'||LA2_0=='e') ) {
                                        alt2=1;
                                    }
                                    switch (alt2) {
                                        case 1 :
                                            // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:30: Exponent
                                            {
                                            mExponent(); 

                                            }
                                            break;

                                    }


                                    }
                                    break;

                            }


                            }
                            break;
                        case 2 :
                            // /Users/frank/tmp/trunk/jython/grammar/Python.g:919:45: Exponent
                            {
                            mExponent(); 

                            }
                            break;

                    }


                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end FLOAT

    // $ANTLR start LONGINT
    public final void mLONGINT() throws RecognitionException {
        try {
            int _type = LONGINT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:923:5: ( INT ( 'l' | 'L' ) )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:923:9: INT ( 'l' | 'L' )
            {
            mINT(); 
            if ( input.LA(1)=='L'||input.LA(1)=='l' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LONGINT

    // $ANTLR start Exponent
    public final void mExponent() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:928:5: ( ( 'e' | 'E' ) ( '+' | '-' )? DIGITS )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:928:10: ( 'e' | 'E' ) ( '+' | '-' )? DIGITS
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:928:22: ( '+' | '-' )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0=='+'||LA6_0=='-') ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recover(mse);    throw mse;
                    }


                    }
                    break;

            }

            mDIGITS(); 

            }

        }
        finally {
        }
    }
    // $ANTLR end Exponent

    // $ANTLR start INT
    public final void mINT() throws RecognitionException {
        try {
            int _type = INT;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:931:5: ( '0' ( 'x' | 'X' ) ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+ | '0' ( DIGITS )* | '1' .. '9' ( DIGITS )* )
            int alt10=3;
            int LA10_0 = input.LA(1);

            if ( (LA10_0=='0') ) {
                int LA10_1 = input.LA(2);

                if ( (LA10_1=='X'||LA10_1=='x') ) {
                    alt10=1;
                }
                else {
                    alt10=2;}
            }
            else if ( ((LA10_0>='1' && LA10_0<='9')) ) {
                alt10=3;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("931:1: INT : ( '0' ( 'x' | 'X' ) ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+ | '0' ( DIGITS )* | '1' .. '9' ( DIGITS )* );", 10, 0, input);

                throw nvae;
            }
            switch (alt10) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:932:9: '0' ( 'x' | 'X' ) ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
                    {
                    match('0'); 
                    if ( input.LA(1)=='X'||input.LA(1)=='x' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recover(mse);    throw mse;
                    }

                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:932:25: ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
                    int cnt7=0;
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( ((LA7_0>='0' && LA7_0<='9')||(LA7_0>='A' && LA7_0<='F')||(LA7_0>='a' && LA7_0<='f')) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
                    	    {
                    	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='F')||(input.LA(1)>='a' && input.LA(1)<='f') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt7 >= 1 ) break loop7;
                                EarlyExitException eee =
                                    new EarlyExitException(7, input);
                                throw eee;
                        }
                        cnt7++;
                    } while (true);


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:934:9: '0' ( DIGITS )*
                    {
                    match('0'); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:934:13: ( DIGITS )*
                    loop8:
                    do {
                        int alt8=2;
                        int LA8_0 = input.LA(1);

                        if ( ((LA8_0>='0' && LA8_0<='9')) ) {
                            alt8=1;
                        }


                        switch (alt8) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:934:13: DIGITS
                    	    {
                    	    mDIGITS(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop8;
                        }
                    } while (true);


                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:935:9: '1' .. '9' ( DIGITS )*
                    {
                    matchRange('1','9'); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:935:18: ( DIGITS )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( ((LA9_0>='0' && LA9_0<='9')) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:935:18: DIGITS
                    	    {
                    	    mDIGITS(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);


                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end INT

    // $ANTLR start COMPLEX
    public final void mCOMPLEX() throws RecognitionException {
        try {
            int _type = COMPLEX;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:939:5: ( INT ( 'j' | 'J' ) | FLOAT ( 'j' | 'J' ) )
            int alt11=2;
            alt11 = dfa11.predict(input);
            switch (alt11) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:939:9: INT ( 'j' | 'J' )
                    {
                    mINT(); 
                    if ( input.LA(1)=='J'||input.LA(1)=='j' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recover(mse);    throw mse;
                    }


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:940:9: FLOAT ( 'j' | 'J' )
                    {
                    mFLOAT(); 
                    if ( input.LA(1)=='J'||input.LA(1)=='j' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse =
                            new MismatchedSetException(null,input);
                        recover(mse);    throw mse;
                    }


                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COMPLEX

    // $ANTLR start DIGITS
    public final void mDIGITS() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:944:8: ( ( '0' .. '9' )+ )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:944:10: ( '0' .. '9' )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:944:10: ( '0' .. '9' )+
            int cnt12=0;
            loop12:
            do {
                int alt12=2;
                int LA12_0 = input.LA(1);

                if ( ((LA12_0>='0' && LA12_0<='9')) ) {
                    alt12=1;
                }


                switch (alt12) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:944:12: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

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


            }

        }
        finally {
        }
    }
    // $ANTLR end DIGITS

    // $ANTLR start NAME
    public final void mNAME() throws RecognitionException {
        try {
            int _type = NAME;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:946:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )* )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:946:10: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse =
                    new MismatchedSetException(null,input);
                recover(mse);    throw mse;
            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:947:9: ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
            loop13:
            do {
                int alt13=2;
                int LA13_0 = input.LA(1);

                if ( ((LA13_0>='0' && LA13_0<='9')||(LA13_0>='A' && LA13_0<='Z')||LA13_0=='_'||(LA13_0>='a' && LA13_0<='z')) ) {
                    alt13=1;
                }


                switch (alt13) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop13;
                }
            } while (true);


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NAME

    // $ANTLR start STRING
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:954:5: ( ( 'r' | 'u' | 'ur' )? ( '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\'' | '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"' | '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"' | '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\'' ) )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:954:9: ( 'r' | 'u' | 'ur' )? ( '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\'' | '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"' | '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"' | '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\'' )
            {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:954:9: ( 'r' | 'u' | 'ur' )?
            int alt14=4;
            int LA14_0 = input.LA(1);

            if ( (LA14_0=='r') ) {
                alt14=1;
            }
            else if ( (LA14_0=='u') ) {
                int LA14_2 = input.LA(2);

                if ( (LA14_2=='r') ) {
                    alt14=3;
                }
                else if ( (LA14_2=='\"'||LA14_2=='\'') ) {
                    alt14=2;
                }
            }
            switch (alt14) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:954:10: 'r'
                    {
                    match('r'); 

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:954:14: 'u'
                    {
                    match('u'); 

                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:954:18: 'ur'
                    {
                    match("ur"); 


                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:955:9: ( '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\'' | '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"' | '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"' | '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\'' )
            int alt19=4;
            int LA19_0 = input.LA(1);

            if ( (LA19_0=='\'') ) {
                int LA19_1 = input.LA(2);

                if ( (LA19_1=='\'') ) {
                    int LA19_3 = input.LA(3);

                    if ( (LA19_3=='\'') ) {
                        alt19=1;
                    }
                    else {
                        alt19=4;}
                }
                else if ( ((LA19_1>='\u0000' && LA19_1<='\t')||(LA19_1>='\u000B' && LA19_1<='&')||(LA19_1>='(' && LA19_1<='\uFFFE')) ) {
                    alt19=4;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("955:9: ( '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\'' | '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"' | '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"' | '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\'' )", 19, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA19_0=='\"') ) {
                int LA19_2 = input.LA(2);

                if ( (LA19_2=='\"') ) {
                    int LA19_5 = input.LA(3);

                    if ( (LA19_5=='\"') ) {
                        alt19=2;
                    }
                    else {
                        alt19=3;}
                }
                else if ( ((LA19_2>='\u0000' && LA19_2<='\t')||(LA19_2>='\u000B' && LA19_2<='!')||(LA19_2>='#' && LA19_2<='\uFFFE')) ) {
                    alt19=3;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("955:9: ( '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\'' | '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"' | '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"' | '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\'' )", 19, 2, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("955:9: ( '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\'' | '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"' | '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"' | '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\'' )", 19, 0, input);

                throw nvae;
            }
            switch (alt19) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:955:13: '\\'\\'\\'' ( options {greedy=false; } : TRIAPOS )* '\\'\\'\\''
                    {
                    match("\'\'\'"); 

                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:955:22: ( options {greedy=false; } : TRIAPOS )*
                    loop15:
                    do {
                        int alt15=2;
                        int LA15_0 = input.LA(1);

                        if ( (LA15_0=='\'') ) {
                            int LA15_1 = input.LA(2);

                            if ( (LA15_1=='\'') ) {
                                int LA15_3 = input.LA(3);

                                if ( (LA15_3=='\'') ) {
                                    alt15=2;
                                }
                                else if ( ((LA15_3>='\u0000' && LA15_3<='&')||(LA15_3>='(' && LA15_3<='\uFFFE')) ) {
                                    alt15=1;
                                }


                            }
                            else if ( ((LA15_1>='\u0000' && LA15_1<='&')||(LA15_1>='(' && LA15_1<='\uFFFE')) ) {
                                alt15=1;
                            }


                        }
                        else if ( ((LA15_0>='\u0000' && LA15_0<='&')||(LA15_0>='(' && LA15_0<='\uFFFE')) ) {
                            alt15=1;
                        }


                        switch (alt15) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:955:47: TRIAPOS
                    	    {
                    	    mTRIAPOS(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop15;
                        }
                    } while (true);

                    match("\'\'\'"); 


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:956:13: '\"\"\"' ( options {greedy=false; } : TRIQUOTE )* '\"\"\"'
                    {
                    match("\"\"\""); 

                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:956:19: ( options {greedy=false; } : TRIQUOTE )*
                    loop16:
                    do {
                        int alt16=2;
                        int LA16_0 = input.LA(1);

                        if ( (LA16_0=='\"') ) {
                            int LA16_1 = input.LA(2);

                            if ( (LA16_1=='\"') ) {
                                int LA16_3 = input.LA(3);

                                if ( (LA16_3=='\"') ) {
                                    alt16=2;
                                }
                                else if ( ((LA16_3>='\u0000' && LA16_3<='!')||(LA16_3>='#' && LA16_3<='\uFFFE')) ) {
                                    alt16=1;
                                }


                            }
                            else if ( ((LA16_1>='\u0000' && LA16_1<='!')||(LA16_1>='#' && LA16_1<='\uFFFE')) ) {
                                alt16=1;
                            }


                        }
                        else if ( ((LA16_0>='\u0000' && LA16_0<='!')||(LA16_0>='#' && LA16_0<='\uFFFE')) ) {
                            alt16=1;
                        }


                        switch (alt16) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:956:44: TRIQUOTE
                    	    {
                    	    mTRIQUOTE(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop16;
                        }
                    } while (true);

                    match("\"\"\""); 


                    }
                    break;
                case 3 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:957:13: '\"' ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )* '\"'
                    {
                    match('\"'); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:957:17: ( ESC | ~ ( '\\\\' | '\\n' | '\"' ) )*
                    loop17:
                    do {
                        int alt17=3;
                        int LA17_0 = input.LA(1);

                        if ( (LA17_0=='\\') ) {
                            alt17=1;
                        }
                        else if ( ((LA17_0>='\u0000' && LA17_0<='\t')||(LA17_0>='\u000B' && LA17_0<='!')||(LA17_0>='#' && LA17_0<='[')||(LA17_0>=']' && LA17_0<='\uFFFE')) ) {
                            alt17=2;
                        }


                        switch (alt17) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:957:18: ESC
                    	    {
                    	    mESC(); 

                    	    }
                    	    break;
                    	case 2 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:957:22: ~ ( '\\\\' | '\\n' | '\"' )
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFE') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop17;
                        }
                    } while (true);

                    match('\"'); 

                    }
                    break;
                case 4 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:958:13: '\\'' ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )* '\\''
                    {
                    match('\''); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:958:18: ( ESC | ~ ( '\\\\' | '\\n' | '\\'' ) )*
                    loop18:
                    do {
                        int alt18=3;
                        int LA18_0 = input.LA(1);

                        if ( (LA18_0=='\\') ) {
                            alt18=1;
                        }
                        else if ( ((LA18_0>='\u0000' && LA18_0<='\t')||(LA18_0>='\u000B' && LA18_0<='&')||(LA18_0>='(' && LA18_0<='[')||(LA18_0>=']' && LA18_0<='\uFFFE')) ) {
                            alt18=2;
                        }


                        switch (alt18) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:958:19: ESC
                    	    {
                    	    mESC(); 

                    	    }
                    	    break;
                    	case 2 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:958:23: ~ ( '\\\\' | '\\n' | '\\'' )
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFE') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop18;
                        }
                    } while (true);

                    match('\''); 

                    }
                    break;

            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end STRING

    // $ANTLR start TRIQUOTE
    public final void mTRIQUOTE() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:5: ( ( '\"' )? ( '\"' )? ( ESC | ~ ( '\\\\' | '\"' ) )+ )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:7: ( '\"' )? ( '\"' )? ( ESC | ~ ( '\\\\' | '\"' ) )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:7: ( '\"' )?
            int alt20=2;
            int LA20_0 = input.LA(1);

            if ( (LA20_0=='\"') ) {
                alt20=1;
            }
            switch (alt20) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:7: '\"'
                    {
                    match('\"'); 

                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:12: ( '\"' )?
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0=='\"') ) {
                alt21=1;
            }
            switch (alt21) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:12: '\"'
                    {
                    match('\"'); 

                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:17: ( ESC | ~ ( '\\\\' | '\"' ) )+
            int cnt22=0;
            loop22:
            do {
                int alt22=3;
                int LA22_0 = input.LA(1);

                if ( (LA22_0=='\\') ) {
                    alt22=1;
                }
                else if ( ((LA22_0>='\u0000' && LA22_0<='!')||(LA22_0>='#' && LA22_0<='[')||(LA22_0>=']' && LA22_0<='\uFFFE')) ) {
                    alt22=2;
                }


                switch (alt22) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:18: ESC
            	    {
            	    mESC(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:965:22: ~ ( '\\\\' | '\"' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


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


            }

        }
        finally {
        }
    }
    // $ANTLR end TRIQUOTE

    // $ANTLR start TRIAPOS
    public final void mTRIAPOS() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:5: ( ( '\\'' )? ( '\\'' )? ( ESC | ~ ( '\\\\' | '\\'' ) )+ )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:7: ( '\\'' )? ( '\\'' )? ( ESC | ~ ( '\\\\' | '\\'' ) )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:7: ( '\\'' )?
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( (LA23_0=='\'') ) {
                alt23=1;
            }
            switch (alt23) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:7: '\\''
                    {
                    match('\''); 

                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:13: ( '\\'' )?
            int alt24=2;
            int LA24_0 = input.LA(1);

            if ( (LA24_0=='\'') ) {
                alt24=1;
            }
            switch (alt24) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:13: '\\''
                    {
                    match('\''); 

                    }
                    break;

            }

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:19: ( ESC | ~ ( '\\\\' | '\\'' ) )+
            int cnt25=0;
            loop25:
            do {
                int alt25=3;
                int LA25_0 = input.LA(1);

                if ( (LA25_0=='\\') ) {
                    alt25=1;
                }
                else if ( ((LA25_0>='\u0000' && LA25_0<='&')||(LA25_0>='(' && LA25_0<='[')||(LA25_0>=']' && LA25_0<='\uFFFE')) ) {
                    alt25=2;
                }


                switch (alt25) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:20: ESC
            	    {
            	    mESC(); 

            	    }
            	    break;
            	case 2 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:971:24: ~ ( '\\\\' | '\\'' )
            	    {
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFE') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt25 >= 1 ) break loop25;
                        EarlyExitException eee =
                            new EarlyExitException(25, input);
                        throw eee;
                }
                cnt25++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end TRIAPOS

    // $ANTLR start ESC
    public final void mESC() throws RecognitionException {
        try {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:976:5: ( '\\\\' . )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:976:10: '\\\\' .
            {
            match('\\'); 
            matchAny(); 

            }

        }
        finally {
        }
    }
    // $ANTLR end ESC

    // $ANTLR start CONTINUED_LINE
    public final void mCONTINUED_LINE() throws RecognitionException {
        try {
            int _type = CONTINUED_LINE;
            Token nl=null;

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:984:5: ( '\\\\' ( '\\r' )? '\\n' ( ' ' | '\\t' )* (nl= NEWLINE | ) )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:984:10: '\\\\' ( '\\r' )? '\\n' ( ' ' | '\\t' )* (nl= NEWLINE | )
            {
            match('\\'); 
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:984:15: ( '\\r' )?
            int alt26=2;
            int LA26_0 = input.LA(1);

            if ( (LA26_0=='\r') ) {
                alt26=1;
            }
            switch (alt26) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:984:16: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }

            match('\n'); 
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:984:28: ( ' ' | '\\t' )*
            loop27:
            do {
                int alt27=2;
                int LA27_0 = input.LA(1);

                if ( (LA27_0=='\t'||LA27_0==' ') ) {
                    alt27=1;
                }


                switch (alt27) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
            	    {
            	    if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop27;
                }
            } while (true);

             channel=HIDDEN; 
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:985:10: (nl= NEWLINE | )
            int alt28=2;
            int LA28_0 = input.LA(1);

            if ( (LA28_0=='\n'||(LA28_0>='\f' && LA28_0<='\r')) ) {
                alt28=1;
            }
            else {
                alt28=2;}
            switch (alt28) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:985:12: nl= NEWLINE
                    {
                    int nlStart1534 = getCharIndex();
                    mNEWLINE(); 
                    nl = new CommonToken(input, Token.INVALID_TOKEN_TYPE, Token.DEFAULT_CHANNEL, nlStart1534, getCharIndex()-1);
                    emit(new ClassicToken(NEWLINE,nl.getText()));

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:987:10: 
                    {
                    }
                    break;

            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end CONTINUED_LINE

    // $ANTLR start NEWLINE
    public final void mNEWLINE() throws RecognitionException {
        try {
            int _type = NEWLINE;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:5: ( ( ( '\\u000C' )? ( '\\r' )? '\\n' )+ )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:9: ( ( '\\u000C' )? ( '\\r' )? '\\n' )+
            {
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:9: ( ( '\\u000C' )? ( '\\r' )? '\\n' )+
            int cnt31=0;
            loop31:
            do {
                int alt31=2;
                int LA31_0 = input.LA(1);

                if ( (LA31_0=='\n'||(LA31_0>='\f' && LA31_0<='\r')) ) {
                    alt31=1;
                }


                switch (alt31) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:10: ( '\\u000C' )? ( '\\r' )? '\\n'
            	    {
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:10: ( '\\u000C' )?
            	    int alt29=2;
            	    int LA29_0 = input.LA(1);

            	    if ( (LA29_0=='\f') ) {
            	        alt29=1;
            	    }
            	    switch (alt29) {
            	        case 1 :
            	            // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:11: '\\u000C'
            	            {
            	            match('\f'); 

            	            }
            	            break;

            	    }

            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:21: ( '\\r' )?
            	    int alt30=2;
            	    int LA30_0 = input.LA(1);

            	    if ( (LA30_0=='\r') ) {
            	        alt30=1;
            	    }
            	    switch (alt30) {
            	        case 1 :
            	            // /Users/frank/tmp/trunk/jython/grammar/Python.g:997:22: '\\r'
            	            {
            	            match('\r'); 

            	            }
            	            break;

            	    }

            	    match('\n'); 

            	    }
            	    break;

            	default :
            	    if ( cnt31 >= 1 ) break loop31;
                        EarlyExitException eee =
                            new EarlyExitException(31, input);
                        throw eee;
                }
                cnt31++;
            } while (true);

            if ( startPos==0 || implicitLineJoiningLevel>0 )
                        channel=HIDDEN;
                    

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end NEWLINE

    // $ANTLR start WS
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1003:5: ({...}? => ( ' ' | '\\t' | '\\u000C' )+ )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1003:10: {...}? => ( ' ' | '\\t' | '\\u000C' )+
            {
            if ( !(startPos>0) ) {
                throw new FailedPredicateException(input, "WS", "startPos>0");
            }
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1003:26: ( ' ' | '\\t' | '\\u000C' )+
            int cnt32=0;
            loop32:
            do {
                int alt32=2;
                int LA32_0 = input.LA(1);

                if ( (LA32_0=='\t'||LA32_0=='\f'||LA32_0==' ') ) {
                    alt32=1;
                }


                switch (alt32) {
            	case 1 :
            	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
            	    {
            	    if ( input.LA(1)=='\t'||input.LA(1)=='\f'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse =
            	            new MismatchedSetException(null,input);
            	        recover(mse);    throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt32 >= 1 ) break loop32;
                        EarlyExitException eee =
                            new EarlyExitException(32, input);
                        throw eee;
                }
                cnt32++;
            } while (true);

            channel=HIDDEN;

            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end WS

    // $ANTLR start LEADING_WS
    public final void mLEADING_WS() throws RecognitionException {
        try {
            int _type = LEADING_WS;

                int spaces = 0;

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1016:5: ({...}? => ({...}? ( ' ' | '\\t' )+ | ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )* ) )
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1016:9: {...}? => ({...}? ( ' ' | '\\t' )+ | ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )* )
            {
            if ( !(startPos==0) ) {
                throw new FailedPredicateException(input, "LEADING_WS", "startPos==0");
            }
            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1017:9: ({...}? ( ' ' | '\\t' )+ | ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )* )
            int alt37=2;
            int LA37_0 = input.LA(1);

            if ( (LA37_0==' ') ) {
                int LA37_1 = input.LA(2);

                if ( (implicitLineJoiningLevel>0) ) {
                    alt37=1;
                }
                else if ( (true) ) {
                    alt37=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("1017:9: ({...}? ( ' ' | '\\t' )+ | ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )* )", 37, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA37_0=='\t') ) {
                int LA37_2 = input.LA(2);

                if ( (implicitLineJoiningLevel>0) ) {
                    alt37=1;
                }
                else if ( (true) ) {
                    alt37=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("1017:9: ({...}? ( ' ' | '\\t' )+ | ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )* )", 37, 2, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("1017:9: ({...}? ( ' ' | '\\t' )+ | ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )* )", 37, 0, input);

                throw nvae;
            }
            switch (alt37) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1017:13: {...}? ( ' ' | '\\t' )+
                    {
                    if ( !(implicitLineJoiningLevel>0) ) {
                        throw new FailedPredicateException(input, "LEADING_WS", "implicitLineJoiningLevel>0");
                    }
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1017:43: ( ' ' | '\\t' )+
                    int cnt33=0;
                    loop33:
                    do {
                        int alt33=2;
                        int LA33_0 = input.LA(1);

                        if ( (LA33_0=='\t'||LA33_0==' ') ) {
                            alt33=1;
                        }


                        switch (alt33) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
                    	    {
                    	    if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt33 >= 1 ) break loop33;
                                EarlyExitException eee =
                                    new EarlyExitException(33, input);
                                throw eee;
                        }
                        cnt33++;
                    } while (true);

                    channel=HIDDEN;

                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1018:17: ( ' ' | '\\t' )+ ( ( '\\r' )? '\\n' )*
                    {
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1018:17: ( ' ' | '\\t' )+
                    int cnt34=0;
                    loop34:
                    do {
                        int alt34=3;
                        int LA34_0 = input.LA(1);

                        if ( (LA34_0==' ') ) {
                            alt34=1;
                        }
                        else if ( (LA34_0=='\t') ) {
                            alt34=2;
                        }


                        switch (alt34) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1018:23: ' '
                    	    {
                    	    match(' '); 
                    	     spaces++; 

                    	    }
                    	    break;
                    	case 2 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1019:18: '\\t'
                    	    {
                    	    match('\t'); 
                    	     spaces += 8; spaces -= (spaces % 8); 

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


                                // make a string of n spaces where n is column number - 1
                                char[] indentation = new char[spaces];
                                for (int i=0; i<spaces; i++) {
                                    indentation[i] = ' ';
                                }
                                String s = new String(indentation);
                                emit(new ClassicToken(LEADING_WS,new String(indentation)));
                                
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1031:13: ( ( '\\r' )? '\\n' )*
                    loop36:
                    do {
                        int alt36=2;
                        int LA36_0 = input.LA(1);

                        if ( (LA36_0=='\n'||LA36_0=='\r') ) {
                            alt36=1;
                        }


                        switch (alt36) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1031:15: ( '\\r' )? '\\n'
                    	    {
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1031:15: ( '\\r' )?
                    	    int alt35=2;
                    	    int LA35_0 = input.LA(1);

                    	    if ( (LA35_0=='\r') ) {
                    	        alt35=1;
                    	    }
                    	    switch (alt35) {
                    	        case 1 :
                    	            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1031:16: '\\r'
                    	            {
                    	            match('\r'); 

                    	            }
                    	            break;

                    	    }

                    	    match('\n'); 
                    	    if (token!=null) token.setChannel(HIDDEN); else channel=HIDDEN;

                    	    }
                    	    break;

                    	default :
                    	    break loop36;
                        }
                    } while (true);


                    }
                    break;

            }


            }

            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end LEADING_WS

    // $ANTLR start COMMENT
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;

                channel=HIDDEN;

            // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:5: ({...}? => ( ' ' | '\\t' )* '#' (~ '\\n' )* ( '\\n' )+ | {...}? => '#' (~ '\\n' )* )
            int alt42=2;
            alt42 = dfa42.predict(input);
            switch (alt42) {
                case 1 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:10: {...}? => ( ' ' | '\\t' )* '#' (~ '\\n' )* ( '\\n' )+
                    {
                    if ( !(startPos==0) ) {
                        throw new FailedPredicateException(input, "COMMENT", "startPos==0");
                    }
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:27: ( ' ' | '\\t' )*
                    loop38:
                    do {
                        int alt38=2;
                        int LA38_0 = input.LA(1);

                        if ( (LA38_0=='\t'||LA38_0==' ') ) {
                            alt38=1;
                        }


                        switch (alt38) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:
                    	    {
                    	    if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop38;
                        }
                    } while (true);

                    match('#'); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:43: (~ '\\n' )*
                    loop39:
                    do {
                        int alt39=2;
                        int LA39_0 = input.LA(1);

                        if ( ((LA39_0>='\u0000' && LA39_0<='\t')||(LA39_0>='\u000B' && LA39_0<='\uFFFE')) ) {
                            alt39=1;
                        }


                        switch (alt39) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:44: ~ '\\n'
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\uFFFE') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop39;
                        }
                    } while (true);

                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:52: ( '\\n' )+
                    int cnt40=0;
                    loop40:
                    do {
                        int alt40=2;
                        int LA40_0 = input.LA(1);

                        if ( (LA40_0=='\n') ) {
                            alt40=1;
                        }


                        switch (alt40) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1057:52: '\\n'
                    	    {
                    	    match('\n'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt40 >= 1 ) break loop40;
                                EarlyExitException eee =
                                    new EarlyExitException(40, input);
                                throw eee;
                        }
                        cnt40++;
                    } while (true);


                    }
                    break;
                case 2 :
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1058:10: {...}? => '#' (~ '\\n' )*
                    {
                    if ( !(startPos>0) ) {
                        throw new FailedPredicateException(input, "COMMENT", "startPos>0");
                    }
                    match('#'); 
                    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1058:30: (~ '\\n' )*
                    loop41:
                    do {
                        int alt41=2;
                        int LA41_0 = input.LA(1);

                        if ( ((LA41_0>='\u0000' && LA41_0<='\t')||(LA41_0>='\u000B' && LA41_0<='\uFFFE')) ) {
                            alt41=1;
                        }


                        switch (alt41) {
                    	case 1 :
                    	    // /Users/frank/tmp/trunk/jython/grammar/Python.g:1058:31: ~ '\\n'
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\uFFFE') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse =
                    	            new MismatchedSetException(null,input);
                    	        recover(mse);    throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop41;
                        }
                    } while (true);


                    }
                    break;

            }
            this.type = _type;
        }
        finally {
        }
    }
    // $ANTLR end COMMENT

    public void mTokens() throws RecognitionException {
        // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:8: ( T169 | T170 | T171 | T172 | T173 | T174 | T175 | T176 | T177 | T178 | T179 | T180 | T181 | T182 | T183 | T184 | T185 | T186 | T187 | T188 | T189 | T190 | T191 | T192 | T193 | T194 | T195 | T196 | LPAREN | RPAREN | LBRACK | RBRACK | COLON | COMMA | SEMI | PLUS | MINUS | STAR | SLASH | VBAR | AMPER | LESS | GREATER | ASSIGN | PERCENT | BACKQUOTE | LCURLY | RCURLY | CIRCUMFLEX | TILDE | EQUAL | NOTEQUAL | ALT_NOTEQUAL | LESSEQUAL | LEFTSHIFT | GREATEREQUAL | RIGHTSHIFT | PLUSEQUAL | MINUSEQUAL | DOUBLESTAR | STAREQUAL | DOUBLESLASH | SLASHEQUAL | VBAREQUAL | PERCENTEQUAL | AMPEREQUAL | CIRCUMFLEXEQUAL | LEFTSHIFTEQUAL | RIGHTSHIFTEQUAL | DOUBLESTAREQUAL | DOUBLESLASHEQUAL | DOT | AT | AND | OR | NOT | FLOAT | LONGINT | INT | COMPLEX | NAME | STRING | CONTINUED_LINE | NEWLINE | WS | LEADING_WS | COMMENT )
        int alt43=87;
        alt43 = dfa43.predict(input);
        switch (alt43) {
            case 1 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:10: T169
                {
                mT169(); 

                }
                break;
            case 2 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:15: T170
                {
                mT170(); 

                }
                break;
            case 3 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:20: T171
                {
                mT171(); 

                }
                break;
            case 4 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:25: T172
                {
                mT172(); 

                }
                break;
            case 5 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:30: T173
                {
                mT173(); 

                }
                break;
            case 6 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:35: T174
                {
                mT174(); 

                }
                break;
            case 7 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:40: T175
                {
                mT175(); 

                }
                break;
            case 8 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:45: T176
                {
                mT176(); 

                }
                break;
            case 9 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:50: T177
                {
                mT177(); 

                }
                break;
            case 10 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:55: T178
                {
                mT178(); 

                }
                break;
            case 11 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:60: T179
                {
                mT179(); 

                }
                break;
            case 12 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:65: T180
                {
                mT180(); 

                }
                break;
            case 13 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:70: T181
                {
                mT181(); 

                }
                break;
            case 14 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:75: T182
                {
                mT182(); 

                }
                break;
            case 15 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:80: T183
                {
                mT183(); 

                }
                break;
            case 16 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:85: T184
                {
                mT184(); 

                }
                break;
            case 17 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:90: T185
                {
                mT185(); 

                }
                break;
            case 18 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:95: T186
                {
                mT186(); 

                }
                break;
            case 19 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:100: T187
                {
                mT187(); 

                }
                break;
            case 20 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:105: T188
                {
                mT188(); 

                }
                break;
            case 21 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:110: T189
                {
                mT189(); 

                }
                break;
            case 22 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:115: T190
                {
                mT190(); 

                }
                break;
            case 23 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:120: T191
                {
                mT191(); 

                }
                break;
            case 24 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:125: T192
                {
                mT192(); 

                }
                break;
            case 25 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:130: T193
                {
                mT193(); 

                }
                break;
            case 26 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:135: T194
                {
                mT194(); 

                }
                break;
            case 27 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:140: T195
                {
                mT195(); 

                }
                break;
            case 28 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:145: T196
                {
                mT196(); 

                }
                break;
            case 29 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:150: LPAREN
                {
                mLPAREN(); 

                }
                break;
            case 30 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:157: RPAREN
                {
                mRPAREN(); 

                }
                break;
            case 31 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:164: LBRACK
                {
                mLBRACK(); 

                }
                break;
            case 32 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:171: RBRACK
                {
                mRBRACK(); 

                }
                break;
            case 33 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:178: COLON
                {
                mCOLON(); 

                }
                break;
            case 34 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:184: COMMA
                {
                mCOMMA(); 

                }
                break;
            case 35 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:190: SEMI
                {
                mSEMI(); 

                }
                break;
            case 36 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:195: PLUS
                {
                mPLUS(); 

                }
                break;
            case 37 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:200: MINUS
                {
                mMINUS(); 

                }
                break;
            case 38 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:206: STAR
                {
                mSTAR(); 

                }
                break;
            case 39 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:211: SLASH
                {
                mSLASH(); 

                }
                break;
            case 40 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:217: VBAR
                {
                mVBAR(); 

                }
                break;
            case 41 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:222: AMPER
                {
                mAMPER(); 

                }
                break;
            case 42 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:228: LESS
                {
                mLESS(); 

                }
                break;
            case 43 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:233: GREATER
                {
                mGREATER(); 

                }
                break;
            case 44 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:241: ASSIGN
                {
                mASSIGN(); 

                }
                break;
            case 45 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:248: PERCENT
                {
                mPERCENT(); 

                }
                break;
            case 46 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:256: BACKQUOTE
                {
                mBACKQUOTE(); 

                }
                break;
            case 47 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:266: LCURLY
                {
                mLCURLY(); 

                }
                break;
            case 48 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:273: RCURLY
                {
                mRCURLY(); 

                }
                break;
            case 49 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:280: CIRCUMFLEX
                {
                mCIRCUMFLEX(); 

                }
                break;
            case 50 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:291: TILDE
                {
                mTILDE(); 

                }
                break;
            case 51 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:297: EQUAL
                {
                mEQUAL(); 

                }
                break;
            case 52 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:303: NOTEQUAL
                {
                mNOTEQUAL(); 

                }
                break;
            case 53 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:312: ALT_NOTEQUAL
                {
                mALT_NOTEQUAL(); 

                }
                break;
            case 54 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:325: LESSEQUAL
                {
                mLESSEQUAL(); 

                }
                break;
            case 55 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:335: LEFTSHIFT
                {
                mLEFTSHIFT(); 

                }
                break;
            case 56 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:345: GREATEREQUAL
                {
                mGREATEREQUAL(); 

                }
                break;
            case 57 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:358: RIGHTSHIFT
                {
                mRIGHTSHIFT(); 

                }
                break;
            case 58 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:369: PLUSEQUAL
                {
                mPLUSEQUAL(); 

                }
                break;
            case 59 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:379: MINUSEQUAL
                {
                mMINUSEQUAL(); 

                }
                break;
            case 60 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:390: DOUBLESTAR
                {
                mDOUBLESTAR(); 

                }
                break;
            case 61 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:401: STAREQUAL
                {
                mSTAREQUAL(); 

                }
                break;
            case 62 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:411: DOUBLESLASH
                {
                mDOUBLESLASH(); 

                }
                break;
            case 63 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:423: SLASHEQUAL
                {
                mSLASHEQUAL(); 

                }
                break;
            case 64 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:434: VBAREQUAL
                {
                mVBAREQUAL(); 

                }
                break;
            case 65 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:444: PERCENTEQUAL
                {
                mPERCENTEQUAL(); 

                }
                break;
            case 66 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:457: AMPEREQUAL
                {
                mAMPEREQUAL(); 

                }
                break;
            case 67 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:468: CIRCUMFLEXEQUAL
                {
                mCIRCUMFLEXEQUAL(); 

                }
                break;
            case 68 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:484: LEFTSHIFTEQUAL
                {
                mLEFTSHIFTEQUAL(); 

                }
                break;
            case 69 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:499: RIGHTSHIFTEQUAL
                {
                mRIGHTSHIFTEQUAL(); 

                }
                break;
            case 70 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:515: DOUBLESTAREQUAL
                {
                mDOUBLESTAREQUAL(); 

                }
                break;
            case 71 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:531: DOUBLESLASHEQUAL
                {
                mDOUBLESLASHEQUAL(); 

                }
                break;
            case 72 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:548: DOT
                {
                mDOT(); 

                }
                break;
            case 73 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:552: AT
                {
                mAT(); 

                }
                break;
            case 74 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:555: AND
                {
                mAND(); 

                }
                break;
            case 75 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:559: OR
                {
                mOR(); 

                }
                break;
            case 76 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:562: NOT
                {
                mNOT(); 

                }
                break;
            case 77 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:566: FLOAT
                {
                mFLOAT(); 

                }
                break;
            case 78 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:572: LONGINT
                {
                mLONGINT(); 

                }
                break;
            case 79 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:580: INT
                {
                mINT(); 

                }
                break;
            case 80 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:584: COMPLEX
                {
                mCOMPLEX(); 

                }
                break;
            case 81 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:592: NAME
                {
                mNAME(); 

                }
                break;
            case 82 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:597: STRING
                {
                mSTRING(); 

                }
                break;
            case 83 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:604: CONTINUED_LINE
                {
                mCONTINUED_LINE(); 

                }
                break;
            case 84 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:619: NEWLINE
                {
                mNEWLINE(); 

                }
                break;
            case 85 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:627: WS
                {
                mWS(); 

                }
                break;
            case 86 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:630: LEADING_WS
                {
                mLEADING_WS(); 

                }
                break;
            case 87 :
                // /Users/frank/tmp/trunk/jython/grammar/Python.g:1:641: COMMENT
                {
                mCOMMENT(); 

                }
                break;

        }

    }


    protected DFA5 dfa5 = new DFA5(this);
    protected DFA11 dfa11 = new DFA11(this);
    protected DFA42 dfa42 = new DFA42(this);
    protected DFA43 dfa43 = new DFA43(this);
    static final String DFA5_eotS =
        "\3\uffff\1\4\2\uffff";
    static final String DFA5_eofS =
        "\6\uffff";
    static final String DFA5_minS =
        "\1\56\1\uffff\1\56\1\105\2\uffff";
    static final String DFA5_maxS =
        "\1\71\1\uffff\2\145\2\uffff";
    static final String DFA5_acceptS =
        "\1\uffff\1\1\2\uffff\1\3\1\2";
    static final String DFA5_specialS =
        "\6\uffff}>";
    static final String[] DFA5_transitionS = {
            "\1\1\1\uffff\12\2",
            "",
            "\1\3\1\uffff\12\2\13\uffff\1\4\37\uffff\1\4",
            "\1\5\37\uffff\1\5",
            "",
            ""
    };

    static final short[] DFA5_eot = DFA.unpackEncodedString(DFA5_eotS);
    static final short[] DFA5_eof = DFA.unpackEncodedString(DFA5_eofS);
    static final char[] DFA5_min = DFA.unpackEncodedStringToUnsignedChars(DFA5_minS);
    static final char[] DFA5_max = DFA.unpackEncodedStringToUnsignedChars(DFA5_maxS);
    static final short[] DFA5_accept = DFA.unpackEncodedString(DFA5_acceptS);
    static final short[] DFA5_special = DFA.unpackEncodedString(DFA5_specialS);
    static final short[][] DFA5_transition;

    static {
        int numStates = DFA5_transitionS.length;
        DFA5_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA5_transition[i] = DFA.unpackEncodedString(DFA5_transitionS[i]);
        }
    }

    class DFA5 extends DFA {

        public DFA5(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 5;
            this.eot = DFA5_eot;
            this.eof = DFA5_eof;
            this.min = DFA5_min;
            this.max = DFA5_max;
            this.accept = DFA5_accept;
            this.special = DFA5_special;
            this.transition = DFA5_transition;
        }
        public String getDescription() {
            return "916:1: FLOAT : ( '.' DIGITS ( Exponent )? | DIGITS '.' Exponent | DIGITS ( '.' ( DIGITS ( Exponent )? )? | Exponent ) );";
        }
    }
    static final String DFA11_eotS =
        "\7\uffff";
    static final String DFA11_eofS =
        "\7\uffff";
    static final String DFA11_minS =
        "\3\56\2\uffff\2\56";
    static final String DFA11_maxS =
        "\1\71\1\170\1\152\2\uffff\2\152";
    static final String DFA11_acceptS =
        "\3\uffff\1\2\1\1\2\uffff";
    static final String DFA11_specialS =
        "\7\uffff}>";
    static final String[] DFA11_transitionS = {
            "\1\3\1\uffff\1\1\11\2",
            "\1\3\1\uffff\12\5\13\uffff\1\3\4\uffff\1\4\15\uffff\1\4\14\uffff"+
            "\1\3\4\uffff\1\4\15\uffff\1\4",
            "\1\3\1\uffff\12\6\13\uffff\1\3\4\uffff\1\4\32\uffff\1\3\4\uffff"+
            "\1\4",
            "",
            "",
            "\1\3\1\uffff\12\5\13\uffff\1\3\4\uffff\1\4\32\uffff\1\3\4\uffff"+
            "\1\4",
            "\1\3\1\uffff\12\6\13\uffff\1\3\4\uffff\1\4\32\uffff\1\3\4\uffff"+
            "\1\4"
    };

    static final short[] DFA11_eot = DFA.unpackEncodedString(DFA11_eotS);
    static final short[] DFA11_eof = DFA.unpackEncodedString(DFA11_eofS);
    static final char[] DFA11_min = DFA.unpackEncodedStringToUnsignedChars(DFA11_minS);
    static final char[] DFA11_max = DFA.unpackEncodedStringToUnsignedChars(DFA11_maxS);
    static final short[] DFA11_accept = DFA.unpackEncodedString(DFA11_acceptS);
    static final short[] DFA11_special = DFA.unpackEncodedString(DFA11_specialS);
    static final short[][] DFA11_transition;

    static {
        int numStates = DFA11_transitionS.length;
        DFA11_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA11_transition[i] = DFA.unpackEncodedString(DFA11_transitionS[i]);
        }
    }

    class DFA11 extends DFA {

        public DFA11(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 11;
            this.eot = DFA11_eot;
            this.eof = DFA11_eof;
            this.min = DFA11_min;
            this.max = DFA11_max;
            this.accept = DFA11_accept;
            this.special = DFA11_special;
            this.transition = DFA11_transition;
        }
        public String getDescription() {
            return "938:1: COMPLEX : ( INT ( 'j' | 'J' ) | FLOAT ( 'j' | 'J' ) );";
        }
    }
    static final String DFA42_eotS =
        "\2\uffff\2\4\1\uffff";
    static final String DFA42_eofS =
        "\5\uffff";
    static final String DFA42_minS =
        "\1\11\1\uffff\2\0\1\uffff";
    static final String DFA42_maxS =
        "\1\43\1\uffff\2\ufffe\1\uffff";
    static final String DFA42_acceptS =
        "\1\uffff\1\1\2\uffff\1\2";
    static final String DFA42_specialS =
        "\1\2\1\uffff\1\1\1\0\1\uffff}>";
    static final String[] DFA42_transitionS = {
            "\1\1\26\uffff\1\1\2\uffff\1\2",
            "",
            "\12\3\1\1\ufff4\3",
            "\12\3\1\1\ufff4\3",
            ""
    };

    static final short[] DFA42_eot = DFA.unpackEncodedString(DFA42_eotS);
    static final short[] DFA42_eof = DFA.unpackEncodedString(DFA42_eofS);
    static final char[] DFA42_min = DFA.unpackEncodedStringToUnsignedChars(DFA42_minS);
    static final char[] DFA42_max = DFA.unpackEncodedStringToUnsignedChars(DFA42_maxS);
    static final short[] DFA42_accept = DFA.unpackEncodedString(DFA42_acceptS);
    static final short[] DFA42_special = DFA.unpackEncodedString(DFA42_specialS);
    static final short[][] DFA42_transition;

    static {
        int numStates = DFA42_transitionS.length;
        DFA42_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA42_transition[i] = DFA.unpackEncodedString(DFA42_transitionS[i]);
        }
    }

    class DFA42 extends DFA {

        public DFA42(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 42;
            this.eot = DFA42_eot;
            this.eof = DFA42_eof;
            this.min = DFA42_min;
            this.max = DFA42_max;
            this.accept = DFA42_accept;
            this.special = DFA42_special;
            this.transition = DFA42_transition;
        }
        public String getDescription() {
            return "1036:1: COMMENT : ({...}? => ( ' ' | '\\t' )* '#' (~ '\\n' )* ( '\\n' )+ | {...}? => '#' (~ '\\n' )* );";
        }
        public int specialStateTransition(int s, IntStream input) throws NoViableAltException {
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA42_3 = input.LA(1);

                         
                        int index42_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA42_3>='\u0000' && LA42_3<='\t')||(LA42_3>='\u000B' && LA42_3<='\uFFFE')) && ((startPos>0||startPos==0))) {s = 3;}

                        else if ( (LA42_3=='\n') && (startPos==0)) {s = 1;}

                        else s = 4;

                         
                        input.seek(index42_3);
                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA42_2 = input.LA(1);

                         
                        int index42_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( ((LA42_2>='\u0000' && LA42_2<='\t')||(LA42_2>='\u000B' && LA42_2<='\uFFFE')) && ((startPos>0||startPos==0))) {s = 3;}

                        else if ( (LA42_2=='\n') && (startPos==0)) {s = 1;}

                        else s = 4;

                         
                        input.seek(index42_2);
                        if ( s>=0 ) return s;
                        break;
                    case 2 : 
                        int LA42_0 = input.LA(1);

                         
                        int index42_0 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA42_0=='\t'||LA42_0==' ') && (startPos==0)) {s = 1;}

                        else if ( (LA42_0=='#') && ((startPos>0||startPos==0))) {s = 2;}

                         
                        input.seek(index42_0);
                        if ( s>=0 ) return s;
                        break;
            }
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 42, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA43_eotS =
        "\1\uffff\16\55\7\uffff\1\117\1\121\1\124\1\127\1\131\1\133\1\137"+
        "\1\142\1\144\1\146\3\uffff\1\150\2\uffff\1\152\1\uffff\2\55\2\157"+
        "\1\55\3\uffff\1\166\1\uffff\1\167\1\172\1\uffff\10\55\1\u0084\1"+
        "\55\1\u0086\1\u0087\3\55\1\u008c\11\55\5\uffff\1\u0099\1\uffff\1"+
        "\u009b\7\uffff\1\u009d\3\uffff\1\u009f\7\uffff\1\u00a0\1\uffff\1"+
        "\u00a2\1\55\1\uffff\1\157\2\uffff\1\u00a0\2\uffff\1\157\1\55\5\uffff"+
        "\1\u00a9\1\u00aa\7\55\1\uffff\1\55\2\uffff\1\u00b3\3\55\1\uffff"+
        "\1\u00b7\7\55\1\u00bf\2\55\13\uffff\1\u00c4\1\157\1\u00a0\2\uffff"+
        "\1\u00a0\2\uffff\1\55\1\u00c9\6\55\1\uffff\1\55\1\u00d1\1\55\1\uffff"+
        "\1\55\1\u00d4\1\u00d5\1\55\1\u00d7\1\55\1\u00d9\1\uffff\2\55\1\uffff"+
        "\1\u00a0\3\uffff\1\u00a0\1\u00de\1\uffff\1\u00df\1\55\1\u00e1\1"+
        "\u00e2\3\55\1\uffff\2\55\2\uffff\1\55\1\uffff\1\u00e9\1\uffff\1"+
        "\55\1\u00eb\1\uffff\1\u00a0\2\uffff\1\55\2\uffff\1\u00ed\1\u00ee"+
        "\1\55\1\u00f0\1\u00f1\1\u00f2\1\uffff\1\u00f3\1\uffff\1\55\2\uffff"+
        "\1\u00f5\4\uffff\1\u00f6\2\uffff";
    static final String DFA43_eofS =
        "\u00f7\uffff";
    static final String DFA43_minS =
        "\1\11\1\145\1\141\1\162\1\154\1\42\1\146\1\151\1\156\2\154\1\150"+
        "\1\162\1\141\1\151\7\uffff\2\75\1\52\1\57\2\75\1\74\3\75\3\uffff"+
        "\1\75\2\uffff\1\60\1\uffff\1\162\1\157\2\56\1\42\3\uffff\1\12\1"+
        "\uffff\2\11\1\uffff\1\146\1\151\1\163\1\145\1\156\1\141\1\151\1"+
        "\164\1\60\1\160\2\60\1\162\1\156\1\157\1\60\1\144\1\157\1\151\1"+
        "\143\1\151\1\164\1\171\1\155\1\145\5\uffff\1\75\1\uffff\1\75\7\uffff"+
        "\1\75\3\uffff\1\75\7\uffff\1\60\1\uffff\1\60\1\164\1\60\1\56\2\uffff"+
        "\1\60\1\53\1\uffff\1\56\1\42\1\uffff\1\0\2\uffff\1\0\2\60\1\156"+
        "\1\163\1\141\1\164\2\163\1\165\1\uffff\1\157\2\uffff\1\60\1\141"+
        "\1\155\1\145\1\uffff\1\60\1\142\1\145\1\146\1\145\1\143\1\154\1"+
        "\150\1\60\1\142\1\154\11\uffff\1\53\1\uffff\3\60\1\53\2\60\2\uffff"+
        "\1\164\1\60\1\153\1\151\1\163\1\145\2\162\1\uffff\1\154\1\60\1\162"+
        "\1\uffff\1\141\2\60\1\160\1\60\1\145\1\60\1\uffff\2\144\2\60\1\uffff"+
        "\1\53\3\60\1\uffff\1\60\1\156\2\60\1\156\1\164\1\154\1\uffff\1\164"+
        "\1\154\2\uffff\1\164\1\uffff\1\60\1\uffff\1\141\3\60\2\uffff\1\165"+
        "\2\uffff\2\60\1\171\3\60\1\uffff\1\60\1\uffff\1\145\2\uffff\1\60"+
        "\4\uffff\1\60\2\uffff";
    static final String DFA43_maxS =
        "\1\176\1\145\2\162\1\157\1\145\1\163\1\162\1\163\1\154\1\170\1\151"+
        "\1\162\1\141\1\151\7\uffff\6\75\2\76\2\75\3\uffff\1\75\2\uffff\1"+
        "\71\1\uffff\1\162\1\157\1\170\1\154\1\162\3\uffff\1\15\1\uffff\2"+
        "\43\1\uffff\1\154\1\151\1\163\1\145\1\156\1\141\1\151\1\164\1\172"+
        "\1\160\2\172\1\162\1\156\1\157\1\172\1\144\1\157\1\163\1\145\1\151"+
        "\1\164\1\171\1\155\1\145\5\uffff\1\75\1\uffff\1\75\7\uffff\1\75"+
        "\3\uffff\1\75\7\uffff\1\152\1\uffff\1\172\1\164\1\146\1\154\2\uffff"+
        "\1\152\1\71\1\uffff\1\154\1\47\1\uffff\1\0\2\uffff\1\0\2\172\1\156"+
        "\1\163\1\141\1\164\2\163\1\165\1\uffff\1\157\2\uffff\1\172\1\141"+
        "\1\155\1\145\1\uffff\1\172\1\142\1\145\1\146\1\145\1\143\1\154\1"+
        "\150\1\172\1\142\1\154\11\uffff\1\71\1\uffff\1\172\1\154\1\152\2"+
        "\71\1\152\2\uffff\1\164\1\172\1\153\1\151\1\163\1\145\2\162\1\uffff"+
        "\1\154\1\172\1\162\1\uffff\1\141\2\172\1\160\1\172\1\145\1\172\1"+
        "\uffff\2\144\1\71\1\152\1\uffff\2\71\1\152\1\172\1\uffff\1\172\1"+
        "\156\2\172\1\156\1\164\1\154\1\uffff\1\164\1\154\2\uffff\1\164\1"+
        "\uffff\1\172\1\uffff\1\141\1\172\1\71\1\152\2\uffff\1\165\2\uffff"+
        "\2\172\1\171\3\172\1\uffff\1\172\1\uffff\1\145\2\uffff\1\172\4\uffff"+
        "\1\172\2\uffff";
    static final String DFA43_acceptS =
        "\17\uffff\1\35\1\36\1\37\1\40\1\41\1\42\1\43\12\uffff\1\56\1\57"+
        "\1\60\1\uffff\1\62\1\64\1\uffff\1\111\5\uffff\1\121\1\122\1\123"+
        "\1\uffff\1\124\2\uffff\1\127\31\uffff\1\72\1\44\1\73\1\45\1\75\1"+
        "\uffff\1\46\1\uffff\1\77\1\47\1\100\1\50\1\102\1\51\1\65\1\uffff"+
        "\1\66\1\52\1\70\1\uffff\1\53\1\63\1\54\1\101\1\55\1\103\1\61\1\uffff"+
        "\1\110\4\uffff\1\117\1\120\2\uffff\1\116\2\uffff\1\125\1\uffff\1"+
        "\126\1\127\12\uffff\1\20\1\uffff\1\16\1\31\4\uffff\1\13\13\uffff"+
        "\1\106\1\74\1\107\1\76\1\104\1\67\1\105\1\71\1\115\1\uffff\1\113"+
        "\6\uffff\1\3\1\1\10\uffff\1\24\3\uffff\1\112\7\uffff\1\25\4\uffff"+
        "\1\114\4\uffff\1\4\7\uffff\1\12\2\uffff\1\21\1\22\1\uffff\1\15\1"+
        "\uffff\1\27\4\uffff\1\2\1\5\1\uffff\1\33\1\10\6\uffff\1\23\1\uffff"+
        "\1\34\1\uffff\1\7\1\11\1\uffff\1\17\1\14\1\30\1\32\1\uffff\1\26"+
        "\1\6";
    static final String DFA43_specialS =
        "\1\3\57\uffff\1\2\1\uffff\1\0\1\1\103\uffff\1\4\2\uffff\1\5\174"+
        "\uffff}>";
    static final String[] DFA43_transitionS = {
            "\1\63\1\61\1\uffff\1\60\1\61\22\uffff\1\62\1\45\1\56\1\64\1"+
            "\uffff\1\37\1\33\1\56\1\17\1\20\1\30\1\26\1\24\1\27\1\46\1\31"+
            "\1\52\11\53\1\23\1\25\1\34\1\36\1\35\1\uffff\1\47\32\55\1\21"+
            "\1\57\1\22\1\43\1\55\1\40\1\10\1\3\1\4\1\1\1\12\1\7\1\11\1\55"+
            "\1\6\2\55\1\15\1\55\1\51\1\50\1\2\1\55\1\5\1\55\1\14\1\54\1"+
            "\55\1\13\1\55\1\16\1\55\1\41\1\32\1\42\1\44",
            "\1\65",
            "\1\67\20\uffff\1\66",
            "\1\70",
            "\1\72\2\uffff\1\71",
            "\1\56\4\uffff\1\56\71\uffff\1\73\3\uffff\1\74",
            "\1\75\6\uffff\1\76\1\77\4\uffff\1\100",
            "\1\102\5\uffff\1\101\2\uffff\1\103",
            "\1\105\4\uffff\1\104",
            "\1\106",
            "\1\107\13\uffff\1\110",
            "\1\111\1\112",
            "\1\113",
            "\1\114",
            "\1\115",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\116",
            "\1\120",
            "\1\123\22\uffff\1\122",
            "\1\125\15\uffff\1\126",
            "\1\130",
            "\1\132",
            "\1\135\1\136\1\134",
            "\1\140\1\141",
            "\1\143",
            "\1\145",
            "",
            "",
            "",
            "\1\147",
            "",
            "",
            "\12\151",
            "",
            "\1\153",
            "\1\154",
            "\1\161\1\uffff\12\156\13\uffff\1\162\4\uffff\1\160\1\uffff\1"+
            "\163\13\uffff\1\155\14\uffff\1\162\4\uffff\1\160\1\uffff\1\163"+
            "\13\uffff\1\155",
            "\1\161\1\uffff\12\164\13\uffff\1\162\4\uffff\1\160\1\uffff\1"+
            "\163\30\uffff\1\162\4\uffff\1\160\1\uffff\1\163",
            "\1\56\4\uffff\1\56\112\uffff\1\165",
            "",
            "",
            "",
            "\1\61\2\uffff\1\61",
            "",
            "\1\63\1\170\1\uffff\1\166\1\170\22\uffff\1\62\2\uffff\1\171",
            "\1\63\1\170\1\uffff\1\166\1\170\22\uffff\1\62\2\uffff\1\171",
            "",
            "\1\174\5\uffff\1\173",
            "\1\175",
            "\1\176",
            "\1\177",
            "\1\u0080",
            "\1\u0081",
            "\1\u0082",
            "\1\u0083",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u0085",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u0088",
            "\1\u0089",
            "\1\u008a",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\22\55\1\u008b\7\55",
            "\1\u008d",
            "\1\u008e",
            "\1\u0090\11\uffff\1\u008f",
            "\1\u0091\1\uffff\1\u0092",
            "\1\u0093",
            "\1\u0094",
            "\1\u0095",
            "\1\u0096",
            "\1\u0097",
            "",
            "",
            "",
            "",
            "",
            "\1\u0098",
            "",
            "\1\u009a",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\u009c",
            "",
            "",
            "",
            "\1\u009e",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\12\151\13\uffff\1\u00a1\4\uffff\1\160\32\uffff\1\u00a1\4\uffff"+
            "\1\160",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00a3",
            "\12\u00a4\7\uffff\6\u00a4\32\uffff\6\u00a4",
            "\1\161\1\uffff\12\156\13\uffff\1\162\4\uffff\1\160\1\uffff\1"+
            "\163\30\uffff\1\162\4\uffff\1\160\1\uffff\1\163",
            "",
            "",
            "\12\u00a5\13\uffff\1\u00a6\4\uffff\1\160\32\uffff\1\u00a6\4"+
            "\uffff\1\160",
            "\1\u00a7\1\uffff\1\u00a7\2\uffff\12\u00a8",
            "",
            "\1\161\1\uffff\12\164\13\uffff\1\162\4\uffff\1\160\1\uffff\1"+
            "\163\30\uffff\1\162\4\uffff\1\160\1\uffff\1\163",
            "\1\56\4\uffff\1\56",
            "",
            "\1\uffff",
            "",
            "",
            "\1\uffff",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00ab",
            "\1\u00ac",
            "\1\u00ad",
            "\1\u00ae",
            "\1\u00af",
            "\1\u00b0",
            "\1\u00b1",
            "",
            "\1\u00b2",
            "",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00b4",
            "\1\u00b5",
            "\1\u00b6",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00b8",
            "\1\u00b9",
            "\1\u00ba",
            "\1\u00bb",
            "\1\u00bc",
            "\1\u00bd",
            "\1\u00be",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00c0",
            "\1\u00c1",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\u00c2\1\uffff\1\u00c2\2\uffff\12\u00c3",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\u00a4\7\uffff\6\u00a4\3\uffff\1\160\1\uffff\1\163\24\uffff"+
            "\6\u00a4\3\uffff\1\160\1\uffff\1\163",
            "\12\u00a5\13\uffff\1\u00c5\4\uffff\1\160\32\uffff\1\u00c5\4"+
            "\uffff\1\160",
            "\1\u00c6\1\uffff\1\u00c6\2\uffff\12\u00c7",
            "\12\u00a8",
            "\12\u00a8\20\uffff\1\160\37\uffff\1\160",
            "",
            "",
            "\1\u00c8",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00ca",
            "\1\u00cb",
            "\1\u00cc",
            "\1\u00cd",
            "\1\u00ce",
            "\1\u00cf",
            "",
            "\1\u00d0",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00d2",
            "",
            "\1\u00d3",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00d6",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00d8",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            "\1\u00da",
            "\1\u00db",
            "\12\u00c3",
            "\12\u00c3\20\uffff\1\160\37\uffff\1\160",
            "",
            "\1\u00dc\1\uffff\1\u00dc\2\uffff\12\u00dd",
            "\12\u00c7",
            "\12\u00c7\20\uffff\1\160\37\uffff\1\160",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00e0",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00e3",
            "\1\u00e4",
            "\1\u00e5",
            "",
            "\1\u00e6",
            "\1\u00e7",
            "",
            "",
            "\1\u00e8",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            "\1\u00ea",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\u00dd",
            "\12\u00dd\20\uffff\1\160\37\uffff\1\160",
            "",
            "",
            "\1\u00ec",
            "",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\1\u00ef",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            "\1\u00f4",
            "",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            "",
            "",
            "",
            "\12\55\7\uffff\32\55\4\uffff\1\55\1\uffff\32\55",
            "",
            ""
    };

    static final short[] DFA43_eot = DFA.unpackEncodedString(DFA43_eotS);
    static final short[] DFA43_eof = DFA.unpackEncodedString(DFA43_eofS);
    static final char[] DFA43_min = DFA.unpackEncodedStringToUnsignedChars(DFA43_minS);
    static final char[] DFA43_max = DFA.unpackEncodedStringToUnsignedChars(DFA43_maxS);
    static final short[] DFA43_accept = DFA.unpackEncodedString(DFA43_acceptS);
    static final short[] DFA43_special = DFA.unpackEncodedString(DFA43_specialS);
    static final short[][] DFA43_transition;

    static {
        int numStates = DFA43_transitionS.length;
        DFA43_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA43_transition[i] = DFA.unpackEncodedString(DFA43_transitionS[i]);
        }
    }

    class DFA43 extends DFA {

        public DFA43(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 43;
            this.eot = DFA43_eot;
            this.eof = DFA43_eof;
            this.min = DFA43_min;
            this.max = DFA43_max;
            this.accept = DFA43_accept;
            this.special = DFA43_special;
            this.transition = DFA43_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T169 | T170 | T171 | T172 | T173 | T174 | T175 | T176 | T177 | T178 | T179 | T180 | T181 | T182 | T183 | T184 | T185 | T186 | T187 | T188 | T189 | T190 | T191 | T192 | T193 | T194 | T195 | T196 | LPAREN | RPAREN | LBRACK | RBRACK | COLON | COMMA | SEMI | PLUS | MINUS | STAR | SLASH | VBAR | AMPER | LESS | GREATER | ASSIGN | PERCENT | BACKQUOTE | LCURLY | RCURLY | CIRCUMFLEX | TILDE | EQUAL | NOTEQUAL | ALT_NOTEQUAL | LESSEQUAL | LEFTSHIFT | GREATEREQUAL | RIGHTSHIFT | PLUSEQUAL | MINUSEQUAL | DOUBLESTAR | STAREQUAL | DOUBLESLASH | SLASHEQUAL | VBAREQUAL | PERCENTEQUAL | AMPEREQUAL | CIRCUMFLEXEQUAL | LEFTSHIFTEQUAL | RIGHTSHIFTEQUAL | DOUBLESTAREQUAL | DOUBLESLASHEQUAL | DOT | AT | AND | OR | NOT | FLOAT | LONGINT | INT | COMPLEX | NAME | STRING | CONTINUED_LINE | NEWLINE | WS | LEADING_WS | COMMENT );";
        }
        public int specialStateTransition(int s, IntStream input) throws NoViableAltException {
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA43_50 = input.LA(1);

                         
                        int index43_50 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA43_50==' ') && ((startPos>0||startPos==0))) {s = 50;}

                        else if ( (LA43_50=='\n'||LA43_50=='\r') && (startPos==0)) {s = 120;}

                        else if ( (LA43_50=='\t') && ((startPos>0||startPos==0))) {s = 51;}

                        else if ( (LA43_50=='#') && (startPos==0)) {s = 121;}

                        else if ( (LA43_50=='\f') && (startPos>0)) {s = 118;}

                        else s = 119;

                         
                        input.seek(index43_50);
                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA43_51 = input.LA(1);

                         
                        int index43_51 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA43_51==' ') && ((startPos>0||startPos==0))) {s = 50;}

                        else if ( (LA43_51=='#') && (startPos==0)) {s = 121;}

                        else if ( (LA43_51=='\n'||LA43_51=='\r') && (startPos==0)) {s = 120;}

                        else if ( (LA43_51=='\t') && ((startPos>0||startPos==0))) {s = 51;}

                        else if ( (LA43_51=='\f') && (startPos>0)) {s = 118;}

                        else s = 122;

                         
                        input.seek(index43_51);
                        if ( s>=0 ) return s;
                        break;
                    case 2 : 
                        int LA43_48 = input.LA(1);

                         
                        int index43_48 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA43_48=='\n'||LA43_48=='\r') ) {s = 49;}

                        else s = 118;

                         
                        input.seek(index43_48);
                        if ( s>=0 ) return s;
                        break;
                    case 3 : 
                        int LA43_0 = input.LA(1);

                         
                        int index43_0 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (LA43_0=='d') ) {s = 1;}

                        else if ( (LA43_0=='p') ) {s = 2;}

                        else if ( (LA43_0=='b') ) {s = 3;}

                        else if ( (LA43_0=='c') ) {s = 4;}

                        else if ( (LA43_0=='r') ) {s = 5;}

                        else if ( (LA43_0=='i') ) {s = 6;}

                        else if ( (LA43_0=='f') ) {s = 7;}

                        else if ( (LA43_0=='a') ) {s = 8;}

                        else if ( (LA43_0=='g') ) {s = 9;}

                        else if ( (LA43_0=='e') ) {s = 10;}

                        else if ( (LA43_0=='w') ) {s = 11;}

                        else if ( (LA43_0=='t') ) {s = 12;}

                        else if ( (LA43_0=='l') ) {s = 13;}

                        else if ( (LA43_0=='y') ) {s = 14;}

                        else if ( (LA43_0=='(') ) {s = 15;}

                        else if ( (LA43_0==')') ) {s = 16;}

                        else if ( (LA43_0=='[') ) {s = 17;}

                        else if ( (LA43_0==']') ) {s = 18;}

                        else if ( (LA43_0==':') ) {s = 19;}

                        else if ( (LA43_0==',') ) {s = 20;}

                        else if ( (LA43_0==';') ) {s = 21;}

                        else if ( (LA43_0=='+') ) {s = 22;}

                        else if ( (LA43_0=='-') ) {s = 23;}

                        else if ( (LA43_0=='*') ) {s = 24;}

                        else if ( (LA43_0=='/') ) {s = 25;}

                        else if ( (LA43_0=='|') ) {s = 26;}

                        else if ( (LA43_0=='&') ) {s = 27;}

                        else if ( (LA43_0=='<') ) {s = 28;}

                        else if ( (LA43_0=='>') ) {s = 29;}

                        else if ( (LA43_0=='=') ) {s = 30;}

                        else if ( (LA43_0=='%') ) {s = 31;}

                        else if ( (LA43_0=='`') ) {s = 32;}

                        else if ( (LA43_0=='{') ) {s = 33;}

                        else if ( (LA43_0=='}') ) {s = 34;}

                        else if ( (LA43_0=='^') ) {s = 35;}

                        else if ( (LA43_0=='~') ) {s = 36;}

                        else if ( (LA43_0=='!') ) {s = 37;}

                        else if ( (LA43_0=='.') ) {s = 38;}

                        else if ( (LA43_0=='@') ) {s = 39;}

                        else if ( (LA43_0=='o') ) {s = 40;}

                        else if ( (LA43_0=='n') ) {s = 41;}

                        else if ( (LA43_0=='0') ) {s = 42;}

                        else if ( ((LA43_0>='1' && LA43_0<='9')) ) {s = 43;}

                        else if ( (LA43_0=='u') ) {s = 44;}

                        else if ( ((LA43_0>='A' && LA43_0<='Z')||LA43_0=='_'||LA43_0=='h'||(LA43_0>='j' && LA43_0<='k')||LA43_0=='m'||LA43_0=='q'||LA43_0=='s'||LA43_0=='v'||LA43_0=='x'||LA43_0=='z') ) {s = 45;}

                        else if ( (LA43_0=='\"'||LA43_0=='\'') ) {s = 46;}

                        else if ( (LA43_0=='\\') ) {s = 47;}

                        else if ( (LA43_0=='\f') ) {s = 48;}

                        else if ( (LA43_0=='\n'||LA43_0=='\r') ) {s = 49;}

                        else if ( (LA43_0==' ') && ((startPos>0||startPos==0))) {s = 50;}

                        else if ( (LA43_0=='\t') && ((startPos>0||startPos==0))) {s = 51;}

                        else if ( (LA43_0=='#') && ((startPos>0||startPos==0))) {s = 52;}

                         
                        input.seek(index43_0);
                        if ( s>=0 ) return s;
                        break;
                    case 4 : 
                        int LA43_119 = input.LA(1);

                         
                        int index43_119 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (startPos>0) ) {s = 118;}

                        else if ( (((startPos==0&&implicitLineJoiningLevel>0)||startPos==0)) ) {s = 120;}

                         
                        input.seek(index43_119);
                        if ( s>=0 ) return s;
                        break;
                    case 5 : 
                        int LA43_122 = input.LA(1);

                         
                        int index43_122 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (startPos>0) ) {s = 118;}

                        else if ( (((startPos==0&&implicitLineJoiningLevel>0)||startPos==0)) ) {s = 120;}

                         
                        input.seek(index43_122);
                        if ( s>=0 ) return s;
                        break;
            }
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 43, _s, input);
            error(nvae);
            throw nvae;
        }
    }
 

}