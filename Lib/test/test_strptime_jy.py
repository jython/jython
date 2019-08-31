# Java locale differences from JDK 9 onwards, and locale variation on
# developer machines, break test_strptime tests. This manifests more on Windows.
# Rather than diverge from the Python source, this overrides with extra locale
# setup.
# Merging back into CPython is desirable, but is a bigger discussion around
# library merging generally.

import unittest
from datetime import datetime
from time import strptime
from test.test_strptime import *
from test import test_support


class ParsingTests(unittest.TestCase):

    def test_iso8601(self):
        now = datetime.utcnow().replace(microsecond=0)
        self.assertEqual(now, datetime.strptime(now.isoformat('T'), "%Y-%m-%dT%H:%M:%S"))
        # tests bug 1662
        self.assertEqual(now, datetime.strptime(now.isoformat('T') + 'Z', "%Y-%m-%dT%H:%M:%SZ"))

    def test_IllegalArgument_to_ValueError(self):
        with self.assertRaises(ValueError):
            d = strptime('', '%e')

    def test_issue1964(self):
        d = strptime('0', '%f')
        self.assertEqual(1900, d.tm_year)

    def test_issue2112(self):
        d = strptime('1', '%d')
        self.assertEqual(1900, d.tm_year)


def test_main(initialize=True):
    test_support.force_reset_locale(initialize)

    test_support.run_unittest(
        getlang_Tests,
        LocaleTime_Tests,
        TimeRETests,
        StrptimeTests,
        Strptime12AMPMTests,
        JulianTests,
        CalculationTests,
        CacheTests,
        ParsingTests
    )


if __name__ == '__main__':
    test_main(initialize=False)

