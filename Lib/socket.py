"""Preliminary socket module.

XXX Restrictions:

- Only INET sockets
- No asynchronous behavior
- No socket options
- Can't do a very good gethostbyaddr() right...

"""

import java.net
import org.python.core
import jarray
import string

__all__ = ['AF_INET', 'SOCK_DGRAM', 'SOCK_RAW', 'SOCK_RDM', 'SOCK_SEQPACKET',
           'SOCK_STREAM', 'SocketType', 'error', 'getfqdn', 'gethostbyaddr',
           'gethostbyname', 'gethostname', 'socket']

error = IOError

AF_INET = 2

SOCK_DGRAM = 1
SOCK_STREAM = 2
SOCK_RAW = 3 # not supported
SOCK_RDM = 4 # not supported
SOCK_SEQPACKET = 5 # not supported

def _gethostbyaddr(name):
    # This is as close as I can get; at least the types are correct...
    addresses = java.net.InetAddress.getAllByName(gethostbyname(name))
    names = []
    addrs = []
    for addr in addresses:
      names.append(addr.getHostName())
      addrs.append(addr.getHostAddress())
    return (names, addrs)

def getfqdn(name=None):
    """
    Return a fully qualified domain name for name. If name is omitted or empty
    it is interpreted as the local host.  To find the fully qualified name,
    the hostname returned by gethostbyaddr() is checked, then aliases for the
    host, if available. The first name which includes a period is selected.
    In case no fully qualified domain name is available, the hostname is retur
    New in version 2.0.
    """
    if not name:
        name = gethostname()
    names, addrs = _gethostbyaddr(name)
    for a in names:
        if a.find(".") >= 0:
            return a
    return name

def gethostname():
    return java.net.InetAddress.getLocalHost().getHostName()

def gethostbyname(name):
    return java.net.InetAddress.getByName(name).getHostAddress()

def gethostbyaddr(name):
    names, addrs = _gethostbyaddr(name)
    return (names[0], names, addrs)


def socket(family, type, flags=0):
    assert family == AF_INET
    assert type in (SOCK_DGRAM, SOCK_STREAM)
    assert flags == 0
    if type == SOCK_STREAM:
	return _tcpsocket()
    else:
	return _udpsocket()


class _tcpsocket:

    sock = None
    istream = None
    ostream = None
    addr = None
    server = 0
    file_count = 0

    def bind(self, addr, port=None):
	if port is not None:
	    addr = (addr, port)
	assert not self.sock
	assert not self.addr
	host, port = addr # format check
	self.addr = addr

    def listen(self, backlog=50):
	"This signifies a server socket"
	assert not self.sock
	self.server = 1
	if self.addr:
	    host, port = self.addr
	else:
	    host, port = "", 0
	if host:
	    a = java.net.InetAddress.getByName(host)
	    self.sock = java.net.ServerSocket(port, backlog, a)
	else:
	    self.sock = java.net.ServerSocket(port, backlog)

    def accept(self):
	"This signifies a server socket"
	if not self.sock:
	    self.listen()
	assert self.server
	sock = self.sock.accept()
	host = sock.getInetAddress().getHostName()
	port = sock.getPort()
	conn = _tcpsocket()
	conn._setup(sock)
	return conn, (host, port)

    def connect(self, addr, port=None):
	"This signifies a client socket"
	if port is not None:
	    addr = (addr, port)
	assert not self.sock
	host, port = addr
	if host == "":
	    host = java.net.InetAddress.getLocalHost()
	self._setup(java.net.Socket(host, port))

    def _setup(self, sock):
	self.sock = sock
	self.istream = sock.getInputStream()
	self.ostream = sock.getOutputStream()

    def recv(self, n):
	assert self.sock
	data = jarray.zeros(n, 'b')
	m = self.istream.read(data)
	if m <= 0:
	    return ""
	if m < n:
	    data = data[:m]
	return data.tostring()

    def send(self, s):
	assert self.sock
	n = len(s)
	self.ostream.write(s)
	return n

    def getsockname(self):
	if not self.sock:
	    host, port = self.addr or ("", 0)
	    host = java.net.InetAddress.getByName(host).getHostAddress()
	else:
	    if self.server:
		host = self.sock.getInetAddress().getHostAddress()
	    else:
		host = self.sock.getLocalAddress().getHostAddress()
	    port = self.sock.getLocalPort()
	return (host, port)

    def getpeername(self):
	assert self.sock
	assert not self.server
	host = self.sock.getInetAddress().getHostAddress()
	port = self.sock.getPort()
	return (host, port)

    def makefile(self, mode="r", bufsize=-1):
        file = None
	if self.istream:
	    if self.ostream:
		file = org.python.core.PyFile(self.istream, self.ostream,
					      "<socket>", mode)
	    else:
		file = org.python.core.PyFile(self.istream, "<socket>", mode)
	elif self.ostream:
	    file = org.python.core.PyFile(self.ostream, "<socket>", mode)
	else:
	    raise IOError, "both istream and ostream have been shut down"
        if file:
            return _tcpsocket.FileWrapper(self, file)

    class FileWrapper:
        def __init__(self, socket, file):
            self.socket = socket
            self.sock = socket.sock
            self.istream = socket.istream
            self.ostream = socket.ostream

            self.file = file
            self.read       = file.read
            self.readline   = file.readline
            self.readlines  = file.readlines
            self.write      = file.write
            self.writelines = file.writelines
            self.flush      = file.flush
            self.seek       = file.seek
            self.tell       = file.tell

            self.socket.file_count += 1

        def close(self):
            if self.file.closed:
                # Already closed
                return

            self.socket.file_count -= 1
            self.file.close()

            if self.socket.file_count == 0 and self.socket.sock == 0:
                # This is the last file Only close the socket and streams 
                # if there are no outstanding files left.
                if self.sock:
                     self.sock.close()
                if self.istream:
                     self.istream.close()
                if self.ostream:
                     self.ostream.close()

    def shutdown(self, how):
	assert how in (0, 1, 2)
	assert self.sock
	if how in (0, 2):
	    self.istream = None
	if how in (1, 2):
	    self.ostream = None

    def close(self):
	sock = self.sock
	istream = self.istream
	ostream = self.ostream
	self.sock = 0
	self.istream = 0
	self.ostream = 0
        # Only close the socket and streams if there are no 
        # outstanding files left.
        if self.file_count == 0:
	    if istream:
	        istream.close()
	    if ostream:
	        ostream.close()
	    if sock:
	        sock.close()


