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
 * The PatternMatcher interface defines the operations a regular
 * expression matcher must implement.  However, the types of the Pattern 
 * implementations recognized by a matcher are not restricted.  Typically
 * PatternMatcher instances will only recognize a specific type of Pattern.
 * For example, the Perl5Matcher only recognizes Perl5Pattern instances.
 * However, none of the PatternMatcher methods are required to throw an
 * exception in case of the use of an invalid pattern.  This is done for
 * efficiency reasons, although usually a CastClassException will be
 * thrown by the Java runtime system if you use the wrong Pattern
 * implementation.  It is the responsibility of the programmer to make
 * sure he uses the correct Pattern instance with a given PatternMatcher
 * instance.  The current version of this package only contains the Perl5
 * suite of pattern matching classes, but future ones for other regular
 * expression grammars may be added and users may also create their own
 * implementations of the provided interfaces. Therefore the programmer
 * should be careful not to mismatch classes.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Pattern
 * @see PatternCompiler
 * @see MatchResult
 */
public interface PatternMatcher {

  /**
   * Determines if a prefix of a string (represented as a char[])
   * matches a given pattern, starting from a given offset into the string.
   * If a prefix of the string matches the pattern, a MatchResult instance
   * representing the match is made accesible via
   * {@link #getMatch()}.
   * <p>
   * This method is useful for certain common token identification tasks
   * that are made more difficult without this functionality.
   * <p>
   * @param input  The char[] to test for a prefix match.
   * @param pattern  The Pattern to be matched.
   * @param offset The offset at which to start searching for the prefix.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matchesPrefix(char[] input, Pattern pattern, int offset);

  /**
   * Determines if a prefix of a string matches a given pattern.
   * If a prefix of the string matches the pattern, a MatchResult instance
   * representing the match is made accesible via
   * {@link #getMatch()}.
   * <p>
   * This method is useful for certain common token identification tasks
   * that are made more difficult without this functionality.
   * <p>
   * @param input  The String to test for a prefix match.
   * @param pattern  The Pattern to be matched.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matchesPrefix(String input, Pattern pattern);

  /**
   * Determines if a prefix of a string (represented as a char[])
   * matches a given pattern.
   * If a prefix of the string matches the pattern, a MatchResult instance
   * representing the match is made accesible via
   * {@link #getMatch()}.
   * <p>
   * This method is useful for certain common token identification tasks
   * that are made more difficult without this functionality.
   * <p>
   * @param input  The char[] to test for a prefix match.
   * @param pattern  The Pattern to be matched.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matchesPrefix(char[] input, Pattern pattern);

  /**
   * Determines if a prefix of a PatternMatcherInput instance
   * matches a given pattern.  If there is a match, a MatchResult instance
   * representing the match is made accesible via
   * {@link #getMatch()}.  Unlike the
   * {@link #contains(PatternMatcherInput, Pattern)}
   * method, the current offset of the PatternMatcherInput argument
   * is not updated.  You should remember that the region starting
   * from the begin offset of the PatternMatcherInput will be
   * tested for a prefix match.
   * <p>
   * This method is useful for certain common token identification tasks
   * that are made more difficult without this functionality.
   * <p>
   * @param input  The PatternMatcherInput to test for a prefix match.
   * @param pattern  The Pattern to be matched.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matchesPrefix(PatternMatcherInput input, Pattern pattern);

  /**
   * Determines if a string exactly matches a given pattern.  If
   * there is an exact match, a MatchResult instance
   * representing the match is made accesible via
   * {@link #getMatch()}.
   * <p>
   * @param input  The String to test for an exact match.
   * @param pattern  The Pattern to be matched.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matches(String input, Pattern pattern);

  /**
   * Determines if a string (represented as a char[]) exactly matches
   * a given pattern.  If there is an exact match, a MatchResult
   * instance representing the match is made accesible via
   * {@link #getMatch()}.
   * <p>
   * @param input  The char[] to test for a match.
   * @param pattern  The Pattern to be matched.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matches(char[] input, Pattern pattern);

  /**
   * Determines if the contents of a PatternMatcherInput instance
   * exactly matches a given pattern.  If
   * there is an exact match, a MatchResult instance
   * representing the match is made accesible via
   * {@link #getMatch()}.  Unlike the
   * {@link #contains(PatternMatcherInput, Pattern)}
   * method, the current offset of the PatternMatcherInput argument
   * is not updated.  You should remember that the region between
   * the begin and end offsets of the PatternMatcherInput will be
   * tested for an exact match.
   * <p>
   * @param input  The PatternMatcherInput to test for a match.
   * @param pattern  The Pattern to be matched.
   * @return True if input matches pattern, false otherwise.
   */
  public boolean matches(PatternMatcherInput input, Pattern pattern);

