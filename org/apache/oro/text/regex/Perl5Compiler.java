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
 * The Perl5Compiler class is used to create compiled regular expressions
 * conforming to the Perl5 regular expression syntax.  It generates
 * Perl5Pattern instances upon compilation to be used in conjunction
 * with a Perl5Matcher instance.  Please see the user's guide for more 
 * information about Perl5 regular expressions.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see PatternCompiler
 * @see MalformedPatternException
 * @see Perl5Pattern
 * @see Perl5Matcher
 */

public final class Perl5Compiler implements PatternCompiler {
  private static final int __WORSTCASE = 0, __NONNULL = 0x1, __SIMPLE = 0x2,
                           __SPSTART = 0x4, __TRYAGAIN = 0x8;

  private static final char
    __CASE_INSENSITIVE = 0x0001,
    __GLOBAL           = 0x0002,
    __KEEP             = 0x0004,
    __MULTILINE        = 0x0008,
    __SINGLELINE       = 0x0010,
    __EXTENDED         = 0x0020,
    __READ_ONLY        = 0x8000;

  private static final String __META_CHARS = "^$.[()|?+*\\";
  private static final String __HEX_DIGIT =
  "0123456789abcdef0123456789ABCDEFx";
  private CharStringPointer __input;
  private boolean __sawBackreference;
  private char[] __modifierFlags = { 0 };

  // IMPORTANT: __numParentheses starts out equal to 1 during compilation.
  // It is always one greater than the number of parentheses encountered
  // so far in the regex.  That is because it refers to the number of groups
  // to save, and the entire match is always saved (group 0)
  private int __numParentheses, __programSize, __cost;

  // When doing the second pass and actually generating code, __programSize
  // keeps track of the current offset.
  private char[] __program;

  /**
   * The default mask for the {@link #compile compile} methods.
   * It is equal to 0.
   * The default behavior is for a regular expression to be case sensitive
   * and to not specify if it is multiline or singleline.  When MULITLINE_MASK
   * and SINGLINE_MASK are not defined, the <b>^</b>, <b>$</b>, and <b>.</b>
   * metacharacters are
   * interpreted according to the value of isMultiline() in Perl5Matcher.
   * The default behavior of Perl5Matcher is to treat the Perl5Pattern
   * as though MULTILINE_MASK were enabled.  If isMultiline() returns false,
   * then the pattern is treated as though SINGLINE_MASK were set.  However,
   * compiling a pattern with the MULTILINE_MASK or SINGLELINE_MASK masks
   * will ALWAYS override whatever behavior is specified by the setMultiline()
   * in Perl5Matcher.
   */
  public static final int DEFAULT_MASK          = 0;

  /**
   * A mask passed as an option to the {@link #compile compile} methods
   * to indicate a compiled regular expression should be case insensitive.
   */
  public static final int CASE_INSENSITIVE_MASK = __CASE_INSENSITIVE;

  /**
   * A mask passed as an option to the  {@link #compile compile} methods
   * to indicate a compiled regular expression should treat input as having
   * multiple lines.  This option affects the interpretation of
   * the <b>^</b> and <b>$</b> metacharacters.  When this mask is used,
   * the <b>^</b> metacharacter matches at the beginning of every line,
   * and the <b>$</b> metacharacter matches at the end of every line.
   * Additionally the <b> . </b> metacharacter will not match newlines when
   * an expression is compiled with <b> MULTILINE_MASK </b>, which is its
   * default behavior.
   * The <b>SINGLELINE_MASK</b> and <b>MULTILINE_MASK</b> should not be
   * used together.
   */
  public static final int MULTILINE_MASK        = __MULTILINE;

  /**
   * A mask passed as an option to the {@link #compile compile} methods
   * to indicate a compiled regular expression should treat input as being
   * a single line.  This option affects the interpretation of
   * the <b>^</b> and <b>$</b> metacharacters.  When this mask is used,
   * the <b>^</b> metacharacter matches at the beginning of the input,
   * and the <b>$</b> metacharacter matches at the end of the input.
   * The <b>^</b> and <b>$</b> metacharacters will not match at the beginning
   * and end of lines occurring between the begnning and end of the input.
   * Additionally, the <b> . </b> metacharacter will match newlines when
   * an expression is compiled with <b> SINGLELINE_MASK </b>, unlike its
   * default behavior.
   * The <b>SINGLELINE_MASK</b> and <b>MULTILINE_MASK</b> should not be
   * used together.
   */
  public static final int SINGLELINE_MASK       = __SINGLELINE;

  /**
   * A mask passed as an option to the {@link #compile compile} methods
   * to indicate a compiled regular expression should be treated as a Perl5
   * extended pattern (i.e., a pattern using the <b>/x</b> modifier).  This 
   * option tells the compiler to ignore whitespace that is not backslashed or
   * within a character class.  It also tells the compiler to treat the
   * <b>#</b> character as a metacharacter introducing a comment as in
   * Perl.  In other words, the <b>#</b> character will comment out any
   * text in the regular expression between it and the next newline.
   * The intent of this option is to allow you to divide your patterns
   * into more readable parts.  It is provided to maintain compatibility
   * with Perl5 regular expressions, although it will not often
   * make sense to use it in Java.
   */
  public static final int EXTENDED_MASK         = __EXTENDED;

  /**
   * A mask passed as an option to the {@link #compile compile} methods
   * to indicate that the resulting Perl5Pattern should be treated as a
   * read only data structure by Perl5Matcher, making it safe to share
   * a single Perl5Pattern instance among multiple threads without needing
   * synchronization.  Without this option, Perl5Matcher reserves the right
   * to store heuristic or other information in Perl5Pattern that might
   * accelerate future matches.  When you use this option, Perl5Matcher will
   * not store or modify any information in a Perl5Pattern.  Use this option
   * when you want to share a Perl5Pattern instance among multiple threads
   * using different Perl5Matcher instances.
   */
  public static final int READ_ONLY_MASK        = __READ_ONLY;

  /**
   * Given a character string, returns a Perl5 expression that interprets
   * each character of the original string literally.  In other words, all
   * special metacharacters are quoted/escaped.  This method is useful for
   * converting user input meant for literal interpretation into a safe
   * regular expression representing the literal input.
   * <p>
   * In effect, this method is the analog of the Perl5 quotemeta() builtin
   * method.
   * <p>
   * @param expression The expression to convert.
   * @return A String containing a Perl5 regular expression corresponding to
   *         a literal interpretation of the pattern.
   */
  public static final String quotemeta(char[] expression) {
    int ch;
    StringBuffer buffer;

    buffer = new StringBuffer(2*expression.length);
    for(ch = 0; ch < expression.length; ch++) {
      if(!OpCode._isWordCharacter(expression[ch]))
	buffer.append('\\');
      buffer.append(expression[ch]);
    }

    return buffer.toString();
  }

