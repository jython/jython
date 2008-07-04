package org.python.antlr;

/*
 [The "BSD licence"]
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

import org.antlr.runtime.*;
import java.util.*;

/** This file is a copy of PythonTokenSource with some comments removed
 *  so I can play with it to implement interactive mode partial parsing.
 */

public class PythonPartialTokenSource implements TokenSource {
    public static final int MAX_INDENTS = 100;
    public static final int FIRST_CHAR_POSITION = 0;

    /** The stack of indent levels (column numbers) */
    int[] indentStack = new int[MAX_INDENTS];
    /** stack pointer */
    int sp=-1; // grow upwards

    /** The queue of tokens */
    Vector tokens = new Vector();

    /** We pull real tokens from this lexer */
    CommonTokenStream stream;

    int lastTokenAddedIndex = -1;

    boolean atEnd = false;

    public PythonPartialTokenSource(PythonLexer lexer) {
    }

    public PythonPartialTokenSource(CommonTokenStream stream) {
        this.stream = stream;
        // "state" of indent level is FIRST_CHAR_POSITION
        push(FIRST_CHAR_POSITION);
    }

    public Token nextToken() {
        // if something in queue, just remove and return it
        if ( tokens.size()>0 ) {
            Token t = (Token)tokens.firstElement();
            tokens.removeElementAt(0);
            //System.out.println(t);
            return t;
        }

        insertImaginaryIndentDedentTokens();

        return nextToken();
    }

    protected void insertImaginaryIndentDedentTokens()
    {
        Token t = stream.LT(1);
        stream.consume();
        if ( t.getType()==Token.EOF ) {
            atEnd = true;
            Token em = new ClassicToken(PythonPartialParser.ENDMARKER,"");
            em.setCharPositionInLine(t.getCharPositionInLine());
            em.setLine(t.getLine());
            tokens.addElement(em);
         }

        // if not a NEWLINE, doesn't signal indent/dedent work; just enqueue
        if ( t.getType()!=PythonPartialLexer.NEWLINE ) {
            List hiddenTokens = stream.getTokens(lastTokenAddedIndex+1,t.getTokenIndex()-1);
            if ( hiddenTokens!=null ) {
                tokens.addAll(hiddenTokens);
            }
            lastTokenAddedIndex = t.getTokenIndex();
            tokens.addElement(t);
            return;
        }

        // save NEWLINE in the queue
        //System.out.println("found newline: "+t+" stack is "+stackString());
        List hiddenTokens = stream.getTokens(lastTokenAddedIndex+1,t.getTokenIndex()-1);
        if ( hiddenTokens!=null ) {
            tokens.addAll(hiddenTokens);
        }
        lastTokenAddedIndex = t.getTokenIndex();
        tokens.addElement(t);

        // grab first token of next line
        t = stream.LT(1);
        stream.consume();

        hiddenTokens = stream.getTokens(lastTokenAddedIndex+1,t.getTokenIndex()-1);
        if ( hiddenTokens!=null ) {
            tokens.addAll(hiddenTokens);
        }
        lastTokenAddedIndex = t.getTokenIndex();

        // compute cpos as the char pos of next non-WS token in line
        int cpos = t.getCharPositionInLine(); // column dictates indent/dedent
        if ( t.getType()==Token.EOF ) {
            atEnd = true;
            Token em = new ClassicToken(PythonPartialParser.ENDMARKER,"");
            em.setCharPositionInLine(t.getCharPositionInLine());
            em.setLine(t.getLine());
            tokens.addElement(em);
            
            cpos = -1; // pretend EOF always happens at left edge
        }
        else if ( t.getType()==PythonPartialLexer.LEADING_WS ) {
            cpos = t.getText().length();
        }

        //System.out.println("next token is: "+t);

        // compare to last indent level
        int lastIndent = peek();
        //System.out.println("cpos, lastIndent = "+cpos+", "+lastIndent);
        if ( cpos > lastIndent ) { // they indented; track and gen INDENT
            push(cpos);
            //System.out.println("push("+cpos+"): "+stackString());
            Token indent = new ClassicToken(PythonPartialParser.INDENT,"");
            indent.setCharPositionInLine(t.getCharPositionInLine());
            indent.setLine(t.getLine());
            tokens.addElement(indent);
        }
        else if ( cpos < lastIndent ) { // they dedented
            // how far back did we dedent?
            int prevIndex = findPreviousIndent(cpos);
            //System.out.println("dedented; prevIndex of cpos="+cpos+" is "+prevIndex);
            // generate DEDENTs for each indent level we backed up over
            for (int d=sp-1; d>=prevIndex; d--) {
                Token tok;
                if (atEnd) {
                    tok = new ClassicToken(PythonPartialParser.ENDMARKER,"");
                } else {
                    tok = new ClassicToken(PythonPartialParser.DEDENT,"");
                }
                tok.setCharPositionInLine(t.getCharPositionInLine());
                tok.setLine(t.getLine());
                tokens.addElement(tok);
            }
            sp = prevIndex; // pop those off indent level
        }
        if ( t.getType()!=PythonPartialLexer.LEADING_WS ) { // discard WS
            tokens.addElement(t);
        }
    }

    //  T O K E N  S T A C K  M E T H O D S

    protected void push(int i) {
        if (sp>=MAX_INDENTS) {
            throw new IllegalStateException("stack overflow");
        }
        sp++;
        indentStack[sp] = i;
    }

    protected int pop() {
        if (sp<0) {
            throw new IllegalStateException("stack underflow");
        }
        int top = indentStack[sp];
        sp--;
        return top;
    }

    protected int peek() {
        return indentStack[sp];
    }

    /** Return the index on stack of previous indent level == i else -1 */
    protected int findPreviousIndent(int i) {
        for (int j=sp-1; j>=0; j--) {
            if ( indentStack[j]==i ) {
                return j;
            }
        }
        return FIRST_CHAR_POSITION;
    }

    public String stackString() {
        StringBuffer buf = new StringBuffer();
        for (int j=sp; j>=0; j--) {
            buf.append(" ");
            buf.append(indentStack[j]);
        }
        return buf.toString();
    }

    //FIXME: needed this for the Antlr 3.1b interface change.
    public String getSourceName() {
        return "XXX-need-real-name.py";
    }

}
