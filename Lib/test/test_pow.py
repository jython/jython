import sys
from test_support import *

print_test("power (test_pow.py)", 2)

def powtest(type):
    if (type!=float):
        print_test("simple identities", 4)
        for i in range(-1000, 1000):
            assert type(i)**0 == 1
            assert type(i)**1 == type(i)
            if i > 0:
            	assert type(0)**i == type(0)
            	assert type(1)**i == type(1)
            
        print_test("cubes")
        for i in range(-100, 100):
            assert type(i)**3 == i*i*i
    
        print_test("powers of two")
        pow2=1
        for i in range(0,31):
            assert 2**i == pow2
            if i!=30: pow2=pow2*2
    # modPow() has known assertion failures in all Sun 1.1 JDKs after 1.1.5.
    # Apparently, Sun is not going to fix this for 1.1 since the bug still
    # exists in 1.1.8 and the bug ID 4098742 is closed.
    if sys.platform.startswith('java1.1') and type == long:
        print_test('modpow... skipping due to JVM bugs', 4)
        return
    print_test("modpow", 4)
    il, ih = -20, 20
    jl, jh = -5,   5
    kl, kh = -10, 10
    compare = cmp
    if (type==float):
        il=1
        compare = fcmp
    elif (type==int):
        jl=0
    elif (type==long):
        jl,jh = 0, 15
    for i in range(il, ih+1):
         for j in range(jl,jh+1):
             for k in range(kl, kh+1):
                 if (k!=0):
                     assert compare(pow(type(i),j,k), type(i)**j % type(k)) == 0, '%s, %s, %s' % (i,j,k)

print_test("integers", 3)
powtest(int)
print_test("longs", 3)
powtest(long)
print_test("floats", 3)
powtest(float)

print_test("mixed-mode", 3)

assert 3**3%8 == pow(3,3,8) == 3
assert 3**3%-8 == pow(3,3,-8) == -5
assert 3**2%-2 == pow(3,2,-2) == -1
assert -3**3%8 == pow(-3,3,8) == 5
assert -3**3%-8 == pow(-3,3,-8) == -3
assert 5**2%-8 == pow(5,2,-8) == -7

assert 3L**3%8 == pow(3L,3,8) == 3L
assert 3L**3%-8 == pow(3,3L,-8) == -5L
assert 3L**2%-2 == pow(3,2,-2L) == -1L
assert -3L**3%8 == pow(-3L,3,8) == 5L
assert -3L**3%-8 == pow(-3,3L,-8) == -3L
assert 5L**2%-8 == pow(5,2,-8L) == -7L

assert 3.**3%8 == pow(3.,3,8) == 3.
assert 3.**3%-8 == pow(3,3.,-8) == -5.
assert 3.**2%-2 == pow(3,2,-2.) == -1.
assert -3.**3%8 == pow(-3.,3,8) == 5.
assert -3.**3%-8 == pow(-3,3.,-8) == -3.
assert 5.**2%-8 == pow(5,2,-8.) == -7.

if sys.platform.startswith('java1.1'):
    print_test("miscellaneous... skipping due to JVM bugs", 3)
else:    
    print_test("miscellaneous", 3)
    for i in range(-10, 11):
        for j in range(0, 6):
            for k in range(-7, 11):
                if (j>=0 and k!=0):
                    o=pow(i,j) % k
                    n=pow(i,j,k)
                    assert o == n, 'Integer mismatch: %d, %d, %d' % (i,j,k)
                if (j>=0 and k<>0):
                    o=pow(long(i),j) % k
                    n=pow(long(i),j,k)
                    assert o == n, 'Long mismatch: %s, %s, %s' % (i,j,k)
                if (i>=0 and k<>0):
                    o=pow(float(i),j) % k
                    n=pow(float(i),j,k)
                    assert o == n, 'Float mismatch: %g, %g, %g' % (i,j,k)
