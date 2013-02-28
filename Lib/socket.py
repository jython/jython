"""
This is an updated socket module for use on JVMs >= 1.5; it is derived from the old jython socket module.
It is documented, along with known issues and workarounds, on the jython wiki.
http://wiki.python.org/jython/NewSocketModule
"""

_defaulttimeout = None

import errno
import jarray
import string
import struct
import sys
import threading
import time
import types

# Java.io classes
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
# Java.io exceptions
import java.io.InterruptedIOException
import java.io.IOException

# Java.lang classes
import java.lang.String
# Java.lang exceptions
import java.lang.Exception

# Java.net classes
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
# Java.net exceptions
import java.net.BindException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

# Java.nio classes
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
# Java.nio exceptions
import java.nio.channels.AlreadyConnectedException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.CancelledKeyException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import java.nio.channels.ConnectionPendingException
import java.nio.channels.IllegalBlockingModeException
import java.nio.channels.IllegalSelectorException
import java.nio.channels.NoConnectionPendingException
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.NotYetBoundException
import java.nio.channels.NotYetConnectedException
import java.nio.channels.UnresolvedAddressException
import java.nio.channels.UnsupportedAddressTypeException

# Javax.net.ssl classes
import javax.net.ssl.SSLSocketFactory
# Javax.net.ssl exceptions
javax.net.ssl.SSLException
javax.net.ssl.SSLHandshakeException
javax.net.ssl.SSLKeyException
javax.net.ssl.SSLPeerUnverifiedException
javax.net.ssl.SSLProtocolException

import org.python.core.io.DatagramSocketIO
import org.python.core.io.ServerSocketIO
import org.python.core.io.SocketIO
from org.python.core.Py import newString as asPyString

class error(IOError): pass
class herror(error): pass
class gaierror(error): pass
class timeout(error): pass
class sslerror(error): pass

def _add_exception_attrs(exc):
    setattr(exc, 'errno', exc[0])
    setattr(exc, 'strerror', exc[1])
    return exc

def _unmapped_exception(exc):
    return _add_exception_attrs(error(-1, 'Unmapped exception: %s' % exc))

def java_net_socketexception_handler(exc):
    if exc.message.startswith("Address family not supported by protocol family"):
        return _add_exception_attrs(error(errno.EAFNOSUPPORT, 
                'Address family not supported by protocol family: See http://wiki.python.org/jython/NewSocketModule#IPV6_address_support'))
    return _unmapped_exception(exc)

def would_block_error(exc=None):
    return _add_exception_attrs(error(errno.EWOULDBLOCK, 'The socket operation could not complete without blocking'))

ALL = None

_ssl_message = ": Differences between the SSL socket behaviour of cpython vs. jython are explained on the wiki:  http://wiki.python.org/jython/NewSocketModule#SSL_Support"

_exception_map = {

# (<javaexception>, <circumstance>) : callable that raises the python equivalent exception, or None to stub out as unmapped

(java.io.IOException, ALL)            : lambda x: error(errno.ECONNRESET, 'Software caused connection abort'),
(java.io.InterruptedIOException, ALL) : lambda x: timeout(None, 'timed out'),

(java.net.BindException, ALL)            : lambda x: error(errno.EADDRINUSE, 'Address already in use'),
(java.net.ConnectException, ALL)         : lambda x: error(errno.ECONNREFUSED, 'Connection refused'),
(java.net.NoRouteToHostException, ALL)   : lambda x: error(errno.EHOSTUNREACH, 'No route to host'),
(java.net.PortUnreachableException, ALL) : None,
(java.net.ProtocolException, ALL)        : None,
(java.net.SocketException, ALL)          : java_net_socketexception_handler,
(java.net.SocketTimeoutException, ALL)   : lambda x: timeout(None, 'timed out'),
(java.net.UnknownHostException, ALL)     : lambda x: gaierror(errno.EGETADDRINFOFAILED, 'getaddrinfo failed'),

(java.nio.channels.AlreadyConnectedException, ALL)       : lambda x: error(errno.EISCONN, 'Socket is already connected'),
(java.nio.channels.AsynchronousCloseException, ALL)      : None,
(java.nio.channels.CancelledKeyException, ALL)           : None,
(java.nio.channels.ClosedByInterruptException, ALL)      : None,
(java.nio.channels.ClosedChannelException, ALL)          : lambda x: error(errno.EPIPE, 'Socket closed'),
(java.nio.channels.ClosedSelectorException, ALL)         : None,
(java.nio.channels.ConnectionPendingException, ALL)      : None,
(java.nio.channels.IllegalBlockingModeException, ALL)    : None,
(java.nio.channels.IllegalSelectorException, ALL)        : None,
(java.nio.channels.NoConnectionPendingException, ALL)    : None,
(java.nio.channels.NonReadableChannelException, ALL)     : None,
(java.nio.channels.NonWritableChannelException, ALL)     : None,
(java.nio.channels.NotYetBoundException, ALL)            : None,
(java.nio.channels.NotYetConnectedException, ALL)        : None,
(java.nio.channels.UnresolvedAddressException, ALL)      : lambda x: gaierror(errno.EGETADDRINFOFAILED, 'getaddrinfo failed'),
(java.nio.channels.UnsupportedAddressTypeException, ALL) : None,

# These error codes are currently wrong: getting them correct is going to require
# some investigation. Cpython 2.6 introduced extensive SSL support.

(javax.net.ssl.SSLException, ALL)                        : lambda x: sslerror(-1, 'SSL exception'+_ssl_message),
(javax.net.ssl.SSLHandshakeException, ALL)               : lambda x: sslerror(-1, 'SSL handshake exception'+_ssl_message),
(javax.net.ssl.SSLKeyException, ALL)                     : lambda x: sslerror(-1, 'SSL key exception'+_ssl_message),
(javax.net.ssl.SSLPeerUnverifiedException, ALL)          : lambda x: sslerror(-1, 'SSL peer unverified exception'+_ssl_message),
(javax.net.ssl.SSLProtocolException, ALL)                : lambda x: sslerror(-1, 'SSL protocol exception'+_ssl_message),

}

def _map_exception(java_exception, circumstance=ALL):
    mapped_exception = _exception_map.get((java_exception.__class__, circumstance))
    if mapped_exception:
        py_exception = mapped_exception(java_exception)
    else:
        py_exception = error(-1, 'Unmapped exception: %s' % java_exception)
    setattr(py_exception, 'java_exception', java_exception)
    return _add_exception_attrs(py_exception)

from functools import wraps

# Used to map java exceptions to the equivalent python exception
# And to set the _last_error attribute on socket objects, to support SO_ERROR
def raises_java_exception(method_or_function):
    @wraps(method_or_function)
    def handle_exception(*args, **kwargs):
        is_socket = (len(args) > 0 and isinstance(args[0], _nonblocking_api_mixin))
        try:
            try:
                return method_or_function(*args, **kwargs)
            except java.lang.Exception, jlx:
                raise _map_exception(jlx)
        except error, e:
            if is_socket:
                setattr(args[0], '_last_error', e[0])
            raise
        else:
            if is_socket:
                setattr(args[0], '_last_error', 0)
    return handle_exception

_feature_support_map = {
    'ipv6': True,
    'idna': False,
    'tipc': False,
}

def supports(feature, *args):
    if len(args) == 1:
        _feature_support_map[feature] = args[0]
    return _feature_support_map.get(feature, False)

MODE_BLOCKING    = 'block'
MODE_NONBLOCKING = 'nonblock'
MODE_TIMEOUT     = 'timeout'

_permitted_modes = (MODE_BLOCKING, MODE_NONBLOCKING, MODE_TIMEOUT)

SHUT_RD   = 0
SHUT_WR   = 1
SHUT_RDWR = 2

AF_UNSPEC = 0
AF_INET   = 2
AF_INET6  = 23

AI_PASSIVE     = 1
AI_CANONNAME   = 2
AI_NUMERICHOST = 4
AI_V4MAPPED    = 8
AI_ALL         = 16
AI_ADDRCONFIG  = 32
AI_NUMERICSERV = 1024

