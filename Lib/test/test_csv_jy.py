# -*- coding: utf-8 -*-
# Copyright (C) 2017 Jython Developers

# Additional csv module unit tests for Jython

import csv
import io
import sys
from tempfile import TemporaryFile
from test import test_support
import unittest

# This test has been adapted from Python 3 test_csv.TestUnicode. In Python 3,
# the csv module supports Unicode directly. In Python 2, it does not, except
# that it is transparent to byte data. Many tools, however, accept UTF-8
# encoded text in a CSV file.
#
class EncodingContext(object):
    """Context manager to save and restore the encoding.

    Use like this:

        with EncodingContext("utf-8"):
            self.assertEqual("'caf\xc3\xa9'", u"'caf\xe9'")
    """

    def __init__(self, encoding):
        if not hasattr(sys, "setdefaultencoding"):
            reload(sys)
        self.original_encoding = sys.getdefaultencoding()
        sys.setdefaultencoding(encoding)

    def __enter__(self):
        return self

    def __exit__(self, *ignore_exc):
        sys.setdefaultencoding(self.original_encoding)

class TestUnicode(unittest.TestCase):

    names = [u"Martin von Löwis",
             u"Marc André Lemburg",
             u"Guido van Rossum",
             u"François Pinard",
             u"稲田直樹"]

    def test_decode_read(self):
        # The user code receives byte data and takes care of the decoding
        with TemporaryFile("w+b") as fileobj:
            line = u",".join(self.names) + u"\r\n"
            fileobj.write(line.encode('utf-8'))
            fileobj.seek(0)
            reader = csv.reader(fileobj)
            # The reader yields rows of byte strings that decode to the data
            table = [[e.decode('utf-8') for e in row] for row in reader]
            self.assertEqual(table, [self.names])

    def test_encode_write(self):
        # The user encodes unicode objects to byte data that csv writes
        with TemporaryFile("w+b") as fileobj:
            writer = csv.writer(fileobj)
            # We present a row of encoded strings to the writer
            writer.writerow([n.encode('utf-8') for n in self.names])
            # We expect the file contents to be the UTF-8 of the csv data
            expected = u",".join(self.names) + u"\r\n"
            fileobj.seek(0)
            self.assertEqual(fileobj.read().decode('utf-8'), expected)

    def test_unicode_write(self):
        # The user supplies unicode data that csv.writer default-encodes
        # (undocumented feature relied upon by client code).
        # See Issue #2632  https://github.com/jythontools/jython/issues/90
        with TemporaryFile("w+b") as fileobj:
            with EncodingContext('utf-8'):
                writer = csv.writer(fileobj)
                # We present a row of unicode strings to the writer
                writer.writerow(self.names)
                # We expect the file contents to be the UTF-8 of the csv data
                expected = u",".join(self.names) + u"\r\n"
                fileobj.seek(0)
                self.assertEqual(fileobj.read().decode(), expected)


def test_main():
    # We'll be enabling sys.setdefaultencoding so remember to disable
    had_set = hasattr(sys, "setdefaultencoding")
    try:
        test_support.run_unittest(
            TestUnicode,
        )
    finally:
        if not had_set:
            delattr(sys, "setdefaultencoding")

if __name__ == "__main__":
    test_main()
