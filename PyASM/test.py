import sys, os, marshal

if os.name == 'java': # Jython side
    def cpythonCompile(string, filename='<stdin>'):
        # invoke script with CPython to compile and return marshalled code
        source, drain, error = os.popen3('python2.5 "%s" "%s"' %
                                         (__file__, filename))
        source.write(string) # send source
        source.close() # mark end of source (eof)
        binary = drain.read()
        if len(binary) == 0:
            print >> sys.stderr, error.read()
            sys.exit(1)
        try:
            data = "".join([chr(int(x)) for x in binary.split(',')]) #decode
        except:
            print >> sys.stderr, "Read from CPython:"
            print >> sys.stderr, data
            raise
        code = marshal.loads(data) # read back marshalled code and unmarshal
        return code # return unmarshalled, compiled code

    if __name__ == '__main__': # invoked as a script
        import sys
        filename = sys.argv[1]
        file = open(filename) # open file
        code = cpythonCompile(file.read(), filename) # compile file
        vars = dict(__name__='__main__', __file__=filename, __doc__=None)
        exec code in vars # execute compiled file

elif __name__ == '__main__': # CPython side
    filename = sys.argv[1]
    source = sys.stdin.read() # read source
    code = compile(source, filename, 'exec') # compile code
    data = marshal.dumps(code) # marshal code
    binary = ','.join(["%d"%ord(x) for x in data]) # avoid encoding errors
    sys.stdout.write(binary) # "return" marshalled code

else:
    raise EnvironmentError("Unknown runtime environment")