EAI_NONAME     = -2
EAI_SERVICE    = -8
EAI_ADDRFAMILY = -9

NI_NUMERICHOST              = 1
NI_NUMERICSERV              = 2
NI_NOFQDN                   = 4
NI_NAMEREQD                 = 8
NI_DGRAM                    = 16
NI_MAXSERV                  = 32
NI_IDN                      = 64
NI_IDN_ALLOW_UNASSIGNED     = 128
NI_IDN_USE_STD3_ASCII_RULES = 256
NI_MAXHOST                  = 1025

# For some reason, probably historical, SOCK_DGRAM and SOCK_STREAM are opposite values of what they are on cpython.
# I.E. The following is the way they are on cpython
# SOCK_STREAM    = 1
# SOCK_DGRAM     = 2
# At some point, we should probably switch them around, which *should* not affect anybody

SOCK_DGRAM     = 1
SOCK_STREAM    = 2
SOCK_RAW       = 3 # not supported
SOCK_RDM       = 4 # not supported
SOCK_SEQPACKET = 5 # not supported

SOL_SOCKET = 0xFFFF

IPPROTO_AH       =  51 # not supported
IPPROTO_DSTOPTS  =  60 # not supported
IPPROTO_ESP      =  50 # not supported
IPPROTO_FRAGMENT =  44 # not supported
IPPROTO_GGP      =   3 # not supported
IPPROTO_HOPOPTS  =   0 # not supported
IPPROTO_ICMP     =   1 # not supported
IPPROTO_ICMPV6   =  58 # not supported
IPPROTO_IDP      =  22 # not supported
IPPROTO_IGMP     =   2 # not supported
IPPROTO_IP       =   0
IPPROTO_IPV4     =   4 # not supported
IPPROTO_IPV6     =  41 # not supported
IPPROTO_MAX      = 256 # not supported
IPPROTO_ND       =  77 # not supported
IPPROTO_NONE     =  59 # not supported
IPPROTO_PUP      =  12 # not supported
IPPROTO_RAW      = 255 # not supported
IPPROTO_ROUTING  =  43 # not supported
IPPROTO_TCP      =   6
IPPROTO_UDP      =  17

SO_ACCEPTCONN  = 1
SO_BROADCAST   = 2
SO_ERROR       = 4
SO_KEEPALIVE   = 8
SO_LINGER      = 16
SO_OOBINLINE   = 32
SO_RCVBUF      = 64
SO_REUSEADDR   = 128
SO_SNDBUF      = 256
SO_TIMEOUT     = 512
SO_TYPE        = 1024

TCP_NODELAY    = 2048

INADDR_ANY = "0.0.0.0"
INADDR_BROADCAST = "255.255.255.255"

IN6ADDR_ANY_INIT = "::"

# Options with negative constants are not supported
# They are being added here so that code that refers to them
# will not break with an AttributeError

SO_DEBUG            = -1
SO_DONTROUTE        = -1
SO_EXCLUSIVEADDRUSE = -8
SO_RCVLOWAT         = -16
SO_RCVTIMEO         = -32
SO_REUSEPORT        = -64
SO_SNDLOWAT         = -128
SO_SNDTIMEO         = -256
SO_USELOOPBACK      = -512

__all__ = [
    # Families
    'AF_UNSPEC', 'AF_INET', 'AF_INET6', 
    # getaddrinfo and getnameinfo flags
    'AI_PASSIVE', 'AI_CANONNAME', 'AI_NUMERICHOST', 'AI_V4MAPPED',
    'AI_ALL', 'AI_ADDRCONFIG', 'AI_NUMERICSERV', 'EAI_NONAME', 
    'EAI_SERVICE', 'EAI_ADDRFAMILY',
    'NI_NUMERICHOST', 'NI_NUMERICSERV', 'NI_NOFQDN', 'NI_NAMEREQD',
    'NI_DGRAM', 'NI_MAXSERV', 'NI_IDN', 'NI_IDN_ALLOW_UNASSIGNED',
    'NI_IDN_USE_STD3_ASCII_RULES', 'NI_MAXHOST',
    # socket types
    'SOCK_DGRAM', 'SOCK_STREAM', 'SOCK_RAW', 'SOCK_RDM', 'SOCK_SEQPACKET',
    # levels
    'SOL_SOCKET',
    # protocols
    'IPPROTO_AH', 'IPPROTO_DSTOPTS', 'IPPROTO_ESP', 'IPPROTO_FRAGMENT',
    'IPPROTO_GGP', 'IPPROTO_HOPOPTS', 'IPPROTO_ICMP', 'IPPROTO_ICMPV6',
    'IPPROTO_IDP', 'IPPROTO_IGMP', 'IPPROTO_IP', 'IPPROTO_IPV4',
    'IPPROTO_IPV6', 'IPPROTO_MAX', 'IPPROTO_ND', 'IPPROTO_NONE',
    'IPPROTO_PUP', 'IPPROTO_RAW', 'IPPROTO_ROUTING', 'IPPROTO_TCP', 
    'IPPROTO_UDP',
    # Special hostnames
    'INADDR_ANY', 'INADDR_BROADCAST', 'IN6ADDR_ANY_INIT',
    # support socket options
    'SO_BROADCAST', 'SO_KEEPALIVE', 'SO_LINGER', 'SO_OOBINLINE',
    'SO_RCVBUF', 'SO_REUSEADDR', 'SO_SNDBUF', 'SO_TIMEOUT', 'TCP_NODELAY',
    # unsupported socket options
    'SO_ACCEPTCONN', 'SO_DEBUG', 'SO_DONTROUTE', 'SO_ERROR',
    'SO_EXCLUSIVEADDRUSE', 'SO_RCVLOWAT', 'SO_RCVTIMEO', 'SO_REUSEPORT',
    'SO_SNDLOWAT', 'SO_SNDTIMEO', 'SO_TYPE', 'SO_USELOOPBACK',
    # functions
    'getfqdn', 'gethostname', 'gethostbyname', 'gethostbyaddr',
    'getservbyname', 'getservbyport', 'getprotobyname', 'getaddrinfo',
    'getnameinfo', 'getdefaulttimeout', 'setdefaulttimeout', 'htons',
    'htonl', 'ntohs', 'ntohl', 'inet_pton', 'inet_ntop', 'inet_aton',
    'inet_ntoa', 'create_connection', 'socket', 'ssl',
    # exceptions
    'error', 'herror', 'gaierror', 'timeout', 'sslerror',
    # classes
    'SocketType', 
    # Misc flags     
    'has_ipv6', 'SHUT_RD', 'SHUT_WR', 'SHUT_RDWR',
]

def _constant_to_name(const_value, expected_name_starts):
    sock_module = sys.modules['socket']
    try:
        for name in dir(sock_module):
            if getattr(sock_module, name) is const_value:
                for name_start in expected_name_starts:
                    if name.startswith(name_start):
                        return name
        return "Unknown"
    finally:
        sock_module = None

import _google_ipaddr_r234

def _is_ip_address(addr, version=None):
    try:
        _google_ipaddr_r234.IPAddress(addr, version)
        return True
    except ValueError:
        return False

def is_ipv4_address(addr):
    return _is_ip_address(addr, 4)

def is_ipv6_address(addr):
    return _is_ip_address(addr, 6)

def is_ip_address(addr):
    return _is_ip_address(addr)

