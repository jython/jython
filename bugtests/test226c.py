
cnt = 0

def f(i):
    global cnt
    cnt = cnt + 1
    return i


l=[1,2,3,4,5,6]

l[f(2):f(4)] += ['a', 'b', 'c']

if cnt != 2:
    raise support.TestError('Number of calls is wrong')
 

if l != [1, 2, 3, 4, 'a', 'b', 'c', 5, 6]:
    raise support.TestError('list is wrong')

