"""
test newline in text pickles #437215
"""

import pickle
s1="line1\nline2\nline3"
s2="line4\nline5\nline6"
l = [s1, s2]
p = pickle.dumps(l)   # newlines won't be escaped
l2 = pickle.loads(p)  # blows up

