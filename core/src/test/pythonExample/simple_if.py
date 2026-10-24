# simple_if.py

# Opcodes POP_JUMP_FORWARD_IF_FALSE, JUMP_FORWARD
b = False
if b:
    r0 = 1
else:
    r0 = 0

b = True
if b:
    r1 = 1
else:
    r1 = 0

b = 0
if b:
    r2 = 1
else:
    r2 = 0

b = 1
r3 = 1 if b else 0

b = ""
r4 = 1 if b else 0

b = "something"
r5 = 1 if b else 0

b = None
r6 = 1 if b else 0

# Opcodes POP_JUMP_FORWARD_IF_NOT_NONE
if b is None:
    r7 = 1
else:
    r7 = 0

# Opcode is POP_JUMP_FORWARD_IF_NONE
if b is not None:
    r8 = 1
else:
    r8 = 0
