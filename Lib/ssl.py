"""
This module provides very limited support for the SSL module on jython.

See the jython wiki for more information.
http://wiki.python.org/jython/SSLModule
"""

import socket

wrap_socket = socket.ssl
