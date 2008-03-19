lexer grammar Python;
@members {
/** Handles context-sensitive lexing of implicit line joining such as
 *  the case where newline is ignored in cases like this:
 *  a = [3,
 *       4]
 */
int implicitLineJoiningLevel = 0;
int startPos=-1;
}
@header { 
package org.python.antlr;
}

T169 : 'def' ;
T170 : 'print' ;
T171 : 'del' ;
T172 : 'pass' ;
T173 : 'break' ;
T174 : 'continue' ;
T175 : 'return' ;
T176 : 'raise' ;
T177 : 'import' ;
T178 : 'from' ;
T179 : 'as' ;
T180 : 'global' ;
T181 : 'exec' ;
T182 : 'in' ;
T183 : 'assert' ;
T184 : 'if' ;
T185 : 'else' ;
T186 : 'elif' ;
T187 : 'while' ;
T188 : 'for' ;
T189 : 'try' ;
T190 : 'finally' ;
T191 : 'with' ;
T192 : 'except' ;
T193 : 'is' ;
T194 : 'lambda' ;
T195 : 'class' ;
T196 : 'yield' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 820
LPAREN    : '(' {implicitLineJoiningLevel++;} ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 822
RPAREN    : ')' {implicitLineJoiningLevel--;} ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 824
LBRACK    : '[' {implicitLineJoiningLevel++;} ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 826
RBRACK    : ']' {implicitLineJoiningLevel--;} ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 828
COLON     : ':' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 830
COMMA    : ',' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 832
SEMI    : ';' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 834
PLUS    : '+' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 836
MINUS    : '-' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 838
STAR    : '*' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 840
SLASH    : '/' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 842
VBAR    : '|' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 844
AMPER    : '&' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 846
LESS    : '<' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 848
GREATER    : '>' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 850
ASSIGN    : '=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 852
PERCENT    : '%' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 854
BACKQUOTE    : '`' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 856
LCURLY    : '{' {implicitLineJoiningLevel++;} ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 858
RCURLY    : '}' {implicitLineJoiningLevel--;} ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 860
CIRCUMFLEX    : '^' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 862
TILDE    : '~' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 864
EQUAL    : '==' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 866
NOTEQUAL    : '!=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 868
ALT_NOTEQUAL: '<>' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 870
LESSEQUAL    : '<=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 872
LEFTSHIFT    : '<<' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 874
GREATEREQUAL    : '>=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 876
RIGHTSHIFT    : '>>' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 878
PLUSEQUAL    : '+=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 880
MINUSEQUAL    : '-=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 882
DOUBLESTAR    : '**' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 884
STAREQUAL    : '*=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 886
DOUBLESLASH    : '//' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 888
SLASHEQUAL    : '/=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 890
VBAREQUAL    : '|=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 892
PERCENTEQUAL    : '%=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 894
AMPEREQUAL    : '&=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 896
CIRCUMFLEXEQUAL    : '^=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 898
LEFTSHIFTEQUAL    : '<<=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 900
RIGHTSHIFTEQUAL    : '>>=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 902
DOUBLESTAREQUAL    : '**=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 904
DOUBLESLASHEQUAL    : '//=' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 906
DOT : '.' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 908
AT : '@' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 910
AND : 'and' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 912
OR : 'or' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 914
NOT : 'not' ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 916
FLOAT
    :   '.' DIGITS (Exponent)?
    |   DIGITS '.' Exponent
    |   DIGITS ('.' (DIGITS (Exponent)?)? | Exponent)
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 922
LONGINT
    :   INT ('l'|'L')
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 926
fragment
Exponent
    :    ('e' | 'E') ( '+' | '-' )? DIGITS
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 931
INT :   // Hex
        '0' ('x' | 'X') ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
    |   // Octal
        '0' DIGITS*
    |   '1'..'9' DIGITS*
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 938
COMPLEX
    :   INT ('j'|'J')
    |   FLOAT ('j'|'J')
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 943
fragment
DIGITS : ( '0' .. '9' )+ ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 946
NAME:    ( 'a' .. 'z' | 'A' .. 'Z' | '_')
        ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 950
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

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 962
/** the two '"'? cause a warning -- is there a way to avoid that? */
fragment
TRIQUOTE
    : '"'? '"'? (ESC|~('\\'|'"'))+
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 968
/** the two '\''? cause a warning -- is there a way to avoid that? */
fragment
TRIAPOS
    : '\''? '\''? (ESC|~('\\'|'\''))+
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 974
fragment
ESC
    :    '\\' .
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 979
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

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 990
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

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 1003
WS  :    {startPos>0}?=> (' '|'\t'|'\u000C')+ {$channel=HIDDEN;}
    ;
    
// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 1006
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
            ( ('\r')? '\n' {if (token!=null) token.setChannel(HIDDEN); else $channel=HIDDEN;})*
           // {token.setChannel(99); }
        )
    ;

// $ANTLR src "/Users/frank/tmp/trunk/jython/grammar/Python.g" 1036
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

