# Copyright Finn Bock
# Updated 2017 by Stefan Richthofer to support Unicode 9.0
#
# Generate a ucnhash.dat file with mapping from unicode
# names to codepoints.
#
# python make_ucnhashdat.py UnicodeData.txt mph.exe
#
# The "mph" program must be available on the path.
# This program is used to create the minimum perfect 
# hash used by the wordhash table.
#
# I've used 1.2 from:
#
#  http://www.ibiblio.org/pub/Linux/devel/lang/c/!INDEX.short.html
#

import fileinput, re, os, sys, struct, cStringIO

mph_exec = 'mph' # Formerly: 'mph.exe'

def debug(str):
    print >>debugFile, str

def splitIntoWords(name):
    wordlist = []
    wordstart = 0
    l = len(name)
    for i in range(l):
        c = name[i]
        n = None
        if c == ' ' or c == '-':
            n = name[wordstart:i]
        elif i == l-1:
            n = name[wordstart:i+1]
        if n:
            wordstart = i
            if c == '-' and n != '':
                n += '-'
            if c == ' ' or c == '-':
                wordstart = i+1
            wordlist.append(n)
    return wordlist

def readUnicodeDict(file):
    d = {}
    for l in fileinput.input(file):
        l = l.strip().split(";");

        v,name = l[0:2]
        if name == "<control>": 
            name = l[10]
            if name == '':
                continue
        if name[0] == "<": 
           continue

        #handled by code in ucnhash
        if name.startswith("CJK COMPATIBILITY IDEOGRAPH-"):
            continue

        wordlist = splitIntoWords(name)

        d[name] = (int(v, 16), wordlist, [])

    return d

def count(dict, index):
    c = dict.get(index)
    if c is None: c = 0
    c += 1
    dict[index] = c

def dumpUnicodeDict(title, dict):
    lst = []
    i = 0
    for k,(v,wordlist, rawlist) in dict.items():
        p = wordlist[:]
        lst.append((v, k, p))

    lst.sort()

    print "=======", title
    for v,k,p in lst:
        print "%.4X %s %s" % (v, k, p)


class MphEmitter:
 
    def readint(self):
        return int(self.inf.readline().strip())

    def readfloat(self):
        return float(self.inf.readline().strip())

    def readconst(self):
        global d, n, m, c, maxlen, minklen, maxklen, minchar, maxchar, alphasz

        self.inf.readline()

        self.d = self.readint()
        self.n = self.readint()
        self.m = self.readint()
        self.c = self.readfloat()
        self.maxlen = self.readint()
        self.minklen = self.readint()
        self.maxklen = self.readint()
        self.minchar = self.readint()
        self.maxchar = self.readint()
        self.loop = self.readint()
        self.numiter= self.readint()
        self.readint()
        self.readint()

        debug(" * d=%d" % self.d)
        debug(" * n=%d" % self.n)
        debug(" * m=%d" % self.m)
        debug(" * c=%g" % self.c)
        debug(" * maxlen=%d" % self.maxlen)
        debug(" * minklen=%d" % self.minklen)
        debug(" * maxklen=%d" % self.maxklen)
        debug(" * minchar=%d" % self.minchar)
        debug(" * maxchar=%d" % self.maxchar)

        self.alphasz = self.maxchar - self.minchar+1;

    def readg(self):
        data = Table()
        for i in range(self.n):
            v = self.readint()
            data.write_Short(v)
        return data

    def readT(self, t):
        data = Table()
        for i in range(self.maxlen):
            for j in range(256):
                v = self.readint()
                if j < self.minchar or j > self.maxchar:
                    continue
                data.write_Short(v)
        return data


    def writeFile(self, inf, outf):
        self.inf = inf

        self.readconst();
        outf.write(struct.pack("!hhhhh", self.n,
                                         self.m,
                                         self.minchar,
                                         self.alphasz,
                                         self.maxlen))
        G = self.readg()
        debug("G len: %d" % (G.size()/2))
        G.writeto(outf)

        outf.write(struct.pack("!h", self.d))
        for t in range(self.d):
            T = self.readT(t)
            debug("T%d len: %d" % (t, T.size()/2))
            T.writeto(outf)


