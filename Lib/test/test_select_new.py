"""
AMAK: 20050515: This module is a brand new test_select module, which gives much wider coverage.
"""

import sys
import time
import unittest

import socket
import select

NOT_READY, READY = 0, 1

SERVER_ADDRESS = ("localhost", 54321)

DATA_CHUNK_SIZE = 1000 ; DATA_CHUNK = "." * DATA_CHUNK_SIZE

#
# The timing of these tests depends on the how the unerlying OS socket library
# handles buffering. These values may need tweaking for different platforms
#
# The fundamental problem is that there is no reliable way to fill a socket with bytes
#

if sys.platform[:4] == 'java':
    SELECT_TIMEOUT = 0
else:
    # zero select timeout fails these tests on cpython (on windows 2003 anyway)
    SELECT_TIMEOUT = 0.001

READ_TIMEOUT = 5

class AsynchronousServer:

    def __init__(self):
        self.server_socket = None

    def create_socket(self):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setblocking(0)
        self.server_socket.bind(SERVER_ADDRESS)
        self.server_socket.listen(5)
        try:
            self.server_socket.accept()
        except socket.error:
            pass

    def verify_acceptable_status(self, expected_acceptability):
        actual_acceptability = NOT_READY
        rfds, wfds, xfds = select.select([self.server_socket], [], [], SELECT_TIMEOUT)
        if self.server_socket in rfds:
            actual_acceptability = READY
        assert actual_acceptability == expected_acceptability, "Server socket should %sbe acceptable" % {NOT_READY:'not ',READY:''}[expected_acceptability]

    def accept_connection(self):
        rfds, wfds, xfds = select.select([self.server_socket], [], [], SELECT_TIMEOUT)
        assert self.server_socket in rfds, "Server socket had no pending connections"
        new_socket, address = self.server_socket.accept()
        return AsynchronousHandler(new_socket)

    def close(self):
        self.server_socket.close()

class PeerImpl:

    def fill_outchannel(self):
        """
            This implementation is sub-optimal.
            It is reliant on how the OS handles the socket buffers.
        """
        total_bytes = 0
        while 1:
            try:
                rfds, wfds, xfds = select.select([], [self.socket], [], SELECT_TIMEOUT)
                if self.socket in wfds:
                    bytes_sent = self.socket.send(DATA_CHUNK)
                    total_bytes += bytes_sent
                else:
                    return total_bytes
            except socket.error, se:
                if se.value == 10035:
                    continue
                raise se

    def read_inchannel(self, expected):
        buf_size = expected ; results = "" ; start = time.time()
        while 1:
            if (expected - len(results)) < buf_size:
                buf_size = expected - len(results)
            rfds, wfds, xfds = select.select([self.socket], [], [], SELECT_TIMEOUT)
            if self.socket in rfds:
                recvd_bytes = self.socket.recv(buf_size)
                if len(recvd_bytes):
                    results += recvd_bytes
                if len(results) == expected:
                    return results
            else:
                stop = time.time()
                if (stop - start) > READ_TIMEOUT:
                    raise Exception("Exceeded alloted time (%1.3lf > %1.3lf) to read %d bytes: got %d" % ((stop-start), READ_TIMEOUT, expected, len(results)))

    def verify_status(self, expected_readability, expected_writability):
        actual_readability, actual_writability = NOT_READY, NOT_READY
        rfds, wfds, xfds = select.select([self.socket], [self.socket], [], SELECT_TIMEOUT)
        if self.socket in rfds:
            actual_readability = READY
        if self.socket in wfds:
            actual_writability = READY
        assert actual_readability == expected_readability, "Socket should %sbe ready for reading: %s" % ({NOT_READY:'not ',READY:''}[expected_readability], rfds)
        assert actual_writability == expected_writability, "Socket should %sbe ready for writing: %s" % ({NOT_READY:'not ',READY:''}[expected_writability], wfds)

    def fileno(self):
        return self.socket.fileno()

    def close(self):
        self.socket.close()

class AsynchronousHandler(PeerImpl):

    def __init__(self, new_socket):
        self.socket = new_socket
        self.socket.setblocking(0)

