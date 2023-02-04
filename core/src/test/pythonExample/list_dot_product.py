# list_dot_product.py

# Multiply-add of float vectors (without for loops)
# Also, multiplication as repetition.

n = 2

a = [1.2, 3.4, 5.6, 7.8] * (3 * n)
b = (4 * n) * [1.2, 4.5, 7.8]
n = 12 * n  # lists are this long

i = 0
sum = 0.0

while i < n:
    sum = sum + a[i] * b[i]
    i = i + 1
