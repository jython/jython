"""
exec and eval are not thread safe
"""

import support

from java.lang import Thread

class TestThread(Thread):
    def run(self):
        for i in range(100):
            exec("x=2+2")

testers = []
for i in range(10):
    testers.append(TestThread())

for tester in testers:
    tester.start()

