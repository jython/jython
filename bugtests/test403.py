"""
test fix for bug #1672

"""

import os
import compileall

PACKAGE = "test403"
PYC_GREETER = os.path.join(PACKAGE, "greeter.pyc")
PYCLASS_GREETER = os.path.join(PACKAGE, "greeter$py.class")
PYCLASS_TEST = os.path.join(PACKAGE, "test$py.class")

def cleanup():
    try:
        for f in (PYC_GREETER, PYCLASS_TEST, PYCLASS_GREETER):
            os.unlink(f)
    except OSError:
        pass


# setup
cleanup()
open(PYC_GREETER, "a").close()

# test
compileall.compile_dir(PACKAGE)
print PYCLASS_TEST
print PYCLASS_GREETER
assert os.path.exists(PYCLASS_TEST)
assert os.path.exists(PYCLASS_GREETER)

# teardown
cleanup()
