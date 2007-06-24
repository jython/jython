"""
AMAK: 20070515: New select implementation that uses java.nio
"""

import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
from java.nio.channels.SelectionKey import OP_ACCEPT, OP_CONNECT, OP_WRITE, OP_READ

import socket

try:
    import errno
    ERRNO_EINVAL      = errno.EINVAL
    ERRNO_ENOTSOCK    = errno.ENOTSOCK
except ImportError:
    ERRNO_EINVAL      = 22
    ERRNO_ENOTSOCK    = 88

class error(Exception): pass

POLLIN   = 1
POLLOUT  = 2

# The following event types are completely ignored on jython
# Java does not support them, AFAICT
# They are declared only to support code compatibility with cpython

POLLPRI  = 4
POLLERR  = 8
POLLHUP  = 16
POLLNVAL = 32

class poll:

    def __init__(self):
        self.selector = java.nio.channels.Selector.open()
        self.chanmap = {}
        self.unconnected_sockets = []

    def _getselectable(self, socket_object):
        for st in socket.SocketTypes:
            if isinstance(socket_object, st):
                try:
                    return socket_object.getchannel()
                except:
                    return None
        raise error("Object '%s' is not watchable" % socket_object, ERRNO_ENOTSOCK)

    def _register_channel(self, socket_object, channel, mask):
        jmask = 0
        if mask & POLLIN:
            # Note that OP_READ is NOT a valid event on server socket channels.
            if channel.validOps() & OP_ACCEPT:
                jmask = OP_ACCEPT
            else:
                jmask = OP_READ
        if mask & POLLOUT:
            jmask |= OP_WRITE
            if channel.validOps() & OP_CONNECT:
                jmask |= OP_CONNECT
        selectionkey = channel.register(self.selector, jmask)
        self.chanmap[channel] = (socket_object, selectionkey)

    def _check_unconnected_sockets(self):
        temp_list = []
        for socket_object, mask in self.unconnected_sockets:
            channel = self._getselectable(socket_object)
            if channel is not None:
                self._register_channel(socket_object, channel, mask)
            else:
                temp_list.append( (socket_object, mask) )
        self.unconnected_sockets = temp_list

    def register(self, socket_object, mask = POLLIN|POLLOUT|POLLPRI):
        channel = self._getselectable(socket_object)
        if channel is None:
            # The socket is not yet connected, and thus has no channel
            # Add it to a pending list, and return
            self.unconnected_sockets.append( (socket_object, mask) )
            return
        self._register_channel(socket_object, channel, mask)

    def unregister(self, socket_object):
        channel = self._getselectable(socket_object)
        self.chanmap[channel][1].cancel()
        del self.chanmap[channel]

    def _dopoll(self, timeout):
        if timeout is None or timeout < 0:
            self.selector.select()
        else:
            try:
                timeout = int(timeout)
                if timeout == 0:
                    self.selector.selectNow()
                else:
                    # No multiplication required: both cpython and java use millisecond timeouts
                    self.selector.select(timeout)
            except ValueError, vx:
                raise error("poll timeout must be a number of milliseconds or None", ERRNO_EINVAL)
        # The returned selectedKeys cannot be used from multiple threads!
        return self.selector.selectedKeys()

    def poll(self, timeout=None):
        self._check_unconnected_sockets()
        selectedkeys = self._dopoll(timeout)
        results = []
        for k in selectedkeys.iterator():
            jmask = k.readyOps()
            pymask = 0
            if jmask & OP_READ: pymask |= POLLIN
            if jmask & OP_WRITE: pymask |= POLLOUT
            if jmask & OP_ACCEPT: pymask |= POLLIN
            if jmask & OP_CONNECT: pymask |= POLLOUT
            # Now return the original userobject, and the return event mask
            results.append( (self.chanmap[k.channel()][0], pymask) )
        return results

    def close(self):
        for k in self.selector.keys():
            k.cancel()
        self.selector.close()

def _calcselecttimeoutvalue(value):
    if value is None:
        return None
    try:
        floatvalue = float(value)
    except Exception, x:
        raise TypeError("Select timeout value must be a number or None")
    if value < 0:
        raise error("Select timeout value cannot be negative", ERRNO_EINVAL)
    if floatvalue < 0.000001:
        return 0
    return int(floatvalue * 1000) # Convert to milliseconds

def select ( read_fd_list, write_fd_list, outofband_fd_list, timeout=None):
    timeout = _calcselecttimeoutvalue(timeout)
    # First create a poll object to do the actual watching.
    pobj = poll()
    already_registered = {}
    # Check the read list
    try:
        # AMAK: Need to remove all this list searching, change to a dictionary?
        for fd in read_fd_list:
            mask = POLLIN
            if fd in write_fd_list:
                mask |= POLLOUT
            pobj.register(fd, mask)
            already_registered[fd] = 1
        # And now the write list
        for fd in write_fd_list:
            if not already_registered.has_key(fd):
                pobj.register(fd, POLLOUT)
        results = pobj.poll(timeout)
    except AttributeError, ax:
        if str(ax) == "__getitem__":
            raise TypeError(ax)
        raise ax
    # Now start preparing the results
    read_ready_list, write_ready_list, oob_ready_list = [], [], []
    for fd, mask in results:
        if mask & POLLIN:
            read_ready_list.append(fd)
        if mask & POLLOUT:
            write_ready_list.append(fd)
    pobj.close()
    return read_ready_list, write_ready_list, oob_ready_list

