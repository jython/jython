import sys

class YesNo:
    pass
class Required:
    pass

class Options:
    def __init__(self, opts, args=None):
        if args == None:
            args = sys.argv[1:]
        ret = {}

        index = 0
        while index < len(args):
            arg = args[index]
            if arg[0] == '-':
                name = arg[1:]
                value = opts[name]
                if value is YesNo:
                    ret[name] = 1
                    index = index+1
                else:
                    ret[name] = args[index+1]
                    index = index+2
                continue
            break

        for key, value in opts.items():
            if ret.has_key(key): continue
            elif value is YesNo:
                ret[key] = 0
            elif value is Required:
                raise ValueError, "argument %s is required" % key
            else:
                ret[key] = value

        for key, value in ret.items():
            setattr(self, key, value)

        self.args = args[index:]
