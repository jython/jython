# attr_access_builtin.py

# True, "hello" and 1 will be in co_consts
true_and = True.__and__.__name__

# PyJavaMethod.self exposed as __self__ is target
hello = "hello".replace.__self__

# 'int.__sub__' from logic behind type.__qualname__
one_sub = (1).__sub__.__qualname__

# bool is in the dictionary of builtins
bool_add = bool.__add__.__name__

