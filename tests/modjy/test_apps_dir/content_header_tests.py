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
    A variety of app callables used to test content related headers.
"""

def test_set_content_length(environ, start_response):
    writer = start_response("200 OK", [('content-length', '12')])
    writer('Hello World!')
    return []

def test_set_bad_content_length(environ, start_response):
    writer = start_response("200 OK", [('content-length', 'abcd')])
    writer('Hello World!')
    return []

def test_inferred_content_length(environ, start_response):
    writer = start_response("200 OK", [])
    return [environ["QUERY_STRING"]]

