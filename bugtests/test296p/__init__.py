import sys, os.path

BACKENDS = ['msw', 'x', 'mac', 'wx', 'tk', 'java']

class anygui:

    __all__ = ['Window'] # Etc...

    def __init__(self):
        self.__backend = None
        self.__dirname = __path__[0]
        self.__backends = BACKENDS

    def __try_to_get(self, modulename):
        import os.path # Not available because of module trickery
        # Is there a better way to do this?
        filename = os.path.join(self.__dirname,  modulename + '.py')
        import imp
        try:
            file = open(filename)
            module = imp.load_module(modulename, file, filename, ('.py', 'r', 1))
            assert module.viable
        except (IOError, ImportError, AttributeError, AssertionError):
            return None
        else:
            return module

    def __import_backend(self, wishlist):
        candidates = self.__backends[:]
        for wish in wishlist:
            if wish in candidates:
                candidates.remove(wish)
            else:
                wishlist.remove(wish)
        candidates = wishlist + candidates

        for name in candidates:
            #backend = self.__dict__['__try_to_get']('%sgui' % name)
	    backend = self.__try_to_get('%sgui' % name) 
            if backend: break
        else:
            raise Exception, 'not able to import any GUI backends'
        self.__backend = backend

    def __getattr__(self, name):
    	#print "__getattr__", name
        if name.startswith('__'):
            raise AttributeError, name
        else:
            if not self.__backend:
                self.__import_backend(self.__dict__.get('wishlist', []))
            return self.__backend.__dict__[name]

sys.modules[__name__] = anygui()


