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
 * StringSubstitution implements a Substitution consisting of a simple
 * literal string.  This class is intended for use with
 * {@link Util#substitute Util.substitute}.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Substitution
 * @see Util
 * @see Util#substitute
 * @see Substitution
 * @see Perl5Substitution
 */
public class StringSubstitution implements Substitution {
  int _subLength;
  String _substitution;

  /**
   * Default constructor initializing substitution to a zero length
   * String.
   */
  public StringSubstitution() {
    this("");
  }

  /**
   * Creates a StringSubstitution representing the given string.
   * <p>
   * @param substitution The string to use as a substitution.
   */
  public StringSubstitution(String substitution) {
    setSubstitution(substitution);
  }


  /**
   * Sets the substitution represented by this StringSubstitution.  You
   * should use this method in order to avoid repeatedly allocating new
   * StringSubstitutions.  It is recommended that you allocate a single
   * StringSubstitution and reuse it by using this method when appropriate.
   * <p>
   * @param substitution The string to use as a substitution.
   */
  public void setSubstitution(String substitution) {
    _substitution = substitution;
    _subLength = substitution.length();
  }

  /**
   * Returns the string substitution represented by this object.
   * <p>
   * @return The string substitution represented by this object.
   */
  public String getSubstitution() { return _substitution; }

  /**
   * Returns the same value as {@link #getSubstitution()}.
   * <p>
   * @return The string substitution represented by this object.
   */
  public String toString() { return getSubstitution(); }

  /**
   * Appends the substitution to a buffer containing the original input
   * with substitutions applied for the pattern matches found so far.
   * See 
   * {@link Substitution#appendSubstitution Substitution.appendSubstition()}
   * for more details regarding the expected behavior of this method.
   * <p>
   * @param appendBuffer The buffer containing the new string resulting
   * from performing substitutions on the original input.
   * @param match The current match causing a substitution to be made. 
   * @param substitutionCount  The number of substitutions that have been
   *  performed so far by Util.substitute.
   * @param originalInput The original input upon which the substitutions are
   * being performed.
   * @param matcher The PatternMatcher used to find the current match.
   * @param pattern The Pattern used to find the current match.
   */
  public void appendSubstitution(StringBuffer appendBuffer, MatchResult match,
				 int substitutionCount, String originalInput, 
				 PatternMatcher matcher, Pattern pattern)
  {
    if(_subLength == 0) 
      return;
    appendBuffer.append(_substitution);
  }
}
