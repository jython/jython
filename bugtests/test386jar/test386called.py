# make sure we are in 'standalone' mode, without package scan
import sys
skipName = "python.cachedir.skip"
if not sys.registry.containsKey(skipName):
  raise AssertionError, skipName + " is missing"
if not "true" == sys.registry.getProperty(skipName):
  raise AssertionError, skipName + " is not true"

# import a non-builtin module which is not imported by default on startup
# this verifies that /Lib .py files can be imported
# this fixes bug [ 1194650 ]
import getopt

# an early java import # (only works since java.util is an already loaded package)
from java import util
util # used to give a NameError

# import java specific py modules
import os
import javaos

# now do some java imports which previously failed without a package scan
# this (most of the time) solves the famous 'no module named java' problem
import java            # (only works since java is an already loaded package)
import java.lang       # (only works since java.lang is an already loaded package)

# explicit imports
from java.math import BigDecimal
from java.math import BigDecimal, BigInteger
from java.lang.reflect import Method

# verify the self healing
try:
  # assume package javax.imageio.event was never touched before
  import javax.imageio.event
  raise AssertionError, "ImportError expected when executing 'import javax.imageio.event'"
except ImportError:
  pass
from javax.imageio.event import IIOReadProgressListener

# importing this twice was a problem
from org.python.core import PySystemState
from org.python.core import PySystemState

# verify explicit imports of the form 'import java.net.URL'
import javax.security.auth.Policy
javax
javax.security
javax.security.auth
javax.security.auth.Policy