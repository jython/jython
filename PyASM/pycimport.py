_debugging = False

def readPycHeader(file):
    def read():
        return ord(file.read(1))
    magic = read() | (read()<<8)
    if not ( file.read(1) == '\r' and file.read(1) == '\n' ):
        raise TypeError("not valid pyc-file")
    mtime = read() | (read()<<8) | (read()<<16) | (read()<<24)
    return magic, mtime

def makeModule(name, code, path):
    module = _imp.addModule(name)
    frame = _Frame(code, module.__dict__, module.__dict__, None)
    code.call(frame) # execute module code
    module.__file__ = path
    return module

class Importer(object):
    started = False # wait until dependancies are in place before useing this
    def __init__(self, path):
        if _debugging: print "Importer invoked"
        self.__path = path
    def find_module(self, fullname, path=None):
        if not self.started:
            return None
        if _debugging: print "Importer.find_module(fullname=%s, path=%s)" % (
            repr(fullname),repr(path))
        path = fullname.split('.')
        filename = path[-1]
        path = path[:-1]
        pycfile = os.path.join(self.__path, *(path + [filename + '.pyc']))
        pyfile = os.path.join(self.__path, *(path + [filename + '.py']))
        if os.path.exists(pycfile):
            f = open(pycfile, 'rb')
            try:
                magic, mtime = readPycHeader(f)
            except:
                return None # abort! not a valid pyc-file
            f.close()
            if os.path.exists(pyfile):
                pytime = os.stat(pyfile).st_mtime
                if pytime > mtime:
                    return None # abort! py-file was newer
            return self
        else:
            return None # abort! pyc-file does not exist
    def load_module(self, fullname):
        path = fullname.split('.')
        path[-1] += '.pyc'
        filename = os.path.join(self.__path, *path)
        f = open(filename, 'rb')
        magic, mtime = readPycHeader(f)
        code = Unmarshaller(f, magic=magic).load()
        return makeModule( fullname, code, filename )

import sys

sys.path_hooks.append(Importer)
# Defere all the imports so that the import hooks will still be valid
import os

if os.name == 'java':
    from org.python.core import imp as _imp, PyFrame as _Frame
    from marshal import Unmarshaller

if __name__ == '__main__':
    # Start Jython within Jython... with pyc-importing enabled
    from org.python.util import jython
    from java.lang import String
    from jarray import array
    Importer.started = True # Start the import hook mechanism
    print "Python bytecode importing enabled"
    jython.main(array(sys.argv[1:], String))