  /**
   * Given a character string, returns a Perl5 expression that interprets
   * each character of the original string literally.  In other words, all
   * special metacharacters are quoted/escaped.  This method is useful for
   * converting user input meant for literal interpretation into a safe
   * regular expression representing the literal input.
   * <p>
   * In effect, this method is the analog of the Perl5 quotemeta() builtin
   * method.
   * <p>
   * @param pattern The pattern to convert.
   * @return A String containing a Perl5 regular expression corresponding to
   *         a literal interpretation of the pattern.
   */
  public static final String quotemeta(String expression) {
    return quotemeta(expression.toCharArray());
  }

  private static boolean __isSimpleRepetitionOp(char ch) {
    return (ch == '*' || ch == '+' || ch == '?');
  }

  private static boolean __isComplexRepetitionOp(char[] ch, int offset) {
    if(offset < ch.length && offset >= 0)
       return (ch[offset] == '*' || ch[offset] == '+' || ch[offset] == '?'
	       || (ch[offset] == '{' && __parseRepetition(ch, offset)));
    return false;
  }

  // determines if {\d+,\d*} is the next part of the string
  private static boolean __parseRepetition(char[] str, int offset) {
    if(str[offset] != '{')
      return false;
    ++offset;

    if(offset >= str.length || !Character.isDigit(str[offset]))
      return false;

    while(offset < str.length && Character.isDigit(str[offset]))
      ++offset;

    if(offset < str.length && str[offset] == ',')
      ++offset;

    while(offset < str.length && Character.isDigit(str[offset]))
      ++offset;

    if(offset >= str.length || str[offset] != '}')
      return false;

    return true;
  }

  private static int __parseHex(char[] str, int offset, int maxLength,
				int[] scanned)
  {
    int val = 0, index;

    scanned[0] = 0;
    while(offset < str.length && maxLength-- > 0 &&
	  (index = __HEX_DIGIT.indexOf(str[offset])) != -1) {
      val <<= 4;
      val |= (index & 15);
      ++offset;
      ++scanned[0];
    }

    return val;
  }

  private static int __parseOctal(char[] str, int offset, int maxLength,
				 int[] scanned)
  {
    int val = 0, index;

    scanned[0] = 0;
    while(offset < str.length && 
	  maxLength > 0 && str[offset] >= '0' && str[offset] <= '7') {
      val <<= 3;
      val |= (str[offset] - '0');
      --maxLength;
      ++offset;
      ++scanned[0];
    }

    return val;
  }

  private static void __setModifierFlag(char[] flags, char ch) {
    switch(ch) {
    case 'i' : flags[0] |= __CASE_INSENSITIVE; return;
    case 'g' : flags[0] |= __GLOBAL; return;
    case 'o' : flags[0] |= __KEEP; return;
    case 'm' : flags[0] |= __MULTILINE; return;
    case 's' : flags[0] |= __SINGLELINE; return;
    case 'x' : flags[0] |= __EXTENDED; return;
    }
  }

  // Emit a specific character code.
  private void __emitCode(char code) {

    if(__program != null)
      __program[__programSize] = code;

    ++__programSize;
  }


  // Emit an operator with no arguments.
  // Return an offset into the __program array as a pointer to node.
  private int __emitNode(char operator) {
    int offset;

    offset = __programSize;

    if(__program == null)
      __programSize+=2;
    else {
      __program[__programSize++] = operator;
      __program[__programSize++] = OpCode._NULL_POINTER;
    }

    return offset;
  }


  // Emit an operator with arguments.
  // Return an offset into the __programarray as a pointer to node.
  private int __emitArgNode(char operator, char arg) {
    int offset;

    offset = __programSize;

    if(__program== null)
      __programSize+=3;
    else {
      __program[__programSize++] = operator;
      __program[__programSize++] = OpCode._NULL_POINTER;
      __program[__programSize++] = arg;
    }

    return offset;
  }


  // Insert an operator at a given offset.
  private void __programInsertOperator(char operator, int operand) {
    int src, dest, offset;

    offset = (OpCode._opType[operator] == OpCode._CURLY ? 2 : 0);


    if(__program== null) {
      __programSize+=(2 + offset);
      return;
    }

    src = __programSize;
    __programSize+=(2 + offset);
    dest = __programSize;

    while(src > operand) {
      --src;
      --dest;
      __program[dest] = __program[src];
    }

    __program[operand++] = operator;
    __program[operand++] = OpCode._NULL_POINTER;

    while(offset-- > 0)
      __program[operand++] = OpCode._NULL_POINTER;

  }



  private void __programAddTail(int current, int value) {
    int scan, temp, offset;

    if(__program== null || current == OpCode._NULL_OFFSET)
      return;

    scan = current;

    while(true) {
      temp = OpCode._getNext(__program, scan);
      if(temp == OpCode._NULL_OFFSET)
	break;
      scan = temp;
    }

    if(__program[scan] == OpCode._BACK)
      offset = scan - value;
    else
      offset = value - scan;

    __program[scan + 1] = (char)offset;
  }


  private void __programAddOperatorTail(int current, int value) {
    if(__program== null || current == OpCode._NULL_OFFSET ||
       OpCode._opType[__program[current]] != OpCode._BRANCH)
      return;
    __programAddTail(OpCode._getNextOperator(current), value);
  }


  private char __getNextChar() {
    char ret, value;

    ret = __input._postIncrement();

    while(true) {
      value = __input._getValue();

      if(value == '(' && __input._getValueRelative(1) == '?' &&
	 __input._getValueRelative(2) == '#') {
	// Skip comments
	while(value != CharStringPointer._END_OF_STRING && value != ')')
	  value = __input._increment();
	__input._increment();
	continue;
      }

      if((__modifierFlags[0] & __EXTENDED) != 0) {
	if(Character.isWhitespace(value)) {
	  __input._increment();
	  continue;
	} else if(value == '#') {
	  while(value != CharStringPointer._END_OF_STRING && value != '\n')
	    value = __input._increment();
	  __input._increment();
	  continue;
	}
      }

      // System.err.println("next: " + ret + " last: " + __input._getValue()); // debug


      return ret;
    }

  }


