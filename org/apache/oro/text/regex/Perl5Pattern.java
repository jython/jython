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

import java.io.*;

/**
 * An implementation of the Pattern interface for Perl5 regular expressions.
 * This class is compatible with the Perl5Compiler and Perl5Matcher
 * classes.  When a Perl5Compiler instance compiles a regular expression
 * pattern, it produces a Perl5Pattern instance containing internal
 * data structures used by Perl5Matcher to perform pattern matches.
 * This class cannot be subclassed and
 * cannot be directly instantiated by the programmer as it would not
 * make sense.  Perl5Pattern instances should only be created through calls
 * to a Perl5Compiler instance's compile() methods.  The class implements
 * the Serializable interface so that instances may be pre-compiled and
 * saved to disk if desired.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Perl5Compiler
 * @see Perl5Matcher
 */
public final class Perl5Pattern implements Pattern, Serializable, Cloneable {
  static final int _OPT_ANCH = 1, _OPT_SKIP = 2, _OPT_IMPLICIT = 4;

  String _expression;
  char[] _program;
  int _mustUtility;
  int _back;
  int _minLength;
  int _numParentheses;
  boolean _isCaseInsensitive, _isExpensive;
  int _startClassOffset;
  int _anchor;
  int _options;
  char[] _mustString, _startString;

  /**
   * A dummy constructor with default visibility to override the default
   * public constructor that would be created otherwise by the compiler.
   */
  Perl5Pattern(){ }

  /*
  private void readObject(ObjectInputStream stream)
       throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject();
    _beginMatchOffsets = new int[_numParentheses + 1];
    _endMatchOffsets   = new int[_numParentheses + 1];
  }
  */

  /**
   * This method returns the string representation of the pattern.
   * <p>
   * @return The original string representation of the regular expression
   *         pattern.
   */
  public String getPattern() { return _expression; }


  /**
   * This method returns an integer containing the compilation options used
   * to compile this pattern.
   * <p>
   * @return The compilation options used to compile the pattern.
   */
  public int getOptions()    { return _options; }

  /*
  // For testing
  public String toString() {
    return "Parens: " + _numParentheses + " " + _beginMatchOffsets.length + " "
      + _endMatchOffsets.length;
  }
  */
}
