package org.apache.oro.text.regex;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation", "Jakarta-Oro" 
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache" 
 *    or "Jakarta-Oro", nor may "Apache" or "Jakarta-Oro" appear in their 
 *    name, without prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon software originally written 
 * by Daniel F. Savarese. We appreciate his contributions.
 */

/**
 * The OpCode class should not be instantiated.  It is a holder of various
 * constants and static methods pertaining to the manipulation of the 
 * op-codes used in a compiled regular expression.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$
 */
final class OpCode {

  private OpCode() { }

  // Names, values, and descriptions of operators correspond to those of
  // Perl regex bytecodes and for compatibility purposes are drawn from
  // regcomp.h in the Perl source tree by Larry Wall.
  static final char  // Has Operand   Meaning
     _END     = 0,   // no       End of program.
     _BOL     = 1,   // no       Match "" at beginning of line.
     _MBOL    = 2,   // no       Same, assuming multiline.
     _SBOL    = 3,   // no       Same, assuming singleline.
     _EOL     = 4,   // no       Match "" at end of line.
     _MEOL    = 5,   // no       Same, assuming multiline.
     _SEOL    = 6,   // no       Same, assuming singleline.
     _ANY     = 7,   // no       Match any one character (except newline).
     _SANY    = 8,   // no       Match any one character.
     _ANYOF   = 9,   // yes      Match character in (or not in) this class.
     _CURLY   = 10,  // yes      Match this simple thing {n,m} times.
     _CURLYX  = 11,  // yes      Match this complex thing {n,m} times.
     _BRANCH  = 12,  // yes      Match this alternative, or the next...
     _BACK    = 13,  // no       Match "", "next" ptr points backward.
     _EXACTLY = 14,  // yes      Match this string (preceded by length).
     _NOTHING = 15,  // no       Match empty string.
     _STAR    = 16,  // yes      Match this (simple) thing 0 or more times.
     _PLUS    = 17,  // yes      Match this (simple) thing 1 or more times.
     _ALNUM   = 18,  // no       Match any alphanumeric character
     _NALNUM  = 19,  // no       Match any non-alphanumeric character
     _BOUND   = 20,  // no       Match "" at any word boundary
     _NBOUND  = 21,  // no       Match "" at any word non-boundary
     _SPACE   = 22,  // no       Match any whitespace character
     _NSPACE  = 23,  // no       Match any non-whitespace character
     _DIGIT   = 24,  // no       Match any numeric character
     _NDIGIT  = 25,  // no       Match any non-numeric character
     _REF     = 26,  // yes      Match some already matched string
     _OPEN    = 27,  // yes      Mark this point in input as start of #n.
     _CLOSE   = 28,  // yes      Analogous to OPEN.
     _MINMOD  = 29,  // no       Next operator is not greedy.
     _GBOL    = 30,  // no       Matches where last m//g left off.
     _IFMATCH = 31,  // no       Succeeds if the following matches.
     _UNLESSM = 32,  // no       Fails if the following matches.
     _SUCCEED = 33,  // no       Return from a subroutine, basically.
     _WHILEM  = 34;  // no       Do curly processing and see if rest matches.

  // Lengths of the various operands.
  static final int _operandLength[] = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0
  };

  static final char _opType[] = {
	_END, _BOL, _BOL, _BOL, _EOL, _EOL, _EOL, _ANY, _ANY, _ANYOF, _CURLY,
	_CURLY, _BRANCH, _BACK, _EXACTLY, _NOTHING, _STAR, _PLUS, _ALNUM,
	_NALNUM, _BOUND, _NBOUND, _SPACE, _NSPACE, _DIGIT, _NDIGIT, _REF,
	_OPEN, _CLOSE, _MINMOD,	_BOL, _BRANCH, _BRANCH, _END, _WHILEM
  };

  static final char _opLengthVaries[] = {
    _BRANCH, _BACK, _STAR, _PLUS, _CURLY, _CURLYX, _REF, _WHILEM
  };

  static final char _opLengthOne[] = {
    _ANY, _SANY, _ANYOF, _ALNUM, _NALNUM, _SPACE, _NSPACE, _DIGIT, _NDIGIT
  };

  static final int  _NULL_OFFSET  = -1;
  static final char _NULL_POINTER =  0;

  static final int _getNextOffset(char[] program, int offset) {
    return ((int)program[offset + 1]); 
  }

  static final char _getArg1(char[] program, int offset) {
    return program[offset + 2]; 
  }

  static final char _getArg2(char[] program, int offset) {
    return program[offset + 3]; 
  }

  static final int _getOperand(int offset) {
    return (offset + 2);
  }

  static final boolean _isInArray(char ch, char[] array, int start) {
    while(start < array.length)
      if(ch == array[start++])
	return true;
    return false;
  }

  static final int _getNextOperator(int offset) { return (offset + 2); }
  static final int _getPrevOperator(int offset) { return (offset - 2); }

  static final int _getNext(char[] program, int offset) {
    int offs;

    if(program == null)
      return _NULL_OFFSET;


    offs = _getNextOffset(program, offset);
    if(offs == _NULL_POINTER)
      return _NULL_OFFSET;

    if(program[offset] == OpCode._BACK)
      return (offset - offs);

    return (offset + offs);
  }

  // doesn't really belong in this class, but we want Perl5Matcher not to
  // depend on Perl5Compiler
  static final boolean _isWordCharacter(char token) {
    return ((token >= 'a' && token <= 'z') ||
            (token >= 'A' && token <= 'Z') ||
            (token >= '0' && token <= '9') ||
            (token == '_'));
  }        
}
