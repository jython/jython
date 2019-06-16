# Jython variations on the test_ssl tests are concentrated here, where possible
# Due to the structure of the module, some functions still have to be modified 
# directly in the original, but this reduces the diff and ongoing merge effort

import errno
import select
import socket
import ssl
import sys
import unittest
import test.test_ssl
from test.test_ssl import BasicTests
from test.test_ssl import can_clear_options, support, skip_if_broken_ubuntu_ssl
from test.test_ssl import CAPATH, CERTFILE, CAFILE_CACERT
from test.test_ssl import REMOTE_HOST, REMOTE_ROOT_CERT



class BasicSocketTests(test.test_ssl.BasicSocketTests):
    @unittest.skip("Jython does not have _ssl, therefore this test needs to be rewritten")
    def test_parse_cert(self):
        None

    @unittest.skip("Jython does not have _ssl, therefore this test needs to be rewritten")
    def test_parse_cert_CVE_2013_4238(self):
        None

    @unittest.skip("Jython does not have _ssl, therefore this test needs to be rewritten")
    def test_parse_cert_CVE_2019_5010(self):
        None

    @unittest.skip("Jython does not have _ssl, therefore this test needs to be rewritten")
    def test_parse_all_sans(self):
        None

    def test_asn1object(self):
        # Abbreviated version of parent test up to unsupported part
        # TODO Jython better asn1 support, though not sure there's much use for 
        # it
        expected = (129, 'serverAuth', 'TLS Web Server Authentication',
                    '1.3.6.1.5.5.7.3.1')

        val = ssl._ASN1Object('1.3.6.1.5.5.7.3.1')
        self.assertEqual(val, expected)
        self.assertEqual(val.nid, 129)
        self.assertEqual(val.shortname, 'serverAuth')
        self.assertEqual(val.longname, 'TLS Web Server Authentication')
        self.assertEqual(val.oid, '1.3.6.1.5.5.7.3.1')
        self.assertIsInstance(val, ssl._ASN1Object)
        self.assertRaises(ValueError, ssl._ASN1Object, 'serverAuth')


class ContextTests(test.test_ssl.ContextTests):
    @unittest.skip("Currently not supported")
    def test_ciphers(self):
        None

    @skip_if_broken_ubuntu_ssl
    def test_options(self):
        # new default options in later 2.7 versions not yet supported 
        # See CPython b8eaec697a2b5d9d2def2950a0aa50e8ffcf1059
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLSv1)
        # OP_ALL | OP_NO_SSLv2 | OP_NO_SSLv3 is the default value
        self.assertEqual(ssl.OP_ALL | ssl.OP_NO_SSLv2 | ssl.OP_NO_SSLv3,
                         ctx.options)
        ctx.options |= ssl.OP_NO_TLSv1
        self.assertEqual(ssl.OP_ALL | ssl.OP_NO_SSLv2 | ssl.OP_NO_SSLv3 | ssl.OP_NO_TLSv1,
                         ctx.options)
        if can_clear_options():
            ctx.options = (ctx.options & ~ssl.OP_NO_SSLv2) | ssl.OP_NO_TLSv1
            self.assertEqual(ssl.OP_ALL | ssl.OP_NO_TLSv1 | ssl.OP_NO_SSLv3,
                             ctx.options)
            ctx.options = 0
            self.assertEqual(0, ctx.options)
        else:
            with self.assertRaises(ValueError):
                ctx.options = 0

    @unittest.skip("Not yet supported on Jython")
    def test_load_dh_params(self):
        None

    def test_cert_store_stats(self):
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLSv1)
        self.assertEqual(ctx.cert_store_stats(),
            {'x509_ca': 0, 'crl': 0, 'x509': 0})
        # Jython x509 will grow by 1 while openssl remains 0
        # TODO investgate deeper
        ctx.load_cert_chain(CERTFILE)
        self.assertEqual(ctx.cert_store_stats(),
            {'x509_ca': 0, 'crl': 0, 'x509': 1})
        ctx.load_verify_locations(CERTFILE)
        self.assertEqual(ctx.cert_store_stats(),
            {'x509_ca': 0, 'crl': 0, 'x509': 2})
        ctx.load_verify_locations(CAFILE_CACERT)
        self.assertEqual(ctx.cert_store_stats(),
            {'x509_ca': 1, 'crl': 0, 'x509': 2})

    @unittest.skipIf(sys.platform == "win32", "not-Windows specific")
    def test_load_default_certs_env(self):
        # Store behaviour differs from CPython so different stats
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLSv1)
        with support.EnvironmentVarGuard() as env:
            env["SSL_CERT_DIR"] = CAPATH
            env["SSL_CERT_FILE"] = CERTFILE
            ctx.load_default_certs()
            self.assertEqual(ctx.cert_store_stats(), {"crl": 0, "x509": 5, "x509_ca": 0})

    def _assert_context_options(self, ctx):
        self.assertEqual(ctx.options & ssl.OP_NO_SSLv2, ssl.OP_NO_SSLv2)
        # Jython doesn't support OP_NO_COMPRESSION, OP_SINGLE_DH_USE
        # OP_SINGLE_ECDH_USE, OP_CIPHER_SERVER_PREFERENCE 

    @unittest.skip("Jython not using ssl.__https_verify_certificates ")
    def test__https_verify_certificates(self):
        None

    @unittest.skip("Jython not using ssl._https_verify_envvar ")
    def test__https_verify_envvar(self):
        None


