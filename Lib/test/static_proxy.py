# Part of test_java_subclasses.StaticProxyCompilationTest.  This needs to be its own module
# so the statically compiled proxy can import it.
from java.lang import Runnable

class RunnableImpl(Runnable):
    __javaname__ = "test.static_proxy.RunnableImpl"
    def run(self):
        pass

    def meth(self):
        return 78