class _nio_impl:

    timeout = None
    mode = MODE_BLOCKING

    def config(self, mode, timeout):
        self.mode = mode
        if self.mode == MODE_BLOCKING:
            self.jchannel.configureBlocking(1)
        if self.mode == MODE_NONBLOCKING:
            self.jchannel.configureBlocking(0)
        if self.mode == MODE_TIMEOUT:
            self.jchannel.configureBlocking(1)
            self._timeout_millis = int(timeout*1000)
            self.jsocket.setSoTimeout(self._timeout_millis)

    def getsockopt(self, level, option):
        if (level, option) in self.options:
            result = getattr(self.jsocket, "get%s" % self.options[ (level, option) ])()
            if option == SO_LINGER:
                if result == -1:
                    enabled, linger_time = 0, 0
                else:
                    enabled, linger_time = 1, result
                return struct.pack('ii', enabled, linger_time)
            return result
        else:
            raise error(errno.ENOPROTOOPT, "Socket option '%s' (level '%s') not supported on socket(%s)" % \
                (_constant_to_name(option, ['SO_', 'TCP_']), _constant_to_name(level, ['SOL_', 'IPPROTO_']), str(self.jsocket)))

    def setsockopt(self, level, option, value):
        if (level, option) in self.options:
            if option == SO_LINGER:
                values = struct.unpack('ii', value)
                self.jsocket.setSoLinger(*values)
            else:
                getattr(self.jsocket, "set%s" % self.options[ (level, option) ])(value)
        else:
            raise error(errno.ENOPROTOOPT, "Socket option '%s' (level '%s') not supported on socket(%s)" % \
                (_constant_to_name(option, ['SO_', 'TCP_']), _constant_to_name(level,  ['SOL_', 'IPPROTO_']), str(self.jsocket)))

    def close(self):
        self.jsocket.close()

    def getchannel(self):
        return self.jchannel

    def fileno(self):
        return self.socketio

class _client_socket_impl(_nio_impl):

    options = {
        (SOL_SOCKET,  SO_KEEPALIVE):   'KeepAlive',
        (SOL_SOCKET,  SO_LINGER):      'SoLinger',
        (SOL_SOCKET,  SO_OOBINLINE):   'OOBInline',
        (SOL_SOCKET,  SO_RCVBUF):      'ReceiveBufferSize',
        (SOL_SOCKET,  SO_REUSEADDR):   'ReuseAddress',
        (SOL_SOCKET,  SO_SNDBUF):      'SendBufferSize',
        (SOL_SOCKET,  SO_TIMEOUT):     'SoTimeout',
        (IPPROTO_TCP, TCP_NODELAY):    'TcpNoDelay',
    }

    def __init__(self, socket=None, pending_options=None):
        if socket:
            self.jchannel = socket.getChannel()
        else:
            self.jchannel = java.nio.channels.SocketChannel.open()
        self.jsocket = self.jchannel.socket()
        self.socketio = org.python.core.io.SocketIO(self.jchannel, 'rw')
        if pending_options:
            for level, optname in pending_options.keys():
                self.setsockopt(level, optname, pending_options[ (level, optname) ])

    def bind(self, jsockaddr, reuse_addr):
        self.jsocket.setReuseAddress(reuse_addr)
        self.jsocket.bind(jsockaddr)

    def connect(self, jsockaddr):
        if self.mode == MODE_TIMEOUT:
            self.jsocket.connect (jsockaddr, self._timeout_millis)
        else:
            self.jchannel.connect(jsockaddr)

    def finish_connect(self):
        return self.jchannel.finishConnect()

    def _do_read_net(self, buf):
        # Need two separate implementations because the java.nio APIs do not support timeouts
        return self.jsocket.getInputStream().read(buf)

    def _do_read_nio(self, buf):
        bytebuf = java.nio.ByteBuffer.wrap(buf)
        count = self.jchannel.read(bytebuf)
        return count

    def _do_write_net(self, buf):
        self.jsocket.getOutputStream().write(buf)
        return len(buf)

    def _do_write_nio(self, buf):
        bytebuf = java.nio.ByteBuffer.wrap(buf)
        count = self.jchannel.write(bytebuf)
        return count

    def read(self, buf):
        if self.mode == MODE_TIMEOUT:
            return self._do_read_net(buf)
        else:
            return self._do_read_nio(buf)

    def write(self, buf):
        if self.mode == MODE_TIMEOUT:
            return self._do_write_net(buf)
        else:
            return self._do_write_nio(buf)

    def shutdown(self, how):
        if how in (SHUT_RD, SHUT_RDWR):
            self.jsocket.shutdownInput()
        if how in (SHUT_WR, SHUT_RDWR):
            self.jsocket.shutdownOutput()

    def getsockname(self):
        return (self.jsocket.getLocalAddress().getHostAddress(), self.jsocket.getLocalPort())

    def getpeername(self):
        return (self.jsocket.getInetAddress().getHostAddress(), self.jsocket.getPort() )

class _server_socket_impl(_nio_impl):

    options = {
        (SOL_SOCKET, SO_RCVBUF):      'ReceiveBufferSize',
        (SOL_SOCKET, SO_REUSEADDR):   'ReuseAddress',
        (SOL_SOCKET, SO_TIMEOUT):     'SoTimeout',
    }

    def __init__(self, jsockaddr, backlog, reuse_addr):
        self.pending_client_options = {}
        self.jchannel = java.nio.channels.ServerSocketChannel.open()
        self.jsocket = self.jchannel.socket()
        self.jsocket.setReuseAddress(reuse_addr)
        self.jsocket.bind(jsockaddr, backlog)
        self.socketio = org.python.core.io.ServerSocketIO(self.jchannel, 'rw')

    def accept(self):
        if self.mode in (MODE_BLOCKING, MODE_NONBLOCKING):
            new_cli_chan = self.jchannel.accept()
            if new_cli_chan is not None:
                return _client_socket_impl(new_cli_chan.socket(), self.pending_client_options)
            else:
                return None
        else:
            # In timeout mode now
            new_cli_sock = self.jsocket.accept()
            return _client_socket_impl(new_cli_sock, self.pending_client_options)

    def shutdown(self, how):
        # This is no-op on java, for server sockets.
        # What the user wants to achieve is achieved by calling close() on
        # java/jython. But we can't call that here because that would then
        # later cause the user explicit close() call to fail
        pass

    def getsockopt(self, level, option):
        if self.options.has_key( (level, option) ):
            return _nio_impl.getsockopt(self, level, option)
        elif _client_socket_impl.options.has_key( (level, option) ):
            return self.pending_client_options.get( (level, option), None)
        else:
            raise error(errno.ENOPROTOOPT, "Socket option '%s' (level '%s') not supported on socket(%s)" % \
                (_constant_to_name(option, ['SO_', 'TCP_']), _constant_to_name(level,  ['SOL_', 'IPPROTO_']), str(self.jsocket)))

    def setsockopt(self, level, option, value):
        if self.options.has_key( (level, option) ):
            _nio_impl.setsockopt(self, level, option, value)
        elif _client_socket_impl.options.has_key( (level, option) ):
            self.pending_client_options[ (level, option) ] = value
        else:
            raise error(errno.ENOPROTOOPT, "Socket option '%s' (level '%s') not supported on socket(%s)" % \
                (_constant_to_name(option, ['SO_', 'TCP_']), _constant_to_name(level,  ['SOL_', 'IPPROTO_']), str(self.jsocket)))

    def getsockname(self):
        return (self.jsocket.getInetAddress().getHostAddress(), self.jsocket.getLocalPort())

    def getpeername(self):
        # Not a meaningful operation for server sockets.
        raise error(errno.ENOTCONN, "Socket is not connected")

