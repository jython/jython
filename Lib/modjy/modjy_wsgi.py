###
#
# Copyright 2004-2008 Alan Kennedy. 
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

from java.lang import System
from org.python.core import PyFile

from modjy_exceptions import *

server_name = "modjy"
server_param_prefix = "%s.param" % server_name
j2ee_ns_prefix = "j2ee"

class modjy_wsgi:

	#
	#	WSGI constants
	#

	empty_pystring = ""
	wsgi_version = (1,0)

	#
	#	Container-specific constants
	#

	modjy_version = (0, 22, 3)

	def set_string_envvar(self, dict, name, value, default_value):
		if value == default_value:
			dict[name] = self.empty_pystring
		else:
			dict[name] = value

	def set_string_envvar_optional(self, dict, name, value, default_value):
		if value != default_value:
			dict[name] = str(value)

	def set_int_envvar(self, dict, name, value, default_value):
		if value == default_value:
			dict[name] = self.empty_pystring
		else:
			dict[name] = str(value)

	def set_container_specific_wsgi_vars(self, req, resp, dict, params):
		dict["%s.version" % server_name] = self.modjy_version
		for pname in params.keys():
			dict["%s.%s" % (server_param_prefix, pname)] = params[pname]

	def set_j2ee_specific_wsgi_vars(self, dict, j2ee_ns):
		for p in j2ee_ns.keys():
			dict["%s.%s" % (j2ee_ns_prefix, p)] = j2ee_ns[p]

	def set_required_cgi_environ (self, req, resp, dict):
		self.set_string_envvar(dict, "REQUEST_METHOD", req.getMethod(), None)
		self.set_string_envvar(dict, "QUERY_STRING", req.getQueryString(), None)
		self.set_string_envvar(dict, "CONTENT_TYPE", req.getContentType(), None)
		self.set_int_envvar(dict, "CONTENT_LENGTH", req.getContentLength(), -1)
		self.set_string_envvar(dict, "SERVER_NAME", req.getLocalName(), None)
		self.set_int_envvar(dict, "SERVER_PORT", req.getLocalPort(), 0)

	def set_other_cgi_environ (self, req, resp, dict):
		if req.isSecure():
			self.set_string_envvar(dict, "HTTPS", 'on', None)
		else:
			self.set_string_envvar(dict, "HTTPS", 'off', None)
		self.set_string_envvar(dict, "SERVER_PROTOCOL", req.getProtocol(), None)
		self.set_string_envvar(dict, "REMOTE_HOST", req.getRemoteHost(), None)
		self.set_string_envvar(dict, "REMOTE_ADDR", req.getRemoteAddr(), None)
		self.set_int_envvar(dict, "REMOTE_PORT", req.getRemotePort(), -1)
		self.set_string_envvar_optional(dict, "AUTH_TYPE", req.getAuthType(), None)
		self.set_string_envvar_optional(dict, "REMOTE_USER", req.getRemoteUser(), None)

	def set_http_header_environ(self, req, resp, dict):
		for curr_header_name in req.getHeaderNames():
			values = None
			for next_value in req.getHeaders(curr_header_name):
				if values is None:
					values = next_value
				else:
					if isinstance(values, types.ListType):
						values.append(next_value)
					else:
						values = [values]
			dict["HTTP_%s" % curr_header_name.replace('-', '_').upper()] = values

	def set_required_wsgi_vars(self, req, resp, dict):
		dict["wsgi.version"] = self.wsgi_version
		dict["wsgi.url_scheme"] = req.getScheme()
		dict["wsgi.multithread"] = \
			int(dict["%s.cache_callables" % server_param_prefix]) \
				and \
			int(dict["%s.multithread" % server_param_prefix])
		dict["wsgi.multiprocess"] = self.wsgi_multiprocess = 0
		dict["wsgi.run_once"] = not(dict["%s.cache_callables" % server_param_prefix])

	def set_wsgi_streams(self, req, resp, dict):
		try:
			dict["wsgi.input"]  = PyFile(req.getInputStream())
			dict["wsgi.errors"] =  PyFile(System.err)
		except IOException, iox:
			raise ModjyIOException(iox)

	def set_wsgi_classes(self, req, resp, dict):
		# dict["wsgi.file_wrapper"]  = modjy_file_wrapper
		pass

	def set_user_specified_environment(self, req, resp, wsgi_environ, params):
		if not params.has_key('initial_env') or not params['initial_env']:
			return
		user_env_string = params['initial_env']
		for l in user_env_string.split('\n'):
			l = l.strip()
			if l:
				name, value = l.split(':', 1)
				wsgi_environ[name.strip()] = value.strip()

	def set_wsgi_environment(self, req, resp, wsgi_environ, params, j2ee_ns):
		self.set_container_specific_wsgi_vars(req, resp, wsgi_environ, params)
		self.set_j2ee_specific_wsgi_vars(wsgi_environ, j2ee_ns)
		self.set_required_cgi_environ(req, resp, wsgi_environ)
		self.set_other_cgi_environ(req, resp, wsgi_environ)
		self.set_http_header_environ(req, resp, wsgi_environ)
		self.set_required_wsgi_vars(req, resp, wsgi_environ)
		self.set_wsgi_streams(req, resp, wsgi_environ)
		self.set_wsgi_classes(req, resp, wsgi_environ)
		self.set_user_specified_environment(req, resp, wsgi_environ, params)
