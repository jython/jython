import httplib
import socket
import sys
from test import test_support
import unittest


class SocketIPv6Test(unittest.TestCase):

    def test_connect_localhost(self):
        '''Ensures a correct socket.error message'''
        conn = httplib.HTTPConnection('localhost', 18080)
        body = ""
        headers = {}
        try:
            conn.request("GET", "/RELEASE-NOTES.txt", body, headers)
        except socket.error:
            pass # used to get an AssertionError (see bug 1697)


def test_main():
    test_support.run_unittest(SocketIPv6Test)

if __name__ == "__main__":
    test_main()