  private int __parseAlternation(int[] retFlags)
    throws MalformedPatternException 
  {
    int chain, offset, latest;
    int flags = 0;
    char value;

    retFlags[0] = __WORSTCASE;

    offset = __emitNode(OpCode._BRANCH);

    chain  = OpCode._NULL_OFFSET;

    if(__input._getOffset() == 0) {
      __input._setOffset(-1);
      __getNextChar();
    } else {
      __input._decrement();
      __getNextChar();
    }

    value = __input._getValue();

    while(value != CharStringPointer._END_OF_STRING &&
	  value != '|' && value != ')') {
      flags &= ~__TRYAGAIN;
      latest = __parseBranch(retFlags);

      if(latest == OpCode._NULL_OFFSET) {
	if((flags & __TRYAGAIN) != 0){
	  value = __input._getValue();
	  continue;
	}
	return OpCode._NULL_OFFSET;
      }

      retFlags[0] |= (flags & __NONNULL);

      if(chain == OpCode._NULL_OFFSET)
	retFlags[0] |= (flags & __SPSTART);
      else {
	++__cost;
	__programAddTail(chain, latest);
      }
      chain = latest;
      value = __input._getValue();
    }

    // If loop was never entered.
    if(chain == OpCode._NULL_OFFSET)
      __emitNode(OpCode._NOTHING);

    return offset;
  }


