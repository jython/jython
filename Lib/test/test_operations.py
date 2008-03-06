# Python test set -- part 3, built-in operations.

from test_support import *

print_test('Operations (test_operations.py)', 1)

print_test('Numeric', 2)
print_test('+', 3)
assert 0 + 0 == 0
assert 0 + 3 == 3
assert 0 + 42 == 42
assert 3 + 0 == 3
assert 3 + 3 == 6
assert 3 + 42 == 45
assert 42 + 0 == 42
assert 42 + 3 == 45
assert 42 + 42 == 84
assert 0.0 + 0.0 == 0.0
assert 0.0 + 3.0 == 3.0
assert 0.0 + 42.0 == 42.0
assert 3.0 + 0.0 == 3.0
assert 3.0 + 3.0 == 6.0
assert 3.0 + 42.0 == 45.0
assert 42.0 + 0.0 == 42.0
assert 42.0 + 3.0 == 45.0
assert 42.0 + 42.0 == 84.0
assert 0L + 0L == 0L
assert 0L + 3L == 3L
assert 0L + 42L == 42L
assert 3L + 0L == 3L
assert 3L + 3L == 6L
assert 3L + 42L == 45L
assert 42L + 0L == 42L
assert 42L + 3L == 45L
assert 42L + 42L == 84L
print_test('-', 3)
assert 0 - 0 == 0
assert 0 - 3 == -3
assert 0 - 42 == -42
assert 3 - 0 == 3
assert 3 - 3 == 0
assert 3 - 42 == -39
assert 42 - 0 == 42
assert 42 - 3 == 39
assert 42 - 42 == 0
assert 0.0 - 0.0 == 0.0
assert 0.0 - 3.0 == -3.0
assert 0.0 - 42.0 == -42.0
assert 3.0 - 0.0 == 3.0
assert 3.0 - 3.0 == 0.0
assert 3.0 - 42.0 == -39.0
assert 42.0 - 0.0 == 42.0
assert 42.0 - 3.0 == 39.0
assert 42.0 - 42.0 == 0.0
assert 0L - 0L == 0L
assert 0L - 3L == -3L
assert 0L - 42L == -42L
assert 3L - 0L == 3L
assert 3L - 3L == 0L
assert 3L - 42L == -39L
assert 42L - 0L == 42L
assert 42L - 3L == 39L
assert 42L - 42L == 0L
print_test('*', 3)
assert 0 * 0 == 0
assert 0 * 3 == 0
assert 0 * 42 == 0
assert 3 * 0 == 0
assert 3 * 3 == 9
assert 3 * 42 == 126
assert 42 * 0 == 0
assert 42 * 3 == 126
assert 42 * 42 == 1764
assert 0.0 * 0.0 == 0.0
assert 0.0 * 3.0 == 0.0
assert 0.0 * 42.0 == 0.0
assert 3.0 * 0.0 == 0.0
assert 3.0 * 3.0 == 9.0
assert 3.0 * 42.0 == 126.0
assert 42.0 * 0.0 == 0.0
assert 42.0 * 3.0 == 126.0
assert 42.0 * 42.0 == 1764.0
assert 0L * 0L == 0L
assert 0L * 3L == 0L
assert 0L * 42L == 0L
assert 3L * 0L == 0L
assert 3L * 3L == 9L
assert 3L * 42L == 126L
assert 42L * 0L == 0L
assert 42L * 3L == 126L
assert 42L * 42L == 1764L
print_test('/', 3)
assert 0 / 3 == 0
assert 0 / 42 == 0
assert 3 / 3 == 1
assert 3 / 42 == 0
assert 42 / 3 == 14
assert 42 / 42 == 1
assert 0.0 / 3.0 == 0.0
assert 0.0 / 42.0 == 0.0
assert 3.0 / 3.0 == 1.0
assert abs(3.0 / 42.0 - 0.0714285714286) < 0.000001
assert 42.0 / 3.0 == 14.0
assert 42.0 / 42.0 == 1.0
assert 0L / 3L == 0L
assert 0L / 42L == 0L
assert 3L / 3L == 1L
assert 3L / 42L == 0L
assert 42L / 3L == 14L
assert 42L / 42L == 1L
print_test('**', 3)
assert 0 ** 0 == 1
assert 0 ** 3 == 0
assert 0 ** 42 == 0
assert 3 ** 0 == 1
assert 3 ** 3 == 27
assert 42 ** 0 == 1
assert 42 ** 3 == 74088
assert 0.0 ** 0.0 == 1.0
assert 0.0 ** 3.0 == 0.0
assert 0.0 ** 42.0 == 0.0
assert 3.0 ** 0.0 == 1.0
assert 3.0 ** 3.0 == 27.0
assert abs(3.0 ** 42.0 - 1.0941898913151237e+020) < 1e10
assert 42.0 ** 0.0 == 1.0
assert 42.0 ** 3.0 == 74088.0
assert abs(42.0 ** 42.0 - 1.5013093754529659e+068) < 1e58
assert 0L ** 0L == 1L
assert 0L ** 3L == 0L
assert 0L ** 42L == 0L
assert 3L ** 0L == 1L
assert 3L ** 3L == 27L
assert 3L ** 42L == 109418989131512359209L
assert 42L ** 0L == 1L
assert 42L ** 3L == 74088L
assert 42L ** 42L == 150130937545296572356771972164254457814047970568738777235893533016064L
print_test('%', 3)
assert 0 % 3 == 0
assert 0 % 42 == 0
assert 3 % 3 == 0
assert 3 % 42 == 3
assert 42 % 3 == 0
assert 42 % 42 == 0
assert 0.0 % 3.0 == 0.0
assert 0.0 % 42.0 == 0.0
assert 3.0 % 3.0 == 0.0
assert 3.0 % 42.0 == 3.0
assert 42.0 % 3.0 == 0.0
assert 42.0 % 42.0 == 0.0
assert 0L % 3L == 0L
assert 0L % 42L == 0L
assert 3L % 3L == 0L
assert 3L % 42L == 3L
assert 42L % 3L == 0L
assert 42L % 42L == 0L

