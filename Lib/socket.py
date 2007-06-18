"""
This is an updated socket module for use on JVMs > 1.4; it is derived from the
old jython socket module.
The primary extra it provides is non-blocking support.

XXX Restrictions:

- Only INET sockets
- No asynchronous behavior
- No socket options
- Can't do a very good gethostbyaddr() right...
AMAK: 20050527: added socket timeouts
AMAK: 20070515: Added non-blocking (asynchronous) support
AMAK: 20070515: Added client-side SSL support
"""

_defaulttimeout = None

import threading
import time
import types
import jarray
import string
import sys

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InterruptedIOException
import java.lang.Exception
import java.lang.String
import java.net.BindException
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.IllegalBlockingModeException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import javax.net.ssl.SSLSocketFactory
import org.python.core.PyFile

try:
    import errno
    ERRNO_EWOULDBLOCK  = errno.EWOULDBLOCK
    ERRNO_EACCES       = errno.EACCES
    ERRNO_ECONNREFUSED = errno.ECONNREFUSED
    ERRNO_EINPROGRESS  = errno.EINPROGRESS
except ImportError:
    # Support jython 2.1
    ERRNO_EWOULDBLOCK  = 11
    ERRNO_EACCES       = 13
    ERRNO_ECONNREFUSED = 111
    ERRNO_EINPROGRESS  = 115

class error(Exception): pass
class herror(error): pass
class gaierror(error): pass
class timeout(error): pass

ALL = None

exception_map = {

# (<javaexception>, <circumstance>) : lambda: <code that raises the python equivalent>

(java.io.InterruptedIOException, ALL) : lambda exc: timeout('timed out'),
(java.net.BindException, ALL) : lambda exc: error(ERRNO_EACCES, 'Permission denied'),
(java.net.ConnectException, ALL) : lambda exc: error( (ERRNO_ECONNREFUSED, 'Connection refused') ),
(java.net.SocketTimeoutException, ALL) : lambda exc: timeout('timed out'),

}

def would_block_error(exc=None):
    return error( (ERRNO_EWOULDBLOCK, 'The socket operation could not complete without blocking') )

def map_exception(exc, circumstance=ALL):
    try:
#        print "Mapping exception: %s" % str(exc)
        return exception_map[(exc.__class__, circumstance)](exc)
    except KeyError:
        return error('Unmapped java exception: %s' % exc.toString())

exception_map.update({
        (java.nio.channels.IllegalBlockingModeException, ALL) : would_block_error,
    })

MODE_BLOCKING    = 'block'
MODE_NONBLOCKING = 'nonblock'
MODE_TIMEOUT     = 'timeout'

_permitted_modes = (MODE_BLOCKING, MODE_NONBLOCKING, MODE_TIMEOUT)

class _nio_impl:

    timeout = None
    mode = MODE_BLOCKING

    def read(self, buf):
        bytebuf = java.nio.ByteBuffer.wrap(buf)
        count = self.jchannel.read(bytebuf)
        return count

    def write(self, buf):
        bytebuf = java.nio.ByteBuffer.wrap(buf)
        count = self.jchannel.write(bytebuf)
        return count

    def _setreuseaddress(self, flag):
        self.jsocket.setReuseAddress(flag)

    def _getreuseaddress(self, flag):
        return self.jsocket.getReuseAddress()

    def getpeername(self):
        return (self.jsocket.getInetAddress().getHostName(), self.jsocket.getPort() )

    def config(self, mode, timeout):
        self.mode = mode
        if self.mode == MODE_BLOCKING:
            self.jchannel.configureBlocking(1)
        if self.mode == MODE_NONBLOCKING:
            self.jchannel.configureBlocking(0)
        if self.mode == MODE_TIMEOUT:
            # self.channel.configureBlocking(0)
            self.jsocket.setSoTimeout(int(timeout*1000))

    def close1(self):
        self.jsocket.close()

    def close2(self):
        self.jchannel.close()

    def close3(self):
        if not self.jsocket.isClosed():
            self.jsocket.close()

    def close4(self):
        if not self.jsocket.isClosed():
            if hasattr(self.jsocket, 'shutdownInput') and not self.jsocket.isInputShutdown():
                self.jsocket.shutdownInput()
            if hasattr(self.jsocket, 'shutdownOutput') and not self.jsocket.isOutputShutdown():
                self.jsocket.shutdownOutput()
            self.jsocket.close()

    close = close1
