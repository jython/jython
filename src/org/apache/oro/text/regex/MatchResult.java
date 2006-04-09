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
 * The MatchResult interface allows PatternMatcher implementors to return
 * results storing match information in whatever format they like, while
 * presenting a consistent way of accessing that information.  However,
 * MatchResult implementations should strictly follow the behavior
 * described for the interface methods.
 * <p>
 *
 * A MatchResult instance contains a pattern match and its saved groups.
 * You can access the entire match directly using the
 * {@link #group(int)} method with an argument of 0,
 * or by the {@link #toString()} method which is
 * defined to return the same thing.  It is also possible to obtain
 * the beginning and ending offsets of a match relative to the input
 * producing the match by using the 
 * {@link #beginOffset(int)} and {@link #endOffset(int)} methods.  The
 * {@link #begin(int)} and {@link #end(int)} are useful in some
 * circumstances and return the begin and end offsets of the subgroups
 * of a match relative to the beginning of the match.
 * <p>
 *
 * You might use a MatchResult as follows:
 * <blockquote><pre>
 * int groups;
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
 *   // Here we just print out all its elements to show how its
 *   // methods are used.
 * 
 *   System.out.println("Match: " + result.toString());
 *   System.out.println("Length: " + result.length());
 *   groups = result.groups();
 *   System.out.println("Groups: " + groups);
 *   System.out.println("Begin offset: " + result.beginOffset(0));
 *   System.out.println("End offset: " + result.endOffset(0));
 *   System.out.println("Saved Groups: ");
 *
 *   // Start at 1 because we just printed out group 0
 *   for(int group = 1; group < groups; group++) {
 *	 System.out.println(group + ": " + result.group(group));
 *	 System.out.println("Begin: " + result.begin(group));
 *	 System.out.println("End: " + result.end(group));
 *   }
 * }
 * </pre></blockquote>

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see PatternMatcher
 */

public interface MatchResult {
  /**
   * A convenience method returning the length of the entire match.
   * If you want to get the length of a particular subgroup you should
   * use the {@link #group(int)} method to get
   * the string and then access its length() method as follows:
   * <p>
   * <blockquote><pre>
   * int length = -1; // Use -1 to indicate group doesn't exist
   * MatchResult result;
   * String subgroup;
   * 
   * // Initialization of result omitted
   *
   * subgroup = result.group(1);
   * if(subgroup != null)
   *   length = subgroup.length();
   *
   * </pre></blockquote>
   * <p>
   *
   * The length() method serves as a more a more efficient way to do:
   * <p>
   * <blockquote><pre>
   * length = result.group(0).length();
   * </pre></blockquote>
   * <p>
   *
   * @return The length of the match.
   */
  public int length();


  /**
   * @return The number of groups contained in the result.  This number
   *         includes the 0th group.  In other words, the result refers
   *         to the number of parenthesized subgroups plus the entire match
   *         itself.
   */
  public int groups();

  /**
   * Returns the contents of the parenthesized subgroups of a match,
   * counting parentheses from left to right and starting from 1.
   * Group 0 always refers to the entire match.  For example, if the
   * pattern <code> foo(\d+) </code> is used to extract a match
   * from the input <code> abfoo123 </code>, then <code> group(0) </code>
   * will return <code> foo123 </code> and <code> group(1) </code> will return
   * <code> 123 </code>.  <code> group(2) </code> will return
   * <code> null </code> because there is only one subgroup in the original
   * pattern.
   * <p>
   * @param group The pattern subgroup to return.
   * @return A string containing the indicated pattern subgroup.  Group
   *         0 always refers to the entire match.  If a group was never
   *         matched, it returns null.  This is not to be confused with
   *         a group matching the null string, which will return a String
   *         of length 0.
   */
  public String group(int group);


  /**
   * @param group The pattern subgroup.
   * @return The offset into group 0 of the first token in the indicated
   *         pattern subgroup.  If a group was never matched or does
   *         not exist, returns -1.  Be aware that a group that matches
   *         the null string at the end of a match will have an offset
   *         equal to the length of the string, so you shouldn't blindly
   *         use the offset to index an array or String.
   */
  public int begin(int group);


  /**
   * @param group The pattern subgroup.
   * @return Returns one plus the offset into group 0 of the last token in
   *         the indicated pattern subgroup.  If a group was never matched
   *         or does not exist, returns -1.  A group matching the null
   *         string will return its start offset.
   */
  public int end(int group);


  /**
   * Returns an offset marking the beginning of the pattern match
   * relative to the beginning of the input from which the match
   * was extracted.
   * <p>
   * @param group The pattern subgroup.
   * @return The offset of the first token in the indicated
   *         pattern subgroup.  If a group was never matched or does
   *         not exist, returns -1.
   */
  public int beginOffset(int group);


  /**
   * Returns an offset marking the end of the pattern match
   * relative to the beginning of the input from which the match was
   * extracted.
   * <p>
   * @param group The pattern subgroup.
   * @return Returns one plus the offset of the last token in
   *         the indicated pattern subgroup.  If a group was never matched
   *         or does not exist, returns -1.  A group matching the null
   *         string will return its start offset.
   */
  public int endOffset(int group);


  /**
   * Returns the same as group(0).
   *
   * @return A string containing the entire match.
   */
  public String toString();
}
