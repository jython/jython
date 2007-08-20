import os, marshal

if os.name == 'java': # Jython side

    def cpythonCompile(string, filename='<stdin>'):
        # invoke script with CPython to compile and return marshalled code
        source, binary = os.popen2('python %s "%s"' % (__file__, filename))
        source.write(string) # send source
        source.close() # mark end of source (eof)
        data = "".join([chr(int(x)) for x in binary.read().split(',')]) #decode
        code = marshal.loads(data) # read back marshalled code and unmarshal
        return code # return unmarshalled, compiled code

    if __name__ == '__main__': # invoked as a script
        import sys
        filename = sys.argv[1]
        file = open(filename) # open file
        code = cpythonCompile(file.read(), filename) # compile file
        exec code # execute compiled file

elif __name__ == '__main__': # CPython side
    import sys
    filename = sys.argv[1]
    source = sys.stdin.read() # read source
    code = compile(source, filename, 'exec') # compile code
    data = marshal.dumps(code) # marshal code
    binary = ','.join(["%d"%ord(x) for x in data]) # avoid encoding errors
    sys.stdout.write(binary) # "return" marshalled code

else:
    raise EnvironmentError("Unknown runtime environment")
