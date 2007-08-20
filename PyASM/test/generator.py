def gen(x):
    while True:
        x -= 1
        if x < 0:
            break
        else:
            print (yield x)

for x in gen(4):
    print x

it = gen(2)
print it.next()
print it.send("hi")

it = gen(3)
try:
    it.throw(SystemError, "ok")
except SystemError, e:
    print "Caught exception:", e
try:
    print it.next()
except StopIteration:
    print "ok"

it = gen(3)
print it.next()
print it.send('hi')
try: # this works outside of try-block, why not inside?
    it.throw(SystemError('ok'))
except SystemError, e:
    print "Caught exception:", e
try:
    print it.next()
except StopIteration:
    print "Caught StopIteration: ok"

it = gen(1)
it.close()

it = gen(2)
print it.next()
it.close()
