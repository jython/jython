"""

"""

import support

import glob
l = glob.glob('*')

if l[0][:2] == "//":
   raise support.TestError("cwd files should not start with //")
