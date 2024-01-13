import errno
import os
import socket
import ssl
import sys
import threading
import time
import unittest
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
from SocketServer import ThreadingMixIn
from test import test_support
from test.test_socket import SocketConnectedTest, ThreadedUDPSocketTest

def data_file(*name):
    return os.path.join(os.path.dirname(__file__), *name)

CERTFILE = data_file("keycert.pem")
ONLYCERT = data_file("ssl_cert.pem")
ONLYKEY = data_file("ssl_key.pem")

MSG = 'Michael Gilfix was here\n'

def start_server():
    server_address = ('127.0.0.1', 0)

    class DaemonThreadingMixIn(ThreadingMixIn):
        daemon_threads = True

    class ThreadedHTTPServer(DaemonThreadingMixIn, HTTPServer):
        """Handle requests in a separate thread."""

    # not actually going to do anything with this server, so a
    # do-nothing handler is reasonable
    httpd = ThreadedHTTPServer(server_address, BaseHTTPRequestHandler)
    server_thread = threading.Thread(target=httpd.serve_forever)
    server_thread.daemon = True
    server_thread.start()
    return httpd, server_thread


class SocketConnectTest(unittest.TestCase):

    def setUp(self):
        self.httpd, self.server_thread = start_server()
        self.address = self.httpd.server_name, self.httpd.server_port

    def tearDown(self):
        self.httpd.shutdown()
        self.server_thread.join()

    def do_nonblocking_connection(self, results, index):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setblocking(0)
        connect_errno = 0
        connect_attempt = 0

        while connect_errno != errno.EISCONN and connect_attempt < 10000:
            connect_attempt += 1
            connect_errno = sock.connect_ex(self.address)
            results[index].append(connect_errno)
            time.sleep(0.01)
        sock.close()

    def do_workout(self, num_threads=10):
        connect_results = []
        connect_threads = []
        for i in xrange(num_threads):
            connect_results.append([])
            connect_threads.append(threading.Thread(
                target=self.do_nonblocking_connection,
                name="socket-workout-%s" % i,
                args=(connect_results, i)))

        for thread in connect_threads:
            thread.start()
        for thread in connect_threads:
            thread.join()
        return connect_results

    def test_connect_ex_workout(self):
        """Verify connect_ex states go through EINPROGRESS?, EALREADY*, EISCONN"""
        # Tests fix for http://bugs.jython.org/issue2428; based in part on the
        # code showing failure that was submitted with that bug
        for result in self.do_workout():
            if len(result) == 0: self.fail("A socket-workout thread failed to run")
            self.assertIn(result[0], {errno.EINPROGRESS, errno.EISCONN})
            self.assertEqual(result[-1], errno.EISCONN)
            for code in result[1:-1]:
                self.assertEqual(code, errno.EALREADY)


class SSLSocketConnectTest(unittest.TestCase):

    def setUp(self):
        self.httpd, self.server_thread = start_server()
        self.httpd.socket = ssl.wrap_socket(
            self.httpd.socket,
            certfile=ONLYCERT,
            server_side=True,
            keyfile=ONLYKEY,
        )
        self.address = self.httpd.server_name, self.httpd.server_port

    def tearDown(self):
        self.httpd.shutdown()
        self.server_thread.join()

    def do_nonblocking_connection(self, results, index):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.setblocking(0)
        connect_errno = 0
        connect_attempt = 0
        sock = ssl.wrap_socket(sock, certfile=CERTFILE, do_handshake_on_connect=True)

        while connect_errno != errno.EISCONN and connect_attempt < 10000:
            connect_attempt += 1
            connect_errno = sock.connect_ex(self.address)
            results[index].append(connect_errno)
            time.sleep(0.01)
        sock.close()

    def do_workout(self, num_threads=10):
        connect_results = []
        connect_threads = []
        for i in xrange(num_threads):
            connect_results.append([])
            connect_threads.append(threading.Thread(
                target=self.do_nonblocking_connection,
                name="socket-workout-%s" % i,
                args=(connect_results, i)))

        for thread in connect_threads:
            thread.start()
        for thread in connect_threads:
            thread.join()
        return connect_results

    @unittest.skipIf(test_support.is_jython and test_support.get_java_version() >= (9,),  # FIXME
                     "Fails on Java 9+. See b.j.o. issue #2710")
    def test_connect_ex_workout(self):
        """Verify connect_ex states go through EINPROGRESS?, EALREADY*, EISCONN"""
        # Tests fix for http://bugs.jython.org/issue2428; based in part on the
        # code showing failure that was submitted with that bug
        for result in self.do_workout():
            if len(result) == 0: self.fail("A socket-workout thread failed to run")
            self.assertIn(result[0], {errno.EINPROGRESS, errno.EISCONN})
            self.assertEqual(result[-1], errno.EISCONN)
            for code in result[1:-1]:
                self.assertEqual(code, errno.EALREADY)