#    close = close2
#    close = close3
#    close = close4

    def getchannel(self):
        return self.jchannel

    fileno = getchannel

class _client_socket_impl(_nio_impl):

    def __init__(self, socket=None):
        if socket:
            self.jchannel = socket.getChannel()
            self.host = socket.getInetAddress().getHostName()
            self.port = socket.getPort()
        else:
            self.jchannel = java.nio.channels.SocketChannel.open()
            self.host = None
            self.port = None
        self.jsocket = self.jchannel.socket()

    def bind(self, host, port):
        self.jsocket.bind(java.net.InetSocketAddress(host, port))

    def connect(self, host, port):
        self.host = host
        self.port = port
        self.jchannel.connect(java.net.InetSocketAddress(self.host, self.port))

    def finish_connect(self):
        return self.jchannel.finishConnect()

    def close(self):
        _nio_impl.close(self)

class _server_socket_impl(_nio_impl):

    def __init__(self, host, port, backlog, reuse_addr):
        self.jchannel = java.nio.channels.ServerSocketChannel.open()
        self.jsocket = self.jchannel.socket()
        if host:
            bindaddr = java.net.InetSocketAddress(host, port)
        else:
            bindaddr = java.net.InetSocketAddress(port)
        self._setreuseaddress(reuse_addr)
        self.jsocket.bind(bindaddr, backlog)

    def accept(self):
        try:
            if self.mode in (MODE_BLOCKING, MODE_NONBLOCKING):
                new_cli_chan = self.jchannel.accept()
                if new_cli_chan != None:
                    return _client_socket_impl(new_cli_chan.socket())
                else:
                    return None
            else:
                # In timeout mode now
                new_cli_sock = self.jsocket.accept()
                return _client_socket_impl(new_cli_sock)
        except java.lang.Exception, jlx:
            raise map_exception(jlx)
        
    def close(self):
        _nio_impl.close(self)

class _datagram_socket_impl(_nio_impl):

    def __init__(self, port=None, address=None, reuse_addr=0):
        self.jchannel = java.nio.channels.DatagramChannel.open()
        self.jsocket = self.jchannel.socket()
        if port:
            if address is not None:
                local_address = java.net.InetSocketAddress(address, port)
            else:
                local_address = java.net.InetSocketAddress(port)
            self.jsocket.bind(local_address)
        self._setreuseaddress(reuse_addr)

    def connect(self, host, port):
        self.jchannel.connect(java.net.InetSocketAddress(host, port))

    def finish_connect(self):
        return self.jchannel.finishConnect()

    def receive(self, packet):
        self.jsocket.receive(packet)

    def send(self, packet):
        self.jsocket.send(packet)

__all__ = [ 'AF_INET', 'SO_REUSEADDR', 'SOCK_DGRAM', 'SOCK_RAW',
        'SOCK_RDM', 'SOCK_SEQPACKET', 'SOCK_STREAM', 'SOL_SOCKET',
        'SocketType', 'SocketTypes', 'error', 'herror', 'gaierror', 'timeout',
        'getfqdn', 'gethostbyaddr', 'gethostbyname', 'gethostname',
        'socket', 'getaddrinfo', 'getdefaulttimeout', 'setdefaulttimeout',
        'has_ipv6', 'htons', 'htonl', 'ntohs', 'ntohl',
        ]

AF_INET = 2

SOCK_DGRAM = 1
SOCK_STREAM = 2
SOCK_RAW = 3 # not supported
SOCK_RDM = 4 # not supported
SOCK_SEQPACKET = 5 # not supported
SOL_SOCKET = 0xFFFF
SO_REUSEADDR = 4

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

def getservbyname(servicename, protocolname=None):
    # http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4071389
    # How complex is the structure of /etc/services?
    raise NotImplementedError("getservbyname not yet supported on jython.")

def getservbyport(port, protocolname=None):
    # Same situation as above
    raise NotImplementedError("getservbyport not yet supported on jython.")

def getprotobyname(protocolname=None):
    # Same situation as above
    raise NotImplementedError("getprotobyname not yet supported on jython.")

def socket(family = AF_INET, type = SOCK_STREAM, flags=0):
    assert family == AF_INET
    assert type in (SOCK_DGRAM, SOCK_STREAM)
    assert flags == 0
    if type == SOCK_STREAM:
        return _tcpsocket()
    else:
        return _udpsocket()

