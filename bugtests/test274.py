"""

"""

import support

import shutil, os

if os.path.isdir("test274d"):
    shutil.rmtree("test274d", 1)
if os.path.isdir("test274d1"):
    shutil.rmtree("test274d1", 1)

os.mkdir("test274d")
open("test274d/file", "w").close()

#os.utime = os.chmod = lambda f, t: None

shutil.copytree("test274d", "test274d1")

open("test274d1/file", "r").close()

shutil.rmtree("test274d", 1)
shutil.rmtree("test274d1", 1)

