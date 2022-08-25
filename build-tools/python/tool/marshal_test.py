# Generate test material for simple marshal tests

import io, sys, os, os.path, math
import marshal, array, py_compile
import dis, inspect, types

def make_interned(b):
    "Change type code to interned equivalent"
    tc = chr(b[0]&0x7f)
    a0 = None
    if tc == 'a': a0 = b'A'
    elif tc == 'u': a0 = b't'
    elif tc == 'z': a0 = b'Z'

    if a0:
        return a0 + b[1:]
    else:
        return b


def as_byte_array(s):
    a = array.array('B', s)
    vals = map(lambda v: format(v, "#04x"), a)
    return "new byte[] {" + ", ".join(vals) + "}"


def as_java(v):
    "Limited translation to Java"
    if isinstance(v, bool):
        return str(v).lower()
    elif isinstance(v, int):
        if v < 2**31 and v >= -2**31:
            return str(v)
        else:
            return f"new BigInteger(\"{v:d}\")"
    elif isinstance(v, float):
        if math.isinf(v):
            if v > 0:
                return "Double.POSITIVE_INFINITY"
            else:
                return "Double.NEGATIVE_INFINITY"
        elif math.isnan(v):
            return "Double.NaN"
        else:
            return v.hex()
    elif isinstance(v, str):
        return f"\"{v:s}\""
    elif isinstance(v, tuple):
        args = ", ".join(map(as_java, v))
        return "Py.tuple(" + args + ")"
    elif isinstance(v, list):
        args = ", ".join(map(as_java, v))
        return "new PyList(List.of(" + args + "))"
    elif isinstance(v, dict):
        args = ", ".join(map(as_java, v.items()))
        return "PyDict.fromKeyValuePairs(" + args + ")"
    elif isinstance(v, bytes):
        return as_bytes(v)
    elif isinstance(v, StopIteration):
        return "Py.StopIteration"
    else:
        return "Py.None"


def as_bytes(s):
    a = array.array('B', s)
    values = map(lambda v: format(v, "#04x"), a)
    return "bytes(" + ", ".join(values) + ")"


def print_load_example(expr, env = locals()):
    result = eval(expr, None, env)
    b = marshal.dumps(result)
    tc = chr(b[0]&0x7f)
    if tc in 'auz':
        # Force to intern the string
        b = make_interned(b)
        tc = chr(b[0]&0x7f)
    print(f"loadExample( \"{expr:s}\", // tc='{tc:s}'")
    javabytes = as_bytes(b)
    print(f"{javabytes:s},")
    java = as_java(result)
    print(f"{java:s} ),")


# str
sa = "hello"
sb = "sæll"
su = "\U0001f40d"


# tuple
t = (1,2,3)

# list
list0 = []
list1 = [sa]
list3 = [sa, 2, t]
listself = [1, 2, 3]
listself[1] = listself

expressions = [
    "None", 
    "False", "True",
    "0", "1", "-42", "2**31-1", "2047**4", "2**45", "-42**15",
    "0.", "1.", "-42.", "1e42", "1.8e300", "1.12e-308",
    "float.fromhex('0x1.fffffffffffffp1023')", "float.fromhex('-0x1.p-1022')",
    "float('inf')", "float('-inf')", "float('nan')",
    "'hello'", "'sæll'", "'\U0001f40d'",
    "()", "(sa,sa,sa)", "(sb,sb,t,t)",
    "[]", "[sa]", "[sa, 2, t]",
    "{}", "{sa:sb}", "dict(python=su)", "{sa:1, sb:2, su:t}",
]

for x in expressions:
    print_load_example(x)

