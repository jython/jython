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
	A variety of app callables used to test the WSGI headers.
"""

def test_invalid_status_code(environ, start_response):
	writer = start_response("twohundred ok", [])
	# Should never get here
	writer("D'oh!")
	return []

def test_non_latin1_status_string(environ, start_response):
	writer = start_response(u"200 \u03c9\u03ba", []) # "200 Omega Kappa"
	# Should never get here
	writer("D'oh!")
	return []

def test_control_chars_in_status_string(environ, start_response):
	writer = start_response(u"200 OK\x08\x08Hoopy", []) # "200 OK^H^HHoopy"
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_not_list(environ, start_response):
	qstring = environ['QUERY_STRING']
	if qstring == '1':
		headers = {}
	elif qstring == '2':
		headers = ()
	elif qstring == '3':
		headers = 1
	else:
		headers = None
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_list_non_tuples(environ, start_response):
	qstring = environ['QUERY_STRING']
	if qstring == '1':
		header = {}
	elif qstring == '2':
		header = []
	elif qstring == '3':
		header = 1
	else:
		header = None
	headers = [header]
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_list_wrong_length_tuples(environ, start_response):
	qstring = environ['QUERY_STRING']
	length = int(qstring)
	if length == 2: length = 3
	header_tuple = tuple(range(int(qstring)))
	headers = [header_tuple]
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_list_wrong_types_in_tuples(environ, start_response):
	qstring = environ['QUERY_STRING']
	if qstring == '1':
		headers = [(1, 1)]
	elif qstring == '2':
		headers = [('header_name', 1L)]
	elif qstring == '3':
		headers = [('header_name', None)]
	elif qstring == '4':
		headers = [('header_name', 42.0)]
	else:
		headers = [(None, 'value')]
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_list_contains_non_latin1_values(environ, start_response):
	headers = [('x-unicoded-header', u'\u03b1\u03b2\u03b3\u03b4\u03b5')]
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_list_contains_values_with_control_chars(environ, start_response):
	headers = [('x-control-coded-header', 'your father smelled of elder\x08\x08\x08\x08\x08loganberries')]
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []

def test_headers_list_contains_accented_latin1_values(environ, start_response):
	name, value = environ['QUERY_STRING'].split('=')
	headers = [(name, value)]
	writer = start_response("200 OK", headers)
	writer("Doesn't matter")
	return []

def test_headers_list_contains_accented_latin1_values(environ, start_response):
	name, value = environ['QUERY_STRING'].split('=')
	headers = [(name, value)]
	writer = start_response("200 OK", headers)
	writer("Doesn't matter")
	return []

def test_hop_by_hop(environ, start_response):
	qstring = environ['QUERY_STRING']
	headers = [(qstring, 'doesnt matter')]
	writer = start_response("200 OK", headers)
	# Should never get here
	writer("D'oh!")
	return []
