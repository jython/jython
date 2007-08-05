lexer grammar Python;
options {
  language=Java;

}
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

T155 : 'def' ;
T156 : 'print' ;
T157 : 'del' ;
T158 : 'pass' ;
T159 : 'break' ;
T160 : 'continue' ;
T161 : 'return' ;
T162 : 'yield' ;
T163 : 'raise' ;
T164 : 'import' ;
T165 : 'from' ;
T166 : 'as' ;
T167 : 'global' ;
T168 : 'exec' ;
T169 : 'in' ;
T170 : 'assert' ;
T171 : 'for' ;
T172 : 'else' ;
T173 : 'while' ;
T174 : 'if' ;
T175 : 'elif' ;
T176 : 'with' ;
T177 : 'try' ;
T178 : 'finally' ;
T179 : 'except' ;
T180 : 'or' ;
T181 : 'and' ;
T182 : 'not' ;
T183 : 'is' ;
T184 : 'lambda' ;
T185 : 'class' ;

// $ANTLR src "org/python/antlr/Python.g" 687
LPAREN	: '(' {implicitLineJoiningLevel++;} ;

// $ANTLR src "org/python/antlr/Python.g" 689
RPAREN	: ')' {implicitLineJoiningLevel--;} ;

// $ANTLR src "org/python/antlr/Python.g" 691
LBRACK	: '[' {implicitLineJoiningLevel++;} ;

// $ANTLR src "org/python/antlr/Python.g" 693
RBRACK	: ']' {implicitLineJoiningLevel--;} ;

// $ANTLR src "org/python/antlr/Python.g" 695
ATSIGN  : '@' ;

// $ANTLR src "org/python/antlr/Python.g" 697
COLON 	: ':' ;

// $ANTLR src "org/python/antlr/Python.g" 699
COMMA	: ',' ;

// $ANTLR src "org/python/antlr/Python.g" 701
SEMI	: ';' ;

// $ANTLR src "org/python/antlr/Python.g" 703
PLUS	: '+' ;

// $ANTLR src "org/python/antlr/Python.g" 705
MINUS	: '-' ;

// $ANTLR src "org/python/antlr/Python.g" 707
STAR	: '*' ;

// $ANTLR src "org/python/antlr/Python.g" 709
SLASH	: '/' ;

// $ANTLR src "org/python/antlr/Python.g" 711
VBAR	: '|' ;

// $ANTLR src "org/python/antlr/Python.g" 713
AMPER	: '&' ;

// $ANTLR src "org/python/antlr/Python.g" 715
LESS	: '<' ;

// $ANTLR src "org/python/antlr/Python.g" 717
GREATER	: '>' ;

// $ANTLR src "org/python/antlr/Python.g" 719
ASSIGN	: '=';

// $ANTLR src "org/python/antlr/Python.g" 721
PERCENT	: '%' ;

// $ANTLR src "org/python/antlr/Python.g" 723
BACKQUOTE	: '`' ;

// $ANTLR src "org/python/antlr/Python.g" 725
LCURLY	: '{' {implicitLineJoiningLevel++;} ;

// $ANTLR src "org/python/antlr/Python.g" 727
RCURLY	: '}' {implicitLineJoiningLevel--;} ;

// $ANTLR src "org/python/antlr/Python.g" 729
CIRCUMFLEX	: '^' ;

// $ANTLR src "org/python/antlr/Python.g" 731
TILDE	: '~' ;

// $ANTLR src "org/python/antlr/Python.g" 733
EQUAL	: '==' ;

// $ANTLR src "org/python/antlr/Python.g" 735
NOTEQUAL	: '!=' ;

// $ANTLR src "org/python/antlr/Python.g" 737
ALT_NOTEQUAL: '<>' ;

// $ANTLR src "org/python/antlr/Python.g" 739
LESSEQUAL	: '<=' ;

// $ANTLR src "org/python/antlr/Python.g" 741
LEFTSHIFT	: '<<' ;

// $ANTLR src "org/python/antlr/Python.g" 743
GREATEREQUAL	: '>=' ;

