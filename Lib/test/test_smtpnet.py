#!/usr/bin/env python

import unittest
from test import test_support
import smtplib
import os

test_support.requires("network")


def _external_http_proxy_configured():
    return any(os.environ.get(name) for name in (
        'http_proxy', 'https_proxy', 'all_proxy',
        'HTTP_PROXY', 'HTTPS_PROXY', 'ALL_PROXY'))

class SmtpSSLTest(unittest.TestCase):
    testServer = 'smtp.gmail.com'
    remotePort = 465

    @unittest.skipIf(_external_http_proxy_configured(),
                     "external SMTP SSL tests require direct network access")
    def test_connect(self):
        test_support.get_attribute(smtplib, 'SMTP_SSL')
        with test_support.transient_internet(self.testServer):
            server = smtplib.SMTP_SSL(self.testServer, self.remotePort)
        server.ehlo()
        server.quit()

    @unittest.skipIf(_external_http_proxy_configured(),
                     "external SMTP SSL tests require direct network access")
    def test_connect_default_port(self):
        test_support.get_attribute(smtplib, 'SMTP_SSL')
        with test_support.transient_internet(self.testServer):
            server = smtplib.SMTP_SSL(self.testServer)
        server.ehlo()
        server.quit()

def test_main():
    test_support.run_unittest(SmtpSSLTest)

if __name__ == "__main__":
    test_main()