class _datagram_socket_impl(_nio_impl):

    options = {
        (SOL_SOCKET, SO_BROADCAST):   'Broadcast',
        (SOL_SOCKET, SO_RCVBUF):      'ReceiveBufferSize',
        (SOL_SOCKET, SO_REUSEADDR):   'ReuseAddress',
        (SOL_SOCKET, SO_SNDBUF):      'SendBufferSize',
        (SOL_SOCKET, SO_TIMEOUT):     'SoTimeout',
    }

    def __init__(self, jsockaddr=None, reuse_addr=0):
        self.jchannel = java.nio.channels.DatagramChannel.open()
        self.jsocket = self.jchannel.socket()
        if jsockaddr is not None:
            self.jsocket.setReuseAddress(reuse_addr)
            self.jsocket.bind(jsockaddr)
        self.socketio = org.python.core.io.DatagramSocketIO(self.jchannel, 'rw')

    def connect(self, jsockaddr):
        self.jchannel.connect(jsockaddr)

    def disconnect(self):
        """
            Disconnect the datagram socket.
            cpython appears not to have this operation
        """
        self.jchannel.disconnect()

    def shutdown(self, how):
        # This is no-op on java, for datagram sockets.
        # What the user wants to achieve is achieved by calling close() on
        # java/jython. But we can't call that here because that would then
        # later cause the user explicit close() call to fail
        pass

    def _do_send_net(self, byte_array, socket_address, flags):
        # Need two separate implementations because the java.nio APIs do not support timeouts
        num_bytes = len(byte_array)
        if self.jsocket.isConnected() and socket_address is None:
            packet = java.net.DatagramPacket(byte_array, num_bytes)
        else:
            packet = java.net.DatagramPacket(byte_array, num_bytes, socket_address)
        self.jsocket.send(packet)
        return num_bytes

    def _do_send_nio(self, byte_array, socket_address, flags):
        byte_buf = java.nio.ByteBuffer.wrap(byte_array)
        if self.jchannel.isConnected() and socket_address is None:
            bytes_sent = self.jchannel.write(byte_buf)
        else:
            bytes_sent = self.jchannel.send(byte_buf, socket_address)
        return bytes_sent

    def sendto(self, byte_array, jsockaddr, flags):
        if self.mode == MODE_TIMEOUT:
            return self._do_send_net(byte_array, jsockaddr, flags)
        else:
            return self._do_send_nio(byte_array, jsockaddr, flags)

    def send(self, byte_array, flags):
        if self.mode == MODE_TIMEOUT:
            return self._do_send_net(byte_array, None, flags)
        else:
            return self._do_send_nio(byte_array, None, flags)

    def _do_receive_net(self, return_source_address, num_bytes, flags):
        byte_array = jarray.zeros(num_bytes, 'b')
        packet = java.net.DatagramPacket(byte_array, num_bytes)
        self.jsocket.receive(packet)
        bytes_rcvd = packet.getLength()
        if bytes_rcvd < num_bytes:
            byte_array = byte_array[:bytes_rcvd]
        return_data = byte_array.tostring()
        if return_source_address:
            host = None
            if packet.getAddress():
                host = packet.getAddress().getHostAddress()
            port = packet.getPort()
            return return_data, (host, port)
        else:
            return return_data

    def _do_receive_nio(self, return_source_address, num_bytes, flags):
        byte_array = jarray.zeros(num_bytes, 'b')
        byte_buf = java.nio.ByteBuffer.wrap(byte_array)
        source_address = self.jchannel.receive(byte_buf)
        if source_address is None and not self.jchannel.isBlocking():
            raise would_block_error()
        byte_buf.flip() ; bytes_read = byte_buf.remaining()
        if bytes_read < num_bytes:
            byte_array = byte_array[:bytes_read]
        return_data = byte_array.tostring()
        if return_source_address:
            return return_data, (source_address.getAddress().getHostAddress(), source_address.getPort())
        else:
            return return_data

    def recvfrom(self, num_bytes, flags):
        if self.mode == MODE_TIMEOUT:
            return self._do_receive_net(1, num_bytes, flags)
        else:
            return self._do_receive_nio(1, num_bytes, flags)

    def recv(self, num_bytes, flags):
        if self.mode == MODE_TIMEOUT:
            return self._do_receive_net(0, num_bytes, flags)
        else:
            return self._do_receive_nio(0, num_bytes, flags)

    def getsockname(self):
        return (self.jsocket.getLocalAddress().getHostAddress(), self.jsocket.getLocalPort())

    def getpeername(self):
        peer_address = self.jsocket.getInetAddress()
        if peer_address is None:
            raise error(errno.ENOTCONN, "Socket is not connected")
        return (peer_address.getHostAddress(), self.jsocket.getPort() )

has_ipv6 = True # IPV6 FTW!

# Name and address functions

def _gethostbyaddr(name):
    # This is as close as I can get; at least the types are correct...
    addresses = java.net.InetAddress.getAllByName(gethostbyname(name))
    names = []
    addrs = []
    for addr in addresses:
        names.append(asPyString(addr.getHostName()))
        addrs.append(asPyString(addr.getHostAddress()))
    return (names, addrs)

@raises_java_exception
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

@raises_java_exception
def gethostname():
    return asPyString(java.net.InetAddress.getLocalHost().getHostName())

@raises_java_exception
def gethostbyname(name):
    return asPyString(java.net.InetAddress.getByName(name).getHostAddress())

#
# Skeleton implementation of gethostbyname_ex
# Needed because urllib2 refers to it
#

@raises_java_exception
def gethostbyname_ex(name):
    return (name, [], gethostbyname(name))

@raises_java_exception
def gethostbyaddr(name):
    names, addrs = _gethostbyaddr(name)
    return (names[0], names, addrs)

def getservbyname(service_name, protocol_name=None):
    try:
        from jnr.netdb import Service
    except ImportError:
        return None
    service = Service.getServiceByName(service_name, protocol_name)
    if service is None:
        raise error('service/proto not found')
    return service.getPort()

def getservbyport(port, protocol_name=None):
    try:
        from jnr.netdb import Service
    except ImportError:
        return None
    service = Service.getServiceByPort(port, protocol_name)
    if service is None:
        raise error('port/proto not found')
    return service.getName()

def getprotobyname(protocol_name=None):
    try:
        from jnr.netdb import Protocol
    except ImportError:
        return None
    proto = Protocol.getProtocolByName(protocol_name)
    if proto is None:
        raise error('protocol not found')
    return proto.getProto()

def _realsocket(family = AF_INET, sock_type = SOCK_STREAM, protocol=0):
    assert family in (AF_INET, AF_INET6), "Only AF_INET and AF_INET6 sockets are currently supported on jython"
    assert sock_type in (SOCK_DGRAM, SOCK_STREAM), "Only SOCK_STREAM and SOCK_DGRAM sockets are currently supported on jython"
    if sock_type == SOCK_STREAM:
        if protocol != 0:
            assert protocol == IPPROTO_TCP, "Only IPPROTO_TCP supported on SOCK_STREAM sockets"
        else:
            protocol = IPPROTO_TCP
        result = _tcpsocket()
    else:
        if protocol != 0:
            assert protocol == IPPROTO_UDP, "Only IPPROTO_UDP supported on SOCK_DGRAM sockets"
        else:
            protocol = IPPROTO_UDP
        result = _udpsocket()
    setattr(result, "family", family)
    setattr(result, "type",   sock_type)
    setattr(result, "proto",  protocol)
    return result

#
# Attempt to provide IDNA (RFC 3490) support.
#
# Try java.net.IDN, built into java 6
#

idna_libraries = [
    ('java.net.IDN', 'toASCII', 'toUnicode', 
        'ALLOW_UNASSIGNED', 'USE_STD3_ASCII_RULES', 
        java.lang.IllegalArgumentException)
]
  
for idna_lib, efn, dfn, au, usar, exc in idna_libraries:
    try:
        m = __import__(idna_lib, globals(), locals(), [efn, dfn, au, usar])
        encode_fn = getattr(m, efn)
        def _encode_idna(name):
            try:
                return encode_fn(name)
            except exc:
                raise UnicodeEncodeError(name)
        decode_fn = getattr(m, dfn)
        def _decode_idna(name, flags=0):
            try:
                jflags = 0
                if flags & NI_IDN_ALLOW_UNASSIGNED:
                    jflags |= au
                if flags & NI_IDN_USE_STD3_ASCII_RULES:
                    jflags |= usar
                return decode_fn(name, jflags)
            except Exception, x:
                raise UnicodeDecodeError(name)
        supports('idna', True)
        break
    except (AttributeError, ImportError), e:
        pass
else:
    _encode_idna = lambda x: x.encode("ascii")
    _decode_idna = lambda x, y=0: x.decode("ascii")

#
# Define data structures to support IPV4 and IPV6.
#

class _ip_address_t: pass

class _ipv4_address_t(_ip_address_t):

    def __init__(self, sockaddr, port, jaddress):
        self.sockaddr = sockaddr
        self.port     = port
        self.jaddress = jaddress

    def __getitem__(self, index):
        if   0 == index:
            return self.sockaddr
        elif 1 == index:
            return self.port
        else:
            raise IndexError()

    def __len__(self):
        return 2

    def __str__(self):
        return "('%s', %d)" % (self.sockaddr, self.port)

    __repr__ = __str__

