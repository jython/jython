"""
Choose wrong method when overloaded on both Object and Object[]
"""

import support

from pawt import swing

treePath = swing.tree.TreePath([1,2,3])


if len(treePath.getPath()) != 3:
    raise support.TestError("Object[] not passed correctly")

if swing.tree.TreePath(treePath.getPath()).getPath() != treePath.getPath():
    raise support.TestError("Object[] not passed and returned correctly")
