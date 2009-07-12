# Part of test_java_subclasses.StaticProxyCompilationTest
from java.lang import Class

# Grab the proxy class statically compiled by the containing test
cls = Class.forName("test.static_proxy.RunnableImpl")
# Instantiating the proxy class should import the module containing it and create the Python side
assert cls.newInstance().meth() == 78