class _ipv6_address_t(_ip_address_t):

    def __init__(self, sockaddr, port, jaddress):
        self.sockaddr = sockaddr
        self.port     = port
        self.jaddress = jaddress

    def __getitem__(self, index):
        if   0 == index:
            return self.sockaddr
        elif 1 == index:
            return self.port
        elif 2 == index:
            return 0
        elif 3 == index:
            return self.jaddress.scopeId
        else:
            raise IndexError()

    def __len__(self):
        return 4

    def __str__(self):
        return "('%s', %d, 0, %d)" % (self.sockaddr, self.port, self.jaddress.scopeId)

    __repr__ = __str__

def _get_jsockaddr(address_object, family, sock_type, proto, flags):
    # Is this an object that was returned from getaddrinfo? If so, it already contains an InetAddress
    if isinstance(address_object, _ip_address_t):
        return java.net.InetSocketAddress(address_object.jaddress, address_object[1]) 
    # The user passed an address tuple, not an object returned from getaddrinfo
    # So we must call getaddrinfo, after some translations and checking
    if address_object is None:
        address_object = ("", 0)
    error_message = "Address must be a 2-tuple (ipv4: (host, port)) or a 4-tuple (ipv6: (host, port, flow, scope))"
    if not isinstance(address_object, tuple) or \
            ((family == AF_INET and len(address_object) != 2) or (family == AF_INET6 and len(address_object) not in [2,4] )) or \
            not isinstance(address_object[0], (basestring, types.NoneType)) or \
            not isinstance(address_object[1], (int, long)):
        raise TypeError(error_message)
    if len(address_object) == 4 and not isinstance(address_object[3], (int, long)):
        raise TypeError(error_message)
    hostname = address_object[0]
    if hostname is not None:
        hostname = hostname.strip()
    port = address_object[1]
    if family == AF_INET and sock_type == SOCK_DGRAM and hostname == "<broadcast>":
        hostname = INADDR_BROADCAST
    if hostname in ["", None]:
        if flags & AI_PASSIVE:
            hostname = {AF_INET: INADDR_ANY, AF_INET6: IN6ADDR_ANY_INIT}[family]
        else:
            hostname = "localhost"
    if isinstance(hostname, unicode):
        hostname = _encode_idna(hostname)
    addresses = getaddrinfo(hostname, port, family, sock_type, proto, flags)
    if len(addresses) == 0:
        raise gaierror(errno.EGETADDRINFOFAILED, 'getaddrinfo failed')
    return java.net.InetSocketAddress(addresses[0][4].jaddress, port)

# Workaround for this (predominantly windows) issue
# http://wiki.python.org/jython/NewSocketModule#IPV6_address_support

_ipv4_addresses_only = False

def _use_ipv4_addresses_only(value):
    global _ipv4_addresses_only
    _ipv4_addresses_only = value

def _getaddrinfo_get_host(host, family, flags):
    if not isinstance(host, basestring) and host is not None:
        raise TypeError("getaddrinfo() argument 1 must be string or None")
    if flags & AI_NUMERICHOST:
        if not is_ip_address(host):
            raise gaierror(EAI_NONAME, "Name or service not known")
        if family == AF_INET and not is_ipv4_address(host):
            raise gaierror(EAI_ADDRFAMILY, "Address family for hostname not supported")
        if family == AF_INET6 and not is_ipv6_address(host):
            raise gaierror(EAI_ADDRFAMILY, "Address family for hostname not supported")
    if isinstance(host, unicode):
        host = _encode_idna(host)
    return host

def _getaddrinfo_get_port(port, flags):
    if isinstance(port, basestring):
        try:
            int_port = int(port)
        except ValueError:
            if flags & AI_NUMERICSERV:
                raise gaierror(EAI_NONAME, "Name or service not known")
            # Lookup the service by name
            try:
                int_port = getservbyname(port)
            except error:
                raise gaierror(EAI_SERVICE, "Servname not supported for ai_socktype")
    elif port is None:
        int_port = 0
    elif not isinstance(port, (int, long)):
        raise error("Int or String expected")
    else:
        int_port = int(port)
    return int_port % 65536

@raises_java_exception
def getaddrinfo(host, port, family=AF_UNSPEC, socktype=0, proto=0, flags=0):
    if _ipv4_addresses_only:
        family = AF_INET
    if not family in [AF_INET, AF_INET6, AF_UNSPEC]:
        raise gaierror(errno.EIO, 'ai_family not supported')
    host = _getaddrinfo_get_host(host, family, flags)
    port = _getaddrinfo_get_port(port, flags)
    if socktype not in [0, SOCK_DGRAM, SOCK_STREAM]:
        raise error(errno.ESOCKTNOSUPPORT, "Socket type %s is not supported" % _constant_to_name(socktype, ['SOCK_']))
    filter_fns = []
    filter_fns.append({
        AF_INET:   lambda x: isinstance(x, java.net.Inet4Address),
        AF_INET6:  lambda x: isinstance(x, java.net.Inet6Address),
        AF_UNSPEC: lambda x: isinstance(x, java.net.InetAddress),
    }[family])
    if host in [None, ""]:
        if flags & AI_PASSIVE:
             hosts = {AF_INET: [INADDR_ANY], AF_INET6: [IN6ADDR_ANY_INIT], AF_UNSPEC: [INADDR_ANY, IN6ADDR_ANY_INIT]}[family]
        else:
             hosts = ["localhost"]
    else:
        hosts = [host]
    results = []
    for h in hosts:
        for a in java.net.InetAddress.getAllByName(h):
            if len([f for f in filter_fns if f(a)]):
                family = {java.net.Inet4Address: AF_INET, java.net.Inet6Address: AF_INET6}[a.getClass()]
                if flags & AI_CANONNAME:
                    canonname = asPyString(a.getCanonicalHostName())
                else:
                    canonname = ""
                sockaddr = asPyString(a.getHostAddress())
                # TODO: Include flowinfo and scopeid in a 4-tuple for IPv6 addresses
                sock_tuple = {AF_INET : _ipv4_address_t, AF_INET6 : _ipv6_address_t}[family](sockaddr, port, a)
                if socktype == 0:
                    socktypes = [SOCK_DGRAM, SOCK_STREAM]
                else:
                    socktypes = [socktype]
                for result_socktype in socktypes:
                    result_proto = {SOCK_DGRAM: IPPROTO_UDP, SOCK_STREAM: IPPROTO_TCP}[result_socktype]
                    if proto in [0, result_proto]:
                        # The returned socket will only support the result_proto
                        # If this does not match the requested proto, don't return it
                        results.append((family, result_socktype, result_proto, canonname, sock_tuple))
    return results

def _getnameinfo_get_host(address, flags):
    if not isinstance(address, basestring):
        raise TypeError("getnameinfo() address 1 must be string, not None")
    if isinstance(address, unicode):
        address = _encode_idna(address)
    jia = java.net.InetAddress.getByName(address)
    result = jia.getCanonicalHostName()
    if flags & NI_NAMEREQD:
        if is_ip_address(result):
            raise gaierror(EAI_NONAME, "Name or service not known")
    elif flags & NI_NUMERICHOST:
        result = jia.getHostAddress()
    # Ignoring NI_NOFQDN for now
    if flags & NI_IDN:
        result = _decode_idna(result, flags)
    return result

def _getnameinfo_get_port(port, flags):
    if not isinstance(port, (int, long)):
        raise TypeError("getnameinfo() port number must be an integer")
    if flags & NI_NUMERICSERV:
        return port
    proto = None
    if flags & NI_DGRAM:
        proto = "udp"
    return getservbyport(port, proto)

@raises_java_exception
def getnameinfo(sock_addr, flags):
    if not isinstance(sock_addr, tuple) or len(sock_addr) < 2:
        raise TypeError("getnameinfo() argument 1 must be a tuple")
    host = _getnameinfo_get_host(sock_addr[0], flags)
    port = _getnameinfo_get_port(sock_addr[1], flags)
    return (host, port)

def getdefaulttimeout():
    return _defaulttimeout

