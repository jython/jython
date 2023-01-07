# tuple_dot_product.py

# Multiply-add of int and float vectors (without for loops)

a = (2, 3, 4)
b = (3, 4, 6)
n = 3

# ? sum

sum = a[0] * b[0]
i = 1
while i < n:
    sum = sum + a[i] * b[i]
    i = i + 1

a= (1., 2., 3., 4.)
b = (4., 3., 4., 5.)
n = 4

sum2 = a[0] * b[0]
i = 1
while i < n:
    sum2 = sum2 + a[i] * b[i]
    i = i + 1
