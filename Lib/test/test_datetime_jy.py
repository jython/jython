import unittest
from test import test_support

from datetime import timedelta
from datetime import tzinfo
from datetime import time
from datetime import date, datetime

from java.util import Calendar
from java.sql import Date


class TestJavaDatetime(unittest.TestCase):

    def test_datetime(self):
        self.assertTrue(hasattr(datetime, "__tojava__"))
        x = datetime(2007, 1, 3)
        y = x.__tojava__(Calendar)
        self.assertTrue(isinstance(y, Calendar))

    def test_date(self):
        self.assertTrue(hasattr(date, "__tojava__"))
        x = date(2007, 1, 3)
        y = x.__tojava__(Calendar)
        self.assertTrue(isinstance(y, Calendar))

    def test_time(self):
        self.assertTrue(hasattr(time, "__tojava__"))
        x = time(1, 3)
        y = x.__tojava__(Calendar)
        self.assertTrue(isinstance(y, Calendar))


if __name__ == '__main__':
    unittest.main()