  private int __parseAtom(int[] retFlags) throws MalformedPatternException {
    boolean doDefault;
    char value;
    int offset, flags[] = { 0 };
    
    
    retFlags[0] = __WORSTCASE;
    doDefault = false;
    offset = OpCode._NULL_OFFSET;

  tryAgain:
    while(true) {

      value = __input._getValue();

      switch(value) {
      case '^' :
	__getNextChar();
	// The order here is important in order to support /ms.
	// /m takes precedence over /s for ^ and $, but not for .
	if((__modifierFlags[0] & __MULTILINE) != 0)
	  offset = __emitNode(OpCode._MBOL);
	else if((__modifierFlags[0] & __SINGLELINE) != 0)
	  offset = __emitNode(OpCode._SBOL);
	else
	  offset = __emitNode(OpCode._BOL);
	break tryAgain;

      case '$':
	__getNextChar();
	// The order here is important in order to support /ms.
	// /m takes precedence over /s for ^ and $, but not for .
	if((__modifierFlags[0] & __MULTILINE) != 0)
	  offset = __emitNode(OpCode._MEOL);
	else if((__modifierFlags[0] & __SINGLELINE) != 0)
	  offset = __emitNode(OpCode._SEOL);
	else
	  offset = __emitNode(OpCode._EOL);
	break tryAgain;

      case '.':
	__getNextChar();
	// The order here is important in order to support /ms.
	// /m takes precedence over /s for ^ and $, but not for .
	if((__modifierFlags[0] & __SINGLELINE) != 0)
	  offset = __emitNode(OpCode._SANY);
	else
	  offset = __emitNode(OpCode._ANY);
	++__cost;
	retFlags[0] |= (__NONNULL | __SIMPLE);
	break tryAgain;

      case '[':
	__input._increment();
	offset = __parseCharacterClass();
	retFlags[0] |= (__NONNULL | __SIMPLE);
	break tryAgain;

      case '(':
	__getNextChar();
	offset = __parseExpression(true, flags);
	if(offset == OpCode._NULL_OFFSET) {
	  if((flags[0] & __TRYAGAIN) != 0)
	    continue tryAgain;
	  return OpCode._NULL_OFFSET;
	}
	retFlags[0] |= (flags[0] & (__NONNULL | __SPSTART));
	break tryAgain;

      case '|':
      case ')':
	if((flags[0] & __TRYAGAIN) != 0) {
	  retFlags[0] |= __TRYAGAIN;
	  return OpCode._NULL_OFFSET;
	}

	throw new MalformedPatternException("Error in expression at " +
				   __input._toString(__input._getOffset()));
	//break tryAgain;

      case '?':
      case '+':
      case '*':
	throw new MalformedPatternException(
                 "?+* follows nothing in expression");
	//break tryAgain;

      case '\\':
	value = __input._increment();

	switch(value) {
	case 'A' :
	  offset = __emitNode(OpCode._SBOL);
	  retFlags[0] |= __SIMPLE;
	  __getNextChar();
	  break;
	case 'G':
	  offset = __emitNode(OpCode._GBOL);
	  retFlags[0] |= __SIMPLE;
	  __getNextChar();
	  break;
	case 'Z':
	  offset = __emitNode(OpCode._SEOL);
	  retFlags[0] |= __SIMPLE;
	  __getNextChar();
	  break;
	case 'w':
	  offset = __emitNode(OpCode._ALNUM);
	  retFlags[0] |= (__NONNULL | __SIMPLE);
	  __getNextChar();
	  break;
	case 'W':
	  offset = __emitNode(OpCode._NALNUM);
	  retFlags[0] |= (__NONNULL | __SIMPLE);
	  __getNextChar();
	  break;
	case 'b':
	  offset = __emitNode(OpCode._BOUND);
	  retFlags[0] |= __SIMPLE;
	  __getNextChar();
	  break;
	case 'B':
	  offset = __emitNode(OpCode._NBOUND);
	  retFlags[0] |= __SIMPLE;
	  __getNextChar();
	  break;
	case 's':
	  offset = __emitNode(OpCode._SPACE);
	  retFlags[0] |= (__NONNULL | __SIMPLE);
	  __getNextChar();
	  break;
	case 'S':
	  offset = __emitNode(OpCode._NSPACE);
	  retFlags[0] |= (__NONNULL | __SIMPLE);
	  __getNextChar();
	  break;
	case 'd':
	  offset = __emitNode(OpCode._DIGIT);
	  retFlags[0] |= (__NONNULL | __SIMPLE);
	  __getNextChar();
	  break;
	case 'D':
	  offset = __emitNode(OpCode._NDIGIT);
	  retFlags[0] |= (__NONNULL | __SIMPLE);
	  __getNextChar();
	  break;
	case 'n': case 'r': case 't': case 'f': case 'e': case 'a': case 'x':
	case 'c': case '0':
	  doDefault = true;
	  break tryAgain;
	case '1': case '2': case '3': case '4': case '5': case '6': case '7':
	case '8': case '9':
	  int num;
	  StringBuffer buffer = new StringBuffer(10);

	  num = 0;
	  value = __input._getValueRelative(num);

	  while(Character.isDigit(value)) {
	    buffer.append(value);
	    ++num;
	    value = __input._getValueRelative(num);
	  }

	  try {
	    num = Integer.parseInt(buffer.toString());
	  } catch(NumberFormatException e) {
	    throw new MalformedPatternException(
	   "Unexpected number format exception.  Please report this bug." +
	   "NumberFormatException message: " + e.getMessage());
	  }

	  if(num > 9 && num >= __numParentheses) {
	    doDefault = true;
	    break tryAgain;
	  } else {
	    // A backreference may only occur AFTER its group
	    if(num >= __numParentheses)
	      throw new MalformedPatternException("Invalid backreference: \\" +
						  num);
	    __sawBackreference = true;
	    offset = __emitArgNode(OpCode._REF, (char)num);
	    retFlags[0] |= __NONNULL;

	    value = __input._getValue();
	    while(Character.isDigit(value))
	      value = __input._increment();

	    __input._decrement();
	    __getNextChar();
	  }
	  break;
	case '\0':
	case CharStringPointer._END_OF_STRING:
	  if(__input._isAtEnd())
	    throw new
	      MalformedPatternException("Trailing \\ in expression.");
	  // fall through to default
	default:
	  doDefault = true;
	  break tryAgain;
	}
	break tryAgain;

      case '#':
	// skip over comments
	if((__modifierFlags[0] & __EXTENDED) != 0) {
	  while(!__input._isAtEnd() && __input._getValue() != '\n')
	    __input._increment();
	  if(!__input._isAtEnd())
	    continue tryAgain;
	}
	// fall through to default
      default:
	__input._increment();
	doDefault = true;
	break tryAgain;
      }// end master switch
    } // end tryAgain


    if(doDefault) {
      char ender;
      int length, pOffset, maxOffset, lastOffset, numLength[];

      offset = __emitNode(OpCode._EXACTLY);
      // Not sure that it's ok to use 0 to mark end.
      //__emitCode((char)0);
      __emitCode((char)CharStringPointer._END_OF_STRING);

    forLoop:
      for(length = 0, pOffset = __input._getOffset() - 1,
	    maxOffset = __input._getLength();
	  length < 127 && pOffset < maxOffset; ++length) {

	lastOffset = pOffset;
	value = __input._getValue(pOffset);

	switch(value) {
	case '^': case '$': case '.': case '[': case '(': case ')':
	case '|':
	  break forLoop;
	case '\\':
	  value = __input._getValue(++pOffset);

	  switch(value) {
	  case 'A': case 'G': case 'Z': case 'w': case 'W': case 'b':
	  case 'B': case 's': case 'S': case 'd': case 'D':
	    --pOffset;
	    break forLoop;
	  case 'n':
	    ender = '\n';
	    ++pOffset;
	    break;
	  case 'r':
	    ender = '\r';
	    ++pOffset;
	    break;
	  case 't':
	    ender = '\t';
	    ++pOffset;
	    break;
	  case 'f':
	    ender = '\f';
	    ++pOffset;
	    break;
	  case 'e':
	    ender = '\033';
	    ++pOffset;
	    break;
	  case 'a':
	    ender = '\007';
	    ++pOffset;
	    break;
	  case 'x':
	    numLength = new int[1];
	    ender = (char)__parseHex(__input._array, ++pOffset, 2, numLength);
	    pOffset+=numLength[0];
	    break;
	  case 'c':
	    ++pOffset;
	    ender = __input._getValue(pOffset++);
	    if(Character.isLowerCase(ender))
	      ender = Character.toUpperCase(ender);
	    ender ^= 64;
	    break;
	  case '0': case '1': case '2': case'3': case '4': case '5':
	  case '6': case '7': case '8': case '9':
	    boolean doOctal = false;
	    value = __input._getValue(pOffset);

	    if(value == '0')
	      doOctal = true;
	    value = __input._getValue(pOffset + 1);

	    if(Character.isDigit(value)) {
	      int num;
	      StringBuffer buffer = new StringBuffer(10);

	      num = pOffset;
	      value = __input._getValue(num);

	      while(Character.isDigit(value)){
		buffer.append(value);
		++num;
		value = __input._getValue(num);
	      }

	      try {
		num = Integer.parseInt(buffer.toString());
	      } catch(NumberFormatException e) {
		throw new MalformedPatternException(
	     "Unexpected number format exception.  Please report this bug." +
	     "NumberFormatException message: " + e.getMessage());
	      }

	      if(!doOctal)
		doOctal = (num >= __numParentheses);
	    }

	    if(doOctal) {
	      numLength = new int[1];
	      ender = (char)__parseOctal(__input._array, pOffset, 3, numLength);
	      pOffset+=numLength[0];
	    } else {
	      --pOffset;
	      break forLoop;
	    }
	    break;

	  case CharStringPointer._END_OF_STRING:
	  case '\0':
	    if(pOffset >= maxOffset)
	      throw new
		MalformedPatternException("Trailing \\ in expression.");
	    // fall through to default
	  default:
	    ender = __input._getValue(pOffset++);
	    break;
	  } // end backslash switch
	  break;

	case '#':
	  if((__modifierFlags[0] & __EXTENDED) != 0) {
	    while(pOffset < maxOffset && __input._getValue(pOffset) != '\n')
	      ++pOffset;
	  }
	  // fall through to whitespace handling
	case ' ': case '\t': case '\n': case '\r': case '\f': case '\013':
	  if((__modifierFlags[0] & __EXTENDED) != 0) {
	    ++pOffset;
	    --length;
	    continue;
	  }
	  // fall through to default
	default:
	  ender = __input._getValue(pOffset++);
	  break;

	}   // end master switch

	if((__modifierFlags[0] & __CASE_INSENSITIVE) != 0 &&
	   Character.isUpperCase(ender))
	  ender = Character.toLowerCase(ender);

	if(pOffset < maxOffset && __isComplexRepetitionOp(__input._array, pOffset)) {
	  if(length > 0)
	    pOffset = lastOffset;
	  else {
	    ++length;
	    __emitCode(ender);
	  }
	  break;
	}

	__emitCode(ender);


      } // end for loop


      __input._setOffset(pOffset - 1);
      __getNextChar();

      if(length < 0)
	throw new MalformedPatternException(
         "Unexpected compilation failure.  Please report this bug!");
      if(length > 0)
	retFlags[0] |= __NONNULL;
      if(length == 1)
	retFlags[0] |= __SIMPLE;
      if(__program!= null)
	__program[OpCode._getOperand(offset)] = (char)length;
      //__emitCode('\0'); // debug
      __emitCode(CharStringPointer._END_OF_STRING);
    }

    return offset;
  }