// $ANTLR src "org/python/antlr/Python.g" 745
RIGHTSHIFT	: '>>' ;

// $ANTLR src "org/python/antlr/Python.g" 747
PLUSEQUAL	: '+=' ;

// $ANTLR src "org/python/antlr/Python.g" 749
MINUSEQUAL	: '-=' ;

// $ANTLR src "org/python/antlr/Python.g" 751
DOUBLESTAR	: '**' ;

// $ANTLR src "org/python/antlr/Python.g" 753
STAREQUAL	: '*=' ;

// $ANTLR src "org/python/antlr/Python.g" 755
DOUBLESLASH	: '//' ;

// $ANTLR src "org/python/antlr/Python.g" 757
SLASHEQUAL	: '/=' ;

// $ANTLR src "org/python/antlr/Python.g" 759
VBAREQUAL	: '|=' ;

// $ANTLR src "org/python/antlr/Python.g" 761
PERCENTEQUAL	: '%=' ;

// $ANTLR src "org/python/antlr/Python.g" 763
AMPEREQUAL	: '&=' ;

// $ANTLR src "org/python/antlr/Python.g" 765
CIRCUMFLEXEQUAL	: '^=' ;

// $ANTLR src "org/python/antlr/Python.g" 767
LEFTSHIFTEQUAL	: '<<=' ;

// $ANTLR src "org/python/antlr/Python.g" 769
RIGHTSHIFTEQUAL	: '>>=' ;

// $ANTLR src "org/python/antlr/Python.g" 771
DOUBLESTAREQUAL	: '**=' ;

// $ANTLR src "org/python/antlr/Python.g" 773
DOUBLESLASHEQUAL	: '//=' ;

// $ANTLR src "org/python/antlr/Python.g" 775
DOT : '.' ;

// $ANTLR src "org/python/antlr/Python.g" 777
FLOAT
	:	'.' DIGITS (Exponent)?
    |   DIGITS ('.' (DIGITS (Exponent)?)? | Exponent)
    ;

// $ANTLR src "org/python/antlr/Python.g" 786
LONGINT
    :   INT ('l'|'L')
    ;



// $ANTLR src "org/python/antlr/Python.g" 792
fragment
Exponent
	:	('e' | 'E') ( '+' | '-' )? DIGITS
	;

// $ANTLR src "org/python/antlr/Python.g" 797
INT:    (options{greedy=false;}:MINUS)?
	(
	// Hex
        '0' ('x' | 'X') ( '0' .. '9' | 'a' .. 'f' | 'A' .. 'F' )+
        ('l' | 'L')?
    |   // Octal
        '0' DIGITS*
    |   '1'..'9' DIGITS*)
    ;
	
// $ANTLR src "org/python/antlr/Python.g" 807
COMPLEX
    :   INT ('j'|'J')
    |   FLOAT ('j'|'J')
    ;

// $ANTLR src "org/python/antlr/Python.g" 812
fragment
DIGITS : ( '0' .. '9' )+ ;

// $ANTLR src "org/python/antlr/Python.g" 815
NAME:	( 'a' .. 'z' | 'A' .. 'Z' | '_')
        ( 'a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9' )*
    ;

// $ANTLR src "org/python/antlr/Python.g" 819
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

// $ANTLR src "org/python/antlr/Python.g" 831
fragment
ESC
	:	'\\' .
	;

// $ANTLR src "org/python/antlr/Python.g" 836
/** Consume a newline and any whitespace at start of next line */
CONTINUED_LINE
	:	'\\' ('\r')? '\n' (' '|'\t')* { $channel=HIDDEN; }
	;

// $ANTLR src "org/python/antlr/Python.g" 841
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

// $ANTLR src "org/python/antlr/Python.g" 852
WS	:	{startPos>0}?=> (' '|'\t')+ {$channel=HIDDEN;}
	;
	
// $ANTLR src "org/python/antlr/Python.g" 855
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

// $ANTLR src "org/python/antlr/Python.g" 894
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
