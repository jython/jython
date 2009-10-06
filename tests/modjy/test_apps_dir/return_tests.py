# -*- coding: windows-1252 -*-

###
#
# Copyright Alan Kennedy. 
# 
# You may contact the copyright holder at this uri:
# 
# http://www.xhaus.com/contact/modjy
# 
# The licence under which this code is released is the Apache License v2.0.
# 
# The terms and conditions of this license are listed in a file contained
# in the distribution that also contained this file, under the name
# LICENSE.txt.
# 
# You may also read a copy of the license at the following web address.
# 
# http://modjy.xhaus.com/LICENSE.txt
#
###

"""
    A variety of app callables used to test return types.
"""

def test_non_iterable_return(environ, start_response):
    writer = start_response("200 OK", [])
    query_string = environ['QUERY_STRING']
    return_object = {
        'int':      42,
        'float':    42.0,
        'str':      "Strings are not permitted return values",
        'unicode':  u"Unicode strings are not permitted return values",
        'none':     None,
    }[query_string]
    return return_object

def test_iterable_containing_non_strings_return(environ, start_response):
    writer = start_response("200 OK", [])
    query_string = environ['QUERY_STRING']
    return_object = {
        'int':      42,
        'float':    42.0,
        'none':     None,
    }[query_string]
    return [return_object]

def test_start_response_not_called(environ, start_response):
    return ["Should fail because start_response not called"]

class IterClass:

    def __init__(self, data, length):
        self.pieces = 0
        self.data = data
        self.length = length

    def __iter__(self):
        return self

    def __len__(self):
        return self.length

    def next(self):
        if self.pieces == 0:
            self.pieces = 1
            return self.data
        else:
            raise StopIteration

def test_bad_length_iterator(environ, start_response):
    start_response("200 OK", [])
    return IterClass("Hello World!", 2)

def test_return_list_strings(environ, start_response):
    writer = start_response("200 OK", [])
    query_string = environ['QUERY_STRING']
    return [query_string]

def test_return_generator(environ, start_response):
    writer = start_response("200 OK", [])
    query_string = environ['QUERY_STRING']
    yield query_string

def test_return_file_like(environ, start_response):
    writer = start_response("200 OK", [])
    query_string = environ['QUERY_STRING']
    from StringIO import StringIO
    return StringIO(query_string)

def test_iterable_instance(environ, start_response):
    writer = start_response("200 OK", [])
    query_string = environ['QUERY_STRING']
    return IterClass(query_string, 1)
