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
import java.util.*;

/**
 * The Util class is a holder for useful static utility methods that can
 * be generically applied to Pattern and PatternMatcher instances.
 * This class cannot and is not meant to be instantiated.
 * The Util class currently contains versions of the split() and substitute()
 * methods inspired by Perl's split function and <b>s</b> operation
 * respectively, although they are implemented in such a way as not to
 * rely on the Perl5 implementations of the OROMatcher packages regular
 * expression interfaces.  They may operate on any interface implementations
 * conforming to the OROMatcher API specification for the PatternMatcher,
 * Pattern, and MatchResult interfaces. Future versions of the class may
 * include additional utility methods.
 * <p>
 * A grep method is not included for two reasons:
 * <ol>
 *     <li> The details of reading a line at a time from an input stream
 *          differ in JDK 1.0.2 and JDK 1.1, making it difficult to
 *          retain compatibility across both Java releases.
 *     <li> Grep style processing is trivial for the programmer to implement
 *          in a while loop.  Rarely does anyone want to retrieve all
 *          occurences of a pattern and then process them.  More often a
 *          programmer will retrieve pattern matches and process them as they
 *          are retrieved, which is more efficient than storing them all in a
 *          Vector and then accessing them.
 * </ol>

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Pattern
 * @see PatternMatcher
 */
public final class Util {
  /**
   * A constant passed to the {@link #substitute substitute()}
   * methods indicating that all occurrences of a pattern should be 
   * substituted.
   */
  public static final int SUBSTITUTE_ALL = -1;

  /**
   * A constant passed to the {@link #split split()} methods
   * indicating that all occurrences of a pattern should be used to
   * split a string.
   */
  public static final int SPLIT_ALL = 0;

  /**
   * The default destructor for the Util class.  It is made private
   * to prevent the instantiation of the class.
   */
  private Util() { }

  /**
   * Splits up a <code>String</code> instance into strings contained in a
   * <code>Vector</code> of size not greater than a specified limit.  The
   * string is split with a regular expression as the delimiter. 
   * The <b>limit</b> parameter essentially says to split the
   * string only on at most the first <b>limit - 1</b> number of pattern
   * occurences.
   * <p>
   * This method is inspired by the Perl split() function and behaves 
   * identically to it when used in conjunction with the Perl5Matcher and
   * Perl5Pattern classes except for the following difference:
   * <ul><p>
   * In Perl, if the split expression contains parentheses, the split()
   * method creates additional list elements from each of the matching
   * subgroups in the pattern.  In other words:
   * <ul><p><code>split("/([,-])/", "8-12,15,18")</code></ul>
   * <p> produces the Vector containing:
   * <ul><p><code> { "8", "-", "12", ",", "15", ",", "18" } </code> </ul>
   * <p> The OROMatcher split method does not follow this behavior.  The
   * following Vector would be produced by OROMatcher:
   * <ul><p><code> { "8", "12",  "15", "18" } </code> </ul>
   * <p> To obtain the Perl behavior, use split method in the PerlTools
   * package available from
   * <a href="http://www.oroinc.com/"> http://www.oroinc.com/ </a>.
   * </ul>
   * <p>
   * @param matcher The regular expression matcher to execute the split.
   * @param pattern The regular expression to use as a split delimiter.
   * @param input  The <code>String</code> to split.
   * @param limit  The limit on the size of the returned <code>Vector</code>.
   *               Values <= 0 produce the same behavior as using the
   *               <b>SPLIT_ALL</b> constant which causes the limit to be 
   *               ignored and splits to be performed on all occurrences of
   *               the pattern.  You should use the <b>SPLIT_ALL</b> constant
   *               to achieve this behavior instead of relying on the default
   *               behavior associated with non-positive limit values.
   * @return A <code>Vector</code> containing the substrings of the input
   *         that occur between the regular expression delimiter occurences.
   *         The input will not be split into any more substrings than the
   *         specified <code>limit</code>.  A way of thinking of this is that
   *         only the first <code>limit - 1</code> matches of the delimiting
   *         regular expression will be used to split the input.
   */
  public static Vector split(PatternMatcher matcher, Pattern pattern,
			     String input, int limit)
  {
    int beginOffset;
    Vector results = new Vector(20); 
    MatchResult currentResult;
    PatternMatcherInput pinput;

    pinput = new PatternMatcherInput(input);
    beginOffset = 0;

    while(--limit != 0 && matcher.contains(pinput, pattern)) {
      currentResult = matcher.getMatch();
      results.addElement(input.substring(beginOffset,
					 currentResult.beginOffset(0)));
      beginOffset = currentResult.endOffset(0);
    }
    results.addElement(input.substring(beginOffset, input.length()));

    return results;
  }


