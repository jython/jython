# Copyright (C) 2001,2002 Python Software Foundation
# Jython override to enforce C locale during locale beta 
from test import test_email
from test import test_support

def test_main(initialize=True):
    test_support.force_reset_locale(initialize)
    test_email.test_main()

if __name__ == '__main__':
    test_main(False)