def _calctimeoutvalue(value):
    if value is None:
        return None
    try:
        floatvalue = float(value)
    except:
        raise TypeError('Socket timeout value must be a number or None')
    if floatvalue < 0.0:
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

@raises_java_exception
def inet_pton(family, ip_string):
    if family == AF_INET:
        if not is_ipv4_address(ip_string):
            raise error("illegal IP address string passed to inet_pton")
    elif family == AF_INET6:
        if not is_ipv6_address(ip_string):
            raise error("illegal IP address string passed to inet_pton")
    else:
        raise error(errno.EAFNOSUPPORT, "Address family not supported by protocol")
    ia = java.net.InetAddress.getByName(ip_string)
    bytes = []
    for byte in ia.getAddress():
        if byte < 0:
            bytes.append(byte+256)
        else:
            bytes.append(byte)
    return "".join([chr(byte) for byte in bytes])

@raises_java_exception
def inet_ntop(family, packed_ip):
    jByteArray = jarray.array(packed_ip, 'b')
    if family == AF_INET:
        if len(jByteArray) != 4:
            raise ValueError("invalid length of packed IP address string")
    elif family == AF_INET6:
        if len(jByteArray) != 16:
            raise ValueError("invalid length of packed IP address string")
    else:
        raise ValueError("unknown address family %s" % family)
    ia = java.net.InetAddress.getByAddress(jByteArray)
    return ia.getHostAddress()

def inet_aton(ip_string):
    return inet_pton(AF_INET, ip_string)

def inet_ntoa(packed_ip):
    return inet_ntop(AF_INET, packed_ip)

class _nonblocking_api_mixin:

    mode            = MODE_BLOCKING
    reference_count = 0
    close_lock      = threading.Lock()

    def __init__(self):
        self.timeout = _defaulttimeout
        if self.timeout is not None:
            self.mode = MODE_TIMEOUT
        self.pending_options = {
            (SOL_SOCKET, SO_REUSEADDR):  0,
        }

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

    def getblocking(self):
        return self.mode == MODE_BLOCKING

    @raises_java_exception
    def setsockopt(self, level, optname, value):
        if self.sock_impl:
            self.sock_impl.setsockopt(level, optname, value)
        else:
            self.pending_options[ (level, optname) ] = value

    @raises_java_exception
    def getsockopt(self, level, optname):
        # Handle "pseudo" options first
        if level == SOL_SOCKET and optname == SO_TYPE:
            return getattr(self, "type")
        if level == SOL_SOCKET and optname == SO_ERROR:
            return_value = self._last_error
            self._last_error = 0
            return return_value
        # Now handle "real" options
        if self.sock_impl:
            return self.sock_impl.getsockopt(level, optname)
        else:
            return self.pending_options.get( (level, optname), None)

    @raises_java_exception
    def shutdown(self, how):
        assert how in (SHUT_RD, SHUT_WR, SHUT_RDWR)
        if not self.sock_impl:
            raise error(errno.ENOTCONN, "Transport endpoint is not connected")
        self.sock_impl.shutdown(how)

    @raises_java_exception
    def close(self):
        if self.sock_impl:
            self.sock_impl.close()

    @raises_java_exception
    def getsockname(self):
        if self.sock_impl is None:
            # If the user has already bound an address, return that
            if self.local_addr:
                return self.local_addr
            # The user has not bound, connected or listened
            # This is what cpython raises in this scenario
            raise error(errno.EINVAL, "Invalid argument")
        return self.sock_impl.getsockname()

    @raises_java_exception
    def getpeername(self):
        if self.sock_impl is None:
            raise error(errno.ENOTCONN, "Socket is not connected")
        return self.sock_impl.getpeername()

    def _config(self):
        assert self.mode in _permitted_modes
        if self.sock_impl:
            self.sock_impl.config(self.mode, self.timeout)
            for level, optname in self.pending_options.keys():
                if optname != SO_REUSEADDR:
                    self.sock_impl.setsockopt(level, optname, self.pending_options[ (level, optname) ])

    def getchannel(self):
        if not self.sock_impl:
            return None
        return self.sock_impl.getchannel()

    def fileno(self):
        if not self.sock_impl:
            return None
        return self.sock_impl.fileno()

    def _get_jsocket(self):
        return self.sock_impl.jsocket

class _tcpsocket(_nonblocking_api_mixin):

    sock_impl   = None
    istream     = None
    ostream     = None
    local_addr  = None
    server      = 0
    _last_error = 0

    def __init__(self):
        _nonblocking_api_mixin.__init__(self)

    def getsockopt(self, level, optname):
        if level == SOL_SOCKET and optname == SO_ACCEPTCONN:
            return self.server
        return _nonblocking_api_mixin.getsockopt(self, level, optname)

    @raises_java_exception
    def bind(self, addr):
        assert not self.sock_impl
        assert not self.local_addr
        # Do the address format check
        _get_jsockaddr(addr, self.family, self.type, self.proto, AI_PASSIVE)
        self.local_addr = addr

    @raises_java_exception
    def listen(self, backlog):
        "This signifies a server socket"
        assert not self.sock_impl
        self.server = 1
        self.sock_impl = _server_socket_impl(_get_jsockaddr(self.local_addr, self.family, self.type, self.proto, AI_PASSIVE), 
                              backlog, self.pending_options[ (SOL_SOCKET, SO_REUSEADDR) ])
        self._config()

    @raises_java_exception
    def accept(self):
        "This signifies a server socket"
        if not self.sock_impl:
            self.listen()
        assert self.server
        new_sock = self.sock_impl.accept()
        if not new_sock:
            raise would_block_error()
        cliconn = _tcpsocket()
        cliconn.pending_options[ (SOL_SOCKET, SO_REUSEADDR) ] = new_sock.jsocket.getReuseAddress()
        cliconn.sock_impl = new_sock
        cliconn._setup()
        return cliconn, new_sock.getpeername()

    def _do_connect(self, addr):
        assert not self.sock_impl
        self.sock_impl = _client_socket_impl()
        if self.local_addr: # Has the socket been bound to a local address?
            self.sock_impl.bind(_get_jsockaddr(self.local_addr, self.family, self.type, self.proto, 0), 
                                 self.pending_options[ (SOL_SOCKET, SO_REUSEADDR) ])
        self._config() # Configure timeouts, etc, now that the socket exists
        self.sock_impl.connect(_get_jsockaddr(addr, self.family, self.type, self.proto, 0))

    @raises_java_exception
    def connect(self, addr):
        "This signifies a client socket"
        self._do_connect(addr)
        self._setup()

    @raises_java_exception
    def connect_ex(self, addr):
        "This signifies a client socket"
        if not self.sock_impl:
            self._do_connect(addr)
        if self.sock_impl.finish_connect():
            self._setup()
            if self.mode == MODE_NONBLOCKING:
                return errno.EISCONN
            return 0
        return errno.EINPROGRESS

    def _setup(self):
        if self.mode != MODE_NONBLOCKING:
            self.istream = self.sock_impl.jsocket.getInputStream()
            self.ostream = self.sock_impl.jsocket.getOutputStream()

    @raises_java_exception
    def recv(self, n):
        if not self.sock_impl: raise error(errno.ENOTCONN, 'Socket is not connected')
        if self.sock_impl.jchannel.isConnectionPending():
            self.sock_impl.jchannel.finishConnect()
        data = jarray.zeros(n, 'b')
        m = self.sock_impl.read(data)
        if m == -1:#indicates EOF has been reached, so we just return the empty string
            return ""
        elif m <= 0:
            if self.mode == MODE_NONBLOCKING:
                raise would_block_error()
            return ""
        if m < n:
            data = data[:m]
        return data.tostring()

    @raises_java_exception
    def recvfrom(self, n):
        return self.recv(n), self.getpeername()

    @raises_java_exception
    def send(self, s):
        if not self.sock_impl: raise error(errno.ENOTCONN, 'Socket is not connected')
        if self.sock_impl.jchannel.isConnectionPending():
            self.sock_impl.jchannel.finishConnect()
        numwritten = self.sock_impl.write(s)
        if numwritten == 0 and self.mode == MODE_NONBLOCKING:
            raise would_block_error()
        return numwritten

    sendall = send

    @raises_java_exception
    def close(self):
        if self.istream:
            self.istream.close()
        if self.ostream:
            self.ostream.close()
        if self.sock_impl:
            self.sock_impl.close()