class _udpsocket:

    def __init__(self):
	self.sock = None
	self.addr = None

    def bind(self, addr, port=None):
	if port is not None:
	    addr = (addr, port)
	assert not self.sock
	host, port = addr
	if host == "":
	    self.sock = java.net.DatagramSocket(port)
	else:
	    a = java.net.InetAddress.getByName(host)
	    self.sock = java.net.DatagramSocket(port, a)

    def connect(self, addr, port=None):
	if port is not None:
	    addr = (addr, port)
	host, port = addr # format check
	assert not self.addr
	if not self.sock:
	    self.sock = java.net.DatagramSocket()
	self.addr = addr # convert host to InetAddress instance?

    def sendto(self, data, addr):
	n = len(data)
	if not self.sock:
	    self.sock = java.net.DatagramSocket()
	host, port = addr
	bytes = jarray.array(map(ord, data), 'b')
	a = java.net.InetAddress.getByName(host)
	packet = java.net.DatagramPacket(bytes, n, a, port)
	self.sock.send(packet)
	return n

    def send(self, data):
	assert self.addr
	return self.sendto(self.addr)

    def recvfrom(self, n):
	assert self.sock
	bytes = jarray.zeros(n, 'b')
	packet = java.net.DatagramPacket(bytes, n)
	self.sock.receive(packet)
	host = packet.getAddress().getHostName()
	port = packet.getPort()
	m = packet.getLength()
	if m < n:
	    bytes = bytes[:m]
	return bytes.tostring(), (host, port)

    def recv(self, n):
	assert self.sock
	bytes = jarray.zeros(n, 'b')
	packet = java.net.DatagramPacket(bytes, n)
	self.sock.receive(packet)
	m = packet.getLength()
	if m < n:
	    bytes = bytes[:m]
	return bytes.tostring()

    def getsockname(self):
	assert self.sock
	host = self.sock.getLocalAddress().getHostName()
	port = self.sock.getLocalPort()
	return (host, port)

    def getpeername(self):
	assert self.sock
	host = self.sock.getInetAddress().getHostName()
	port = self.sock.getPort()
	return (host, port)

    def __del__(self):
	self.close()

    def close(self):
	sock = self.sock
	self.sock = 0
	sock.close()

SocketType = _tcpsocket

def test():
    s = socket(AF_INET, SOCK_STREAM)
    s.connect(("", 80))
    s.send("GET / HTTP/1.0\r\n\r\n")
    while 1:
	data = s.recv(2000)
	print data
	if not data:
	    break

if __name__ == '__main__':
    test()
