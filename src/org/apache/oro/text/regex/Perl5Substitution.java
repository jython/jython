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

import java.util.Vector;

/**
 * Perl5Substitution implements a Substitution consisting of a
 * literal string, but allowing Perl5 variable interpolation referencing
 * saved groups in a match.  This class is intended for use with
 * {@link Util#substitute Util.substitute}.
 * <p>
 * The substitution string may contain variable interpolations referring
 * to the saved parenthesized groups of the search pattern.
 * A variable interpolation is denoted by <b>$1</b>, or <b>$2</b>,
 * or <b>$3</b>, etc.  If you don want such expressions to be
 * interpreted literally, you should set the <b> numInterpolations </b>
 * parameter to <b> INTERPOLATE_NONE </b>.  It is easiest to explain
 * what an interpolated variable does by giving an example:
 * <ul>
 * Suppose you have the pattern <b>b\d+:</b> and you want to substitute
 * the <b>b</b>'s for <b>a</b>'s and the colon for a dash in parts of
 * your input matching the pattern.  You can do this by changing the
 * pattern to <b>b(\d+):</b> and using the substitution expression
 * <b>a$1-</b>.  When a substitution is made, the <b>$1</b> means
 * "Substitute whatever was matched by the first saved group of the
 *  matching pattern."  An input of <b>b123:</b> after substitution
 * would yield a result of <b>a123-</b>.  But there's a little more
 * to be aware of.  If you set the <b>numInterpolations</b> parameter to
 * <b>INTERPOLATE_ALL</b>, then every time a match is found, the 
 * interpolation variables are computed relative to that match.
 * But if <b>numInterpolations</b> is set to some positive integer, then
 * only the interpolation variables for the first <b>numInterpolations</b>
 * matches are computed relative to the most recent match.  After that,
 * the remaining substitutions have their variable interpolations performed
 * relative to the <b> numInterpolations </b>'th match.  So using the
 * previously mentioned pattern and substitution expression, if you have
 * an input of <pre><b>Tank b123: 85  Tank b256: 32  Tank b78: 22</b></pre>
 * and use a <b> numInterpolations </b> value of <b>INTERPOLATE_ALL</b> and
 * <b> numSubs </b> value (see
 * {@link Util#substitute Util.substitute})
 * of <b> SUBSTITUTE_ALL</b>, then your result  will be:
 * <pre><b>Tank a123- 85  Tank a256- 32  Tank a78- 22</b></pre>
 * But if you set <b> numInterpolations </b> to 2 and keep 
 * <b> numSubs </b> with a value of <b>SUBSTITUTE_ALL</b>, your result is:
 * <pre><b>Tank a123- 85  Tank a256- 32  Tank a256- 22</b></pre>
 * Notice how the last substitution uses the same value for <b>$1</b>
 * as the second substitution.
 * </ul>
 * <p>
 * A final thing to keep in mind is that if you use an interpolation variable
 * that corresponds to a group not contained in the match, then it is
 * interpreted literally.  So given the regular expression from the
 * example, and a substitution expression of <b>a$2-</b>, the result
 * of the last sample input would be:
 * <pre><b>Tank a$2- 85  Tank a$2- 32  Tank a$2- 22</b></pre>
 * Also, <b>$0</b> is always interpreted literally.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 * @see Substitution
 * @see Util
 * @see Util#substitute
 * @see Substitution
 * @see StringSubstitution
 */
public class Perl5Substitution extends StringSubstitution {
  /**
   * A constant used when creating a Perl5Substitution indicating that
   * interpolation variables should be computed relative to the most
   * recent pattern match.
   */
  public static final int INTERPOLATE_ALL = 0;

  /**
   * A constant used when creating a Perl5Substitution indicating that 
   * interpolation variables should be interpreted literally, effectively 
   * disabling interpolation.
   */
  public static final int INTERPOLATE_NONE = -1;

  int _numInterpolations;
  Vector _substitutions;
  transient String _lastInterpolation;

  static Vector _parseSubs(String sub) {
    boolean saveDigits, storedInterpolation;
    int current;
    char[] str;
    Vector subs;
    StringBuffer numBuffer, strBuffer;

    subs = new Vector(5);
    numBuffer = new StringBuffer(5);
    strBuffer = new StringBuffer(10);

    str = sub.toCharArray();
    current = 0;
    saveDigits = false;
    storedInterpolation = false;

    while(current < str.length) {
      if(saveDigits && Character.isDigit(str[current])) {
	numBuffer.append(str[current]);

	if(strBuffer.length() > 0) {
	  subs.addElement(strBuffer.toString());
	  strBuffer.setLength(0);
	}
      } else {
	if(saveDigits) {
	  try {
	    subs.addElement(new Integer(numBuffer.toString()));
	    storedInterpolation = true;
	  } catch(NumberFormatException e) {
	    subs.addElement(numBuffer.toString());
	  }

	  numBuffer.setLength(0);
	  saveDigits = false;
	}

	if(str[current] == '$' &&
	   current + 1 < str.length && str[current + 1] != '0' &&
	   Character.isDigit(str[current + 1]))
	  saveDigits = true;
	else
	  strBuffer.append(str[current]);
      }

      ++current;
    } // end while


    if(saveDigits) {
      try {
	subs.addElement(new Integer(numBuffer.toString()));
	storedInterpolation = true;
      } catch(NumberFormatException e) {
	subs.addElement(numBuffer.toString());
      }
    } else if(strBuffer.length() > 0)
      subs.addElement(strBuffer.toString());

    return (storedInterpolation ? subs : null);
  }


