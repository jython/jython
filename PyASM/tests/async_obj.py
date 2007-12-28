from __future__ import with_statement
import new
import threading
from types import MethodType
from contextlib import contextmanager
from Queue import Queue

# proxy support inspired by this recipe http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/519639/

class Future(object):
    """Creates a Future on a method call, that can be used later.

    Until a client uses the reference, not just keeps it around, it
    can be in an unresolved state. The use of a threading.Event
    ensures that it has (atomically) been resolved.
    
    If the user throws away the future ;), we no longer worry
    about getting the result.
    """
        
    def __init__(self):
        self.target = None
        self.event = threading.Event()

    # can I avoid this via __getattribute__ ? that would be yes, just
    # a little more bookkeeping
    def __str__(self):
        self.event.wait()
        target = self.target
        return self.target.__str__()

    def __getattr__(self, aname):
        self.event.wait()
        target = self.target

        f = getattr(target, aname)
        if isinstance(f, MethodType):
            # Rebind the method to the target.
            return new.instancemethod(f.im_func, self, target.__class__)
        else:
            return f


@contextmanager
def async(obj):
    """Returns a context such that `obj` runs asynchronously as an actor.

    Adapts a generic object such that it runs asynchronously in a
    thread as aa actor. Objects must be a newclass instance. All
    interaction is queued up via a standard Queue, with instances of
    the Future class returned. Unless the Future is instantiated, no
    waiting occurs.

    TBD: would it be possible to turn this into a class decorator such that
    any newly constructed objects are automatically actors? That would
    be an interesting twist!
    """

    class TerminateCls(object): pass
    TerminateReceiver = TerminateCls()
    incoming = Queue()

    def receiver():
        """Runs `obj` within new thread, adapting via incoming queue"""
        while True:
            msg, future = incoming.get(True)
            if msg == TerminateReceiver:
                break
            else:
                method, args, kw = msg
                future.target = getattr(obj, method)(*args, **kw)
                future.event.set()

    class Proxy(object):
        def __init__(self, target, incoming):
            self._target = target
            self._incoming = incoming

        def __getattr__(self, aname):
            def send(*args, **kw):
                future = Future()
                self._incoming.put(((aname, args, kw), future))
                return future
            return send

    t = threading.Thread(target=receiver).start()
    yield Proxy(obj, incoming)
    incoming.put((TerminateReceiver, None))


def test():
    class X(object):
        def __init__(self, x, y):
            self.x = x
            self.y = y
        def write(self):
            print "x,y", self.x, self.y
        def mult(self, n):
            self.x = n*2
            self.y = n*2
        def get_x(self):
            return self.x

    with async(X(1,2)) as x:
        x.write()
        x.mult(10)
        x.write()
        foo = x.get_x()
        x.mult(5)
        y = x.get_x()
        print "Future %r" % (y,)
        x.write()
        print "y", y

print __name__
if __name__ == '__main__':
    test()

