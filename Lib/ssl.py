import logging
import time

try:
    # jarjar-ed version
    from org.python.netty.channel import ChannelInitializer
    from org.python.netty.handler.ssl import SslHandler
except ImportError:
    # dev version from extlibs
    from io.netty.channel import ChannelInitializer
    from io.netty.handler.ssl import SslHandler

from _socket import (
    SSLError, raises_java_exception,
    SSL_ERROR_SSL,
    SSL_ERROR_WANT_READ,
    SSL_ERROR_WANT_WRITE,
    SSL_ERROR_WANT_X509_LOOKUP,
    SSL_ERROR_SYSCALL,
    SSL_ERROR_ZERO_RETURN,
    SSL_ERROR_WANT_CONNECT,
    SSL_ERROR_EOF,
    SSL_ERROR_INVALID_ERROR_CODE)
from _sslcerts import _get_ssl_context

from java.text import SimpleDateFormat
from java.util import Locale, TimeZone
from javax.naming.ldap import LdapName
from javax.security.auth.x500 import X500Principal


log = logging.getLogger("socket")


CERT_NONE, CERT_OPTIONAL, CERT_REQUIRED = range(3)

# FIXME need to map to java names as well; there's also possibility some difference between 
# SSLv2 (Java) and PROTOCOL_SSLv23 (Python) but reading the docs suggest not
# http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext

# Currently ignored, since we just use the default in Java. FIXME
PROTOCOL_SSLv2, PROTOCOL_SSLv3, PROTOCOL_SSLv23, PROTOCOL_TLSv1 = range(4)
_PROTOCOL_NAMES = {PROTOCOL_SSLv2: 'SSLv2', PROTOCOL_SSLv3: 'SSLv3', PROTOCOL_SSLv23: 'SSLv23', PROTOCOL_TLSv1: 'TLSv1'}

_rfc2822_date_format = SimpleDateFormat("MMM dd HH:mm:ss yyyy z", Locale.US)
_rfc2822_date_format.setTimeZone(TimeZone.getTimeZone("GMT"))

_ldap_rdn_display_names = {
    # list from RFC 2253
    "CN": "commonName",
    "L":  "localityName",
    "ST": "stateOrProvinceName",
    "O":  "organizationName",
    "OU": "organizationalUnitName",
    "C":  "countryName",
    "STREET": "streetAddress",
    "DC": "domainComponent",
    "UID": "userid"
}

_cert_name_types = [
    # FIXME only entry 2 - DNS - has been confirmed w/ cpython;
    # everything else is coming from this doc:
    # http://docs.oracle.com/javase/7/docs/api/java/security/cert/X509Certificate.html#getSubjectAlternativeNames()
    "other",
    "rfc822",
    "DNS",
    "x400Address",
    "directory",
    "ediParty",
    "uniformResourceIdentifier",
    "ipAddress",
    "registeredID"]


class SSLInitializer(ChannelInitializer):

    def __init__(self, ssl_handler):
        self.ssl_handler = ssl_handler

    def initChannel(self, ch):
        pipeline = ch.pipeline()
        pipeline.addLast("ssl", self.ssl_handler) 


