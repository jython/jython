"""

"""

import support

support.compileJava("test277p/Test.java")

from test277p import Test

cnt = 0

class pytest(Test):

        def initialize(self):
                global cnt
                Test.initialize(self)
                cnt += 1

pt=pytest()

support.compare(cnt, "2")

cnt = 0

import java
pt=java.lang.Class.newInstance(pytest)

support.compare(cnt, "2")

