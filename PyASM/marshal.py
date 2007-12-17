"""Marshal module written in Python.

Written by Tobias Ivarsson <tobias@thobe.org> for the Jython project.

"""

__all__ = ('dump','dumps','load','loads',)

from StringIO import StringIO
import string
import struct
from types import *#NoneType, EllipsisType, CodeType
from org.python.newcompiler.pyasm import CodeReader
from org.python.newcompiler.pyasm.util import PythonDis
from pyasm import ASMVisitor as CodeVisitor
from jarray import array
from java.io import PrintWriter
from java.lang.System import out
stdout = PrintWriter(out)
try:
    import new
except ImportError:
    new = None

def byteArray(string):
    return array(list(string),'c')

# marshal types
mappings = dict(
    TYPE_NULL		= ('0', 0),
    TYPE_NONE		= ('N', NoneType),
    TYPE_FALSE		= ('F', 0),
    TYPE_TRUE		= ('T', 0),
    TYPE_STOPITER	= ('S', 0),
    TYPE_ELLIPSIS   	= ('.', EllipsisType),
    TYPE_INT		= ('i', int),
    TYPE_INT64		= ('I', 0),
    TYPE_FLOAT		= ('f', float),
    TYPE_BINARY_FLOAT	= ('g', 0),
    TYPE_COMPLEX	= ('x', complex),
    TYPE_BINARY_COMPLEX	= ('y', 0),
    TYPE_LONG		= ('l', long),
    TYPE_STRING		= ('s', str),
    TYPE_INTERNED	= ('t', 0),
    TYPE_STRINGREF	= ('R', 0),
    TYPE_TUPLE		= ('(', tuple),
    TYPE_LIST		= ('[', list),
    TYPE_DICT		= ('{', dict),
    TYPE_CODE		= ('c', CodeType),
    TYPE_UNICODE	= ('u', unicode),
    TYPE_UNKNOWN	= ('?', 0),
    TYPE_SET		= ('<', set),
    TYPE_FROZENSET  	= ('>', frozenset),
    )

class Marshaller:
    
    dispatch = {}
    
    def __init__(self, f):
	self.f = f
        
    def dump(self, x):
	self.dispatch[type(x)](self, x)

    def w_long64(self, x):
	self.w_long(x)
	self.w_long(x>>32)

    def w_long(self, x):
	write = self.f.write
	write(chr((x)     & 0xff))
	write(chr((x>> 8) & 0xff))
	write(chr((x>>16) & 0xff))
	write(chr((x>>24) & 0xff))

    def w_short(self, x):
	write = self.f.write
	write(chr((x)     & 0xff))
	write(chr((x>> 8) & 0xff))

    def dump_none(self, x):
	self.f.write(TYPE_NONE)
    dispatch[NoneType] = dump_none

    def dump_bool(self, x):
        if x:
            self.f.write(TYPE_TRUE)
        else:
            self.f.write(TYPE_FALSE)
    dispatch[BooleanType] = dump_bool

    def dump_ellipsis(self, x):
	self.f.write(TYPE_ELLIPSIS)
    try:
	dispatch[EllipsisType] = dump_ellipsis
    except NameError:
	pass

    def dump_int(self, x):
	y = x>>31
	if y and y != -1:
	    self.f.write(TYPE_INT64)
	    self.w_long64(x)
	else:
	    self.f.write(TYPE_INT)
	    self.w_long(x)
    dispatch[IntType] = dump_int

    def dump_long(self, x):
	self.f.write(TYPE_LONG)
	sign = 1
	if x < 0:
	    sign = -1
	    x = -x
	digits = []
	while x:
	    digits.append(x & 0x7FFF)
	    x = x>>15
	self.w_long(len(digits) * sign)
	for d in digits:
	    self.w_short(d)
    dispatch[LongType] = dump_long

    def dump_float(self, x):
	write = self.f.write
	write(TYPE_FLOAT)
	s = `x`
	write(chr(len(s)))
	write(s)
    dispatch[FloatType] = dump_float

    def dump_complex(self, x):
	write = self.f.write
	write(TYPE_COMPLEX)
	s = `x.real`
	write(chr(len(s)))
	write(s)
	s = `x.imag`
	write(chr(len(s)))
	write(s)
    try:
	dispatch[ComplexType] = dump_complex
    except NameError:
	pass

    def dump_string(self, x):
	self.f.write(TYPE_STRING)
	self.w_long(len(x))
	self.f.write(x)
    dispatch[StringType] = dump_string

    def dump_tuple(self, x):
	self.f.write(TYPE_TUPLE)
	self.w_long(len(x))
	for item in x:
	    self.dump(item)
    dispatch[TupleType] = dump_tuple

    def dump_list(self, x):
	self.f.write(TYPE_LIST)
	self.w_long(len(x))
	for item in x:
	    self.dump(item)
    dispatch[ListType] = dump_list

    def dump_dict(self, x):
	self.f.write(TYPE_DICT)
	for key, value in x.items():
	    self.dump(key)
	    self.dump(value)
	self.f.write(TYPE_NULL)
    dispatch[DictionaryType] = dump_dict

    def dump_code(self, x):
	self.f.write(TYPE_CODE)
	self.w_short(x.co_argcount)
	self.w_short(x.co_nlocals)
	self.w_short(x.co_stacksize)
	self.w_short(x.co_flags)
	self.dump(x.co_code)
	self.dump(x.co_consts)
	self.dump(x.co_names)
	self.dump(x.co_varnames)
	self.dump(x.co_filename)
	self.dump(x.co_name)
	self.w_short(x.co_firstlineno)
	self.dump(x.co_lnotab)
    try:
	dispatch[CodeType] = dump_code
    except NameError:
	pass


