# PR#235, JPython crashes (i.e. uncaught Java exception) under strange
# (illegal) input.

bogus = '''\
def f(x, z, x):
    pass

f(y=1)
'''

try:
    compile(bogus, '<string>', 'exec')
except SyntaxError:
    pass