class SocketOptionsTest(unittest.TestCase):

    def test_socket_options_defined(self):
        # Basic existence test to verify trivial fix for
        # http://bugs.jython.org/issue2436
        self.assertEqual(socket.SOL_TCP, socket.IPPROTO_TCP)


class TimedBasicTCPTest(SocketConnectedTest):

    BIG_SIZE = test_support.SOCK_MAX_SIZE // 3 + 1

    def __init__(self, methodName='runTest'):
        SocketConnectedTest.__init__(self, methodName=methodName)

    def receiveAll(self):
        # Testing sendall() with a max-size string over TCP
        msg = bytearray()
        t0 = time.clock()
        while 1:
            read = self.cli_conn.recv(8192)
            if not read:
                break
            msg += read
        t = time.clock() - t0
        if test_support.verbose:
            print>>sys.stderr, "%d bytes in %5.3f sec ... " % (len(msg), t),
        self.assertEqual(''.join(map(chr, sorted(set(msg)))), 'xyz')
        self.assertEqual(len(msg), TimedBasicTCPTest.BIG_SIZE*3)

    def testSendAllBytes(self):
        # Testing sendall() with a max-size string over TCP
        self.receiveAll()

    def _testSendAllBytes(self):
        big = bytearray('xyz') * TimedBasicTCPTest.BIG_SIZE
        self.serv_conn.sendall(big)

    def testSendAllStr(self):
        # Testing sendall() with a max-size string over TCP
        self.receiveAll()

    def _testSendAllStr(self):
        big = 'xyz' * TimedBasicTCPTest.BIG_SIZE
        self.serv_conn.sendall(big)

    def testSendAllBuffer(self):
        # Testing sendall() with a max-size string over TCP
        self.receiveAll()

    def _testSendAllBuffer(self):
        big = buffer('xyz' * TimedBasicTCPTest.BIG_SIZE)
        self.serv_conn.sendall(big)


