import base64
from collections import namedtuple
import errno
from java.security.cert import CertificateFactory
import uuid
from java.io import BufferedInputStream
from java.security import KeyStore
from java.security.cert import CertificateParsingException
from javax.net.ssl import TrustManagerFactory
from javax.naming.ldap import LdapName
import logging
import os.path
import textwrap
import time
import threading

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
    SSL_ERROR_INVALID_ERROR_CODE,
    error as socket_error)
from _sslcerts import _get_openssl_key_manager, NoVerifyX509TrustManager
from _sslcerts import SSLContext as _JavaSSLContext

from java.text import SimpleDateFormat
from java.util import ArrayList, Locale, TimeZone, NoSuchElementException
from java.util.concurrent import CountDownLatch
from javax.naming.ldap import LdapName
from javax.security.auth.x500 import X500Principal

log = logging.getLogger("_socket")


# Pretend to be OpenSSL
OPENSSL_VERSION = "OpenSSL 1.0.0 (as emulated by Java SSL)"
OPENSSL_VERSION_NUMBER = 0x1000000L
OPENSSL_VERSION_INFO = (1, 0, 0, 0, 0)

CERT_NONE, CERT_OPTIONAL, CERT_REQUIRED = range(3)

# Do not support PROTOCOL_SSLv2, it is highly insecure and it is optional
_, PROTOCOL_SSLv3, PROTOCOL_SSLv23, PROTOCOL_TLSv1, PROTOCOL_TLSv1_1, PROTOCOL_TLSv1_2 = range(6)
_PROTOCOL_NAMES = {
    PROTOCOL_SSLv3: 'SSLv3',
    PROTOCOL_SSLv23: 'SSLv23',
    PROTOCOL_TLSv1: 'TLSv1',
    PROTOCOL_TLSv1_1: 'TLSv1.1',
    PROTOCOL_TLSv1_2: 'TLSv1.2'
}

OP_ALL, OP_NO_SSLv2, OP_NO_SSLv3, OP_NO_TLSv1 = range(4)


# https://docs.python.org/2/library/ssl.html#ssl.HAS_ALPN etc...
HAS_ALPN, HAS_NPN, HAS_ECDH, HAS_SNI = False, False, True, True


_rfc2822_date_format = SimpleDateFormat("MMM dd HH:mm:ss yyyy z", Locale.US)
_rfc2822_date_format.setTimeZone(TimeZone.getTimeZone("GMT"))

_ldap_rdn_display_names = {
    # list from RFC 2253
    "CN": "commonName",
    "L": "localityName",
    "ST": "stateOrProvinceName",
    "O": "organizationName",
    "OU": "organizationalUnitName",
    "C": "countryName",
    "STREET": "streetAddress",
    "DC": "domainComponent",
    "UID": "userid"
}

