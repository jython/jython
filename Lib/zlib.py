
from java import util, lang
import jarray, binascii

class error(Exception):
    pass


DEFLATED = 8
MAX_WBITS = 15
DEF_MEM_LEVEL = 8
ZLIB_VERSION = "1.1.3"
Z_BEST_COMPRESSION = 9
Z_BEST_SPEED = 1

Z_FILTERED = 1
Z_HUFFMAN_ONLY = 2

Z_DEFAULT_COMPRESSION = -1
Z_DEFAULT_STRATEGY = 0

# Most options are removed because java does not support them
# Z_NO_FLUSH = 0
# Z_SYNC_FLUSH = 2
# Z_FULL_FLUSH = 3
Z_FINISH = 4

def adler32(string, value=1):
    if value != 1: 
        raise ValueError, "adler32 only support start value of 1"
    checksum = util.zip.Adler32()
    checksum.update(lang.String.getBytes(string))
    return lang.Long(checksum.getValue()).intValue()

def crc32(string, value=0):
    return binascii.crc32(string, value)


def compress(string, level=6):
    if level < Z_BEST_SPEED or level > Z_BEST_COMPRESSION:
        raise error, "Bad compression level"
    deflater = util.zip.Deflater(level, 0)
    deflater.setInput(string, 0, len(string))
    deflater.finish()
    return _get_deflate_data(deflater)

def decompress(string, wbits=0, bufsize=16384):
    inflater = util.zip.Inflater(wbits < 0)
    inflater.setInput(string)
    return _get_inflate_data(inflater)
    

class compressobj:
    def __init__(self, level=6, method=DEFLATED, wbits=MAX_WBITS,
                       memLevel=0, strategy=0):
        if abs(wbits) > MAX_WBITS or abs(wbits) < 8:
            raise ValueError, "Invalid initialization option"
        self.deflater = util.zip.Deflater(level, wbits < 0)
        self.deflater.setStrategy(strategy)
        if wbits < 0:
            _get_deflate_data(self.deflater)

    def compress(self, string):
        self.deflater.setInput(string, 0, len(string))
        return _get_deflate_data(self.deflater)
        
    def flush(self, mode=Z_FINISH):
        if mode != Z_FINISH:
            raise ValueError, "Invalid flush option"
        self.deflater.finish()
        return _get_deflate_data(self.deflater)

class decompressobj:
    def __init__(self, wbits=0):
        if abs(wbits) > MAX_WBITS or abs(wbits) < 8:
            raise ValueError, "Invalid initialization option"
        self.inflater = util.zip.Inflater(wbits < 0)
        self.unused_data = ""

    def decompress(self, string):
        self.inflater.setInput(string)
        r = _get_inflate_data(self.inflater)
        # Arrgh. This suck.
        self.unused_data = " " * self.inflater.getRemaining()
        return r

    def flush(self):
        #self.inflater.finish()
        return _get_inflate_data(self.inflater)


def _get_deflate_data(deflater):
    buf = jarray.zeros(1024, 'b')
    sb = lang.StringBuffer()
    while not deflater.finished():
        l = deflater.deflate(buf)
        if l == 0:
            break
        sb.append(lang.String(buf, 0, 0, l))
    return sb.toString()

        
def _get_inflate_data(inflater):
    buf = jarray.zeros(1024, 'b')
    sb = lang.StringBuffer()
    while not inflater.finished():
        l = inflater.inflate(buf)
        if l == 0:
            break
        sb.append(lang.String(buf, 0, 0, l))
    return sb.toString()

