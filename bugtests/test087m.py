"""
Test multiple inheritance of 2 java classes
"""

import support

import java
class T(java.awt.Panel, java.awt.event.MouseAdapter):
    def __init__(self):
	pass
