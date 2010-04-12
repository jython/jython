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
    App callables used to test post data
"""

def test_return_post_data(environ, start_response):
    post_data = environ['wsgi.input'].read()
    return_value = repr(post_data)
    start_response("200 OK", [('content-length', '%s' % len(return_value))])
    return [return_value]