  /**
   * Splits up a <code>String</code> instance into a <code>Vector</code>
   * of all its substrings using a regular expression as the delimiter.
   * This method is inspired by the Perl split() function and behaves 
   * identically to it when used in conjunction with the Perl5Matcher and
   * Perl5Pattern classes except for the following difference:
   * <p>
   * <ul>
   * In Perl, if the split expression contains parentheses, the split()
   * method creates additional list elements from each of the matching
   * subgroups in the pattern.  In other words:
   * <ul><p><code>split("/([,-])/", "8-12,15,18")</code></ul>
   * <p> produces the Vector containing: 
   * <ul><p><code> { "8", "-", "12", ",", "15", ",", "18" } </code> </ul>
   * <p> The OROMatcher split method does not follow this behavior.  The
   * following Vector would be produced by OROMatcher:
   * <ul><p><code> { "8", "12",  "15", "18" } </code> </ul>
   * <p> To obtain the Perl behavior, use split method in the PerlTools
   * package available from
   * <a href="http://www.oroinc.com/"> http://www.oroinc.com/ </a>.
   * </ul>
   * <p>
   * This method is identical to calling:
   * <blockquote><pre>
   * split(matcher, pattern, input, Util.SPLIT_ALL);
   * </pre></blockquote>
   * <p>
   * @param matcher The regular expression matcher to execute the split.
   * @param pattern The regular expression to use as a split delimiter.
   * @param input  The <code>String</code> to split.
   * @return A <code>Vector</code> containing all the substrings of the input
   *         that occur between the regular expression delimiter occurences.
   */
  public static Vector split( PatternMatcher matcher, Pattern pattern,
			      String input)
  {
    return split(matcher, pattern, input, SPLIT_ALL);
  }


  /**
   * Searches a string for a pattern and replaces the first occurrences
   * of the pattern with a Substitution up to the number of
   * substitutions specified by the <b>numSubs</b> parameter.  A 
   * <b>numSubs</b> value of <b>SUBSTITUTE_ALL</b> will cause all occurrences
   * of the pattern to be replaced.
   * <p>
   * @param matcher The regular expression matcher to execute the pattern
   *                search.
   * @param pattern The regular expression to search for and substitute
   *                occurrences of.
   * @param sub     The Substitution used to substitute pattern occurences.
   * @param input  The <code>String</code> on which to perform substitutions.
   * @param numSubs  The number of substitutions to perform.  Only the
   *                 first <b> numSubs </b> patterns encountered are
   *                 substituted.  If you want to substitute all occurences
   *                 set this parameter to <b> SUBSTITUTE_ALL </b>.
   * @return A String comprising the input string with the substitutions,
   *         if any, made.  If no substitutions are made, the returned String
   *         is the original input String.
   */
  public static String substitute(PatternMatcher matcher, Pattern pattern,
				  Substitution sub, String input, int numSubs)
  {
    int beginOffset, subCount;
    MatchResult currentResult;
    PatternMatcherInput pinput;
    StringBuffer buffer = new StringBuffer(input.length());

    pinput = new PatternMatcherInput(input);
    beginOffset = subCount = 0;

    // Must be != 0 because SUBSTITUTE_ALL is represented by -1.
    // Do NOT change to numSubs > 0.
    while(numSubs != 0 && matcher.contains(pinput, pattern)) {
      --numSubs;
      ++subCount;
      currentResult = matcher.getMatch();
      buffer.append(input.substring(beginOffset,
				    currentResult.beginOffset(0)));
      sub.appendSubstitution(buffer, currentResult, subCount,
			     input, matcher, pattern);
      beginOffset = currentResult.endOffset(0);
    }

    // No substitutions performed.  There's no point in duplicating 
    // the string as would happen if this check were omitted.
    if(subCount == 0)
      return input;

    buffer.append(input.substring(beginOffset, input.length()));

    return buffer.toString();
  }

  /**
   * Searches a string for a pattern and substitutes only the first
   * occurence of the pattern.
   * <p>
   * This method is identical to calling:
   * <blockquote><pre>
   * substitute(matcher, pattern, sub, input, 1);
   * </pre></blockquote>
   * <p>
   * @param matcher The regular expression matcher to execute the pattern
   *                search.
   * @param pattern The regular expression to search for and substitute
   *                occurrences of.
   * @param sub     The Substitution used to substitute pattern occurences.
   * @param input  The <code>String</code> on which to perform substitutions.
   * @return A String comprising the input string with the substitutions,
   *         if any, made.  If no substitutions are made, the returned String
   *         is the original input String.
   */
  public static String substitute(PatternMatcher matcher, Pattern pattern,
				  Substitution sub, String input)
  {
    return substitute(matcher, pattern, sub, input, 1);
  }

}
