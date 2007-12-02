"""
Creates codecs and getregentry functions using Java's Charsets.

Error handling with 'strict', 'ignore' and 'replace' is accomplished by mapping
to similar error actions in Java's system, however the results aren't identical
to CPython.  For example, where CPython throws away two bytes in a two byte
encoding when encountering an error in 'replace' mode, Java throws away the
first byte and resumes from the second.  It's close enough to be functional
though.  
"""

import codecs

def create_codec(encoding_name):
    '''Creates a Codec class using a Java charset for encoding_name'''
    from java.nio import ByteBuffer, CharBuffer
    from java.nio.charset import CharacterCodingException, Charset, CodingErrorAction
    error_to_action = {
            'strict':CodingErrorAction.REPORT,
            'ignore':CodingErrorAction.IGNORE,
            'replace':CodingErrorAction.REPLACE}

    def _chr(n):
        "change from -128..127 to 0..255 and chr it"
        return chr((n + 256) % 256)

    def configure_errors(charset, errors):
        action = error_to_action[errors]
        return charset.onMalformedInput(action).onUnmappableCharacter(action)
     
    class Codec(codecs.Codec):
        def encode(self, data, errors='strict'):
            encoder = Charset.forName(encoding_name).newEncoder()
            encoder = configure_errors(encoder, errors)
            charbuffer = CharBuffer.wrap(data)
            try:
                bytebuffer = encoder.encode(charbuffer)
            except CharacterCodingException:
                raise UnicodeError('%s encoding error' % encoding_name)
            bytebuffer.rewind()
            pys = ''.join([_chr(bytebuffer.get()) for c in xrange(bytebuffer.remaining())])
            return (pys, len(data))

        def decode(self, data, errors='strict'):
            decoder = Charset.forName(encoding_name).newDecoder()
            decoder = configure_errors(decoder, errors)
            bytebuffer = ByteBuffer.allocate(len(data))
            bytebuffer.put(data)
            bytebuffer.rewind()
            try:
                charbuffer = decoder.decode(bytebuffer)
            except CharacterCodingException:
                raise UnicodeError('%s decoding error' % (encoding_name))
            return (str(charbuffer.toString()), len(data))
    
    return Codec

def create_getregentry(encoding_name):
    "makes a 'getregentry' function  as in the encoding api"
    Codec = create_codec(encoding_name)
    class StreamReader(Codec, codecs.StreamReader):
        pass

    class StreamWriter(Codec, codecs.StreamWriter):
        pass

    def getregentry():
        return (
            Codec().encode,
            Codec().decode,
            StreamReader,
            StreamWriter,
        )
    
    return getregentry
