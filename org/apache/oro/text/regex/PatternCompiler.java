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
 * The PatternCompiler interface defines the operations a regular
 * expression compiler must implement.  However, the types of
 * regular expressions recognized by a compiler and the Pattern 
 * implementations produced as a result of compilation are not
 * restricted.
 * <p>
 * A PatternCompiler instance is used to compile the string representation
 * (either as a String or char[]) of a regular expression into a Pattern
 * instance.  The Pattern can then be used in conjunction with the appropriate
 * PatternMatcher instance to perform pattern searches.  A form
 * of use might be:
 * <p>
 * <blockquote><pre>
 * PatternCompiler compiler;
 * PatternMatcher matcher;
 * Pattern pattern;
 * String input;
 *
 * // Initialization of compiler, matcher, and input omitted;
 *
 * try {
 *   pattern = compiler.compile("\\d+");
 * } catch(MalformedPatternException e) {
 *   System.out.println("Bad pattern.");
 *   System.out.println(e.getMessage());
 *   System.exit(1);
 * }
 * 
 *
 * if(matcher.matches(input, pattern))
 *    System.out.println(input + " is a number");
 * else
 *    System.out.println(input + " is not a number");
 *
 * </pre></blockquote>
 * <p>
 * Specific PatternCompiler implementations such as Perl5Compiler may have
 * variations of the compile() methods that take extra options affecting
 * the compilation of a pattern.  However, the PatternCompiler method
 * implementations should provide the default behavior of the class.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Pattern
 * @see PatternMatcher
 * @see MalformedPatternException
 */
public interface PatternCompiler {
  /**
   * Compiles a regular expression into a data structure that can be used
   * by a PatternMatcher implementation to perform pattern matching.
   * <p>
   * @param pattern  A regular expression to compile.
   * @return A Pattern instance constituting the compiled regular expression.
   * @exception MalformedPatternException  If the compiled expression
   *  does not conform to the grammar understood by the PatternCompiler or
   *  if some other error in the expression is encountered.
   */
  public Pattern compile(String pattern) throws MalformedPatternException;


  /**
   * Compiles a regular expression into a data structure that can be
   * used by a PatternMatcher implementation to perform pattern matching.
   * Additional regular expression syntax specific options can be passed
   * as a bitmask of options.
   * <p>
   * @param pattern  A regular expression to compile.
   * @param options  A set of flags giving the compiler instructions on
   *                 how to treat the regular expression.  The flags
   *                 are a logical OR of any number of the allowable
   *                 constants permitted by the PatternCompiler
   *                 implementation.
   * @return A Pattern instance constituting the compiled regular expression.
   * @exception MalformedPatternException  If the compiled expression
   *  does not conform to the grammar understood by the PatternCompiler or
   *  if some other error in the expression is encountered.
   */
  public Pattern compile(String pattern, int options)
       throws MalformedPatternException;


  /**
   * Compiles a regular expression into a data structure that can be used
   * by a PatternMatcher implementation to perform pattern matching.
   * <p>
   * @param pattern  A regular expression to compile.
   * @return A Pattern instance constituting the compiled regular expression.
   * @exception MalformedPatternException  If the compiled expression
   *  does not conform to the grammar understood by the PatternCompiler or
   *  if some other error in the expression is encountered.
   */
  public Pattern compile(char[] pattern) throws MalformedPatternException;


  /**
   * Compiles a regular expression into a data structure that can be
   * used by a PatternMatcher implementation to perform pattern matching.
   * Additional regular expression syntax specific options can be passed
   * as a bitmask of options.
   * <p>
   * @param pattern  A regular expression to compile.
   * @param options  A set of flags giving the compiler instructions on
   *                 how to treat the regular expression.  The flags
   *                 are a logical OR of any number of the allowable
   *                 constants permitted by the PatternCompiler
   *                 implementation.
   * @return A Pattern instance constituting the compiled regular expression.
   * @exception MalformedPatternException  If the compiled expression
   *  does not conform to the grammar understood by the PatternCompiler or
   *  if some other error in the expression is encountered.
   */
  public Pattern compile(char[] pattern, int options)
       throws MalformedPatternException;

}