print_test('Binary', 2)
print_test('|', 3)
assert 0 | 0 == 0
assert 0 | 3 == 3
assert 0 | 42 == 42
assert 3 | 0 == 3
assert 3 | 3 == 3
assert 3 | 42 == 43
assert 42 | 0 == 42
assert 42 | 3 == 43
assert 42 | 42 == 42
assert 0L | 0L == 0L
assert 0L | 3L == 3L
assert 0L | 42L == 42L
assert 3L | 0L == 3L
assert 3L | 3L == 3L
assert 3L | 42L == 43L
assert 42L | 0L == 42L
assert 42L | 3L == 43L
assert 42L | 42L == 42L
print_test('^', 3)
assert 0 ^ 0 == 0
assert 0 ^ 3 == 3
assert 0 ^ 42 == 42
assert 3 ^ 0 == 3
assert 3 ^ 3 == 0
assert 3 ^ 42 == 41
assert 42 ^ 0 == 42
assert 42 ^ 3 == 41
assert 42 ^ 42 == 0
assert 0L ^ 0L == 0L
assert 0L ^ 3L == 3L
assert 0L ^ 42L == 42L
assert 3L ^ 0L == 3L
assert 3L ^ 3L == 0L
assert 3L ^ 42L == 41L
assert 42L ^ 0L == 42L
assert 42L ^ 3L == 41L
assert 42L ^ 42L == 0L
print_test('&', 3)
assert 0 & 0 == 0
assert 0 & 3 == 0
assert 0 & 42 == 0
assert 3 & 0 == 0
assert 3 & 3 == 3
assert 3 & 42 == 2
assert 42 & 0 == 0
assert 42 & 3 == 2
assert 42 & 42 == 42
assert 0L & 0L == 0L
assert 0L & 3L == 0L
assert 0L & 42L == 0L
assert 3L & 0L == 0L
assert 3L & 3L == 3L
assert 3L & 42L == 2L
assert 42L & 0L == 0L
assert 42L & 3L == 2L
assert 42L & 42L == 42L
print_test('<<', 3)
assert 0 << 0 == 0
assert 0 << 3 == 0
assert 0 << 42 == 0
assert 3 << 0 == 3
assert 3 << 3 == 24
assert 3 << 42 == 13194139533312L
assert 42 << 0 == 42
assert 42 << 3 == 336
assert 42 << 42 == 184717953466368L
assert 0L << 0L == 0L
assert 0L << 3L == 0L
assert 0L << 42L == 0L
assert 3L << 0L == 3L
assert 3L << 3L == 24L
assert 3L << 42L == 13194139533312L
assert 42L << 0L == 42L
assert 42L << 3L == 336L
assert 42L << 42L == 184717953466368L
print_test('>>', 3)
assert 0 >> 0 == 0
assert 0 >> 3 == 0
assert 0 >> 42 == 0
assert 3 >> 0 == 3
assert 3 >> 3 == 0
assert 3 >> 42 == 0
assert 42 >> 0 == 42
assert 42 >> 3 == 5
assert 42 >> 42 == 0
assert 0L >> 0L == 0L
assert 0L >> 3L == 0L
assert 0L >> 42L == 0L
assert 3L >> 0L == 3L
assert 3L >> 3L == 0L
assert 3L >> 42L == 0L
assert 42L >> 0L == 42L
assert 42L >> 3L == 5L
assert 42L >> 42L == 0L

