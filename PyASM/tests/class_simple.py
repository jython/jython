class Test(object):
    def __init__(self, arg):
        self.arg = arg

    def __str__(self):
        return "Test(%s)" % repr(self.arg)

print Test('hej')
