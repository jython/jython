"""
"%#.0f", "%e" and "%+f" w/ negative numbers don't print correctly.
"""

import support


if "%#.0f" % 5 != "5.":
    raise support.TestWarning("Format Error #1 %#.0f" % 5)
if "%.1f" % 5 != "5.0":
    raise support.TestError("Format Error #2 %.1f" % 5)

if "%e" % -1e-6 != "-1.000000e-006":
    raise support.TestError("Format Error #3 %e" % -1e-6)
if "%e" % 0 != "0.000000e+000":
    raise support.TestError("Format Error #4 %e" % 0)
if "%e" % 1e-6 != "1.000000e-006":
    raise support.TestError("Format Error #5 %e" % 1e-6)
 

if "%+f" % -5 != "-5.000000":
    raise support.TestError("Format Error #6 %+f" % -5)
if "%+f" % 5 != "+5.000000":
    raise support.TestError("Format Error #7 %+f" % 5)



import java 
java.util.Locale.setDefault(java.util.Locale("us", ""))

if "%#.0f" % 5 != "5.":
    raise support.TestError("Format Error #8")
if "%.1f" % 5 != "5.0":
    raise support.TestError("Format Error #9")

if "%e" % -1e-6 != "-1.000000e-006":
    raise support.TestError("Format Error #10")
if "%e" % 0 != "0.000000e+000":
    raise support.TestError("Format Error #11")
if "%e" % 1e-6 != "1.000000e-006":
    raise support.TestError("Format Error #12")
 

if "%+f" % -5 != "-5.000000":
    raise support.TestError("Format Error #13")
if "%+f" % 5 != "+5.000000":
    raise support.TestError("Format Error #14")