class BasicTCPUnicodeTest(SocketConnectedTest):

    def __init__(self, methodName='runTest'):
        SocketConnectedTest.__init__(self, methodName=methodName)

    def testRecv(self):
        # Testing large receive over TCP
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testRecv(self):
        self.serv_conn.send(MSG.decode())

    def testRecvTimeoutMode(self):
        # Do this test in timeout mode, because the code path is different
        self.cli_conn.settimeout(10)
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testRecvTimeoutMode(self):
        self.serv_conn.settimeout(10)
        self.serv_conn.send(MSG.decode())

    def testOverFlowRecv(self):
        # Testing receive in chunks over TCP
        seg1 = self.cli_conn.recv(len(MSG) - 3)
        seg2 = self.cli_conn.recv(1024)
        msg = seg1 + seg2
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testOverFlowRecv(self):
        self.serv_conn.send(MSG.decode())

    def testRecvFrom(self):
        # Testing large recvfrom() over TCP
        msg, addr = self.cli_conn.recvfrom(1024)
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testRecvFrom(self):
        self.serv_conn.send(MSG.decode())

    def testOverFlowRecvFrom(self):
        # Testing recvfrom() in chunks over TCP
        seg1, addr = self.cli_conn.recvfrom(len(MSG)-3)
        seg2, addr = self.cli_conn.recvfrom(1024)
        msg = seg1 + seg2
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testOverFlowRecvFrom(self):
        self.serv_conn.send(MSG.decode())

    def testSendAll(self):
        # Testing sendall() with a 2048 byte string over TCP
        msg = ''
        while 1:
            read = self.cli_conn.recv(1024)
            if not read:
                break
            msg += read
        self.assertEqual(msg, 'f' * 2048)
        self.assertEqual(type(msg), str)

    def _testSendAll(self):
        big_chunk = u'f' * 2048
        self.serv_conn.sendall(big_chunk)

    def _testFromFd(self):
        self.serv_conn.send(MSG.decode())

    def testShutdown(self):
        # Testing shutdown()
        msg = self.cli_conn.recv(1024)
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testShutdown(self):
        self.serv_conn.send(MSG.decode())
        self.serv_conn.shutdown(2)

    def testSendAfterRemoteClose(self):
        self.cli_conn.close()

    def _testSendAfterRemoteClose(self):
        for x in range(5):
            try:
                self.serv_conn.send(u"spam")
            except socket.error, se:
                self.failUnlessEqual(se[0], errno.ECONNRESET)
                return
            except Exception, x:
                self.fail("Sending on remotely closed socket raised wrong exception: %s" % x)
            time.sleep(0.5)
        self.fail("Sending on remotely closed socket should have raised exception")

    def testDup(self):
        msg = self.cli_conn.recv(len(MSG))
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

        dup_conn = self.cli_conn.dup()
        msg = dup_conn.recv(len(u'and ' + MSG))
        self.assertEqual(msg, u'and ' +  MSG)
        dup_conn.close()  # need to ensure all sockets are closed

    def _testDup(self):
        self.serv_conn.send(MSG.decode())
        self.serv_conn.send(u'and ' + MSG)


class BasicUDPUnicodeTest(ThreadedUDPSocketTest):

    def __init__(self, methodName='runTest'):
        ThreadedUDPSocketTest.__init__(self, methodName=methodName)

    def testSendtoAndRecv(self):
        # Testing sendto() and recv() over UDP
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testSendtoAndRecv(self):
        self.cli.sendto(MSG.decode(), 0, (self.HOST.decode(), self.PORT))

    def testSendtoAndRecvTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(1)
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testSendtoAndRecvTimeoutMode(self):
        self.cli.settimeout(10)
        self.cli.sendto(MSG.decode(), 0, (self.HOST.decode(), self.PORT))

    def testSendAndRecv(self):
        # Testing send() and recv() over connect'ed UDP
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testSendAndRecv(self):
        self.cli.connect( (self.HOST.decode(), self.PORT) )
        self.cli.send(MSG.decode(), 0)

    def testSendAndRecvTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(5)
        # Testing send() and recv() over connect'ed UDP
        msg = self.serv.recv(len(MSG))
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testSendAndRecvTimeoutMode(self):
        self.cli.connect( (self.HOST.decode(), self.PORT) )
        self.cli.settimeout(5)
        time.sleep(1)
        self.cli.send(MSG.decode(), 0)

    def testRecvFrom(self):
        # Testing recvfrom() over UDP
        msg, addr = self.serv.recvfrom(len(MSG))
        self.assertEqual(msg, MSG)
        self.assertEqual(type(msg), str)

    def _testRecvFrom(self):
        self.cli.sendto(MSG.decode(), 0, (self.HOST.decode(), self.PORT))

    def testRecvFromTimeoutMode(self):
        # Need to test again in timeout mode, which follows
        # a different code path
        self.serv.settimeout(1)
        msg, addr = self.serv.recvfrom(len(MSG))
        self.assertEqual(msg.decode(), MSG)

    def _testRecvFromTimeoutMode(self):
        self.cli.settimeout(1)
        self.cli.sendto(MSG.decode(), 0, (self.HOST.decode(), self.PORT))


def test_main():
    test_support.run_unittest(
            SocketConnectTest,
            SSLSocketConnectTest,
            SocketOptionsTest,
            TimedBasicTCPTest,
            BasicTCPUnicodeTest,
            BasicUDPUnicodeTest,
    )


if __name__ == "__main__":
    test_main()
