"""
[ #438297 ] SimpleHTTPServer does not work
"""

import support

import sys
import SimpleHTTPServer
import BaseHTTPServer

def test(HandlerClass = SimpleHTTPServer.SimpleHTTPRequestHandler,
         ServerClass = BaseHTTPServer.HTTPServer):
    server_address = ('', 8000)
    # Silense the server
    HandlerClass.log_message = lambda x, b, *arg: None
    httpd = ServerClass(server_address, HandlerClass)
    # do just one request.
    httpd.handle_request()

import thread
thread.start_new_thread(test, ())

import httplib
import time
time.sleep(5)

h = httplib.HTTP()
h.connect("localhost", 8000)
h.putrequest('GET', "/")
h.endheaders()
status, reason, headers = h.getreply()
if status != 200:
    raise support.TestError("Wrong status: %d" % status)
if reason != "OK":
    raise support.TestError("Wrong status: %d" % status)
h.getfile().read()


