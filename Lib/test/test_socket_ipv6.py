"""
AMAK: 20080714: 
This module contains IPv6 related tests.
I have placed them in a separate module from the rest of the socket tests, because
1. The tests pass on my IPv6 enabled windows box, on JVMs >= 1.5
2. They don't pass on Ubuntu, on any JVM version. The equivalent java code to lookup
   IPv6 addresses doesn't work either, so I need to research how to configure Ubuntu
   so that java.net.Inet6Address.getAllByName() actually returns IPv6 addresses.
3. I don't want to include these tests with the standard socket tests until 
   the network status and config of the various test environments, e.g. Mac, BSD, Solaris, etc,
   are known.
"""

import unittest
import test_support

import socket

class NameLookupTests(unittest.TestCase):

    def testLocalhostV4Lookup(self):
        results = socket.getaddrinfo("localhost", 80, socket.AF_INET, socket.SOCK_STREAM, 0)
        self.failUnlessEqual(len(results), 1)
        self.failUnlessEqual('127.0.0.1', results[0][4][0])

    def testRemoteV4Lookup(self):
        results = socket.getaddrinfo("www.python.org", 80, socket.AF_INET, socket.SOCK_STREAM, 0)
        self.failUnlessEqual(len(results), 1)
        self.failUnlessEqual('82.94.237.218', results[0][4][0])

    def testLocalhostV6Lookup(self):
        results = socket.getaddrinfo("localhost", 80, socket.AF_INET6, socket.SOCK_STREAM, 0)
        self.failUnlessEqual(len(results), 1)
        self.failUnlessEqual('0:0:0:0:0:0:0:1', results[0][4][0])

    def testRemoteV6Lookup(self):
        # This test relies on those nice Dutch folks at sixxs.org keeping the same IPv6 address for their gateway
        results = socket.getaddrinfo("www.python.org.sixxs.org", 80, socket.AF_INET6, socket.SOCK_STREAM, 0)
        self.failUnlessEqual(len(results), 1)
        self.failUnlessEqual('2001:838:2:1:2a0:24ff:feab:3b53', results[0][4][0])

    def testLocalhostV4AndV6Lookup(self):
        results = socket.getaddrinfo("localhost", 80, socket.AF_UNSPEC, socket.SOCK_STREAM, 0)
        self.failUnlessEqual(len(results), 2)

    def testRemoteV4AndV6Lookup(self):
        # Need a remote host with both IPv4 and IPv6 addresses; pass for now
        pass

    def testAI_PASSIVE(self):
        pass

def test_main():
    tests = [
        NameLookupTests, 
    ]
    suites = [unittest.makeSuite(klass, 'test') for klass in tests]
    test_support.run_suite(unittest.TestSuite(suites))

if __name__ == "__main__":
    test_main()