  // Set the bits in a character class.  Only recognizes ascii.
  private void __setCharacterClassBits(char[] bits, int offset, char deflt,
				       char ch)
  {
    if(__program== null || ch >= 256)
      return;
    ch &= 0xffff;

    if(deflt == 0) {
      bits[offset + (ch >> 4)] |= (1 << (ch & 0xf));
    } else {
      bits[offset + (ch >> 4)] &= ~(1 << (ch & 0xf));
    }
  }


  private int __parseCharacterClass() throws MalformedPatternException {
    boolean range = false, skipTest;
    char clss, deflt, lastclss = Character.MAX_VALUE;
    int offset, bits, numLength[] = { 0 };

    offset = __emitNode(OpCode._ANYOF);

    if(__input._getValue() == '^') {
      ++__cost;
      __input._increment();
      deflt = 0;
    } else {
      deflt = 0xffff;
    }

    bits = __programSize;
    for(clss = 0; clss < 16; clss++)
      __emitCode(deflt);

    clss = __input._getValue();

    if(clss == ']' || clss == '-')
      skipTest = true;
    else
      skipTest = false;

    while((!__input._isAtEnd() && (clss = __input._getValue()) != ']')
	  || skipTest) {
      // It sucks, but we have to make this assignment every time
      skipTest = false;
      __input._increment();
      if(clss == '\\') {
	clss = __input._postIncrement();

	switch(clss){
	case 'w':
	  for(clss = 0; clss < 256; clss++)
	    if(OpCode._isWordCharacter(clss))
	      __setCharacterClassBits(__program, bits, deflt, clss);
	  lastclss = Character.MAX_VALUE;
	  continue;
	case 'W':
	  for(clss = 0; clss < 256; clss++)
	    if(!OpCode._isWordCharacter(clss))
	      __setCharacterClassBits(__program, bits, deflt, clss);
	  lastclss = Character.MAX_VALUE;
	  continue;
	case 's':
	  for(clss = 0; clss < 256; clss++)
	    if(Character.isWhitespace(clss))
	      __setCharacterClassBits(__program, bits, deflt, clss);
	  lastclss = Character.MAX_VALUE;
	  continue;
	case 'S':
	  for(clss = 0; clss < 256; clss++)
	    if(!Character.isWhitespace(clss))
	      __setCharacterClassBits(__program, bits, deflt, clss);
	  lastclss = Character.MAX_VALUE;
	  continue;
	case 'd':
	  for(clss = '0'; clss <= '9'; clss++)
	    __setCharacterClassBits(__program, bits, deflt, clss);
	  lastclss = Character.MAX_VALUE;
	  continue;
	case 'D':
	  for(clss = 0; clss < '0'; clss++)
	    __setCharacterClassBits(__program, bits, deflt, clss);
	  for(clss = (char)('9' + 1); clss < 256; clss++)
	    __setCharacterClassBits(__program, bits, deflt, clss);
	  lastclss = Character.MAX_VALUE;
	  continue;
	case 'n':
	  clss = '\n';
	  break;
	case 'r':
	  clss = '\r';
	  break;
	case 't':
	  clss = '\t';
	  break;
	case 'f':
	  clss = '\f';
	  break;
	case 'b':
	  clss = '\b';
	  break;
	case 'e':
	  clss = '\033';
	  break;
	case 'a':
	  clss = '\007';
	  break;
	case 'x':
	  clss = (char)__parseHex(__input._array, __input._getOffset(), 2,
				  numLength);
	  __input._increment(numLength[0]);
	  break;
	case 'c':
	  clss = __input._postIncrement();
	  if(Character.isLowerCase(clss))
	    clss = Character.toUpperCase(clss);
	  clss ^= 64;
	  break;
	case '0': case '1': case '2': case '3': case '4':
	case '5': case '6': case '7': case '8': case '9':
	  clss = (char)__parseOctal(__input._array, __input._getOffset() - 1,
				    3, numLength);
	  __input._increment(numLength[0] - 1);
	  break;
	}
      }

      if(range) {
	if(lastclss > clss)
	  throw new MalformedPatternException(
			 "Invalid [] range in expression.");
	range = false;
      } else {
	lastclss = clss;

	if(__input._getValue() == '-' &&
	   __input._getOffset() + 1 < __input._getLength() &&
	   __input._getValueRelative(1) != ']') {
	  __input._increment();
	  range = true;
	  continue;
	}
      }

      while(lastclss <= clss) {
	__setCharacterClassBits(__program, bits, deflt, lastclss);
	if((__modifierFlags[0] & __CASE_INSENSITIVE) != 0 &&
	   Character.isUpperCase(lastclss))
	  __setCharacterClassBits(__program, bits, deflt,
				 Character.toLowerCase(lastclss));

	++lastclss;
      }

      lastclss = clss;
    }

    if(__input._getValue() != ']')
      throw new MalformedPatternException("Unmatched [] in expression.");

    __getNextChar();

    return offset;
  }


