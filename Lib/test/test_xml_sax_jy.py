import unittest
from test import test_support

from StringIO import StringIO
from java.io import File

import xml.sax
import xml.sax.handler
import xml.dom.minidom
import xml.dom.pulldom


class XmlHandler(xml.sax.ContentHandler):

    def __init__(self, root_node, connection):
        self.connection = connection
        self.nodes = [('root', root_node)]
        self.current_text = ''

    def startElement(self, name, attrs):
        self.current_text = ''
        new_node = self.nodes[-1][1].startElement(name, attrs, self.connection)
        if new_node is not None:
            self.nodes.append((name, new_node))

    def endElement(self, name):
        self.nodes[-1][1].endElement(name, self.current_text, self.connection)
        if self.nodes[-1][0] == name:
            if hasattr(self.nodes[-1][1], 'endNode'):
                self.nodes[-1][1].endNode(self.connection)
            self.nodes.pop()
        self.current_text = ''

    def characters(self, content):
        self.current_text += content


class RootElement(object):
    def __init__(self):
        self.start_elements = []
        self.end_elements = []
    def startElement(self, name, attrs, connection):
        self.start_elements.append([name, attrs, connection])

    def endElement(self, name, value, connection):
        self.end_elements.append([name, value, connection])


class JavaSaxTestCase(unittest.TestCase):

    def setUp(self):
        self._paths = []

    def tearDown(self):
        for path in self._paths:
            test_support.unlink(path)

    def _make_temp_file(self, suffix, contents):
        path = test_support.TESTFN + suffix
        self._paths.append(path)
        with open(path, "wb") as f:
            f.write(contents)
        return path

    def _file_url(self, path):
        return File(path).toURI().toString()

    def test_javasax_with_skipEntity(self):
        content = '<!DOCTYPE Message [<!ENTITY xxe SYSTEM "http://aws.amazon.com/">]><Message>error:&xxe;</Message>'

        root = RootElement()
        handler = XmlHandler(root, root)
        parser = xml.sax.make_parser()
        parser.setContentHandler(handler)
        parser.setFeature(xml.sax.handler.feature_external_ges, 0)
        parser.parse(StringIO(content))

        self.assertEqual('Message', root.start_elements[0][0])
        self.assertItemsEqual([['Message', 'error:', root]], root.end_elements)

    def test_javasax_uses_safe_defaults(self):
        parser = xml.sax.make_parser()
        self.assertFalse(parser.getFeature(xml.sax.handler.feature_external_ges))
        self.assertFalse(parser.getFeature(xml.sax.handler.feature_external_pes))

    def test_javasax_default_parser_skips_external_general_entities(self):
        secret = self._make_temp_file(".txt", "JYTHON_XXE_SECRET")
        content = '<!DOCTYPE Message [<!ENTITY xxe SYSTEM "%s">]><Message>error:&xxe;</Message>' % (
            self._file_url(secret),)

        root = RootElement()
        handler = XmlHandler(root, root)
        parser = xml.sax.make_parser()
        parser.setContentHandler(handler)
        parser.parse(StringIO(content))

        self.assertEqual('Message', root.start_elements[0][0])
        self.assertItemsEqual([['Message', 'error:', root]], root.end_elements)

    def test_javasax_default_parser_skips_external_parameter_entities(self):
        bad_dtd = self._make_temp_file(".dtd", "<!ELEMENT")
        content = '<!DOCTYPE Message [<!ENTITY %% ext SYSTEM "%s">%%ext;]><Message>ok</Message>' % (
            self._file_url(bad_dtd),)

        root = RootElement()
        handler = XmlHandler(root, root)
        parser = xml.sax.make_parser()
        parser.setContentHandler(handler)
        parser.parse(StringIO(content))

        self.assertItemsEqual([['Message', 'ok', root]], root.end_elements)

    def test_pulldom_default_parser_skips_external_general_entities(self):
        secret = self._make_temp_file(".txt", "JYTHON_PULLDOM_XXE_SECRET")
        content = '<!DOCTYPE Message [<!ENTITY xxe SYSTEM "%s">]><Message>error:&xxe;</Message>' % (
            self._file_url(secret),)

        events = xml.dom.pulldom.parseString(content)
        for toktype, root in events:
            if toktype == xml.dom.pulldom.START_ELEMENT:
                break
        events.expandNode(root)
        try:
            self.assertEqual("error:", root.firstChild.data)
        finally:
            events.clear()

    def test_minidom_default_parser_skips_external_general_entities(self):
        secret = self._make_temp_file(".txt", "JYTHON_MINIDOM_XXE_SECRET")
        content = '<!DOCTYPE Message [<!ENTITY xxe SYSTEM "%s">]><Message>error:&xxe;</Message>' % (
            self._file_url(secret),)

        doc = xml.dom.minidom.parseString(content)
        try:
            self.assertEqual("error:", doc.documentElement.firstChild.data)
        finally:
            doc.unlink()


def test_main():
    test_support.run_unittest(JavaSaxTestCase)


if __name__ == '__main__':
    test_main()
