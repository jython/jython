# merge into upstream test_strptime.py at some point

import unittest
from datetime import datetime
from test import test_support


class ParsingTests(unittest.TestCase):

    def test_iso8601(self):
        now = datetime.utcnow().replace(microsecond=0)
        self.assertEqual(now, datetime.strptime(now.isoformat('T'), "%Y-%m-%dT%H:%M:%S"))
        # tests bug 1662
        self.assertEqual(now, datetime.strptime(now.isoformat('T') + 'Z', "%Y-%m-%dT%H:%M:%SZ"))

def test_main():
    test_support.run_unittest(
        ParsingTests
    )


if __name__ == '__main__':
    test_main()