  /**
   * Determines if a string contains a pattern.  If the pattern is
   * matched by some substring of the input, a MatchResult instance
   * representing the <b> first </b> such match is made acessible via 
   * {@link #getMatch()}.  If you want to access
   * subsequent matches you should either use a PatternMatcherInput object
   * or use the offset information in the MatchResult to create a substring
   * representing the remaining input.  Using the MatchResult offset 
   * information is the recommended method of obtaining the parts of the
   * string preceeding the match and following the match.
   * <p>
   * @param input  The String to test for a match.
   * @param pattern  The Pattern to be matched.
   * @return True if the input contains a pattern match, false otherwise.
   */
  public boolean contains(String input, Pattern pattern);

  /**
   * Determines if a string (represented as a char[]) contains a pattern.
   * If the pattern is matched by some substring of the input, a MatchResult
   * instance representing the <b>first</b> such match is made acessible via 
   * {@link #getMatch()}.  If you want to access
   * subsequent matches you should either use a PatternMatcherInput object
   * or use the offset information in the MatchResult to create a substring
   * representing the remaining input.  Using the MatchResult offset 
   * information is the recommended method of obtaining the parts of the
   * string preceeding the match and following the match.
   * <p>
   * @param input  The String to test for a match.
   * @param pattern  The Pattern to be matched.
   * @return True if the input contains a pattern match, false otherwise.
   */
  public boolean contains(char[] input, Pattern pattern);

  /**
   * Determines if the contents of a PatternMatcherInput, starting from the
   * current offset of the input contains a pattern.
   * If a pattern match is found, a MatchResult
   * instance representing the <b>first</b> such match is made acessible via 
   * {@link #getMatch()}.  The current offset of the
   * PatternMatcherInput is set to the offset corresponding to the end
   * of the match, so that a subsequent call to this method will continue
   * searching where the last call left off.  You should remember that the
   * region between the begin and end offsets of the PatternMatcherInput are
   * considered the input to be searched, and that the current offset
   * of the PatternMatcherInput reflects where a search will start from.
   * Matches extending beyond the end offset of the PatternMatcherInput
   * will not be matched.  In other words, a match must occur entirely
   * between the begin and end offsets of the input.  See
   * {@link PatternMatcherInput} for more details.
   * <p>
   * This method is usually used in a loop as follows:
   * <blockquote><pre>
   * PatternMatcher matcher;
   * PatternCompiler compiler;
   * Pattern pattern;
   * PatternMatcherInput input;
   * MatchResult result;
   *
   * compiler = new Perl5Compiler();
   * matcher  = new Perl5Matcher();
   *
   * try {
   *   pattern = compiler.compile(somePatternString);
   * } catch(MalformedPatternException e) {
   *   System.out.println("Bad pattern.");
   *   System.out.println(e.getMessage());
   *   return;
   * }
   *
   * input   = new PatternMatcherInput(someStringInput);
   *
   * while(matcher.contains(input, pattern)) {
   *   result = matcher.getMatch();  
   *   // Perform whatever processing on the result you want.
   * }
   *
   * </pre></blockquote>
   * <p>
   * @param input  The PatternMatcherInput to test for a match.
   * @param pattern  The Pattern to be matched.
   * @return True if the input contains a pattern match, false otherwise.
   */
  public boolean contains(PatternMatcherInput input, Pattern pattern);

  /**
   * Fetches the last match found by a call to a matches() or contains()
   * method.
   * <p>
   * @return A MatchResult instance containing the pattern match found
   *         by the last call to any one of the matches() or contains()
   *         methods.  If no match was found by the last call,
   *         returns null.
   */
  public MatchResult getMatch();
}
