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
 * The Substitution interface provides a means for you to control how
 * a substitution is performed when using the
 * {@link Util#substitute Util.substitute} method.  Two standard
 * implementations are provided,
 * {@link StringSubstitution} and {@link Perl5Substitution}.  To
 * achieve custom control over the behavior of substitutions, you can 
 * create your own implementations.  A common use for customization is
 * to make a substitution a function of a match.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Util
 * @see Util#substitute
 * @see StringSubstitution
 * @see Perl5Substitution
 */
public interface Substitution {

  /**
   * Appends the substitution to a buffer containing the original input
   * with substitutions applied for the pattern matches found so far.
   * For maximum flexibility, the original input as well as the
   * PatternMatcher and Pattern used to find the match are included as
   * arguments.  However, you will almost never find a need to use those
   * arguments when creating your own Substitution implementations.
   * <p>
   * For performance reasons, rather than provide a getSubstitution method
   * that returns a String used by Util.substitute, we have opted to pass
   * a StringBuffer argument from Util.substitute to which the Substitution
   * must append data.  The contract that an appendSubstitution 
   * implementation must abide by is that the appendBuffer may only be
   * appended to.  appendSubstitution() may not alter the appendBuffer in
   * any way other than appending to it.
   * <p>
   * This method is invoked by Util.substitute every time it finds a match.
   * After finding a match, Util.substitute appends to the appendBuffer
   * all of the original input occuring between the end of the last match
   * and the beginning of the current match.  Then it invokes 
   * appendSubstitution(), passing the appendBuffer, current match, and
   * other information as arguments.  The substitutionCount keeps track
   * of how many substitutions have been performed so far by an invocation
   * of Util.substitute.  Its value starts at 1 when the first substitution
   * is found and appendSubstitution is invoked for the first time.  It
   * will NEVER be zero or a negative value.
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
				 PatternMatcher matcher, Pattern pattern);
}