class NULL:
    pass

class Unmarshaller:

    def __init__(self, f, magic=None):
        self.magic = magic
        self.strings = []
        self.__visitor = None
	self.f = f

    def load(self):
	ident = self.f.read(1)
	if not ident:
	    raise EOFError
	return self.dispatch[ident](self)

    def read_long(self):
        a = long( self.read_byte() )
	b = long( self.read_byte() )
	c = long( self.read_byte() )
	d = long( self.read_byte() )
	res = a | (b<<8) | (c<<16) | (d<<24)
	if res & 0x80000000 and res > 0:
	    res = res - 0x100000000L
        return int(res)

    def read_long64(self):
        a = long( self.read_byte() )
	b = long( self.read_byte() )
	c = long( self.read_byte() )
	d = long( self.read_byte() )
        e = long( self.read_byte() )
	f = long( self.read_byte() )
	g = long( self.read_byte() )
	h = long( self.read_byte() )
        res = a | (b<<8) | (c<<16) | (d<<24)
        res |= (e<<32) | (f<<40) | (g<<48) | (h<<56)
        if res & (0x8<<60L) and res > 0:
            res = res - (1<<64L)
        return res

    def read_short(self):
	lo = self.read_byte()
	hi = self.read_byte()
	res = lo | (hi<<8)
	if res & 0x8000:
	    res = res - 0x10000
	return res

    def read_byte(self):
        res = self.f.read(1)
        if not res:
            raise EOFError
        return ord(res)

    def read_string(self, size):
        res = self.f.read(size)
        if len(res) != size:
            raise EOFError
        return res

    def load_null(self):
        return NULL

    def load_none(self):
        return None

    def load_false(self):
        return False

    def load_true(self):
        return True

    def load_stopiter(self):
        return StopIteration

    def load_ellipsis(self):
        return Ellipsis

    def load_int(self):
        return self.read_long()

    def load_int64(self):
        return self.read_long64()

    def load_float(self):
        size = self.read_byte()
        return string.atof( self.read_string(size) )
    
    def load_binary_float(self):
        # There is a bug in the Jython struct module, it produces and reads
        # packed strings reversed. This method compensates for that.
        def reverse(lizt):
            while lizt:
                yield lizt.pop()
        data = "".join(reverse(list(self.read_string(8))))
        # had the struct module woked correctly, this would just be a matter of
        # data = self.read_string(8)
        return struct.unpack('d', data)[0]

    def load_complex(self):
        real = self.load_float()
        imag = self.load_float()
        return complex(real, imag)

    def load_binary_complex(self):
        real = self.load_binary_float()
        imag = self.load_binary_float()
        return complex(real, imag)

    def load_long(self):
        size = self.read_long()
        sign = 1
        if size < 0:
            sign = -1
            size = -size
        res = 0L
        for i in xrange(size):
            x = self.read_short()
	    res = res | (x<<(i*15L))
        return res * sign

    def load_string(self):
        size = self.read_long()
        return self.read_string(size)

    def load_interned(self):
        string = self.load_string()
        self.strings.append(string)
        return string

    def load_stringref(self):
        return self.strings[self.read_long()]

    def load_tuple(self):
        return tuple(self.load_list())

    def load_list(self):
        size = self.read_long()
        res = []
        for i in xrange(size):
            res.append(self.load())
        return res

    def load_dict(self):
        res = {}
        while True:
            key = self.load()
            if key is NULL:
                break
            value = self.load()
            res[key] = value
        return res
    

    def load_code(self):
        lastVisitor = self.__visitor
        visitor= _visitor= self.__visitor= CodeVisitor(self.magic, lastVisitor)
        #visitor = PythonDis(visitor, stdout, True)
        try:
            argcount = self.read_long()
            nlocals = self.read_long()
            stacksize = self.read_long()
            flags = self.read_long()
            code = self.load()
            constants = self.load()
            names = self.load()
            varnames = self.load()
            freevars = self.load()
            cellvars = self.load()
            filename = self.load()
            name = self.load()
            firstlineno = self.read_long()
            lnotab = self.load()

            visitor.visitCode(
                argcount,
                nlocals,
                stacksize,
                flags,
                constants,
                names,
                varnames,
                freevars,
                cellvars,
                filename,
                name,
                firstlineno,)
            CodeReader(byteArray(code), firstlineno,
                       byteArray(lnotab)).accept(visitor)
            
            return _visitor.getCode()
        finally:
            self.__visitor = lastVisitor

    def load_unicode(self):
        size = self.read_long()
        return EncodedFile(StringIO(self.read_string(size)),'utf8').read()
    
    def load_unknown(self):
        raise TypeError("Unknown type in unmarshalling")

    def load_set(self):
        return set(self.load_list())

    def load_frozenset(self):
        return frozenset(self.load_list())

    dispatch=dict([(i,locals()['load'+n[4:].lower()]) for n,(i,t) in mappings.items()])


    def load(self):
	c = self.f.read(1)
	if not c:
	    raise EOFError
        return self.dispatch[c](self)


    def r_short(self):
	read = self.f.read
	lo = ord(read(1))
	hi = ord(read(1))
	x = lo | (hi<<8)
	if x & 0x8000:
	    x = x - 0x10000
	return x

    def r_long(self):
	read = self.f.read
	a = ord(read(1))
	b = ord(read(1))
	c = ord(read(1))
	d = ord(read(1))
	x = a | (b<<8) | (c<<16) | (d<<24)
	if x & 0x80000000 and x > 0:
	    x = string.atoi(x - 0x100000000L)
	return x

    def r_long64(self):
	a = self.r_long()
	b = self.r_long()
	return a | (b<<32)


def dump(x, f):
    Marshaller(f).dump(x)

def load(f):
    return Unmarshaller(f).load()

def dumps(x):
    f = StringIO()
    dump(x, f)
    return f.getvalue()

def loads(s):
    f = StringIO(s)
    return load(f)
