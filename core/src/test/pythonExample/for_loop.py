# for_loop.py

# Execute various kinds of for loop
# This is primarily a test of creating iterators
# on the types involved.

# Opcodes GET_ITER, FOR_ITER, JUMP_BACKWARD

tuple_sum = 0
for i in (13, 14, 15):
    tuple_sum = tuple_sum + i

list_sum = 0
for j in (a := [-30, 48, -60]):
    list_sum = list_sum + j

hello_list = []
for c in "hello":
    hello_list.append(c)


