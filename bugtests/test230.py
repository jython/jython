
import pdb

#p = pdb.Pdb()



class P:
    def do_help(self, arg):
        print "do_help", arg
    #print do_help, id(do_help)

    def onecmd(self, cmd, arg):
        func = getattr(self, "do_" + cmd)
        func(arg)

class S(P):
    do_h = P.do_help
    #print do_h, id(do_h)

p = S()
a = p.do_help
#print a, id(a)
a = p.do_h
#print a, id(a)

p.onecmd('help', "hello world")
p.onecmd('h', "hello world")