class SSLErrorTests(test.test_ssl.SSLErrorTests):
    def test_str(self):
        # Different error strings for Jython 
        # The str() of a SSLError doesn't include the errno
        e = ssl.SSLError(1, "foo")
        self.assertIn("foo", str(e))
        self.assertEqual(e.errno, 1)
        # Same for a subclass
        e = ssl.SSLZeroReturnError(1, "foo")
        self.assertIn("foo", str(e))
        self.assertEqual(e.errno, 1)

    @unittest.skip("Jython TODO")
    def test_lib_reason(self):
        None

    @unittest.skip("Jython TODO")
    def test_subclass(self):
        None


class NetworkedTests(test.test_ssl.NetworkedTests):
    def test_connect_ex(self):
        # Issue #11326: check connect_ex() implementation
        with support.transient_internet(REMOTE_HOST):
            s = ssl.wrap_socket(socket.socket(socket.AF_INET),
                                cert_reqs=ssl.CERT_REQUIRED,
                                ca_certs=REMOTE_ROOT_CERT)
            try:
                # Jython, errno.EISCONN expected per earlier 2.x versions, not 0
                self.assertEqual(errno.EISCONN, s.connect_ex((REMOTE_HOST, 443)))
                self.assertTrue(s.getpeercert())
            finally:
                s.close()

    def test_non_blocking_connect_ex(self):
        # Issue #11326: non-blocking connect_ex() should allow handshake
        # to proceed after the socket gets ready.
        # Jython behaviour varies
        with support.transient_internet(REMOTE_HOST):
            s = ssl.wrap_socket(socket.socket(socket.AF_INET),
                                cert_reqs=ssl.CERT_REQUIRED,
                                ca_certs=REMOTE_ROOT_CERT,
                                do_handshake_on_connect=False)
            try:
                s.setblocking(False)
                rc = s.connect_ex((REMOTE_HOST, 443))
                # EWOULDBLOCK under Windows, EINPROGRESS elsewhere
                # Jython added EALREADY, as in Jython connect may have already happened
                self.assertIn(rc, (0, errno.EINPROGRESS, errno.EALREADY, errno.EWOULDBLOCK))
                # Wait for connect to finish
                select.select([], [s], [], 5.0)
                # Non-blocking handshake
                while True:
                    try:
                        s.do_handshake()
                        break
                    except ssl.SSLWantReadError:
                        select.select([s], [], [], 5.0)
                    except ssl.SSLWantWriteError:
                        select.select([], [s], [], 5.0)
                # SSL established - not in Jython
                #self.assertTrue(s.getpeercert())
            finally:
                s.close()

    def test_timeout_connect_ex(self):
        # Issue #12065: on a timeout, connect_ex() should return the original
        # errno (mimicking the behaviour of non-SSL sockets).
        # Jython follows earlier 2.x behaviour of errno.EISCONN 
        # it also allows errno.TIMEDOUT
        with support.transient_internet(REMOTE_HOST):
            s = ssl.wrap_socket(socket.socket(socket.AF_INET),
                                cert_reqs=ssl.CERT_REQUIRED,
                                ca_certs=REMOTE_ROOT_CERT,
                                do_handshake_on_connect=False)
            try:
                s.settimeout(0.0000001)
                rc = s.connect_ex((REMOTE_HOST, 443))
                if rc == errno.EISCONN:
                    self.skipTest("REMOTE_HOST responded too quickly")
                self.assertIn(rc, (errno.ETIMEDOUT, errno.EAGAIN, errno.EWOULDBLOCK))
            finally:
                s.close()

    @unittest.skip("Can't use a socket as a file under Jython")
    def test_makefile_close(self):
        None

    @unittest.skip("Currently not supported")
    def test_ciphers(self):
        None

    @unittest.skip("On jython preloaded TODO")
    def test_get_ca_certs_capath(self):
        None


def test_main(verbose=False):
    tests=[ContextTests, BasicTests, BasicSocketTests, SSLErrorTests]
    if support.is_resource_enabled('network'):
        tests.append(NetworkedTests)
    # Jython skip threading tests for now, really don't work :(
    test.test_ssl.test_main(verbose, tests)


if __name__ == "__main__":
    test_main()

