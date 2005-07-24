import test.test_support, unittest

class TestSubclasses(unittest.TestCase):

    def test_float(self):
        class Spam(float):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0.0)
        self.assertEqual(str(Spam()), "hi")

    def test_int(self):
        class Spam(int):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0)
        self.assertEqual(str(Spam()), "hi")

    def test_long(self):
        class Spam(long):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), 0L)
        self.assertEqual(str(Spam()), "hi")

    def test_tuple(self):
        class Spam(tuple):
            def __str__(self):
                return "hi"

        self.assertEqual(Spam(), ())
        #XXX: not done yet.
        #self.assertEqual(str(Spam()), "hi")


def test_suite():
    allsuites = [unittest.makeSuite(klass, 'test')
                 for klass in (TestSubclasses,
                              )
                ]
    return unittest.TestSuite(allsuites)


def test_main():
    import sys

    r = unittest.TextTestRunner(stream=sys.stdout, verbosity=2)
    s = test_suite()
    lastrc = None
    r.run(s)

if __name__ == "__main__":
    test_main()
