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

error = IOError

AF_INET = 2

SOCK_DGRAM = 1
SOCK_STREAM = 2


def gethostname():
    return java.net.InetAddress.getLocalHost().getHostName()

def gethostbyname(name):
    return java.net.InetAddress.getByName(name).getHostAddress()

def gethostbyaddr(name):
    # This is as close as I can get; at least the types are correct...
    addresses = java.net.InetAddress.getAllByName(gethostbyname(name))
    names = []
    addrs = []
    for addr in addresses:
	names.append(addr.getHostName())
	addrs.append(addr.getHostAddress())
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

    def __init__(self):
	self.sock = None
	self.addr = None
	self.server = 0

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
	return self.ostream.write(s)

    def getsockname(self):
	assert self.sock
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
	return org.python.core.PyFile(self.istream, self.ostream,
				      "<socket>", mode)

    def __del__(self):
	self.close()

    def close(self):
	sock = self.sock
	self.sock = 0
	if sock:
	    istream = self.istream
	    ostream = self.ostream
	self.istream = 0
	self.ostream = 0
	if sock:
	    istream.close()
	    ostream.close()
	    sock.close()


class _udpsocket:

    def __init__(self):
	self.sock = None
	self.addr = None

    def bind(self, addr, port=None):
	if port is not None:
	    addr = (addr, port)
	assert not self.sock
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
