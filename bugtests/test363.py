"""
[ 533354 ] bug in xml.dom.minidom.parseString
"""

import support

import xml.dom.minidom
DOM = xml.dom.minidom.parseString("<foo><bar/></foo>") 

#raise support.TestWarning('A test of TestWarning. It is not an error')