class Table:
    def __init__(self):
        self.buf = cStringIO.StringIO()

    def write_Str(self, str):
        self.buf.write(str)

    def write_Short(self, v):
        self.buf.write(struct.pack("!h", v))

    def write_UShort(self, v):
        self.buf.write(struct.pack("!H", v))

    def write_Int32(self, v):
        self.buf.write(struct.pack("!i", v))

    def write_UInt32(self, v):
        self.buf.write(struct.pack("!I", v))

    def writeto(self, file):
        file.write('t')
        file.write(struct.pack("!I", self.size()))
        file.write(self.buf.getvalue())

    def size(self):
        return self.buf.tell()


def calculateSize(dict):
    cnt = 0
    for name in dict.keys():
        cnt += len(name)
    return cnt

def calculateWords(unicodeDict):
    words = {}
    for key, (value, wordlist, rawlist) in unicodeDict.items():
        for name in wordlist:
            wordlist = words.setdefault(name, [])
            wordlist.append(key)
    return words

def replaceWord(word, index, charlist):
    replaced = 0
    for char in charlist:
        (v, wordlist, rawlist) = unicodeDict[char]
        try:
            i = wordlist.index(word)
        except ValueError:
            continue
        wordlist[i] = index
        replaced = 1
    return replaced

def compress():
    #dumpUnicodeDict("UnicodeDict before", unicodeDict)
    words = calculateWords(unicodeDict)
    lenp = [(len(v), k, v) for k, v in words.items()]
    lenp.sort()
    lenp.reverse()
    wordidx = len(chars)
    for (length, word, value) in lenp:
        # Do not lookup single char words or words only used once
        if len(word) == 1 or len(value) == 1:
            continue
        # Do not lookup two char words of the replacement would 
        # be just as big.
        if len(word) == 2 and wordidx >= 238:
            continue
        replaceWord(word, wordidx, value)
        wordmap[wordidx] = word
        wordidx += 1
    #dumpUnicodeDict("UnicodeDict after", unicodeDict)

def writeUcnhashDat():
    cutoff = 255 - ((len(chars) + len(wordmap)) >> 8)

    debug("wordmap entries: %d" % len(wordmap))
    debug("wordmap cutoffs: %d" % cutoff)

    worddata = Table()
    wordoffs = Table()
    wordfile = open("words.in", "wt");
    size = 0
    l = [(k,v) for k,v in wordmap.items()]
    l.sort()
    for k,v in l:
        print >>wordfile, v
        wordoffs.write_UShort(worddata.size())
        mapv = ''.join(map(lambda x: chr(chardict.get(x)), v))
        worddata.write_Str(mapv)
    wordfile.close()

    os.system(mph_exec+" -d3 -S1 -m4  -a < words.in > words.hash")

    outf = open("ucnhash.dat", "wb+")

    m = MphEmitter()
    m.writeFile(open("words.hash"), outf)

    debug("wordhash size %d" % outf.tell())
    debug("wordoffs size %d" % (wordoffs.size()/2))
    debug("worddata size %d" % (worddata.size()))

    wordoffs.writeto(outf)
    worddata.writeto(outf)

    maxklen = 0
    lst = []
    for key, (value, wordlist, rawlist) in unicodeDict.items():
        savewordlist = wordlist[:]

        # Map remaining strings to a list of bytes in chardict
        # range: range(0, 40)
        l = len(wordlist)
        for i in range(l-1, -1, -1):
            part = wordlist[i]
            if type(part) == type(""):
                ipart = map(chardict.get, part)
                if i > 0 and type(wordlist[i-1]) == type(""):
                    ipart[0:0] = [0] # index of space
                wordlist[i:i+1] = ipart
        # Encode high values as two bytes
        for v in wordlist:
            if v <= cutoff:
                rawlist.append(v)
            else:
                rawlist.append((v>>8) + cutoff)
                rawlist.append(v & 0xFF)

        if value in debugChars:
            print key, savewordlist, rawlist

        lst.append((rawlist, wordlist, key, value))
        maxklen = max(maxklen, len(key))
    lst.sort()
    outf.write(struct.pack("!hhh", len(chars), cutoff, maxklen));

    raw = Table()
    datasize = []
    i = 0
    for (rawlist, wordlist, key, value) in lst:
        for r in rawlist:
            raw.write_Str(chr(r))
        datasize.append((len(rawlist), value))
        #debug("%d %s %r" % (i, key, rawlist))
        i += 1
    debug("Raw size = %d" % raw.size())
    raw.writeto(outf)

    rawindex = Table()
    codepoint = Table()

    offset = 0
    maxlen = 0
    maxvl = 0 # for debugging
    # Formerly it was sufficient for rawindex and codepoint
    # to be 16 bit.
    # We leave the old 16 bit write instructions here as
    # comments in case future debugging is necessary.
    # Note that rawindex blocksize therefore shrunk from
    # 5 to 3, which was adjusted in ucnhash.java accordingly.
    # In line 'v = v | (long(size) << (j*5))' the '5' seemingly
    # refers to blocksize, but for some reason it must not be
    # adjusted to 3.
    # (3 would break things, while it works well with 5)
    for i in range(0, len(datasize), 12):
        saveoffset = offset
        #rawindex.write_UShort(offset)
        rawindex.write_UInt32(offset)
        v = 0L
        j = 0
        for (size, value) in datasize[i:i+12]:
            # we keep track of max value to confirm
            # that 32 bit  codepoint table is needed
            if value > maxvl:
                maxvl = value
            offset += size
            v = v | (long(size) << (j*5))

            maxlen = max(maxlen, size)
            #codepoint.write_UShort(value)
            codepoint.write_UInt32(value)
            j += 1
        #debug("%d %d %x" % (i/ 12, saveoffset, v))
        #rawindex.write_UShort((v >> 48) & 0xFFFF)
        #rawindex.write_UShort((v >> 32) & 0xFFFF)
        #rawindex.write_UShort((v >> 16) & 0xFFFF)
        #rawindex.write_UShort(v & 0xFFFF)
        rawindex.write_UInt32((v >> 32) & 0xFFFFFFFF)
        rawindex.write_UInt32(v & 0xFFFFFFFF)
    debug("maxval % d" % maxvl)
    debug("rawindex size % d" % (rawindex.size()/4))
    debug("codepoint size % d" % (codepoint.size()/4))
    rawindex.writeto(outf)
    codepoint.writeto(outf)

    debug("raw entries %d" % len(datasize))
    outf.close();


