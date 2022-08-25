# call_method_builtin.py

# Call methods on some built-in types to exercise CALL_METHOD

a = "abracadabra"

asc = a.isascii()
A = a.upper()

# Signature: strip(self, chars=None, /)
cad = a.strip("bar")
wood = "   \twood \x85\r\n".strip()

# Signature: replace(self, old, new, count=-1, /)
sox = a.replace("bra", "sock")
sock = a.replace("bra", "sock", 1)

# Signature: split(self, /, sep=None, maxsplit=-1)
split1 = a.split('br', 1)
split = a.split('bra')
split0 = a.split()
split1k = a.split('br', maxsplit=1)
split2k = a.split(maxsplit=4, sep='a')

# Force use of CALL_FUNCTION_EX 0
sock_ex = a.replace(*("bra", "sock", 1))

# Force use of CALL_FUNCTION_EX 1
split1k_ex = a.split('br', **{'maxsplit':1})

