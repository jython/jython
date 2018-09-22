# Tests invocation of the interpreter with various command line arguments
# All tests are executed with environment variables ignored
# See test_cmd_line_script.py for testing of script execution

import test.test_support
import sys
import unittest
from test.script_helper import (
    assert_python_ok, assert_python_failure, spawn_python, kill_python,
    python_exit_code
)


class CmdLineTest(unittest.TestCase):

    @classmethod
    def tearDownClass(cls):
        if test.test_support.is_jython:
            # GC is not immediate, so Popen.__del__ may be delayed.
            # Try to force any Popen.__del__ errors within scope of test.
            from test_weakref import extra_collect
            extra_collect()

    def start_python(self, *args):
        p = spawn_python(*args)
        return kill_python(p)

    def exit_code(self, *args):
        return python_exit_code(*args)

    def test_directories(self):
        self.assertNotEqual(self.exit_code('.'), 0)
        self.assertNotEqual(self.exit_code('< .'), 0)

    def verify_valid_flag(self, cmd_line):
        data = self.start_python(cmd_line)
        self.assertTrue(data == '' or data.endswith('\n'))
        self.assertNotIn('Traceback', data)

    def test_optimize(self):
        self.verify_valid_flag('-O')
        self.verify_valid_flag('-OO')

    def test_q(self):
        self.verify_valid_flag('-Qold')
        self.verify_valid_flag('-Qnew')
        self.verify_valid_flag('-Qwarn')
        self.verify_valid_flag('-Qwarnall')

    def test_site_flag(self):
        self.verify_valid_flag('-S')

    def test_usage(self):
        self.assertIn('usage', self.start_python('-h'))

    def test_version(self):
        prefix = 'Jython' if test.test_support.is_jython else 'Python'
        version = (prefix + ' %d.%d') % sys.version_info[:2]
        self.assertTrue(self.start_python('-V').startswith(version))

    def test_run_module(self):
        # Test expected operation of the '-m' switch
        # Switch needs an argument
        self.assertNotEqual(self.exit_code('-m'), 0)
        # Check we get an error for a nonexistent module
        self.assertNotEqual(
            self.exit_code('-m', 'fnord43520xyz'),
            0)
        # Check the runpy module also gives an error for
        # a nonexistent module
        self.assertNotEqual(
            self.exit_code('-m', 'runpy', 'fnord43520xyz'),
            0)
        # All good if module is located and run successfully
        self.assertEqual(
            self.exit_code('-m', 'timeit', '-n', '1'),
            0)

    def test_run_module_bug1764407(self):
        # -m and -i need to play well together
        # Runs the timeit module and checks the __main__
        # namespace has been populated appropriately
        p = spawn_python('-i', '-m', 'timeit', '-n', '1')
        p.stdin.write('Timer\n')
        p.stdin.write('exit()\n')
        data = kill_python(p)
        self.assertTrue(data.startswith('1 loop'))
        self.assertIn('__main__.Timer', data)

    def test_run_code(self):
        # Test expected operation of the '-c' switch
        # Switch needs an argument
        self.assertNotEqual(self.exit_code('-c'), 0)
        # Check we get an error for an uncaught exception
        self.assertNotEqual(
            self.exit_code('-c', 'raise Exception'),
            0)
        # All good if execution is successful
        self.assertEqual(
            self.exit_code('-c', 'pass'),
            0)

    @unittest.skipIf(test.test_support.is_jython,
                     "Hash randomisation is not supported in Jython.")
    def test_hash_randomization(self):
        # Verify that -R enables hash randomization:
        self.verify_valid_flag('-R')
        hashes = []
        for i in range(2):
            code = 'print(hash("spam"))'
            data = self.start_python('-R', '-c', code)
            hashes.append(data)
        self.assertNotEqual(hashes[0], hashes[1])

        # Verify that sys.flags contains hash_randomization
        code = 'import sys; print sys.flags'
        data = self.start_python('-R', '-c', code)
        self.assertTrue('hash_randomization=1' in data)

    def test_del___main__(self):
        # Issue #15001: PyRun_SimpleFileExFlags() did crash because it kept a
        # borrowed reference to the dict of __main__ module and later modify
        # the dict whereas the module was destroyed
        filename = test.test_support.TESTFN
        self.addCleanup(test.test_support.unlink, filename)
        with open(filename, "w") as script:
            print >>script, "import sys"
            print >>script, "del sys.modules['__main__']"
        assert_python_ok(filename)

    def test_unknown_options(self):
        rc, out, err = assert_python_failure('-E', '-z')
        self.assertIn(b'Unknown option: -z', err)
        self.assertEqual(err.splitlines().count(b'Unknown option: -z'), 1)
        self.assertEqual(b'', out)
        # Add "without='-E'" to prevent _assert_python to append -E
        # to env_vars and change the output of stderr
        rc, out, err = assert_python_failure('-z', without='-E')
        self.assertIn(b'Unknown option: -z', err)
        self.assertEqual(err.splitlines().count(b'Unknown option: -z'), 1)
        self.assertEqual(b'', out)
        rc, out, err = assert_python_failure('-a', '-z', without='-E')
        self.assertIn(b'Unknown option: -a', err)
        # only the first unknown option is reported
        self.assertNotIn(b'Unknown option: -z', err)
        self.assertEqual(err.splitlines().count(b'Unknown option: -a'), 1)
        self.assertEqual(b'', out)

    def test_python_startup(self):
        # Test that the file designated by [PJ]YTHONSTARTUP is executed when interactive.
        # Note: this test depends on the -i option forcing Python to treat stdin as interactive.
        filename = test.test_support.TESTFN
        self.addCleanup(test.test_support.unlink, filename)
        with open(filename, "w") as script:
            print >>script, "print 6*7"
            print >>script, "print 'Ni!'"
        expected = ['42', 'Ni!']
        def check(*args, **kwargs):
            result = assert_python_ok(*args, **kwargs)
            self.assertListEqual(expected, result[1].splitlines())
        if test.test_support.is_jython:
            # Jython produces a prompt before exit, but not CPython. Hard to say who is correct.
            expected.append('>>> ')
            # The Jython way is to set a registry item python.startup
            check('-i', '-J-Dpython.startup={}'.format(filename))
            # But a JYTHONSTARTUP environment variable is also supported
            check('-i', JYTHONSTARTUP=filename)
        else:
            check('-i', PYTHONSTARTUP=filename)

    @unittest.skipUnless(test.test_support.is_jython, "Requires write to sys.flags.inspect")
    def test_python_inspect(self):
        # Test that PYTHONINSPECT set during a script causes an interactive session to start.
        # Note: this test depends on the -i option forcing Python to treat stdin as interactive,
        # and on Jython permitting manipulation of sys.flags.inspect (which CPython won't)
        # so that PYTHONINSPECT can have some effect.
        filename = test.test_support.TESTFN
        self.addCleanup(test.test_support.unlink, filename)
        with open(filename, "w") as script:
            print >>script, "import sys, os"
            print >>script, "sys.flags.inspect = False"
            print >>script, "os.environ['PYTHONINSPECT'] = 'whatever'"
            print >>script, "print os.environ['PYTHONINSPECT']"
        expected = ['whatever', '>>> ']
        result = assert_python_ok('-i', filename)
        self.assertListEqual(expected, result[1].splitlines())


def test_main():
    test.test_support.run_unittest(CmdLineTest)
    test.test_support.reap_children()

if __name__ == "__main__":
    test_main()
