# Used by test_codec_jy::CodecsTestCase.test_print_sans_lib.
# Run without importing site module so codec registry is not initialised yet.
import sys
from os.path import basename
# Hide standard library from import mechanism so the Python codec cannot be found.
sys.path = [p for p in sys.path if basename(p).lower() != 'lib']
# Can we still encode and decode utf-8?
encoded = u'hi'.encode("utf-8")
encoded.decode('utf-8')
