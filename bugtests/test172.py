

import support

support.compileJava("test172j.java")

import test172j

support.compare(test172j.foo(1,2,3,4), "foo called with 4 arguments")

support.compare(test172j().bar(1,2,3,4), "bar called with 4 arguments")
