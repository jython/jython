[x] = iter([1])
print x

[x] = [1]
print x

[x, y] = iter([1, 2])
print x, y

[x, y] = [1, 2]
print x, y

try:
    [x] = [1,2]
except ValueError:
    pass
else:
    print 'fail: expected ValueError when sequence too long'

try:
    [x, y] = [1]
except ValueError:
    pass
else:
    print 'fail: expected ValueError when sequence too short'


try:
    [x] = iter([1,2])
except ValueError:
    pass
else:
    print 'fail: expected ValueError when sequence too long'

try:
    [x, y] = iter([1])
except ValueError:
    pass
else:
    print 'fail: expected ValueError when sequence too short'
