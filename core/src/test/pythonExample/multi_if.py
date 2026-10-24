# multi_if.py

# Opcodes POP_JUMP_FORWARD_IF_FALSE, POP_JUMP_FORWARD_IF_TRUE,
# and JUMP_FORWARD.

a = False
b = False

if a and b:
    r = 2
elif a or b:
    r = 1
else:
    r = 0

a = False
b = True
if a and b:
    r1 = 2
elif a or b:
    r1 = 1
else:
    r1 = 0

a = True
b = False
if a and b:
    r2 = 2
elif a or b:
    r2 = 1
else:
    r2 = 0

a = True
b = True
if a and b:
    r3 = 2
elif a or b:
    r3 = 1
else:
    r3 = 0