def getaddrinfo(host, port, family=0, socktype=SOCK_STREAM, proto=0, flags=0):
    return ( (AF_INET, socktype, 0, "", (gethostbyname(host), port)), )

has_ipv6 = 1

def getnameinfo(sock_addr, flags):
    raise NotImplementedError("getnameinfo not yet supported on jython.")

def getdefaulttimeout():
    return _defaulttimeout

def _calctimeoutvalue(value):
    if value is None:
        return None
    try:
        floatvalue = float(value)
    except:
        raise TypeError('Socket timeout value must be a number or None')
    if floatvalue < 0:
        raise ValueError("Socket timeout value cannot be negative")
    if floatvalue < 0.000001:
        return 0.0
    return floatvalue

def setdefaulttimeout(timeout):
    global _defaulttimeout
    try:
        _defaulttimeout = _calctimeoutvalue(timeout)
    finally:
        _nonblocking_api_mixin.timeout = _defaulttimeout

def htons(x): return x
def htonl(x): return x
def ntohs(x): return x
def ntohl(x): return x

class _nonblocking_api_mixin:

    timeout = _defaulttimeout
    mode = MODE_BLOCKING

    def gettimeout(self):
        return self.timeout

    def settimeout(self, timeout):
        self.timeout = _calctimeoutvalue(timeout)
        if self.timeout is None:
            self.mode = MODE_BLOCKING
        elif self.timeout < 0.000001:
            self.mode = MODE_NONBLOCKING
        else:
            self.mode = MODE_TIMEOUT
        self._config()

    def setblocking(self, flag):
        if flag:
            self.mode = MODE_BLOCKING
            self.timeout = None
        else:
            self.mode = MODE_NONBLOCKING
            self.timeout = 0.0
        self._config()

    def _config(self):
        assert self.mode in _permitted_modes
        if self.sock_impl: self.sock_impl.config(self.mode, self.timeout)

    def getchannel(self):
        if not self.sock_impl:
            return None
        return self.sock_impl.getchannel()
#        if hasattr(self.sock_impl, 'getchannel'):
#            return self.sock_impl.getchannel()
#        raise error('Operation not implemented on this JVM')            

    fileno = getchannel

    def _get_jsocket(self):
        return self.sock_impl.jsocket

def _unpack_address_tuple(address_tuple):
    error_message = "Address must be a tuple of (hostname, port)"
    if type(address_tuple) is not type( () ) \
            or type(address_tuple[0]) is not type("") \
            or type(address_tuple[1]) is not type(0):
        raise TypeError(error_message)
    return address_tuple[0], address_tuple[1]

class _tcpsocket(_nonblocking_api_mixin):

    sock_impl = None
    istream = None
    ostream = None
    local_addr = None
    server = 0
    file_count = 0
    #reuse_addr = 1
    reuse_addr = 0

    def bind(self, addr):
        assert not self.sock_impl
        assert not self.local_addr
        # Do the address format check
        host, port = _unpack_address_tuple(addr)
        self.local_addr = addr

    def listen(self, backlog=50):
        "This signifies a server socket"
        try:
            assert not self.sock_impl
            self.server = 1
            if self.local_addr:
                host, port = self.local_addr
            else:
                host, port = "", 0
            self.sock_impl = _server_socket_impl(host, port, backlog, self.reuse_addr)
            self._config()
        except java.lang.Exception, jlx:
            raise map_exception(jlx)