  private int __parseBranch(int[] retFlags) throws MalformedPatternException {
    boolean nestCheck = false, handleRepetition = false;
    int offset, next, min, max, flags[] = { 0 };
    char operator, value;

    min = 0;
    max = Character.MAX_VALUE;
    offset = __parseAtom(flags);

    if(offset == OpCode._NULL_OFFSET) {
      if((flags[0] & __TRYAGAIN) != 0)
	retFlags[0] |= __TRYAGAIN;
      return OpCode._NULL_OFFSET;
    }

    operator = __input._getValue();

    if(operator == '(' && __input._getValueRelative(1) == '?' &&
       __input._getValueRelative(2) == '#') {
      while(operator != CharStringPointer._END_OF_STRING && operator != ')')
	operator = __input._increment();

      if(operator != CharStringPointer._END_OF_STRING) {
	__getNextChar();
	operator = __input._getValue();
      }
    }

    if(operator == '{' &&
       __parseRepetition(__input._array, __input._getOffset())) {
      int maxOffset, pos;

      next = __input._getOffset() + 1;
      pos = maxOffset = __input._getLength();

      value = __input._getValue(next);

      while(Character.isDigit(value) || value == ',') {
	if(value == ',') {
	  if(pos != maxOffset)
	    break;
	  else
	    pos = next;
	}
	++next;
	value = __input._getValue(next);
      }

      if(value == '}') {
	int num;
	StringBuffer buffer = new StringBuffer(10);

	if(pos == maxOffset)
	  pos = next;
	__input._increment();

	num = __input._getOffset();
	value = __input._getValue(num);

	while(Character.isDigit(value)) {
	  buffer.append(value);
	  ++num;
	  value = __input._getValue(num);
	}

	try {
	  min = Integer.parseInt(buffer.toString());
	} catch(NumberFormatException e) {
	  throw new MalformedPatternException(
	 "Unexpected number format exception.  Please report this bug." +
	   "NumberFormatException message: " + e.getMessage());
	}

	value = __input._getValue(pos);
	if(value == ',')
	  ++pos;
	else
	  pos = __input._getOffset();

	num = pos;
	buffer = new StringBuffer(10);

	value = __input._getValue(num);

	while(Character.isDigit(value)){
	  buffer.append(value);
	  ++num;
	  value = __input._getValue(num);
	}

	try {
	  if(num != pos)
	    max = Integer.parseInt(buffer.toString());
	} catch(NumberFormatException e) {
	  throw new MalformedPatternException(
	 "Unexpected number format exception.  Please report this bug." +
	   "NumberFormatException message: " + e.getMessage());
	}

	//System.err.println("min: " + min + " max: " + max); //debug

	if(max == 0 && __input._getValue(pos) != '0')
	  max = Character.MAX_VALUE;
	__input._setOffset(next);
	__getNextChar();

	//System.err.println("min: " + min + " max: " + max); //debug

	nestCheck = true;
	handleRepetition = true;
      }
    }

    if(!nestCheck) {
      handleRepetition = false;

      if(!__isSimpleRepetitionOp(operator)) {
	retFlags[0] = flags[0];
	return offset;
      }

      __getNextChar();

      retFlags[0] = ((operator != '+') ?
		  (__WORSTCASE | __SPSTART) : (__WORSTCASE | __NONNULL));

      if(operator == '*' && ((flags[0] & __SIMPLE) != 0)) {
	__programInsertOperator(OpCode._STAR, offset);
	__cost+=4;
      } else if(operator == '*') {
	min = 0;
	handleRepetition = true;
      } else if(operator == '+' && (flags[0] & __SIMPLE) != 0) {
	__programInsertOperator(OpCode._PLUS, offset);
	__cost+=3;
      } else if(operator == '+') {
	min = 1;
	handleRepetition = true;
      } else if(operator == '?') {
	min = 0;
	max = 1;
	handleRepetition = true;
      }
    }

    if(handleRepetition) {

      // handle repetition
      if((flags[0] & __SIMPLE) != 0){
	__cost+= ((2 + __cost) / 2);
	__programInsertOperator(OpCode._CURLY, offset);
      } else {
	__cost += (4 + __cost);
	__programAddTail(offset, __emitNode(OpCode._WHILEM));
	__programInsertOperator(OpCode._CURLYX, offset);
	__programAddTail(offset, __emitNode(OpCode._NOTHING));
      }

      if(min > 0)
	retFlags[0] = (__WORSTCASE | __NONNULL);

      if(max != 0 && max < min)
	throw new MalformedPatternException(
       "Invalid interval {" + min + "," + max + "}");

      if(__program!= null) {
	__program[offset + 2] = (char)min;
	__program[offset + 3] = (char)max;
      }
    }


    if(__input._getValue() == '?') {
      __getNextChar();
      __programInsertOperator(OpCode._MINMOD, offset);
      __programAddTail(offset, offset + 2);
    }

    if(__isComplexRepetitionOp(__input._array, __input._getOffset()))
      throw new MalformedPatternException(
        "Nested repetitions *?+ in expression");

    return offset;
  }


  private int __parseExpression(boolean isParenthesized, int[] hintFlags)
    throws MalformedPatternException {
    char value, paren;
    int nodeOffset = OpCode._NULL_OFFSET, parenthesisNum = 0, br, ender;
    int[] flags = { 0 };
    String modifiers = "iogmsx";


    // Initially we assume expression doesn't match null string.
    hintFlags[0] = __NONNULL;

    if (isParenthesized) {
      paren = 1;
      if(__input._getValue() == '?') {
	__input._increment();
	paren = value = __input._postIncrement();

	switch(value) {
	case ':' :
	case '=' :
	case '!' : break;
	case '#' :
	  value = __input._getValue();
	  while(value != CharStringPointer._END_OF_STRING && value != ')')
	    value = __input._increment();
	  if(value != ')')
	    throw new MalformedPatternException(
	       "Sequence (?#... not terminated");
	  __getNextChar();
	  hintFlags[0] = __TRYAGAIN;
	  return OpCode._NULL_OFFSET;
	default :
	  __input._decrement();
	  value = __input._getValue();
	  while(value != CharStringPointer._END_OF_STRING &&
		modifiers.indexOf(value) != -1) {
	    __setModifierFlag(__modifierFlags, value);
	    value = __input._increment();
	  }
	  if(value != ')')
	    throw new MalformedPatternException(
	       "Sequence (?" + value + "...) not recognized");
	  __getNextChar();
	  hintFlags[0] = __TRYAGAIN;
	  return OpCode._NULL_OFFSET;
	}
      } else {
	parenthesisNum = __numParentheses;
	++__numParentheses;
	nodeOffset = __emitArgNode(OpCode._OPEN, (char)parenthesisNum);
      }
    } else 
      paren = 0;

    br = __parseAlternation(flags);

    if(br == OpCode._NULL_OFFSET)
      return OpCode._NULL_OFFSET;

    if(nodeOffset != OpCode._NULL_OFFSET)
      __programAddTail(nodeOffset, br);
    else
      nodeOffset = br;

    if((flags[0] & __NONNULL) == 0)
      hintFlags[0] &= ~__NONNULL;

    hintFlags[0] |= (flags[0] & __SPSTART);

    while(__input._getValue() == '|') {
      __getNextChar();
      br = __parseAlternation(flags);

      if(br == OpCode._NULL_OFFSET)
	return OpCode._NULL_OFFSET;

      __programAddTail(nodeOffset, br);

      if((flags[0] & __NONNULL) == 0)
	hintFlags[0] &= ~__NONNULL;

      hintFlags[0] |= (flags[0] & __SPSTART);
    }

    switch(paren) {
    case ':' :
      ender = __emitNode(OpCode._NOTHING);
      break;
    case 1:
      ender = __emitArgNode(OpCode._CLOSE, (char)parenthesisNum);
      break;
    case '=':
    case '!':
      ender = __emitNode(OpCode._SUCCEED);
      hintFlags[0] &= ~__NONNULL;
      break;
    case 0  :
    default :
      ender = __emitNode(OpCode._END);
      break;
    }

    __programAddTail(nodeOffset, ender);

    for(br = nodeOffset; br != OpCode._NULL_OFFSET;
	br = OpCode._getNext(__program, br))
      __programAddOperatorTail(br, ender);

    if(paren == '=') {
      __programInsertOperator(OpCode._IFMATCH, nodeOffset);
      __programAddTail(nodeOffset, __emitNode(OpCode._NOTHING));
    } else if(paren == '!') {
      __programInsertOperator(OpCode._UNLESSM, nodeOffset);
      __programAddTail(nodeOffset, __emitNode(OpCode._NOTHING));
    }

    if(paren != 0 && (__input._isAtEnd() || __getNextChar() != ')')) {
      throw new MalformedPatternException("Unmatched parentheses.");
    } else if(paren == 0 && !__input._isAtEnd()) { 
      if(__input._getValue() == ')')
	throw new MalformedPatternException("Unmatched parentheses.");
      else
	// Should never happen.
	throw new MalformedPatternException(
       "Unreached characters at end of expression.  Please report this bug!");
    }


    return nodeOffset;
  }


