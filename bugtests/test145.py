"""
Check autocreate of proxy.
"""

import support

import java

class myPanel(java.awt.Panel) :
    def __init__(self) :
        self.layout = java.awt.GridLayout(1,2)

p = myPanel()
if p.layout != p.getLayout():
    raise support.TestError("Should be same")
