from __future__ import with_statement

class FooException(Exception): pass
class BarException(Exception): pass

class GeneratorContextManager(object):
    """Helper for @contextmanager decorator."""

    def __init__(self, gen):
        self.gen = gen

    def __enter__(self):
        try:
            return self.gen.next()
        except StopIteration:
            raise RuntimeError("generator didn't yield")

    def __exit__(self, type, value, traceback):
        if type is None:
            try:
                self.gen.next()
            except StopIteration:
                return
            else:
                raise RuntimeError("generator didn't stop")
        else:
            try:
                self.gen.throw(type, value, traceback)
                raise RuntimeError("generator didn't stop after throw()")
            except StopIteration, exc:
                # Suppress the exception *unless* it's the same exception that
                # was passed to throw().  This prevents a StopIteration
                # raised inside the "with" statement from being suppressed
                return exc is not value
            except:
                # only re-raise if it's *not* the exception that was
                # passed to throw(), because __exit__() must not raise
                # an exception unless __exit__() itself failed.  But throw()
                # has to raise the exception to signal propagation, so this
                # fixes the impedance mismatch between the throw() protocol
                # and the __exit__() protocol.
                #
                if sys.exc_info()[1] is not value:
                    raise


def contextmanager(func):
    """@contextmanager decorator.

    Typical usage:

        @contextmanager
        def some_generator(<arguments>):
            <setup>
            try:
                yield <value>
            finally:
                <cleanup>

    This makes this:

        with some_generator(<arguments>) as <variable>:
            <body>

    equivalent to this:

        <setup>
        try:
            <variable> = <value>
            <body>
        finally:
            <cleanup>

    """
    def helper(*args, **kwds):
        return GeneratorContextManager(func(*args, **kwds))
    try:
        helper.__name__ = func.__name__
        helper.__doc__ = func.__doc__
        helper.__dict__ = func.__dict__
    except:
        pass
    return helper



@contextmanager
def rethrow(exception_to_be_rethrown):
    """rethrows arbitrary exceptions via exception_to_be_rethrown.

       Note that this could be a function or callable object that maps
       exceptions in arbitrary ways
    """
    try:
        yield
    except Exception, err:
        raise exception_to_be_rethrown(err)

try:
    with rethrow(BarException):
        # do some interesing stuff here, but we end up
        # raising an exception  
        raise FooException('foo')
except Exception, err:
    # for now we just observe that FooException has been rethrown
    # as BarException
    print FooException, 'was rethrown as ', type(err)


    
