
from java import util as ut, io, awt as aw, text as te
import java

import support

if java.util != ut:
    raise support.TestError("util not the same")
if java.awt != aw:
    raise support.TestError("awt not the same")
if java.io != io:
    raise support.TestError("io not the same")
if java.text != te:
    raise support.TestError("text not the same")


import java.util.Vector as vec
import java.util.Date as date, java.io.File as file

if java.util.Vector != vec:
    raise support.TestError("Vector not the same")
if java.util.Date != date:
    raise support.TestError("Date not the same")
if java.io.File != file:
    raise support.TestError("File not the same")

