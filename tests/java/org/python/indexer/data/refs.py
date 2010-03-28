# single definition with some references in various contexts

foo = 3.14  # 1

print foo, [foo]  # 2, 3

print {foo: foo}  # 4, 5

print "foo %s" % 1 + foo  # 6, 7

print str(foo)  # 8

print (
  # the default parser had this name's offset starting at prev paren:
  foo)  # 9

print (foo,)  # 10

print ([foo]), (-foo)  # 11, 12

print lambda(foo): foo  # 13, 14

print (lambda(x): foo)("bar")  # 15

print (((foo)))  # 16
