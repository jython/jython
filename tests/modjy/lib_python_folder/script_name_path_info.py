# -*- coding: windows-1252 -*-

def test_env_script_name_path_info(environ, start_response):
	writer = start_response("200 OK", [])
	writer("%s:::%s" % (environ['SCRIPT_NAME'], environ['PATH_INFO']))
	return []
