'''
From bug #1548501
Check that __file__ is set on a file run directly from jython.
'''
import support
support.runJython('test393m.py')
