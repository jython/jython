# copyright 2004-2005 Samuele Pedroni
import sys
import os
import re

scriptdir = os.path.dirname(__file__)

import directives
import java_parser
import java_templating
from java_templating import JavaTemplate,jast_make,jast


modif_re = re.compile(r"(?:\((\w+)\))?(\w+)")

class Gen:

    priority_order = ['require','define','base_class',
                      'want_dict',
                      'ctr',
                      'incl',
                      'unary1',
                      'binary', 'ibinary',
                      'rest',
                      'no_toString'
                      ]

    def __init__(self,bindings=None,priority_order=None):
        if bindings is None:
            self.global_bindings = { 'csub': java_templating.csub,
                                     'concat': java_templating.concat,
                                     'strfy': java_templating.strfy }
        else:
            self.global_bindings = bindings
            
        if priority_order:
            self.priority_order = priority_order

        self.decls = JavaTemplate("")

        self.auxiliary = None

        self.base_class = None
        self.want_dict = None
        self.no_toString = False
        self.ctr_done = 0

    def debug(self,bindings):
        for name,val in bindings.items():
            if isinstance(val,JavaTemplate):
                print "%s:" % name
                print val.texpand({})

    def invalid(self,dire,value):
        raise Exception,"invalid '%s': %s" % (dire,value)

    def get_aux(self,name):
        if self.auxiliary is None:
            aux_gen = Gen(priority_order=['require','define'])
            directives.execute(directives.load(os.path.join(scriptdir,'gderived-defs')),aux_gen)
            self.auxiliary = aux_gen.global_bindings
        return self.auxiliary[name]

    def dire_require(self,name,parm,body):
        if body is not None:
            self.invalid('require','non-empty body')
        sub_gen = Gen(bindings=self.global_bindings, priority__order=['require','define'])
        directives.execute(directives.load(parm.strip()),sub_gen)        

    def dire_define(self,name,parm,body):
        parms = parm.split()
        if not parms:
            self.invalid('define',parm)
        parsed_name = modif_re.match(parms[0])
        if not parsed_name:
            self.invalid('define',parm)
        templ_kind = parsed_name.group(1)
        templ_name = parsed_name.group(2)
        if templ_kind is None:
            templ_kind = 'Fragment'
        
        templ = JavaTemplate(body,
                                             parms=':'.join(parms[1:]),
                                             bindings = self.global_bindings,
                                             start = templ_kind)
        self.global_bindings[templ_name] = templ
            
    def dire_base_class(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        if self.base_class is None:
            self.base_class = JavaTemplate(parm.strip())
            self.global_bindings['base'] = self.base_class

    def dire_want_dict(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        if self.want_dict is None:
            self.want_dict = {"true": 1, "false": 0}[parm.strip()]

    def dire_no_toString(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        self.no_toString = True

    def dire_incl(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        directives.execute(directives.load(parm.strip()+'.derived'),self)

    def dire_ctr(self,name,parm,body):
        if self.ctr_done:
            return
        if body is not None:
            self.invalid(name,"non-empty body")
        if self.want_dict:
            self.add_decl(self.get_aux('userdict'))
            ctr = self.get_aux('ctr_userdict')
        else:
            ctr = self.get_aux('ctr')
        extraargs = JavaTemplate(parm.strip(),start="FormalParameterListOpt")
        def visit(node):
            if isinstance(node, jast.VariableDeclaratorId):
                yield node.Identifier
            elif hasattr(node, 'children'):
                for child in node.children:
                    for x in visit(child):
                        yield x
        extra =  jast_make(jast.Expressions, [jast_make(jast.Primary,Identifier=x,ArgumentsOpt=None) for x in visit(extraargs.fragment)])
        extra = JavaTemplate(extra)
        self.add_decl(ctr.tbind({'base': self.base_class, 'extraargs': extraargs, 'extra': extra}))
        self.ctr_done = 1
    def add_decl(self,templ):
        pair = self.get_aux('pair')
        self.decls = pair.tbind({'trailer': self.decls, 'last': templ})

    def dire_unary1(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        parms = parm.split()
        if len(parms) not in (1,2,3):
            self.invalid(name,parm)
        meth_name = parms[0]
        if len(parms) == 1:
            unary_body = self.get_aux('unary')
            self.add_decl(unary_body.tbind({'unary': JavaTemplate(meth_name)}))
        else:
            rettype_name = parms[1]
            if len(parms) == 3:
                rettype_class = parms[2]
            else:
                rettype_class = 'Py'+rettype_name[0].upper()+rettype_name[1:]
            unary_body = self.get_aux('typed_unary')
            self.add_decl(unary_body.tbind({'unary': JavaTemplate(meth_name),
                                            'rettype_name':
                                             JavaTemplate(java_parser.make_qualid(rettype_name)),
                                            'rettype': JavaTemplate(rettype_class)}))
            
            
    def dire_binary(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        meth_names = parm.split()
        binary_body = self.get_aux('binary')
        for meth_name in meth_names:
            self.add_decl(binary_body.tbind({'binary': JavaTemplate(meth_name)}))

    def dire_ibinary(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        meth_names = parm.split()
        binary_body = self.get_aux('ibinary')
        for meth_name in meth_names:
            self.add_decl(binary_body.tbind({'binary': JavaTemplate(meth_name)}))

    def dire_rest(self,name,parm,body):
        if parm:
            self.invalid(name,'non-empty parm')
        if body is None:
            return
        self.add_decl(JavaTemplate(body,start='ClassBodyDeclarations'))   

    def generate(self):
        if not self.no_toString:
            self.add_decl(self.get_aux('toString'))
        derived_templ = self.get_aux('derived_class')
        return derived_templ.texpand({'base': self.base_class, 'decls': self.decls })

def process(fn, outfile, lazy=False):
    if lazy and os.stat(fn).st_mtime < os.stat(outfile).st_mtime:
	return
    print 'Processing %s into %s' % (fn, outfile)
    gen = Gen()
    directives.execute(directives.load(fn),gen)
    result = gen.generate()
    print >> open(outfile, 'w'), result
    #gen.debug()

if __name__ == '__main__':
    from gexpose import load_mappings, usage
    lazy = False
    if len(sys.argv) > 4:
        usage()
	sys.exit(1)
    if len(sys.argv) >= 2:
	if '--help' in sys.argv:
	    usage()
	    sys.exit(0)
	elif '--lazy' in sys.argv:
	    lazy = True
	    sys.argv.remove('--lazy')
    if len(sys.argv) == 1:
	for template, mapping in load_mappings().items():
	    if template.endswith('derived'):
		process(mapping[0], mapping[1], lazy)
    elif len(sys.argv) == 2:
	mapping = load_mappings()[sys.argv[1]]
        process(mapping[0], mapping[1], lazy)
    else:
        process(sys.argv[1], sys.argv[2], lazy)


