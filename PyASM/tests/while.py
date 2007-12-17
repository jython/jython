# expected output: 4 2
x = 5
while x > 0:
    if x == 2:
        break
    x -= 1
    if x == 3:
        continue
    print x,
print