if __name__ == "__main__":
    chars = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-()"
    chardict = {}
    for c in chars:
       chardict[c] = chars.index(c)

    debugChars = [] # [0x41, 0x20AC]

    debugFile = open("ucnhash.lst", "wt")
    #debugFile = sys.stdout

    wordmap = {}

    # Called 2017 with UnicodeData.txt for Unicode 9.0
    unicodeDataFile = "UnicodeData.txt"
    if len(sys.argv) > 1:
        unicodeDataFile = sys.argv[1]
    if len(sys.argv) > 2:
        mph_exec = sys.argv[2]
    unicodeDict = readUnicodeDict(unicodeDataFile)
    print "Size:", calculateSize(unicodeDict)

    compress()
    print "compressed"

    writeUcnhashDat()
    print "done"
        
    sys.exit(0)

# Debugging-hints:
#-----------------
# (with debugFile = sys.stdout, omitting mph-output)

# Output for "UnicodeData-3.0.0.txt":
# Size: 259126
# compressed
# wordmap entries: 1384
# wordmap cutoffs: 250
#  * d=3
#  * n=1703
#  * m=1384
#  * c=1.23
#  * maxlen=4
#  * minklen=2
#  * maxklen=18
#  * minchar=45
#  * maxchar=90
# G len: 1703
# T0 len: 184
# T1 len: 184
# T2 len: 184
# wordhash size 4542
# wordoffs size 1384
# worddata size 7375
# Raw size = 58531
# maxval  65533
# rawindex size  2577
# codepoint size  10298
# raw entries 10298
# done


# Output for "UnicodeData.txt", Unicode 9.0:
# Size: 755323
# compressed
# wordmap entries: 3708
# wordmap cutoffs: 241
#  * d=3
#  * n=4561
#  * m=3708
#  * c=1.23
#  * maxlen=4
#  * minklen=2
#  * maxklen=18
#  * minchar=32
#  * maxchar=90
# G len: 4561
# T0 len: 236
# T1 len: 236
# T2 len: 236
# wordhash size 10570
# wordoffs size 3708
# worddata size 19399
# Raw size = 184818
# maxval  917999
# rawindex size  7389
# codepoint size  29545
# raw entries 29545
# done
