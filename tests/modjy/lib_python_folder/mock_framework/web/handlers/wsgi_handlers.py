# -*- coding: windows-1252 -*-

class WSGIHandlerClass:

    def __init__(self):
        self.counter = 0

    def _response(self, start_response_callable, called):
        start_response_callable("200 OK", [])
        result = "%s counter = %d" % (called, self.counter)
        self.counter += 1
        return [result]

    def __call__(self, environ, start_response_callable):
        return self._response(start_response_callable, "__call__")

    def handler_fn(self, environ, start_response_callable):
        return self._response(start_response_callable, "handler_fn")

def WSGIHandlerFunction(environ, start_response_callable):
    start_response_callable("200 OK", [])
    return ['WSGIHandlerFunction called.']
