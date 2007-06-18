"""
AMAK: 20050515: This module is the test_select.py from cpython 2.4, ported to jython + unittest
"""

try:
    object
except NameError:
    class object: pass

import socket, select

import os
import sys
import unittest

class SelectWrapper:

    def __init__(self):
        self.read_fds = []
        self.write_fds = []
        self.oob_fds = []
        self.timeout = None

    def add_read_fd(self, fd):
        self.read_fds.append(fd)

    def add_write_fd(self, fd):
        self.write_fds.append(fd)

    def add_oob_fd(self, fd):
        self.oob_fds.append(fd)

    def set_timeout(self, timeout):
        self.timeout = timeout

class PollWrapper:

    def __init__(self):
        self.timeout = None
        self.poll_object = select.poll()

    def add_read_fd(self, fd):
        self.poll_object.register(fd, select.POLL_IN)

    def add_write_fd(self, fd):
        self.poll_object.register(fd, select.POLL_OUT)

    def add_oob_fd(self, fd):
        self.poll_object.register(fd, select.POLL_PRI)

class TestSelectInvalidParameters(unittest.TestCase):

    def testBadSelectSetTypes(self):
        # Test some known error conditions
        for bad_select_set in [None, 1,]:
            for pos in range(2): # OOB not supported on Java
                args = [[], [], []]
                args[pos] = bad_select_set
                try:
                    timeout = 0 # Can't wait forever
                    rfd, wfd, xfd = select.select(args[0], args[1], args[2], timeout)
                except TypeError:
                    pass
                else:
                    self.fail("Selecting on '%s' should have raised TypeError" % str(bad_select_set))

    def testBadSelectableTypes(self):
        class Nope: pass

        class Almost1:
            def fileno(self):
                return 'fileno'

        class Almost2:
            def fileno(self):
                return 'fileno'

        # Test some known error conditions
        for bad_selectable in [None, 1, object(), Nope(), Almost1(), Almost2()]:
            try:
                timeout = 0 # Can't wait forever
                rfd, wfd, xfd = select.select([bad_selectable], [], [], timeout)
            except (TypeError, select.error), x:
                pass
            else:
                self.fail("Selecting on '%s' should have raised TypeError or select.error" % str(bad_selectable))

    def testInvalidTimeoutTypes(self):
        for invalid_timeout in ['not a number']:
            try:
                rfd, wfd, xfd = select.select([], [], [], invalid_timeout)
            except TypeError:
                pass
            else:
                self.fail("Invalid timeout value '%s' should have raised TypeError" % invalid_timeout)

    def testInvalidTimeoutValues(self):
        for invalid_timeout in [-1]:
            try:
                rfd, wfd, xfd = select.select([], [], [], invalid_timeout)
            except (ValueError, select.error):
                pass
            else:
                self.fail("Invalid timeout value '%s' should have raised ValueError or select.error" % invalid_timeout)

class TestSelectClientSocket(unittest.TestCase):

    def testUnconnectedSocket(self):
        sockets = [socket.socket(socket.AF_INET, socket.SOCK_STREAM) for x in range(5)]
        for pos in range(2): # OOB not supported on Java
            args = [[], [], []]
            args[pos] = sockets
            timeout = 0 # Can't wait forever
            rfd, wfd, xfd = select.select(args[0], args[1], args[2], timeout)
            for s in sockets:
                self.failIf(s in rfd)
                self.failIf(s in wfd)

def check_server_running_on_localhost_port(port_number):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        s.connect( ('localhost', port_number) )
        s.close()
    except:
        return 0
    return 1

class TestPollClientSocket(unittest.TestCase):

    def testEventConstants(self):
        for event_name in ['IN', 'OUT', 'PRI', 'ERR', 'HUP', 'NVAL', ]:
            self.failUnless(hasattr(select, 'POLL%s' % event_name))

    def testSocketRegisteredBeforeConnected(self):
        # You MUST be running a server on port 80 for this one to work
        if not check_server_running_on_localhost_port(80):
            print "Unable to run testSocketRegisteredBeforeConnected: no server on port 80"
            return
        sockets = [socket.socket(socket.AF_INET, socket.SOCK_STREAM) for x in range(5)]
        timeout = 1 # Can't wait forever
        poll_object = select.poll()
        for s in sockets:
            # Register the sockets before they are connected
            poll_object.register(s, select.POLLOUT)
        result_list = poll_object.poll(timeout)
        result_sockets = [r[0] for r in result_list]
        for s in sockets:
            self.failIf(s in result_sockets)
        # Now connect the sockets, but DO NOT register them again
        for s in sockets:
            s.setblocking(0)
            s.connect( ('localhost', 80) )
        # Now poll again, to see if the poll object has recognised that the sockets are now connected
        result_list = poll_object.poll(timeout)
        result_sockets = [r[0] for r in result_list]
        for s in sockets:
            self.failUnless(s in result_sockets)

    def testUnregisterRaisesKeyError(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        poll_object = select.poll()
        try:
            poll_object.unregister(s)
        except KeyError:
            pass
        else:
            self.fail("Unregistering socket that is not registered should have raised KeyError")

class TestPipes(unittest.TestCase):

    verbose = 1

    def test(self):
        import sys
        from test.test_support import verbose
        if sys.platform[:3] in ('win', 'mac', 'os2', 'riscos'):
            if verbose:
                print "Can't test select easily on", sys.platform
            return
        cmd = 'for i in 0 1 2 3 4 5 6 7 8 9; do echo testing...; sleep 1; done'
        p = os.popen(cmd, 'r')
        for tout in (0, 1, 2, 4, 8, 16) + (None,)*10:
            if verbose:
                print 'timeout =', tout
            rfd, wfd, xfd = select.select([p], [], [], tout)
            if (rfd, wfd, xfd) == ([], [], []):
                continue
            if (rfd, wfd, xfd) == ([p], [], []):
                line = p.readline()
                if verbose:
                    print repr(line)
                if not line:
                    if verbose:
                        print 'EOF'
                    break
                continue
            self.fail('Unexpected return values from select(): %s' % str(rfd, wfd, xfd))
        p.close()

def test_main():
    tests = [
        TestSelectInvalidParameters,
        TestSelectClientSocket,
        TestPollClientSocket,
    ]
    if sys.platform[:4] != 'java':
        tests.append(TestPipes)
    suites = [unittest.makeSuite(klass, 'test') for klass in tests]
    main_suite = unittest.TestSuite(suites)
    runner = unittest.TextTestRunner(verbosity=100)
    runner.run(main_suite)

if __name__ == "__main__":
    test_main()