print_test('Comparison', 2)
print_test('<', 3)
assert (0 < 0) == 0
assert (0 < 3) == 1
assert (0 < 42) == 1
assert (3 < 0) == 0
assert (3 < 3) == 0
assert (3 < 42) == 1
assert (42 < 0) == 0
assert (42 < 3) == 0
assert (42 < 42) == 0
assert (0.0 < 0.0) == 0
assert (0.0 < 3.0) == 1
assert (0.0 < 42.0) == 1
assert (3.0 < 0.0) == 0
assert (3.0 < 3.0) == 0
assert (3.0 < 42.0) == 1
assert (42.0 < 0.0) == 0
assert (42.0 < 3.0) == 0
assert (42.0 < 42.0) == 0
assert (0L < 0L) == 0
assert (0L < 3L) == 1
assert (0L < 42L) == 1
assert (3L < 0L) == 0
assert (3L < 3L) == 0
assert (3L < 42L) == 1
assert (42L < 0L) == 0
assert (42L < 3L) == 0
assert (42L < 42L) == 0
print_test('>', 3)
assert (0 > 0) == 0
assert (0 > 3) == 0
assert (0 > 42) == 0
assert (3 > 0) == 1
assert (3 > 3) == 0
assert (3 > 42) == 0
assert (42 > 0) == 1
assert (42 > 3) == 1
assert (42 > 42) == 0
assert (0.0 > 0.0) == 0
assert (0.0 > 3.0) == 0
assert (0.0 > 42.0) == 0
assert (3.0 > 0.0) == 1
assert (3.0 > 3.0) == 0
assert (3.0 > 42.0) == 0
assert (42.0 > 0.0) == 1
assert (42.0 > 3.0) == 1
assert (42.0 > 42.0) == 0
assert (0L > 0L) == 0
assert (0L > 3L) == 0
assert (0L > 42L) == 0
assert (3L > 0L) == 1
assert (3L > 3L) == 0
assert (3L > 42L) == 0
assert (42L > 0L) == 1
assert (42L > 3L) == 1
assert (42L > 42L) == 0
print_test('==', 3)
assert (0 == 0) == 1
assert (0 == 3) == 0
assert (0 == 42) == 0
assert (3 == 0) == 0
assert (3 == 3) == 1
assert (3 == 42) == 0
assert (42 == 0) == 0
assert (42 == 3) == 0
assert (42 == 42) == 1
assert (0.0 == 0.0) == 1
assert (0.0 == 3.0) == 0
assert (0.0 == 42.0) == 0
assert (3.0 == 0.0) == 0
assert (3.0 == 3.0) == 1
assert (3.0 == 42.0) == 0
assert (42.0 == 0.0) == 0
assert (42.0 == 3.0) == 0
assert (42.0 == 42.0) == 1
assert (0L == 0L) == 1
assert (0L == 3L) == 0
assert (0L == 42L) == 0
assert (3L == 0L) == 0
assert (3L == 3L) == 1
assert (3L == 42L) == 0
assert (42L == 0L) == 0
assert (42L == 3L) == 0
assert (42L == 42L) == 1
print_test('<=', 3)
assert (0 <= 0) == 1
assert (0 <= 3) == 1
assert (0 <= 42) == 1
assert (3 <= 0) == 0
assert (3 <= 3) == 1
assert (3 <= 42) == 1
assert (42 <= 0) == 0
assert (42 <= 3) == 0
assert (42 <= 42) == 1
assert (0.0 <= 0.0) == 1
assert (0.0 <= 3.0) == 1
assert (0.0 <= 42.0) == 1
assert (3.0 <= 0.0) == 0
assert (3.0 <= 3.0) == 1
assert (3.0 <= 42.0) == 1
assert (42.0 <= 0.0) == 0
assert (42.0 <= 3.0) == 0
assert (42.0 <= 42.0) == 1
assert (0L <= 0L) == 1
assert (0L <= 3L) == 1
assert (0L <= 42L) == 1
assert (3L <= 0L) == 0
assert (3L <= 3L) == 1
assert (3L <= 42L) == 1
assert (42L <= 0L) == 0
assert (42L <= 3L) == 0
assert (42L <= 42L) == 1
print_test('>=', 3)
assert (0 >= 0) == 1
assert (0 >= 3) == 0
assert (0 >= 42) == 0
assert (3 >= 0) == 1
assert (3 >= 3) == 1
assert (3 >= 42) == 0
assert (42 >= 0) == 1
assert (42 >= 3) == 1
assert (42 >= 42) == 1
assert (0.0 >= 0.0) == 1
assert (0.0 >= 3.0) == 0
assert (0.0 >= 42.0) == 0
assert (3.0 >= 0.0) == 1
assert (3.0 >= 3.0) == 1
assert (3.0 >= 42.0) == 0
assert (42.0 >= 0.0) == 1
assert (42.0 >= 3.0) == 1
assert (42.0 >= 42.0) == 1
assert (0L >= 0L) == 1
assert (0L >= 3L) == 0
assert (0L >= 42L) == 0
assert (3L >= 0L) == 1
assert (3L >= 3L) == 1
assert (3L >= 42L) == 0
assert (42L >= 0L) == 1
assert (42L >= 3L) == 1
assert (42L >= 42L) == 1
print_test('and', 3)
assert (0 and 0) == 0
assert (0 and 3) == 0
assert (0 and 42) == 0
assert (3 and 0) == 0
assert (3 and 3) == 3
assert (3 and 42) == 42
assert (42 and 0) == 0
assert (42 and 3) == 3
assert (42 and 42) == 42
assert (0.0 and 0.0) == 0.0
assert (0.0 and 3.0) == 0.0
assert (0.0 and 42.0) == 0.0
assert (3.0 and 0.0) == 0.0
assert (3.0 and 3.0) == 3.0
assert (3.0 and 42.0) == 42.0
assert (42.0 and 0.0) == 0.0
assert (42.0 and 3.0) == 3.0
assert (42.0 and 42.0) == 42.0
assert (0L and 0L) == 0L
assert (0L and 3L) == 0L
assert (0L and 42L) == 0L
assert (3L and 0L) == 0L
assert (3L and 3L) == 3L
assert (3L and 42L) == 42L
assert (42L and 0L) == 0L
assert (42L and 3L) == 3L
assert (42L and 42L) == 42L
print_test('or', 3)
assert (0 or 0) == 0
assert (0 or 3) == 3
assert (0 or 42) == 42
assert (3 or 0) == 3
assert (3 or 3) == 3
assert (3 or 42) == 3
assert (42 or 0) == 42
assert (42 or 3) == 42
assert (42 or 42) == 42
assert (0.0 or 0.0) == 0.0
assert (0.0 or 3.0) == 3.0
assert (0.0 or 42.0) == 42.0
assert (3.0 or 0.0) == 3.0
assert (3.0 or 3.0) == 3.0
assert (3.0 or 42.0) == 3.0
assert (42.0 or 0.0) == 42.0
assert (42.0 or 3.0) == 42.0
assert (42.0 or 42.0) == 42.0
assert (0L or 0L) == 0L
assert (0L or 3L) == 3L
assert (0L or 42L) == 42L
assert (3L or 0L) == 3L
assert (3L or 3L) == 3L
assert (3L or 42L) == 3L
assert (42L or 0L) == 42L
assert (42L or 3L) == 42L
assert (42L or 42L) == 42L

print_test('Indexing', 2)
lst = range(5)

print_test('[i]', 3)
assert lst[0] == 0
assert lst[3] == 3
lst[3] = 99 
assert lst[3] == 99
lst[3] = 3

print_test('[i:j]', 3)
assert lst[:2] == [0,1]
assert lst[-3:] == [2,3,4]
assert lst[2:] == [2,3,4]
assert lst[0:2] == [0,1]
assert lst[1:3] == [1,2]

print_test('in', 3)
assert 2 in lst
assert not (8 in lst)

print_test('not in', 3)
assert 8 not in lst
assert not (2 not in lst)
