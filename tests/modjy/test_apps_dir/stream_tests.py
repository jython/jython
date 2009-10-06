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
    A variety of app callables used to test the WSGI streams.
"""

from UserDict import UserDict

def extract_params(qstring):
    params = {}
    if qstring:
        name_vals = [t.split('=', 1) for t in qstring.split('&')]
        for n, v in name_vals: params[n] = v
    return params

def test_read_input_stream(environ, start_response):
    writer = start_response("200 OK", [])
    wsgi_input = environ['wsgi.input']
    params = extract_params(environ['QUERY_STRING'])
    readsize = None
    if params.has_key('readsize'):
        readsize = int(params['readsize'])
    if readsize:
        pieces = []
        piece = wsgi_input.read(readsize)
        while piece:
            pieces.append(piece)
            piece = wsgi_input.read(readsize)
        data = ''.join(pieces)
    else:
        data = wsgi_input.read()
    output_dict = {'data': data}
    writer(repr(output_dict))
    return []

def test_readline_input_stream(environ, start_response):
    writer = start_response("200 OK", [])
    wsgi_input = environ['wsgi.input']
    params = extract_params(environ['QUERY_STRING'])
    readsize = None
    if params.has_key('readsize'):
        readsize = int(params['readsize'])
    if readsize:
        data = wsgi_input.readline(readsize)
    else:
        data = wsgi_input.readline()
    output_dict = {'data': data}
    writer(repr(output_dict))
    return []

def test_readlines_input_stream(environ, start_response):
    writer = start_response("200 OK", [])
    wsgi_input = environ['wsgi.input']
    params = extract_params(environ['QUERY_STRING'])
    readsize = None
    if params.has_key('readsize'):
        readsize = int(params['readsize'])
    if readsize:
        data = wsgi_input.readlines(readsize)
    else:
        data = wsgi_input.readlines()
    output_dict = {'data': "$".join(data)}
    writer(repr(output_dict))
    return []

def test_error_stream(environ, start_response):
    writer = start_response("200 OK", [])
    wsgi_errors = environ['wsgi.errors']
    error_msg = None
    for method in ['flush', 'write', 'writelines', ]:
        if not hasattr(wsgi_errors, method):
            error_msg = "wsgi.errors has no '%s' attr" % method
        if not error_msg and not callable(getattr(wsgi_errors, method)):
            error_msg = "wsgi.errors.%s attr is not callable" % method
        if error_msg: break
    return_msg = error_msg or "success"
    writer(return_msg)
    return []