  /**
   * Compiles a Perl5 regular expression into a Perl5Pattern instance that
   * can be used by a Perl5Matcher object to perform pattern matching.
   * Please see the user's guide for more information about Perl5 regular
   * expressions.
   * <p>
   * @param pattern  A Perl5 regular expression to compile.
   * @param options  A set of flags giving the compiler instructions on
   *                 how to treat the regular expression.  The flags
   *                 are a logical OR of any number of the five <b>MASK</b>
   *                 constants.  For example:
   *                 <pre>
   * regex =
   *   compiler.compile(pattern, Perl5Compiler.
   *                    CASE_INSENSITIVE_MASK |
   *                    Perl5Compiler.MULTILINE_MASK);
   *                 </pre>
   *                  This says to compile the pattern so that it treats
   *                  input as consisting of multiple lines and to perform
   *                  matches in a case insensitive manner.
   * @return A Pattern instance constituting the compiled regular expression.
   *         This instance will always be a Perl5Pattern and can be reliably
   *         casted to a Perl5Pattern.
   * @exception MalformedPatternException  If the compiled expression
   *  is not a valid Perl5 regular expression.
   */
  public Pattern compile(char[] pattern, int options)
       throws MalformedPatternException {
    int[] flags = { 0 };
    int caseInsensitive, scan;
    Perl5Pattern regexp;
    String mustString, startString;

    int first;
    boolean sawOpen = false, sawPlus = false;

    StringBuffer lastLongest, longest;
    int length, minLength = 0, curBack, back, backmost;


    __input = new CharStringPointer(pattern);

    caseInsensitive    = options & __CASE_INSENSITIVE;
    __modifierFlags[0] = (char)options;
    __sawBackreference = false;
    __numParentheses   = 1;
    __programSize      = 0;
    __cost             = 0;
    __program= null;

    __emitCode((char)0);
    if(__parseExpression(false, flags) == OpCode._NULL_OFFSET) {
      //System.err.println("null -- Size: " + __programSize); // debug
      // return null;
      throw new MalformedPatternException("Unknown compilation error.");
    }

    //System.err.println("First Pass Size: " + __programSize); //debug

    if(__programSize >= Character.MAX_VALUE - 1)
      throw new MalformedPatternException("Expression is too large.");


    __program= new char[__programSize];
    regexp = new Perl5Pattern();

    regexp._program    = __program;
    regexp._expression = new String(pattern);

    __input._setOffset(0);

    __numParentheses   = 1;
    __programSize      = 0;
    __cost             = 0;

    __emitCode((char)0);
    if(__parseExpression(false, flags) == OpCode._NULL_OFFSET) {
      //System.err.println("null -- Size: " + __programSize); //debug 
      //return null;
      throw new MalformedPatternException("Unknown compilation error.");
    }

    //System.err.println("Second Pass Size: " + __programSize); //debug

    caseInsensitive = __modifierFlags[0] & __CASE_INSENSITIVE;

    regexp._isExpensive      = (__cost >= 10);
    regexp._startClassOffset = OpCode._NULL_OFFSET;
    regexp._anchor           = 0;
    regexp._back             = -1;
    regexp._options          = options;
    regexp._startString      = null;
    regexp._mustString       = null;
    mustString               = null;
    startString              = null;

    scan = 1;
    if(__program[OpCode._getNext(__program, scan)] == OpCode._END){
      boolean doItAgain;  // bad variables names!
      char op;

      first = scan = OpCode._getNextOperator(scan);
      op = __program[first];

      while((op == OpCode._OPEN && (sawOpen = true)) ||
	    (op == OpCode._BRANCH &&
	     __program[OpCode._getNext(__program, first)] != OpCode._BRANCH) ||
	    op == OpCode._PLUS || op == OpCode._MINMOD ||
	    (OpCode._opType[op] == OpCode._CURLY && 
	     OpCode._getArg1(__program, first) > 0)) {
	if(op == OpCode._PLUS)
	  sawPlus = true;
	else
	  first+=OpCode._operandLength[op];

	first = OpCode._getNextOperator(first);
	op = __program[first];
      }

      doItAgain = true;

      while(doItAgain) {
	doItAgain = false;
	op = __program[first];

	if(op == OpCode._EXACTLY) {
	  startString =
	    new String(__program, OpCode._getOperand(first + 1),
		       __program[OpCode._getOperand(first)]);

	} else if(OpCode._isInArray(op, OpCode._opLengthOne, 2))
	  regexp._startClassOffset = first;
	else if(op == OpCode._BOUND || op == OpCode._NBOUND)
	  regexp._startClassOffset = first;
	else if(OpCode._opType[op] == OpCode._BOL) {
	  regexp._anchor = Perl5Pattern._OPT_ANCH;
	  first = OpCode._getNextOperator(first);
	  doItAgain = true;
	  continue;
	} else if(op == OpCode._STAR &&
		  OpCode._opType[__program[OpCode._getNextOperator(first)]] == 
		  OpCode._ANY && (regexp._anchor & Perl5Pattern._OPT_ANCH) != 0)
	  {
	    regexp._anchor = Perl5Pattern._OPT_ANCH | Perl5Pattern._OPT_IMPLICIT;
	    first = OpCode._getNextOperator(first);
	    doItAgain = true;
	    continue;
	}
      } // end while do it again

      if(sawPlus && (!sawOpen || !__sawBackreference))
	regexp._anchor |= Perl5Pattern._OPT_SKIP;


      //length = OpCode._getNextOperator(first); //debug
      // System.err.println("first: " + first + "nextoper: " + length);
      //System.err.print("first " + (int)op + " next "); // debug
      //if(length >= 0 && length < _program.length) //debug
      //System.err.print((int)(__program[length])); //debug
      //else  //debug
      //System.err.print("out of range"); //debug
      //System.err.println(" offset " + (int)(first - scan)); // debug

      lastLongest   = new StringBuffer();
      longest   = new StringBuffer();
      length    = 0;
      minLength = 0;
      curBack   = 0;
      back   = 0;
      backmost   = 0;

      while(scan > 0 && (op = __program[scan]) != OpCode._END) {

	if(op == OpCode._BRANCH) {
	  if(__program[OpCode._getNext(__program, scan)] == OpCode._BRANCH) {
	    curBack = -30000;
	    while(__program[scan] == OpCode._BRANCH)
	      scan = OpCode._getNext(__program, scan);
	  } else
	    scan = OpCode._getNextOperator(scan);
	  continue;
	}

	if(op == OpCode._UNLESSM) {
	  curBack = -30000;
	  scan = OpCode._getNext(__program, scan);
	  continue;
	}

	if(op == OpCode._EXACTLY) {
	  int temp;

	  first = scan;
	  while(__program[(temp = OpCode._getNext(__program, scan))] == 
		OpCode._CLOSE)
	    scan = temp;

	  minLength += __program[OpCode._getOperand(first)];

	  temp = __program[OpCode._getOperand(first)];

	  if(curBack - back == length) {
	    lastLongest.append(new String(__program, OpCode._getOperand(first) + 1,
				      temp));
	    length  += temp;
	    curBack += temp;
	    first = OpCode._getNext(__program, scan);
	  } else if(temp >= (length + (curBack >= 0 ? 1 : 0))) {
	    length = temp;
	    lastLongest =
	      new StringBuffer(new String(__program,
					  OpCode._getOperand(first) + 1, temp));
	    back = curBack;
	    curBack += length;
	    first = OpCode._getNext(__program, scan);
	  } else
	    curBack += temp;
	} else if(OpCode._isInArray(op, OpCode._opLengthVaries, 0)) {
	  curBack = -30000;
	  length = 0;

	  if(lastLongest.length() > longest.length()) {
	    longest = lastLongest;
	    backmost = back;
	  }

	  lastLongest = new StringBuffer();

	  if(op == OpCode._PLUS && 
	     OpCode._isInArray(__program[OpCode._getNextOperator(scan)],
			    OpCode._opLengthOne, 0))
	    ++minLength;
	  else if(OpCode._opType[op] == OpCode._CURLY &&
		  OpCode._isInArray(__program[OpCode._getNextOperator(scan) + 2],
				 OpCode._opLengthOne, 0))
	    minLength += OpCode._getArg1(__program, scan);
	} else if(OpCode._isInArray(op, OpCode._opLengthOne, 0)) {
	  ++curBack;
	  ++minLength;
	  length = 0;
	  if(lastLongest.length() > longest.length()) {
	    longest = lastLongest;
	    backmost = back;
	  }
	  lastLongest = new StringBuffer();
	}

	scan = OpCode._getNext(__program, scan);
      } // end while

      if(lastLongest.length() +
	 ((OpCode._opType[__program[first]] == OpCode._EOL) ? 1 : 0) >
	 longest.length()) {
	longest = lastLongest;
	backmost = back;
      } else
	lastLongest = new StringBuffer();

      if(longest.length() > 0 && startString == null) {
	mustString = longest.toString();
	if(backmost < 0)
	  backmost = -1;
	regexp._back = backmost;

	/*

	  if(longest.length() > 
	  (((caseInsensitive & __CASE_INSENSITIVE) != 0 ||
	  OpCode._opType[__program[first]] == OpCode._EOL)
	  ? 1 : 0))
	  */	    
      } else
	longest = null;
    } // end if


    regexp._isCaseInsensitive = ((caseInsensitive & __CASE_INSENSITIVE) != 0);
    regexp._numParentheses  = __numParentheses - 1;
    regexp._minLength       = minLength;

    if(mustString != null) {
      regexp._mustString = mustString.toCharArray();
      regexp._mustUtility = 100;
    }

    if(startString != null)
      regexp._startString = startString.toCharArray();

    return regexp;
  }