_cert_name_types = [
    # Fields documented in 
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
        pipeline.addFirst("ssl", self.ssl_handler)


class SSLSocket(object):

    def __init__(self, sock, keyfile=None, certfile=None, server_side=False, cert_reqs=CERT_NONE,
                 ssl_version=PROTOCOL_SSLv23, ca_certs=None,
                 do_handshake_on_connect=True, suppress_ragged_eofs=True, ciphers=None, _context=None):
        # TODO ^^ handle suppress_ragged_eofs
        self.sock = sock
        self.do_handshake_on_connect = do_handshake_on_connect
        self._sock = sock._sock  # the real underlying socket
        self.context = _context
        if _context is None:
            self.context = SSLContext(ssl_version)
        else:
            if server_side and not certfile:
                raise ValueError("certfile must be specified for server-side "
                                 "operations")
            if keyfile and not certfile:
                raise ValueError("certfile must be specified")
            if certfile and not keyfile:
                keyfile = certfile
            self._context = SSLContext(ssl_version)
            self._context.verify_mode = cert_reqs
            if ca_certs:
                self._context.load_verify_locations(ca_certs)
            if certfile:
                self._context.load_cert_chain(certfile, keyfile)
            if ciphers:
                self._context.set_ciphers(ciphers)

        self.engine = self.context._createSSLEngine()
        self.server_side = server_side
        self.engine.setUseClientMode(not server_side)
        self.ssl_handler = None
        # _sslobj is used to follow CPython convention that an object
        # means we have handshaked, as used by existing code that
        # looks at this internal
        self._sslobj = None
        self.handshake_count = 0

        if self.do_handshake_on_connect and self.sock._sock.connected:
            self.do_handshake()

    def connect(self, addr):
        log.debug("Connect SSL with handshaking %s", self.do_handshake_on_connect, extra={"sock": self._sock})
        self._sock._connect(addr)
        if self.do_handshake_on_connect:
            self.do_handshake()

    def connect_ex(self, addr):
        log.debug("Connect SSL with handshaking %s", self.do_handshake_on_connect, extra={"sock": self._sock})
        self._sock._connect(addr)
        if self.do_handshake_on_connect:
            self.do_handshake()
        return self._sock.connect_ex(addr)

    def unwrap(self):
        self._sock.channel.pipeline().remove("ssl")
        self.ssl_handler.close()
        return self._sock

    def do_handshake(self):
        log.debug("SSL handshaking", extra={"sock": self._sock})

        def handshake_step(result):
            log.debug("SSL handshaking completed %s", result, extra={"sock": self._sock})
            if not hasattr(self._sock, "active_latch"):
                log.debug("Post connect step", extra={"sock": self._sock})
                self._sock._post_connect()
                self._sock._unlatch()
            self._sslobj = object()  # we have now handshaked
            self._notify_selectors()

        if self.ssl_handler is None:
            self.ssl_handler = SslHandler(self.engine)
            self.ssl_handler.handshakeFuture().addListener(handshake_step)

            if hasattr(self._sock, "connected") and self._sock.connected:
                # The underlying socket is already connected, so some extra work to manage
                log.debug("Adding SSL handler to pipeline after connection", extra={"sock": self._sock})
                self._sock.channel.pipeline().addFirst("ssl", self.ssl_handler)
            else:
                log.debug("Not connected, adding SSL initializer...", extra={"sock": self._sock})
                self._sock.connect_handlers.append(SSLInitializer(self.ssl_handler))

        handshake = self.ssl_handler.handshakeFuture()
        time.sleep(0.001)  # Necessary apparently for the handler to get into a good state
        try:
            self._sock._handle_channel_future(handshake, "SSL handshake")
        except socket_error, e:
            raise SSLError(SSL_ERROR_SSL, e.strerror)

    # Various pass through methods to the wrapped socket

    def send(self, data):
        return self.sock.send(data)

    write = send

    def sendall(self, data):
        return self.sock.sendall(data)

    def recv(self, bufsize, flags=0):
        return self.sock.recv(bufsize, flags)

    read = recv

    def recvfrom(self, bufsize, flags=0):
        return self.sock.recvfrom(bufsize, flags)

    def recvfrom_into(self, buffer, nbytes=0, flags=0):
        return self.sock.recvfrom_into(buffer, nbytes, flags)

    def recv_into(self, buffer, nbytes=0, flags=0):
        return self.sock.recv_into(buffer, nbytes, flags)

    def sendto(self, string, arg1, arg2=None):
        # as observed on CPython, sendto when wrapped ignores the
        # destination address, thereby behaving just like send
        return self.sock.send(string)

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

    def pending(self):
        # undocumented function, used by some tests
        # see also http://bugs.python.org/issue21430
        return self._sock._pending()

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

@raises_java_exception
def wrap_socket(sock, keyfile=None, certfile=None, server_side=False, cert_reqs=CERT_NONE,
                ssl_version=PROTOCOL_SSLv23, ca_certs=None, do_handshake_on_connect=True,
                suppress_ragged_eofs=True, ciphers=None):

    return SSLSocket(
        sock,
        keyfile=keyfile, certfile=certfile, ca_certs=ca_certs,
        server_side=server_side,
        do_handshake_on_connect=do_handshake_on_connect)


# some utility functions

def cert_time_to_seconds(cert_time):
    """Takes a date-time string in standard ASN1_print form
    ("MON DAY 24HOUR:MINUTE:SEC YEAR TIMEZONE") and return
    a Python time value in seconds past the epoch."""

    import time
    return time.mktime(time.strptime(cert_time, "%b %d %H:%M:%S %Y GMT"))


PEM_HEADER = "-----BEGIN CERTIFICATE-----"
PEM_FOOTER = "-----END CERTIFICATE-----"


def DER_cert_to_PEM_cert(der_cert_bytes):
    """Takes a certificate in binary DER format and returns the
    PEM version of it as a string."""

    if hasattr(base64, 'standard_b64encode'):
        # preferred because older API gets line-length wrong
        f = base64.standard_b64encode(der_cert_bytes)
        return (PEM_HEADER + '\n' +
                textwrap.fill(f, 64) + '\n' +
                PEM_FOOTER + '\n')
    else:
        return (PEM_HEADER + '\n' +
                base64.encodestring(der_cert_bytes) +
                PEM_FOOTER + '\n')


def PEM_cert_to_DER_cert(pem_cert_string):
    """Takes a certificate in ASCII PEM format and returns the
    DER-encoded version of it as a byte sequence"""

    if not pem_cert_string.startswith(PEM_HEADER):
        raise ValueError("Invalid PEM encoding; must start with %s"
                         % PEM_HEADER)
    if not pem_cert_string.strip().endswith(PEM_FOOTER):
        raise ValueError("Invalid PEM encoding; must end with %s"
                         % PEM_FOOTER)
    d = pem_cert_string.strip()[len(PEM_HEADER):-len(PEM_FOOTER)]
    return base64.decodestring(d)


def get_server_certificate(addr, ssl_version=PROTOCOL_SSLv3, ca_certs=None):
    """Retrieve the certificate from the server at the specified address,
    and return it as a PEM-encoded string.
    If 'ca_certs' is specified, validate the server cert against it.
    If 'ssl_version' is specified, use it in the connection attempt."""

    host, port = addr
    if (ca_certs is not None):
        cert_reqs = CERT_REQUIRED
    else:
        cert_reqs = CERT_NONE
    s = wrap_socket(socket(), ssl_version=ssl_version,
                    cert_reqs=cert_reqs, ca_certs=ca_certs)
    s.connect(addr)
    dercert = s.getpeercert(True)
    s.close()
    return DER_cert_to_PEM_cert(dercert)


def get_protocol_name(protocol_code):
    return _PROTOCOL_NAMES.get(protocol_code, '<unknown>')


# a replacement for the old socket.ssl function

def sslwrap_simple(sock, keyfile=None, certfile=None):
    """A replacement for the old socket.ssl function.  Designed
    for compability with Python 2.5 and earlier.  Will disappear in
    Python 3.0."""

    ssl_sock = wrap_socket(sock, keyfile=keyfile, certfile=certfile, ssl_version=PROTOCOL_SSLv23)
    try:
        sock.getpeername()
    except socket_error:
        # no, no connection yet
        pass
    else:
        # yes, do the handshake
        ssl_sock.do_handshake()

    return ssl_sock


# Underlying Java does a good job of managing entropy, so these are just no-ops

def RAND_status():
    return True


def RAND_egd(path):
    if os.path.abspath(str(path)) != path:
        raise TypeError("Must be an absolute path, but ignoring it regardless")


def RAND_add(bytes, entropy):
    pass


class Purpose(object):
    """SSLContext purpose flags with X509v3 Extended Key Usage objects
    """
    SERVER_AUTH = '1.3.6.1.5.5.7.3.1'
    CLIENT_AUTH = '1.3.6.1.5.5.7.3.2'


class SSLContext(object):

    def __init__(self, protocol):
        protocol_name = _PROTOCOL_NAMES[protocol]
        if protocol == PROTOCOL_SSLv23:  # darjus: at least my Java does not let me use v2
            protocol_name = 'SSL'

        self.protocol = protocol
        self.check_hostname = False
        self.options = OP_ALL
        self.verify_flags = None
        self.verify_mode = CERT_NONE
        self._ciphers = None

        self._trust_store = KeyStore.getInstance(KeyStore.getDefaultType())
        self._trust_store.load(None, None)

        self._key_store = KeyStore.getInstance(KeyStore.getDefaultType())
        self._key_store.load(None, None)

        self._context = _JavaSSLContext.getInstance(protocol_name)
        self._key_managers = None

    def wrap_socket(self, sock, server_side=False,
                    do_handshake_on_connect=True,
                    suppress_ragged_eofs=True,
                    server_hostname=None):
        # FIXME do something about server_hostname
        return SSLSocket(sock, keyfile=None, certfile=None, ca_certs=None, suppress_ragged_eofs=suppress_ragged_eofs,
                         do_handshake_on_connect=do_handshake_on_connect, server_side=server_side, _context=self)

    def _createSSLEngine(self):
        trust_managers = [NoVerifyX509TrustManager()]
        if self.verify_mode == CERT_REQUIRED:
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(self._trust_store)
            trust_managers = tmf.getTrustManagers()

        if self._key_managers is None:  # get an e
            self._context.init(_get_openssl_key_manager().getKeyManagers(), trust_managers, None)
        else:
            self._context.init(self._key_managers.getKeyManagers(), trust_managers, None)

        engine = self._context.createSSLEngine()

        if self._ciphers is not None:
            engine.setEnabledCipherSuites(self._ciphers)

        return engine

    def cert_store_stats(self):
        # TODO not sure if we can even get something similar from Java
        return {}

    def load_cert_chain(self, certfile, keyfile=None, password=None):
        self._key_managers = _get_openssl_key_manager(certfile, keyfile, password, _key_store=self._key_store)

    def set_ciphers(self, ciphers):
        # TODO conversion from OpenSSL to http://www.iana.org/assignments/tls-parameters/tls-parameters.xml
        # as Java knows no other
        #self._ciphers = ciphers
        pass

    def load_verify_locations(self, cafile=None, capath=None, cadata=None):
        if cafile is not None:
            with open(cafile) as f:
                self._load_certificates(f)
        if capath is not None:
            for fname in os.listdir(capath):
                _, ext = os.path.splitext()
                if ext.lower() == 'pem':
                    with open(os.path.join(capath, fname)) as f:
                        self._load_certificates(f)
        if cadata is not None:
            self._load_certificates(f)

    @raises_java_exception
    def _load_certificates(self, f):
        cf = CertificateFactory.getInstance("X.509")
        try:
            for cert in cf.generateCertificates(BufferedInputStream(f)):
                self._trust_store.setCertificateEntry(str(uuid.uuid4()), cert)
        except CertificateParsingException:
            log.debug("Failed to parse certificate", exc_info=True)
            raise

    def load_default_certs(self, purpose=None):
        # TODO handle/support purpose
        self.set_default_verify_paths()

    def set_default_verify_paths(self):
        """
        Load a set of default "certification authority" (CA) certificates from a filesystem path defined when building
        the OpenSSL library. Unfortunately, there's no easy way to know whether this method succeeds: no error is
        returned if no certificates are to be found. When the OpenSSL library is provided as part of the operating
        system, though, it is likely to be configured properly.
        """
        # TODO not implemented, we want to use some default Java's loading method.
        return None

    def set_alpn_protocols(self, protocols):
        raise NotImplementedError()

    def set_npn_protocols(self, protocols):
        raise NotImplementedError()

    def set_servername_callback(self, server_name_callback):
        raise NotImplementedError()

    def load_dh_params(self, dhfile):
        # TODO?
        pass

    def set_ecdh_curve(self, curve_name):
        # TODO?
        pass

    def get_ca_certs(self, binary_form=False):
        """get_ca_certs(binary_form=False) -> list of loaded certificate

        Returns a list of dicts with information of loaded CA certs. If the optional argument is True,
        returns a DER-encoded copy of the CA certificate.
        NOTE: Certificates in a capath directory aren't loaded unless they have been used at least once.
        """
        if binary_form:
            raise NotImplementedError()

        certs = []
        enumerator = self._trust_store.aliases()
        while enumerator.hasMoreElements():
            alias = enumerator.next()
            if self._trust_store.isCertificateEntry(alias):
                cert = self._trust_store.getCertificate(alias)
                issuer_info = self._parse_dn(cert.issuerDN)
                subject_info = self._parse_dn(cert.subjectDN)

                cert_info = {'issuer': issuer_info, 'subject': subject_info}
                for k in ('notBefore', 'serialNumber', 'notAfter', 'version'):
                    cert_info[k] = getattr(cert, k)

                certs.append(cert_info)

        return certs

    @classmethod
    def _parse_dn(cls, dn):
        dn_lst = []

        ln = LdapName(unicode(dn))
        ln_iter = ln.getAll()
        try:
            ln_value = ln_iter.nextElement()
            while ln_value:
                dn_lst.append(tuple(ln_value.split('=', 1)))

                ln_value = ln_iter.nextElement()
        except NoSuchElementException:
            pass

        return tuple(dn_lst)
