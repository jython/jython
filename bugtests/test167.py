"""

"""

import support


from java import lang
import synchronize

class test167(lang.Thread):
    def run(self): pass
    run = synchronize.make_synchronized(run)

test167().start()