  /**
   * Same as calling <b>compile(pattern, Perl5Compiler.DEFAULT_MASK);</b>
   * <p>
   * @param pattern  A regular expression to compile.
   * @return A Pattern instance constituting the compiled regular expression.
   *         This instance will always be a Perl5Pattern and can be reliably
   *         casted to a Perl5Pattern.
   * @exception MalformedPatternException  If the compiled expression
   *  is not a valid Perl5 regular expression.
   */
  public Pattern compile(char[] pattern) throws MalformedPatternException {
	 return compile(pattern, DEFAULT_MASK);
  }


  /**
   * Same as calling <b>compile(pattern, Perl5Compiler.DEFAULT_MASK);</b>
   * <p>
   * @param pattern  A regular expression to compile.
   * @return A Pattern instance constituting the compiled regular expression.
   *         This instance will always be a Perl5Pattern and can be reliably
   *         casted to a Perl5Pattern.
   * @exception MalformedPatternException  If the compiled expression
   *  is not a valid Perl5 regular expression.
   */
  public Pattern compile(String pattern) throws MalformedPatternException {
	 return compile(pattern.toCharArray(), DEFAULT_MASK);
  }


  /**
   * Compiles a Perl5 regular expression into a Perl5Pattern instance that
   * can be used by a Perl5Matcher object to perform pattern matching.
   * Please see the user's guide for more information about Perl5 regular
   * expressions.
   * <p>
   * @param pattern  A Perl5 regular expression to compile.
   * @param options  A set of flags giving the compiler instructions on
   *                 how to treat the regular expression.  The flags
   *                 are a logical OR of any number of the five <b>MASK</b>
   *                 constants.  For example:
   *                 <pre>
   * regex =
   *   compiler.compile("^\\w+\\d+$",
   *                    Perl5Compiler.CASE_INSENSITIVE_MASK |
   *                    Perl5Compiler.MULTILINE_MASK);
   *                 </pre>
   *                  This says to compile the pattern so that it treats
   *                  input as consisting of multiple lines and to perform
   *                  matches in a case insensitive manner.
   * @return A Pattern instance constituting the compiled regular expression.
   *         This instance will always be a Perl5Pattern and can be reliably
   *         casted to a Perl5Pattern.
   * @exception MalformedPatternException  If the compiled expression
   *  is not a valid Perl5 regular expression.
   */
  public Pattern compile(String pattern, int options)
       throws MalformedPatternException {
	 return compile(pattern.toCharArray(), options);
  }

}
