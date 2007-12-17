from __future__ import with_statement
# from contextlib import closing

class closing(object):
    """Context to automatically close something at the end of a block.

    Code like this:

        with closing(<module>.open(<arguments>)) as f:
            <block>

    is equivalent to this:

        f = <module>.open(<arguments>)
        try:
            <block>
        finally:
            f.close()

    """
    def __init__(self, thing):
        self.thing = thing
    def __enter__(self):
        return self.thing
    def __exit__(self, *exc_info):
        self.thing.close()

with closing((i * 2 for i in xrange(100))) as g:
    print g.next()
    print g.next()
print g.next()
