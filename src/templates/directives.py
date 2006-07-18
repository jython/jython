# copyright 2004-2005 Samuele Pedroni

class Directive:
    def __init__(self,name,parm,body=None):
        self.name = name
        self.parm = parm
        self.body = body

    def __repr__(self):
        return "<Directive '%s': %s>" % (self.name,self.parm)

def parse(lines):
    directives = []

    i=0
    c = len(lines)
    while i<c:
        line = lines[i]
        if not line or line[0] == '#' or line.isspace():
            i += 1
            continue
        colon = line.find(':')
        if colon == -1:
            if line.endswith('\n'):
                line = line[:-1]
            directives.append(Directive(line,''))
            i += 1
        else:
            name = line[:colon]
            parm = line[colon+1:]
            i += 1
            while parm.endswith('\\\n'):
                parm = parm[:-2]
                if i >= c:
                    break
                parm += lines[i]
                i += 1
            if parm.endswith('\n'):
                parm = parm[:-1]
            body = None
            while i<c:
                line = lines[i]
                if line and not line[0].isspace():
                    break
                body = (body or '') + line
                i += 1
            if body is not None and body.isspace():
                body = None
            directives.append(Directive(name,parm,body))
    return directives


def test():
    v = """
a
b: 1

c: 1 \\
3

#sun beam
d: xyz
  x = 1
  y = 2

z

"""

    for directive in parse(v.splitlines(1)):
        print "%s: %s" % (directive.name,directive.parm)
        body = directive.body
        if body and body.endswith('\n'):
            body = body[:-1]
        print body
        
def execute(directives,processor):
    priority_order = processor.priority_order
    def cmp_directive(dire1,dire2):
        name1 = dire1.name
        name2 = dire2.name
        if name1 == name2:
            return 0
        try:
            i1 = priority_order.index(name1)
        except ValueError:
            i1 = -1
        try:
            i2 = priority_order.index(name2)
        except ValueError:
            i2 = -1
        return i1-i2
    directives = directives[:]
    directives.sort(cmp_directive)
    for dire in directives:
        if dire.name not in priority_order:
            raise Exception,"unexpected directive: %s" % dire.name
        action = getattr(processor,'dire_%s' % dire.name,None)
        if action is None:
            raise Exception,"unsupported directive: %s" % dire.name
        action(dire.name,dire.parm,dire.body)

def load(fn):
    f = open(fn,"r")
    try:
        lines = f.readlines()
    finally:
        f.close()
    return parse(lines)
        
            
    
        
if __name__ == '__main__':
    test()
    
