"""

"""

import support

import re

x = re.compile(r"(?P<first>.*),(?P<second>.*)")
y = x.search("hello,there")

support.compare(y.group("first"), "hello")
support.compare(y.group("second"), "there")
support.compare(y.group(1), "hello")
support.compare(y.group(2), "there")