class _udpsocket(_nonblocking_api_mixin):

    sock_impl   = None
    connected   = False
    local_addr  = None
    _last_error = 0

    def __init__(self):
        _nonblocking_api_mixin.__init__(self)

    @raises_java_exception
    def bind(self, addr):
        assert not self.sock_impl
        assert not self.local_addr
        # Do the address format check
        _get_jsockaddr(addr, self.family, self.type, self.proto, AI_PASSIVE)
        self.local_addr = addr
        self.sock_impl = _datagram_socket_impl(_get_jsockaddr(self.local_addr, self.family, self.type, self.proto, AI_PASSIVE), 
                                                self.pending_options[ (SOL_SOCKET, SO_REUSEADDR) ])
        self._config()

    def _do_connect(self, addr):
        assert not self.connected, "Datagram Socket is already connected"
        if not self.sock_impl:
            self.sock_impl = _datagram_socket_impl()
            self._config()
        self.sock_impl.connect(_get_jsockaddr(addr, self.family, self.type, self.proto, 0))
        self.connected = True

    @raises_java_exception
    def connect(self, addr):
        self._do_connect(addr)

    @raises_java_exception
    def connect_ex(self, addr):
        if not self.sock_impl:
            self._do_connect(addr)
        return 0

    @raises_java_exception
    def sendto(self, data, p1, p2=None):
        if not p2:
            flags, addr = 0, p1
        else:
            flags, addr = 0, p2
        if not self.sock_impl:
            self.sock_impl = _datagram_socket_impl()
            self._config()
        byte_array = java.lang.String(data).getBytes('iso-8859-1')
        result = self.sock_impl.sendto(byte_array, _get_jsockaddr(addr, self.family, self.type, self.proto, 0), flags)
        return result

    @raises_java_exception
    def send(self, data, flags=None):
        if not self.connected: raise error(errno.ENOTCONN, "Socket is not connected")
        byte_array = java.lang.String(data).getBytes('iso-8859-1')
        return self.sock_impl.send(byte_array, flags)

    @raises_java_exception
    def recvfrom(self, num_bytes, flags=None):
        """
        There is some disagreement as to what the behaviour should be if
        a recvfrom operation is requested on an unbound socket.
        See the following links for more information
        http://bugs.jython.org/issue1005
        http://bugs.sun.com/view_bug.do?bug_id=6621689
        """
        # This is the old 2.1 behaviour
        #assert self.sock_impl
        # This is amak's preferred interpretation
        #raise error(errno.ENOTCONN, "Recvfrom on unbound udp socket meaningless operation")
        # And this is the option for cpython compatibility
        if not self.sock_impl:
            self.sock_impl = _datagram_socket_impl()
            self._config()
        return self.sock_impl.recvfrom(num_bytes, flags)

    @raises_java_exception
    def recv(self, num_bytes, flags=None):
        if not self.sock_impl:
            raise error(errno.ENOTCONN, "Socket is not connected")
        return self.sock_impl.recv(num_bytes, flags)

    def __del__(self):
        self.close()

_socketmethods = (
    'bind', 'connect', 'connect_ex', 'fileno', 'listen',
    'getpeername', 'getsockname', 'getsockopt', 'setsockopt',
    'sendall', 'setblocking',
    'settimeout', 'gettimeout', 'shutdown', 'getchannel')

# All the method names that must be delegated to either the real socket
# object or the _closedsocket object.
_delegate_methods = ("recv", "recvfrom", "recv_into", "recvfrom_into",
                     "send", "sendto")

class _closedsocket(object):
    __slots__ = []
    def _dummy(*args):
        raise error(errno.EBADF, 'Bad file descriptor')
    # All _delegate_methods must also be initialized here.
    send = recv = recv_into = sendto = recvfrom = recvfrom_into = _dummy
    __getattr__ = _dummy

_active_sockets = set()

def _closeActiveSockets():
    for socket in _active_sockets.copy():
        try:
            socket.close()
        except error:
            msg = 'Problem closing socket: %s: %r' % (socket, sys.exc_info())
            print >> sys.stderr, msg

class _socketobject(object):

    __doc__ = _realsocket.__doc__

    __slots__ = ["_sock", "__weakref__"] + list(_delegate_methods)

    def __init__(self, family=AF_INET, type=SOCK_STREAM, proto=0, _sock=None):
        if _sock is None:
            _sock = _realsocket(family, type, proto)
            _sock.reference_count += 1
        elif isinstance(_sock, _nonblocking_api_mixin):
            _sock.reference_count += 1
        self._sock = _sock
        for method in _delegate_methods:
            meth = getattr(_sock, method, None)
            if meth:
                setattr(self, method, meth)
        _active_sockets.add(self)

    def close(self):
        try:
            _active_sockets.remove(self)
        except KeyError:
            pass
        _sock = self._sock
        if isinstance(_sock, _nonblocking_api_mixin):
            _sock.close_lock.acquire()
            try:
                _sock.reference_count -=1
                if not _sock.reference_count:
                    _sock.close()
                self._sock = _closedsocket()
                dummy = self._sock._dummy
                for method in _delegate_methods:
                    setattr(self, method, dummy)
                self.send = self.recv = self.sendto = self.recvfrom = \
                    self._sock._dummy
            finally:
                _sock.close_lock.release()
    #close.__doc__ = _realsocket.close.__doc__

    def accept(self):
        sock, addr = self._sock.accept()
        return _socketobject(_sock=sock), addr
    #accept.__doc__ = _realsocket.accept.__doc__

    def dup(self):
        """dup() -> socket object

        Return a new socket object connected to the same system resource."""
        _sock = self._sock
        if not isinstance(_sock, _nonblocking_api_mixin):
            return _socketobject(_sock=_sock)

        _sock.close_lock.acquire()
        try:
            duped = _socketobject(_sock=_sock)
        finally:
            _sock.close_lock.release()
        return duped

    def makefile(self, mode='r', bufsize=-1):
        """makefile([mode[, bufsize]]) -> file object

        Return a regular file object corresponding to the socket.  The mode
        and bufsize arguments are as for the built-in open() function."""
        _sock = self._sock
        if not isinstance(_sock, _nonblocking_api_mixin):
            return _fileobject(_sock, mode, bufsize)

        _sock.close_lock.acquire()
        try:
            fileobject = _fileobject(_sock, mode, bufsize)
        finally:
            _sock.close_lock.release()
        return fileobject

    family = property(lambda self: self._sock.family, doc="the socket family")
    type = property(lambda self: self._sock.type, doc="the socket type")
    proto = property(lambda self: self._sock.proto, doc="the socket protocol")

    _s = ("def %s(self, *args): return self._sock.%s(*args)\n\n"
          #"%s.__doc__ = _realsocket.%s.__doc__\n")
          )
    for _m in _socketmethods:
        #exec _s % (_m, _m, _m, _m)
        exec _s % (_m, _m)
    del _m, _s

socket = SocketType = _socketobject

