"""
thread.LockType doesn't exist
"""

import support

import thread
t = thread.LockType

if t != type(thread.allocate_lock()):
    raise support.TestError("thread.LockType has wrong value " + `t`)
