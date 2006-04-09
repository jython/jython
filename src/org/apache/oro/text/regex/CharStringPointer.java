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
 * The CharStringPointer class is used to facilitate traversal of a char[]
 * in the manner pointer traversals of strings are performed in C/C++.
 * It is expected that the compiler will inline all the functions.

 @author <a href="mailto:dfs@savarese.org">Daniel F. Savarese</a>
 @version $Id$

 */
final class CharStringPointer {
  static final char _END_OF_STRING = Character.MAX_VALUE;
  int _offset;
  char[] _array;

  CharStringPointer(char[] charArray, int offset) {
    _array  = charArray;
    _offset = offset;
  }

  CharStringPointer(char[] charArray) {
    this(charArray, 0);
  }

  char _getValue()  {
    return _getValue(_offset);
  }

  char _getValue(int offset) {
    if(offset < _array.length && offset >= 0)
      return _array[offset];
    return _END_OF_STRING;
  }

  char _getValueRelative(int offset) {
    return _getValue(_offset + offset);
  }

  int _getLength() { return _array.length; }

  int _getOffset() { return _offset; }

  void _setOffset(int offset) { _offset = offset; }

  boolean _isAtEnd() {
    return (_offset >= _array.length);
  }

  char _increment(int inc) {
    _offset+=inc;
    if(_isAtEnd()) {
      _offset = _array.length;
      return _END_OF_STRING;
    }

    return _array[_offset];
  }

  char _increment() { return _increment(1); }

  char _decrement(int inc) {
    _offset-=inc; 
    if(_offset < 0)
      _offset = 0;

    return _array[_offset];
  }

  char _decrement() { return _decrement(1); }

  char _postIncrement() {
    char ret;
    ret = _getValue();
    _increment();
    return ret;
  }

  char _postDecrement() {
    char ret;
    ret = _getValue();
    _decrement();
    return ret;
  }


  String _toString(int offset) {
    return new String(_array, offset, _array.length - offset);
  }

  public String toString() {
    return _toString(0);
  }
}