class AsynchronousClient(PeerImpl):

    def __init__(self):
        self.socket = None
        self.connected = 0

    def create_socket(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setblocking(0)

    def start_connect(self):
        result = self.socket.connect_ex(SERVER_ADDRESS)
        if result == 0:
            self.connected = 1

    def finish_connect(self):
        if self.connected:
            return
        rfds, wfds, xfds = select.select([], [self.socket], [], SELECT_TIMEOUT)
        assert self.socket in wfds, "Client socket incomplete connect"

def log(message):
    print message

class TestSelect(unittest.TestCase):

    def test000_CreateSockets(self):
        # Create the server
        TestSelect.server_socket = AsynchronousServer()
        TestSelect.server_socket.create_socket()

        # Create the client
        TestSelect.client_socket = AsynchronousClient()
        TestSelect.client_socket.create_socket()

    def test100_ServerSocketNoPendingConnections(self):
        # Check the server is not marked "acceptable"
        TestSelect.server_socket.verify_acceptable_status(NOT_READY)

    def test110_ServerSocketPendingConnections(self):
        # Start the client connection process
        TestSelect.client_socket.start_connect()
        # Check the server is now acceptable
        TestSelect.server_socket.verify_acceptable_status(READY)

    def test120_ServerSocketNoPendingConnection(self):
        TestSelect.handler_socket = TestSelect.server_socket.accept_connection()
        TestSelect.server_socket.verify_acceptable_status(NOT_READY)

    def test130_EmptyChannel(self):
        # Finish the connection
        TestSelect.client_socket.finish_connect()

    #
    # Test the client-out -> handler-in channel on its own
    #

    def test200_EmptyChannel(self):
        # And now test the status of both end of the socket
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    def test210_FullChannel(self):
        TestSelect.num_bytes_outstanding = TestSelect.client_socket.fill_outchannel()
        TestSelect.client_socket.verify_status(NOT_READY, NOT_READY)
        TestSelect.handler_socket.verify_status(READY, READY)

    def test220_PartiallyFullChannel(self):
        # Half empty the channel
        num_bytes_to_retrieve = TestSelect.num_bytes_outstanding / 2
        bytes_retrieved = TestSelect.handler_socket.read_inchannel(num_bytes_to_retrieve)
        TestSelect.num_bytes_outstanding -= len(bytes_retrieved)
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(READY, READY)

    def test230_EmptyChannel(self):
        # Empty the channel
        bytes_retrieved = TestSelect.handler_socket.read_inchannel(TestSelect.num_bytes_outstanding)
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    #
    # Test the handler-out -> client-in channel on its own
    #

    def test300_EmptyChannel(self):
        # And now test the status of both end of the socket
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    def test310_FullChannel(self):
        TestSelect.num_bytes_outstanding = TestSelect.handler_socket.fill_outchannel()
        TestSelect.client_socket.verify_status(READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, NOT_READY)

    def test320_PartiallyFullChannel(self):
        # Half empty the channel
        num_bytes_to_retrieve = TestSelect.num_bytes_outstanding / 2
        bytes_retrieved = TestSelect.client_socket.read_inchannel(num_bytes_to_retrieve)
        TestSelect.num_bytes_outstanding -= len(bytes_retrieved)
        TestSelect.client_socket.verify_status(READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    def test330_EmptyChannel(self):
        # Empty the channel
        TestSelect.client_socket.read_inchannel(TestSelect.num_bytes_outstanding)
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    #
    # Test both channels active at the same time
    #

    def test400_EmptyChannels(self):
        # And now test the status of both end of the socket
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    def test410_FullChannels(self):
        TestSelect.num_bytes_outstanding_c = TestSelect.client_socket.fill_outchannel()
        TestSelect.num_bytes_outstanding_h = TestSelect.handler_socket.fill_outchannel()
        TestSelect.client_socket.verify_status(READY, NOT_READY)
        TestSelect.handler_socket.verify_status(READY, NOT_READY)

    def est420_PartiallyFullChannels(self):
        # Half empty the channel
        num_bytes_to_retrieve_c = TestSelect.num_bytes_outstanding_c / 2
        num_bytes_to_retrieve_h = TestSelect.num_bytes_outstanding_h / 2
        bytes_retrieved_c = TestSelect.client_socket.read_inchannel(num_bytes_to_retrieve_c)
        bytes_retrieved_h = TestSelect.handler_socket.read_inchannel(num_bytes_to_retrieve_h)
        TestSelect.num_bytes_outstanding_c -= len(bytes_retrieved_c)
        TestSelect.num_bytes_outstanding_h -= len(bytes_retrieved_h)
        TestSelect.client_socket.verify_status(READY, READY)
        TestSelect.handler_socket.verify_status(READY, READY)

    def test430_EmptyChannels(self):
        # Empty the channel
        TestSelect.client_socket.read_inchannel(TestSelect.num_bytes_outstanding_c)
        TestSelect.handler_socket.read_inchannel(TestSelect.num_bytes_outstanding_h)
        TestSelect.client_socket.verify_status(NOT_READY, READY)
        TestSelect.handler_socket.verify_status(NOT_READY, READY)

    #
    # Now close the whole lot down
    #

    def test99999_CloseSockets(self):
        TestSelect.client_socket.close()
        TestSelect.handler_socket.close()
        TestSelect.server_socket.close()

if __name__ == "__main__":
    unittest.main()
