
import sys, StringIO

from xml.sax import saxutils
from xml.sax import make_parser
from xml.sax.handler import feature_namespaces

def jythonc():
    import xml.sax.drivers2.drv_xmlproc
    import encodings.utf_16_be
    import dumbdbm

file = StringIO.StringIO("""<collection>
  <comic title="Sandman" number='62'>
    <writer>Neil Gaiman</writer>
    <penciller pages='1-9,18-24'>Glyn Dillon</penciller>
    <penciller pages="10-17">Charles Vess</penciller>
  </comic>
  <comic title="Shade, the Changing Man" number="7">
    <writer>Peter Milligan</writer>
    <penciller>Chris Bachalo</penciller>
  </comic>
</collection>""")

class FindIssue(saxutils.DefaultHandler):
    def __init__(self, title, number):
        self.search_title, self.search_number = title, number

    def startElement(self,name,attrs):
        global match
        if name != 'comic' : return

        title = attrs.get('title', None)
        number = attrs.get('number',None)
        if title == self.search_title and number == self.search_number:
            match += 1

parser = make_parser()
#parser.setFeature(feature_namespaces,0)
dh = FindIssue('Sandman', '62')
parser.setContentHandler(dh)

match = 0
parser.parse(file)
assert match == 1

