
import support

support.compileJava("test284j2.java", classpath=".")

import test284j1

assert test284j1().foo() == 'test284j1.foo'

import test284j2

assert test284j2().foo() == 'test284j2.foo'
