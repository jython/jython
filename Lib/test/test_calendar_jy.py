# Java locale differences from JDK 9 onwards, and locale variation on
# developer machines, break test_calendar tests. This manifests more on Windows.
# Rather than diverge from the Python source, this overrides with extra locale
# setup.

from test.test_calendar import *
from test import test_support


def test_main(initialize=True):
    test_support.force_reset_locale(initialize)
    test_support.run_unittest(
        OutputTestCase,
        CalendarTestCase,
        MondayTestCase,
        SundayTestCase,
        MonthRangeTestCase,
        LeapdaysTestCase,
    )


if __name__ == '__main__':
    test_main(initialize=False)

