"""
[ 581785 ] bug in 4DOM
"""

import support

f = open("test369.xml", "w")
f.write("<test>Some <b>text</b> here</test> ")
f.close()


from xml.dom.ext import PrettyPrint
from xml.dom.ext.reader import Sax2
import cStringIO

reader = Sax2.Reader()
doc = reader.fromStream("test369.xml") 

ofile = cStringIO.StringIO()
PrettyPrint(doc, ofile)

if ofile.getvalue() != """\
<?xml version='1.0' encoding='UTF-8'?>\n\
<!DOCTYPE test>\n\
<test>Some <b>text</b> here</test>\n""":
    raise support.TestError("xml was parsed incorrectly")