class SSLSocket(object):
    
    def __init__(self, sock,
                 keyfile, certfile, ca_certs,
                 do_handshake_on_connect, server_side):
        self.sock = sock
        self._sock = sock._sock  # the real underlying socket
        self.context = _get_ssl_context(keyfile, certfile, ca_certs)
        self.engine = self.context.createSSLEngine()
        self.engine.setUseClientMode(not server_side)
        self.ssl_handler = SslHandler(self.engine)
        self.already_handshaked = False
        self.do_handshake_on_connect = do_handshake_on_connect

        if self.do_handshake_on_connect and hasattr(self._sock, "connected") and self._sock.connected:
            self.already_handshaked = True
            log.debug("Adding SSL handler to pipeline after connection", extra={"sock": self._sock})
            self._sock.channel.pipeline().addFirst("ssl", self.ssl_handler)
            self._sock._post_connect()
            self._sock._notify_selectors()
            self._sock._unlatch()

        def handshake_step(result):
            log.debug("SSL handshaking %s", result, extra={"sock": self._sock})
            if not hasattr(self._sock, "activity_latch"):  # need a better discriminant
                self._sock._post_connect()
            self._sock._notify_selectors()

        self.ssl_handler.handshakeFuture().addListener(handshake_step)
        if self.do_handshake_on_connect and self.already_handshaked:
            time.sleep(0.1)  # FIXME do we need this sleep
            self.ssl_handler.handshakeFuture().sync()
            log.debug("SSL handshaking completed", extra={"sock": self._sock})

    def connect(self, addr):
        log.debug("Connect SSL with handshaking %s", self.do_handshake_on_connect, extra={"sock": self._sock})
        self._sock._connect(addr)
        if self.do_handshake_on_connect:
            self.already_handshaked = True
            if self._sock.connected:
                log.debug("Already connected, adding SSL handler to pipeline...", extra={"sock": self._sock})
                self._sock.channel.pipeline().addFirst("ssl", self.ssl_handler)
            else:
                log.debug("Not connected, adding SSL initializer...", extra={"sock": self._sock})
                self._sock.connect_handlers.append(SSLInitializer(self.ssl_handler))

    # Various pass through methods to the wrapper socket

    def send(self, data):
        return self.sock.send(data)

    def sendall(self, data):
        return self.sock.sendall(data)

    def recv(self, bufsize, flags=0):
        return self.sock.recv(bufsize, flags)

    def close(self):
        self.sock.close()

    def setblocking(self, mode):
        self.sock.setblocking(mode)

    def settimeout(self, timeout):
        self.sock.settimeout(timeout)

    def gettimeout(self):
        return self.sock.gettimeout()

    def makefile(self, mode='r', bufsize=-1):
        return self.sock.makefile(mode, bufsize)

    def shutdown(self, how):
        self.sock.shutdown(how)

    # Need to work with the real underlying socket as well

    def _readable(self):
        return self._sock._readable()

    def _writable(self):
        return self._sock._writable()

    def _register_selector(self, selector):
        self._sock._register_selector(selector)

    def _unregister_selector(self, selector):
        return self._sock._unregister_selector(selector)

    def _notify_selectors(self):
        self._sock._notify_selectors()

    def do_handshake(self):
        if not self.already_handshaked:
            log.debug("Not handshaked, so adding SSL handler", extra={"sock": self._sock})
            self.already_handshaked = True
            self._sock.channel.pipeline().addFirst("ssl", self.ssl_handler)

    def getpeername(self):
        return self.sock.getpeername()

    def fileno(self):
        return self

    @raises_java_exception
    def getpeercert(self, binary_form=False):
        cert = self.engine.getSession().getPeerCertificates()[0]
        if binary_form:
            return cert.getEncoded()
        dn = cert.getSubjectX500Principal().getName()
        ldapDN = LdapName(dn)
        # FIXME given this tuple of a single element tuple structure assumed here, is it possible this is
        # not actually the case, eg because of multi value attributes?
        rdns = tuple((((_ldap_rdn_display_names.get(rdn.type), rdn.value),) for rdn in ldapDN.getRdns()))
        # FIXME is it str? or utf8? or some other encoding? maybe a bug in cpython?
        alt_names = tuple(((_cert_name_types[type], str(name)) for (type, name) in cert.getSubjectAlternativeNames()))
        pycert = {
            "notAfter": _rfc2822_date_format.format(cert.getNotAfter()),
            "subject": rdns,
            "subjectAltName": alt_names, 
        }
        return pycert

    @raises_java_exception
    def issuer(self):
        return self.getpeercert().getIssuerDN().toString()

    def cipher(self):
        session = self._sslsocket.session
        suite = str(session.cipherSuite)
        if "256" in suite:  # FIXME!!! this test usually works, but there must be a better approach
            strength = 256
        elif "128" in suite:
            strength = 128
        else:
            strength = None
        return suite, str(session.protocol), strength



# instantiates a SSLEngine, with the following things to keep in mind:

# FIXME not yet supported
# suppress_ragged_eofs - presumably this is an exception we can detect in Netty, the underlying SSLEngine certainly does
# ssl_version - use SSLEngine.setEnabledProtocols(java.lang.String[])
# ciphers - SSLEngine.setEnabledCipherSuites(String[] suites)

def wrap_socket(sock, keyfile=None, certfile=None, server_side=False, cert_reqs=CERT_NONE,
                ssl_version=None, ca_certs=None, do_handshake_on_connect=True,
                suppress_ragged_eofs=True, ciphers=None):
    return SSLSocket(
        sock, 
        keyfile=keyfile, certfile=certfile, ca_certs=ca_certs,
        server_side=server_side,
        do_handshake_on_connect=do_handshake_on_connect)


def unwrap_socket(sock):
    # FIXME removing SSL handler from pipeline should suffice, but low pri for now
    raise NotImplemented()


# Underlying Java does a good job of managing entropy, so these are just no-ops

def RAND_status():
    return True

def RAND_egd(path):
    pass

def RAND_add(bytes, entropy):
    pass


