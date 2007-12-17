def consumer(func):
    def wrapper(*args,**kw):
        gen = func(*args, **kw)
        gen.next()
        return gen
    return wrapper

@consumer
def make_receiver(n):
    while True:
        x = (yield) * n
        print x

if __name__ == 'main':
    receiver = make_receiver(3)
    for i in xrange(20):
        receiver.send(i)
    