  String _finalInterpolatedSub(MatchResult result) {
    StringBuffer buffer = new StringBuffer(10);
    _calcSub(buffer, result);
    return buffer.toString();
  }

  void _calcSub(StringBuffer buffer, MatchResult result) {
    int size, element, value;
    Object obj;
    Integer integer;
    String group;

    size = _substitutions.size();

    for(element=0; element < size; element++) {
      obj = _substitutions.elementAt(element);

      if(obj instanceof String)
	buffer.append(obj);
      else {
	integer = (Integer)obj;
	value = integer.intValue();

	if(value > 0 && value < result.groups()) {
	  group = result.group(value);

	  if(group != null)
	    buffer.append(group);
	} else {
	  buffer.append('$');
	  buffer.append(value);
	}
      }
    }
  }


  /**
   * Default constructor initializing substitution to a zero length
   * String and the number of interpolations to
   * {@link #INTERPOLATE_ALL}.
   */
  public Perl5Substitution() {
    this("", INTERPOLATE_ALL);
  }

  /**
   * Creates a Perl5Substitution using the specified substitution
   * and setting the number of interpolations to
   * {@link #INTERPOLATE_ALL}.
   * <p>
   * @param substitution The string to use as a substitution.
   */
  public Perl5Substitution(String substitution) {
    this(substitution, INTERPOLATE_ALL);
  }

  /**
   * Creates a Perl5Substitution using the specified substitution
   * and setting the number of interpolations to the specified value.
   * <p>
   * @param substitution The string to use as a substitution.
   * @param numInterpolations 
   *            If set to <b>INTERPOLATE_NONE</b>, interpolation variables are
   *            interpreted literally and not as references to the saved
   *            parenthesized groups of a pattern match.  If set to
   *            <b> INTERPOLATE_ALL </b>, all variable interpolations
   *            are computed relative to the pattern match responsible for
   *            the current substitution.  If set to a positive integer,
   *            the first <b> numInterpolations </b> substitutions have
   *            their variable interpolation performed relative to the
   *            most recent match, but the remaining substitutions have
   *            their variable interpolations performed relative to the
   *            <b> numInterpolations </b>'th match.
   */
  public Perl5Substitution(String substitution, int numInterpolations) {
    setSubstitution(substitution, numInterpolations);
  }


  /**
   * Sets the substitution represented by this Perl5Substitution, also
   * setting the number of interpolations to
   * {@link #INTERPOLATE_ALL}.
   * You should use this method in order to avoid repeatedly allocating new
   * Perl5Substitutions.  It is recommended that you allocate a single
   * Perl5Substitution and reuse it by using this method when appropriate.
   * <p>
   * @param substitution The string to use as a substitution.
   */
  public void setSubstitution(String substitution) {
    setSubstitution(substitution, INTERPOLATE_ALL);
  }


  /**
   * Sets the substitution represented by this Perl5Substitution, also
   * setting the number of interpolations to the specified value.
   * You should use this method in order to avoid repeatedly allocating new
   * Perl5Substitutions.  It is recommended that you allocate a single
   * Perl5Substitution and reuse it by using this method when appropriate.
   * <p>
   * @param substitution The string to use as a substitution.
   * @param numInterpolations 
   *            If set to <b>INTERPOLATE_NONE</b>, interpolation variables are
   *            interpreted literally and not as references to the saved
   *            parenthesized groups of a pattern match.  If set to
   *            <b> INTERPOLATE_ALL </b>, all variable interpolations
   *            are computed relative to the pattern match responsible for
   *            the current substitution.  If set to a positive integer,
   *            the first <b> numInterpolations </b> substitutions have
   *            their variable interpolation performed relative to the
   *            most recent match, but the remaining substitutions have
   *            their variable interpolations performed relative to the
   *            <b> numInterpolations </b>'th match.
   */
  public void setSubstitution(String substitution, int numInterpolations) {
    super.setSubstitution(substitution);
    _numInterpolations = numInterpolations;

    if(numInterpolations != INTERPOLATE_NONE && 
       substitution.indexOf('$') != -1)
      _substitutions = _parseSubs(substitution);
    else
      _substitutions = null;
    _lastInterpolation = null;
  }


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
    if(_substitutions == null) {
      super.appendSubstitution(appendBuffer, match, substitutionCount,
			       originalInput, matcher, pattern);
      return;
    }

    if(_numInterpolations < 1 || substitutionCount < _numInterpolations)
      _calcSub(appendBuffer, match);
    else {
      if(substitutionCount == _numInterpolations)
	_lastInterpolation = _finalInterpolatedSub(match);
      appendBuffer.append(_lastInterpolation);
    }
  }

}