#
# The following has information on a java.lang.NullPointerException problem I'm having
#
# http://developer.java.sun.com/developer/bugParade/bugs/4801882.html

    def accept(self):
        "This signifies a server socket"
        try:
            if not self.sock_impl:
                self.listen()
            assert self.server
            new_sock = self.sock_impl.accept()
            if not new_sock:
                raise would_block_error()
            cliconn = _tcpsocket()
            cliconn._setup(new_sock)
            return cliconn, new_sock.getpeername()
        except java.lang.Exception, jlx:
            raise map_exception(jlx)

    def _get_host_port(self, addr):
        host, port = _unpack_address_tuple(addr)
        if host == "":
            host = java.net.InetAddress.getLocalHost()
        return host, port

    def _do_connect(self, addr):
        try:
            assert not self.sock_impl
            host, port = self._get_host_port(addr)
            self.sock_impl = _client_socket_impl()
            if self.local_addr: # Has the socket been bound to a local address?
                bind_host, bind_port = self.local_addr
                self.sock_impl.bind(bind_host, bind_port)
            self._config() # Configure timeouts, etc, now that the socket exists
            self.sock_impl.connect(host, port)
            self._setup(self.sock_impl)
        except java.lang.Exception, jlx:
            raise map_exception(jlx)

    def connect(self, addr):
        "This signifies a client socket"
        self._do_connect(addr)
        self._setup(self.sock_impl)

    def connect_ex(self, addr):
        "This signifies a client socket"
        self._do_connect(addr)
        if self.sock_impl.finish_connect():
            self._setup(self.sock_impl)
            return 0
        return ERRNO_EINPROGRESS

    def _setup(self, sock):
        self.sock_impl = sock
        self.sock_impl._setreuseaddress(self.reuse_addr)
        if self.mode != MODE_NONBLOCKING:
            self.istream = self.sock_impl.jsocket.getInputStream()
            self.ostream = self.sock_impl.jsocket.getOutputStream()

    def recv(self, n):
        try:
            if not self.sock_impl: raise error('Socket not open')
            if self.sock_impl.jchannel.isConnectionPending():
                self.sock_impl.jchannel.finishConnect()
            data = jarray.zeros(n, 'b')
            m = self.sock_impl.read(data)
            if m <= 0:
                if self.mode == MODE_NONBLOCKING:
                    raise would_block_error()
                return ""
            if m < n:
                data = data[:m]
            return data.tostring()
        except java.lang.Exception, jlx:
            raise map_exception(jlx)

    def recvfrom(self, n):
        return self.recv(n), None

    def send(self, s):
        if not self.sock_impl: raise error('Socket not open')
        if self.sock_impl.jchannel.isConnectionPending():
            self.sock_impl.jchannel.finishConnect()
        #n = len(s)
        numwritten = self.sock_impl.write(s)
        return numwritten

    sendall = send

    def getsockname(self):
        if not self.sock_impl:
            host, port = self.local_addr or ("", 0)
            host = java.net.InetAddress.getByName(host).getHostAddress()
        else:
            if self.server:
                host = self.sock_impl.jsocket.getInetAddress().getHostAddress()
            else:
                host = self.sock_impl.jsocket.getLocalAddress().getHostAddress()
            port = self.sock_impl.jsocket.getLocalPort()
        return (host, port)

    def getpeername(self):
        assert self.sock_impl
        assert not self.server
        host = self.sock_impl.jsocket.getInetAddress().getHostAddress()
        port = self.sock_impl.jsocket.getPort()
        return (host, port)
        
    def setsockopt(self, level, optname, value):
        if optname == SO_REUSEADDR:
            self.reuse_addr = value

    def getsockopt(self, level, optname):
        if optname == SO_REUSEADDR:
            return self.reuse_addr

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
            self.sock = socket.sock_impl
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
            self.closed     = file.closed

            self.socket.file_count += 1

        def close(self):
            if self.file.closed:
                # Already closed
                return

            self.socket.file_count -= 1
            self.file.close()
            self.closed = self.file.closed

            if self.socket.file_count == 0 and self.socket.sock_impl == 0:
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
        assert self.sock_impl
        if how in (0, 2):
            self.istream = None
        if how in (1, 2):
            self.ostream = None

    def close(self):
        if not self.sock_impl:
            return
        sock = self.sock_impl
        istream = self.istream
        ostream = self.ostream
        self.sock_impl = 0
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

