"""
[ 522828 ] struct.pack('>NNs', v) fails for NN > 20
"""

import support

import struct 
a = 'abcd' * 8
struct.pack('>32s', a) 

