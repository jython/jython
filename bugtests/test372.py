"""
Test for patch "[ 577728 ] struct.java now accepts 64bits ints"
"""

import support

from struct import *

#unsigned long check
try:
	pack('<Q',-1)
	raise support.TestError('Error: unsigned long should not work')
except (TypeError, error):
	pass
	
try:
	pack('>Q',-1)
	raise support.TestError('Error: unsigned long should not work')
except (TypeError, error):
	pass

big_long=0x10000000000000000L
#oversized unsigned long check
try:
	print pack('<Q',big_long)
	print 'Error, should not try to pack >64bits ints'
except OverflowError:
	pass
	
try:
	print pack('>Q',big_long)
	print 'Error, should not try to pack >64bits ints'
except OverflowError:
	pass

#oversized positive signed long check
try:
	print pack('<q',big_long)
	print 'Error, should not try to pack >64bits ints'
except OverflowError:
	pass
	
try:
	print pack('>q',big_long)
	print 'Error, should not try to pack >64bits ints'
except OverflowError:
	pass

#oversized negative signed long check
big_long=-big_long
try:
	print pack('<q',big_long)
	print 'Error, should not try to pack >64bits ints'
except OverflowError:
	pass
	
try:
	print pack('>q',big_long)
	print 'Error, should not try to pack >64bits ints'
except OverflowError:
	pass

u_data=(0x1L,0x10000L,0x100000000L)
s_data=(0x1L,-0x10000L,0x0FFFFFFFFL,-0x100000000L)
#internal pack-unpack coherence check

s=pack('<QQQ',u_data[0],u_data[1],u_data[2])
if u_data!=unpack('<QQQ',s):
	raise support.TestError("internal coherence error:		%s ==> %s ==> %s"%(`u_data`,`s`,`unpack('<QQQ',s)`))


s=pack('>QQQ',u_data[0],u_data[1],u_data[2])
if u_data!=unpack('>QQQ',s):
	raise support.TestError("internal coherence error:		%s ==> %s ==> %s"%(`u_data`,`s`,`unpack('>QQQ',s)`))

s=pack('<qqqq',s_data[0],s_data[1],s_data[2],s_data[3])
if s_data!=unpack('<qqqq',s):
	raise support.TestError("internal coherence error:		%s ==> %s ==> %s"%(`s_data`,`s`,`unpack('<qqqq',s)`))

s=pack('>qqqq',s_data[0],s_data[1],s_data[2],s_data[3])
if s_data!=unpack('>qqqq',s):
	raise support.TestError("internal coherence error:		%s ==> %s ==> %s"%(`s_data`,`s`,`unpack('>qqqq',s)`))

#external unpack coherence check
string_from_CPython='\x00\x00\x00\x00\x00\x01\x11p\xff\xff\xff\xff\xff\xfe\xc7\x80\xff\xff\xff\xff\xff\xff\xff\xfb\x00\x00\x00\x00\x00\x018\x80'
if (70000,-80000,-5,80000)!=unpack('!Qqqq',string_from_CPython):
	raise support.TestError('Error unpacking from CPython !')