class _udpsocket(_nonblocking_api_mixin):

    def __init__(self):
        self.sock_impl = None
        self.addr = None
        self.reuse_addr = 0

    def bind(self, addr):
        assert not self.sock_impl
        host, port = _unpack_address_tuple(addr)
        host_address = java.net.InetAddress.getByName(host)
        self.sock_impl = _datagram_socket_impl(port, host_address, reuse_addr = self.reuse_addr)
        self._config()

    def connect(self, addr):
        host, port = _unpack_address_tuple(addr)
        assert not self.addr
        if not self.sock_impl:
            self.sock_impl = _datagram_socket_impl()
            self._config()
            self.sock_impl.connect(host, port)
        self.addr = addr # convert host to InetAddress instance?

    def connect_ex(self, addr):
        host, port = _unpack_address_tuple(addr)
        assert not self.addr
        self.addr = addr
        if not self.sock_impl:
            self.sock_impl = _datagram_socket_impl()
            self._config()
            self.sock_impl.connect(host, port)
            if self.sock_impl.finish_connect():
                return 0
            return ERRNO_EINPROGRESS

    def sendto(self, data, p1, p2=None):
        if not p2:
            flags, addr = 0, p1
        else:
            flags, addr = 0, p2
        n = len(data)
        if not self.sock_impl:
            self.sock_impl = _datagram_socket_impl()
        host, port = addr
        bytes = java.lang.String(data).getBytes('iso-8859-1')
        a = java.net.InetAddress.getByName(host)
        packet = java.net.DatagramPacket(bytes, n, a, port)
        self.sock_impl.send(packet)
        return n

    def send(self, data):
        assert self.addr
        return self.sendto(data, self.addr)

    def recvfrom(self, n):
        try:
            assert self.sock_impl
            bytes = jarray.zeros(n, 'b')
            packet = java.net.DatagramPacket(bytes, n)
            self.sock_impl.receive(packet)
            host = None
            if packet.getAddress():
                host = packet.getAddress().getHostName()
            port = packet.getPort()
            m = packet.getLength()
            if m < n:
                bytes = bytes[:m]
            return bytes.tostring(), (host, port)
        except java.lang.Exception, jlx:
            raise map_exception(jlx)

    def recv(self, n):
        try:
            assert self.sock_impl
            bytes = jarray.zeros(n, 'b')
            packet = java.net.DatagramPacket(bytes, n)
            self.sock_impl.receive(packet)
            m = packet.getLength()
            if m < n:
                bytes = bytes[:m]
            return bytes.tostring()
        except java.lang.Exception, jlx:
            raise map_exception(jlx)

    def getsockname(self):
        assert self.sock_impl
        host = self.sock_impl.jsocket.getLocalAddress().getHostName()
        port = self.sock_impl.jsocket.getLocalPort()
        return (host, port)

    def getpeername(self):
        assert self.sock
        host = self.sock_impl.jsocket.getInetAddress().getHostName()
        port = self.sock_impl.jsocket.getPort()
        return (host, port)

    def __del__(self):
        self.close()

    def close(self):
        if not self.sock_impl:
            return
        sock = self.sock_impl
        self.sock_impl = None
        sock.close()

    def setsockopt(self, level, optname, value):
        if optname == SO_REUSEADDR:
            self.reuse_addr = value
#            self.sock._setreuseaddress(value)

    def getsockopt(self, level, optname):
        if optname == SO_REUSEADDR:
            return self.sock_impl._getreuseaddress()
        else:
            return None

SocketType = _tcpsocket
SocketTypes = [_tcpsocket, _udpsocket]

# Define the SSL support

class ssl:

    def __init__(self, plain_sock, keyfile=None, certfile=None):
        self.ssl_sock = self.make_ssl_socket(plain_sock)

    def make_ssl_socket(self, plain_socket, auto_close=0):
        java_net_socket = plain_socket._get_jsocket()
        assert isinstance(java_net_socket, java.net.Socket)
        host = java_net_socket.getInetAddress().getHostName()
        port = java_net_socket.getPort()
        factory = javax.net.ssl.SSLSocketFactory.getDefault();
        ssl_socket = factory.createSocket(java_net_socket, host, port, auto_close)
        ssl_socket.setEnabledCipherSuites(ssl_socket.getSupportedCipherSuites())
        ssl_socket.startHandshake()
        return ssl_socket

    def read(self, n=4096):
        # Probably needs some work on efficency
        in_buf = java.io.BufferedInputStream(self.ssl_sock.getInputStream())
        data = jarray.zeros(n, 'b')
        m = in_buf.read(data, 0, n)
        if m <= 0:
            return ""
        if m < n:
            data = data[:m]
        return data.tostring()

    def write(self, s):
        # Probably needs some work on efficency
        out = java.io.BufferedOutputStream(self.ssl_sock.getOutputStream())
        out.write(s)
        out.flush()

    def _get_server_cert(self):
        return self.ssl_sock.getSession().getPeerCertificates()[0]

    def server(self):
        cert = self._get_server_cert()
        return cert.getSubjectDN().toString()

    def issuer(self):
        cert = self._get_server_cert()
        return cert.getIssuerDN().toString()

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
