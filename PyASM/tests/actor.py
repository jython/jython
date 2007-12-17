from __future__ import with_statement
import new
import sys
import threading
from types import MethodType
from contextlib import contextmanager
from Queue import Queue


class NoneSuch(object):
    def __new__(cls, *args, **kwargs):
        if '_inst' not in vars(cls):
            cls._inst = object.__new__(cls, *args, **kwargs)
        return cls._inst
    def __init__(self, *args, **kwargs): pass
    def __repr__(self): return "NoneSuch()"
    def __call__(self, *args, **kwargs): return self
    def __nonzero__(self): return False

NoneSuch = NoneSuch()


class FutureException(Exception): pass

class Future(object):
    """Creates a Future on a method call, that can be used later.

    Until a client actually uses the reference, the ref can be in an
    unresolved state. The use of a threading.Event ensures that it has
    (atomically) been resolved when it's used.
    
    If the user throws away the future ;), we no longer worry about
    getting the result. The only question is hot to ensure that such
    garbage, including the enclosed event objects, are thrown away in
    a timely fashion. In the typical case, these will be bound to
    local variables, and most code is unlikely to use a large number
    of local variables anyway.

    Java implements support for futures in java.util.concurrent, so it
    would be reasonable to use that functionality instead in a Jython
    implementation, and then layer on it transparency as seen here.

    Other implementations might implement the following alternatives:

    * non-transparent resolution (Java style, via say a get
      method). In general, non-transparency seems to be rather
      interesting because I can always work with the proxy directly if
      desired.

    * proxying all special methods via __getattribute__ (but at the
      cost of some additional bookkeeping)

    TODO: Other special methods need to be proxied as well. __str__
    and __iter__ are just what are necessary for some example code. My
    current thought is to probably use metaclasses since the cost of
    setup is done only in class definition, which seems reasonable
    enough. Note that we still need __getattr__, but that's much more
    efficient (and easier to work with) than __getattribute__.

    proxy support inspired by this recipe
    http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/519639/
    """
        
    def __init__(self, debug=False):
        self.target = None
        self.event = threading.Event()
        self.exception = None
        self.debug = debug

    def __str__(self):
        if self.debug:
            print "Waiting on future"
        self.event.wait()
        if self.exception is not None:
            raise FutureException, self.exception[0], self.exception[1]
        target = self.target
        return self.target.__str__()

    def __iter__(self):
        if self.debug:
            print "Waiting on future"
        self.event.wait()
        if self.exception is not None:
            raise FutureException, self.exception[0], self.exception[1]
        target = self.target
        return self.target.__iter__()

    def __getattr__(self, aname):
        if self.debug:
            print "Waiting on future"
        self.event.wait()
        if self.exception is not None:
            raise FutureException, self.exception[0], self.exception[1]
        target = self.target
        f = getattr(target, aname)
        if isinstance(f, MethodType):
            # Rebind the method to the target.
            return new.instancemethod(f.im_func, self, target.__class__)
        else:
            return f


# TODO: consider exceptions too, how should this be coupled to a
# specific future factory? this might be preferable...

@contextmanager
def acting(obj, make_future=Future, debug=False):
    """Returns a context such that `obj` runs asynchronously as an actor.

    Adapts a generic object such that it runs asynchronously in a
    thread as aa actor. Objects must be a newclass instance. All
    interaction is queued up via a standard Queue, with instances of
    the Future class returned. Unless the Future is used (through a
    supported method), no waiting occurs.

    We can control how exceptions propagage via the use of the
    make_future factory. This allows for different models - do we have
    transparent futures?, futures that propagage exceptions?, etc.

    TBD: would it be possible to turn this into a class decorator such that
    any newly constructed objects are automatically actors? That would
    be an interesting twist!
    """

    class TerminateCls(object): pass
    TerminateReceiver = TerminateCls() # just need a unique sentinel
    incoming = Queue()

    def receiver():
        """Runs `obj` within new thread, adapting via incoming queue"""
        no_problems = True
        tid = threading.currentThread().getName()
        clock = 0
        while True:
            msg, future = incoming.get(True)
            if debug:
                print "%d %s: Processing %r" % (clock, tid, msg)
            if msg == TerminateReceiver:
                break
            elif no_problems:
                method, args, kw = msg
                try:
                    future.target = getattr(obj, method)(*args, **kw)
                except Exception, err:
                    # bubble up to the context manager and stop
                    # processing any additional messages
                    future.exception = (err, sys.exc_info()[2])
                    no_problems = False
                finally:
                    # regardless, we need to set the event to prevent
                    # needless (and endless) waiting
                    future.event.set()
            else:
                # consume any outstanding messages - any unhandled
                # exceptions could leave the object in a bad state
                future.event.set()
                future.target = NoneSuch # differentiate from None
            clock += 1


    class Proxy(object):
        def __init__(self, target, incoming):
            self._target = target
            self._incoming = incoming

        def __getattr__(self, aname):
            def send(*args, **kw):
                future = make_future(debug=debug)
                self._incoming.put(((aname, args, kw), future))
                return future
            return send

        def __call__(self, *args, **kw):
            future = make_future(debug=debug)
            self._incoming.put((('__call__', args, kw), future))
            return future

    t = threading.Thread(target=receiver)
    t.setDaemon(True) # don't keep running if our user no longer is around!
    t.start()
    yield Proxy(obj, incoming)
    incoming.put((TerminateReceiver, None))


# this wrapper needs to create a context such that the function for
# the scope of the call executes asynchronously, with any result
# returned as a future. it's really only interesting in the case of
# something like a recursive generator, and in that case we probably
# want to do something like provide some scheduling information to
# avoid unnececessary threading.

# Now ignored, the schedule=5 is just to remind me to do something
# more intelligent in the future! (Perhaps the scheduling could be
# based on some dynamic tuning?  Or at least the number of cores?)

def async(debug=True, schedule=5):
    """Decorates a function such that it runs asynchronously."""
    def decorator(func):
        from functools import wraps
        @wraps(func)
        def wrapper(*args,**kw):
            with acting(func, debug=debug) as async_func:
                return async_func(*args, **kw)
        return wrapper
    return decorator


# TODO: write some good tests. Basically we can ask questions about
# ordering, exceptions. What else can we do? Probably the
# async_spanning.py code is a better test of robustness anyway.




def test():
    class X(object):
        def __init__(self, x, y):
            self.x = x
            self.y = y
        def write(self):
            print self.x, self.y
        def mult(self, n):
            self.x = n*2
            self.y = n*2
        def get_x(self):
            return self.x
        def __call__(self, mul):
            return self.x * mul

    with acting(X(1,2)) as x:
        x.write()
        x.mult(10)
        z = x(20)
        print "Future %r" % (z,)
        x.write()
        foo = x.get_x()
        x.mult(5)
        y = x.get_x()
        print "Future %r" % (y,)
        x.write()
        print y
        print z
        w = x.hello('there')
        # no guarantees that the following will be the last statement,
        # but execution past here certainly will not continue!
        print "When did it all end? ", w 
        x.mult(10)
        print x(30)
        


if __name__ == '__main__':
    test()
