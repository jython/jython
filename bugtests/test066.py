"""
BigDecimal return coerced into integer
"""

import support

support.compileJava("test066j.java")

import java
import test066j
x = java.math.BigDecimal("123.4321")
y = test066j.asBigDecimal()

if type(x) != type(y):
    raise support.TestError("BigDecimal coerced")

if x != y:
    raise support.TestError("BigDecimal coerced")




