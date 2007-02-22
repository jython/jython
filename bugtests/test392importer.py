import sys
sys.path.append('test392LibDir')
import test392m

assert 'test392LibDir' in test392m.__file__, "test392m.__file__ doesn't contain test392LibDir, the directory it's in"