class _fileobject(object):
    """Faux file object attached to a socket object."""

    default_bufsize = 8192
    name = "<socket>"

    __slots__ = ["mode", "bufsize", "softspace",
                 # "closed" is a property, see below
                 "_sock", "_rbufsize", "_wbufsize", "_rbuf", "_wbuf",
                 "_close"]

    def __init__(self, sock, mode='rb', bufsize=-1, close=False):
        self._sock = sock
        if isinstance(sock, _nonblocking_api_mixin):
            sock.reference_count += 1
        self.mode = mode # Not actually used in this version
        if bufsize < 0:
            bufsize = self.default_bufsize
        self.bufsize = bufsize
        self.softspace = False
        if bufsize == 0:
            self._rbufsize = 1
        elif bufsize == 1:
            self._rbufsize = self.default_bufsize
        else:
            self._rbufsize = bufsize
        self._wbufsize = bufsize
        self._rbuf = "" # A string
        self._wbuf = [] # A list of strings
        self._close = close

    def _getclosed(self):
        return self._sock is None
    closed = property(_getclosed, doc="True if the file is closed")

    def close(self):
        try:
            if self._sock:
                self.flush()
        finally:
            if self._sock:
                if isinstance(self._sock, _nonblocking_api_mixin):
                    self._sock.reference_count -= 1
                    if not self._sock.reference_count or self._close:
                        self._sock.close()
                elif self._close:
                    self._sock.close()
            self._sock = None

    def __del__(self):
        try:
            self.close()
        except:
            # close() may fail if __init__ didn't complete
            pass

    def flush(self):
        if self._wbuf:
            buffer = "".join(self._wbuf)
            self._wbuf = []
            self._sock.sendall(buffer)

    def fileno(self):
        return self._sock.fileno()

    def write(self, data):
        data = str(data) # XXX Should really reject non-string non-buffers
        if not data:
            return
        self._wbuf.append(data)
        if (self._wbufsize == 0 or
            self._wbufsize == 1 and '\n' in data or
            self._get_wbuf_len() >= self._wbufsize):
            self.flush()

    def writelines(self, list):
        # XXX We could do better here for very long lists
        # XXX Should really reject non-string non-buffers
        self._wbuf.extend(filter(None, map(str, list)))
        if (self._wbufsize <= 1 or
            self._get_wbuf_len() >= self._wbufsize):
            self.flush()

    def _get_wbuf_len(self):
        buf_len = 0
        for x in self._wbuf:
            buf_len += len(x)
        return buf_len

    def read(self, size=-1):
        data = self._rbuf
        if size < 0:
            # Read until EOF
            buffers = []
            if data:
                buffers.append(data)
            self._rbuf = ""
            if self._rbufsize <= 1:
                recv_size = self.default_bufsize
            else:
                recv_size = self._rbufsize
            while True:
                data = self._sock.recv(recv_size)
                if not data:
                    break
                buffers.append(data)
            return "".join(buffers)
        else:
            # Read until size bytes or EOF seen, whichever comes first
            buf_len = len(data)
            if buf_len >= size:
                self._rbuf = data[size:]
                return data[:size]
            buffers = []
            if data:
                buffers.append(data)
            self._rbuf = ""
            while True:
                left = size - buf_len
                recv_size = max(self._rbufsize, left)
                data = self._sock.recv(recv_size)
                if not data:
                    break
                buffers.append(data)
                n = len(data)
                if n >= left:
                    self._rbuf = data[left:]
                    buffers[-1] = data[:left]
                    break
                buf_len += n
            return "".join(buffers)

    def readline(self, size=-1):
        data = self._rbuf
        if size < 0:
            # Read until \n or EOF, whichever comes first
            if self._rbufsize <= 1:
                # Speed up unbuffered case
                assert data == ""
                buffers = []
                recv = self._sock.recv
                while data != "\n":
                    data = recv(1)
                    if not data:
                        break
                    buffers.append(data)
                return "".join(buffers)
            nl = data.find('\n')
            if nl >= 0:
                nl += 1
                self._rbuf = data[nl:]
                return data[:nl]
            buffers = []
            if data:
                buffers.append(data)
            self._rbuf = ""
            while True:
                data = self._sock.recv(self._rbufsize)
                if not data:
                    break
                buffers.append(data)
                nl = data.find('\n')
                if nl >= 0:
                    nl += 1
                    self._rbuf = data[nl:]
                    buffers[-1] = data[:nl]
                    break
            return "".join(buffers)
        else:
            # Read until size bytes or \n or EOF seen, whichever comes first
            nl = data.find('\n', 0, size)
            if nl >= 0:
                nl += 1
                self._rbuf = data[nl:]
                return data[:nl]
            buf_len = len(data)
            if buf_len >= size:
                self._rbuf = data[size:]
                return data[:size]
            buffers = []
            if data:
                buffers.append(data)
            self._rbuf = ""
            while True:
                data = self._sock.recv(self._rbufsize)
                if not data:
                    break
                buffers.append(data)
                left = size - buf_len
                nl = data.find('\n', 0, left)
                if nl >= 0:
                    nl += 1
                    self._rbuf = data[nl:]
                    buffers[-1] = data[:nl]
                    break
                n = len(data)
                if n >= left:
                    self._rbuf = data[left:]
                    buffers[-1] = data[:left]
                    break
                buf_len += n
            return "".join(buffers)

    def readlines(self, sizehint=0):
        total = 0
        list = []
        while True:
            line = self.readline()
            if not line:
                break
            list.append(line)
            total += len(line)
            if sizehint and total >= sizehint:
                break
        return list

    # Iterator protocols

    def __iter__(self):
        return self

    def next(self):
        line = self.readline()
        if not line:
            raise StopIteration
        return line

_GLOBAL_DEFAULT_TIMEOUT = object()

def create_connection(address, timeout=_GLOBAL_DEFAULT_TIMEOUT,
                      source_address=None):
    """Connect to *address* and return the socket object.

    Convenience function.  Connect to *address* (a 2-tuple ``(host,
    port)``) and return the socket object.  Passing the optional
    *timeout* parameter will set the timeout on the socket instance
    before attempting to connect.  If no *timeout* is supplied, the
    global default timeout setting returned by :func:`getdefaulttimeout`
    is used.  If *source_address* is set it must be a tuple of (host, port)
    for the socket to bind as a source address before making the connection.
    An host of '' or port 0 tells the OS to use the default.
    """

    host, port = address
    err = None
    for res in getaddrinfo(host, port, 0, SOCK_STREAM):
        af, socktype, proto, canonname, sa = res
        sock = None
        try:
            sock = socket(af, socktype, proto)
            if timeout is not _GLOBAL_DEFAULT_TIMEOUT:
                sock.settimeout(timeout)
            if source_address:
                sock.bind(source_address)
            sock.connect(sa)
            return sock

        except error as _:
            err = _
            if sock is not None:
                sock.close()

    if err is not None:
        raise err
    else:
        raise error("getaddrinfo returns an empty list")

# Define the SSL support

class ssl:

    @raises_java_exception
    def __init__(self, jython_socket_wrapper, keyfile=None, certfile=None):
        self.jython_socket_wrapper = jython_socket_wrapper
        jython_socket = self.jython_socket_wrapper._sock
        self.java_ssl_socket = self._make_ssl_socket(jython_socket)
        self._in_buf = java.io.BufferedInputStream(self.java_ssl_socket.getInputStream())
        self._out_buf = java.io.BufferedOutputStream(self.java_ssl_socket.getOutputStream())

    def _make_ssl_socket(self, jython_socket, auto_close=0):
        java_net_socket = jython_socket._get_jsocket()
        assert isinstance(java_net_socket, java.net.Socket)
        host = java_net_socket.getInetAddress().getHostAddress()
        port = java_net_socket.getPort()
        factory = javax.net.ssl.SSLSocketFactory.getDefault();
        java_ssl_socket = factory.createSocket(java_net_socket, host, port, auto_close)
        java_ssl_socket.setEnabledCipherSuites(java_ssl_socket.getSupportedCipherSuites())
        java_ssl_socket.startHandshake()
        return java_ssl_socket

    @raises_java_exception
    def read(self, n=4096):
        data = jarray.zeros(n, 'b')
        m = self._in_buf.read(data, 0, n)
        if m <= 0:
            return ""
        if m < n:
            data = data[:m]
        return data.tostring()

    recv = read

    @raises_java_exception
    def write(self, s):
        self._out_buf.write(s)
        self._out_buf.flush()
        return len(s)

    send = sendall = write

    def makefile(self, mode='r', bufsize=-1):
        return _fileobject(self, mode, bufsize)

    def _get_server_cert(self):
        return self.java_ssl_socket.getSession().getPeerCertificates()[0]

    @raises_java_exception
    def server(self):
        cert = self._get_server_cert()
        return cert.getSubjectDN().toString()

    @raises_java_exception
    def issuer(self):
        cert = self._get_server_cert()
        return cert.getIssuerDN().toString()

    def close(self):
        self.jython_socket_wrapper.close()

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
