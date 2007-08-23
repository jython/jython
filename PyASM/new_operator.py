import operator as base_op

concat = base_op.concat
eq = base_op.eq

def itemgetter(*args):
    def f(item):
        return item[args[0]]

