# copyright 2004-2005 Samuele Pedroni
import sys
import os
import re

scriptdir = os.path.dirname(__file__)

import directives
import java_parser
import java_templating
from java_templating import JavaTemplate,jast_make,jast, make_id, make_literal


# Some examples for modif_re
# (one)two -> group 1 = "one"
#             group 2 = "two"
# (one,two,three)four -> group 1 = "one,two,three"
#                        group 2 = "four"
# hello -> group 1 = None
#          group 2 = "hello"
modif_re = re.compile(r"(?:\(([\w,]+)\))?(\w+)")

# Explanation of named groups in regular expression ktg_re:
# k = one character key ("o","i" and "s" in "ois")
# opt = optional (the "?" in "o?")
# dfl = default for optional (the "blah" in o?(blah) )
# tg = ? (the "anything_can_go_here in o{anything_can_go_here} )
ktg_re = re.compile("(?P<k>\w)(?P<opt>\?(:?\((?P<dfl>[^)]*)\))?)?(?:\{(?P<tg>[^}]*)\})?")

def make_name(n):
    return JavaTemplate(jast_make(jast.QualifiedIdentifier,[java_parser.make_id(n)]))

class Gen:

    priority_order = ['require','define',
                      'type_as',
                      'type_name','type_class','type_base_class',
                      'incl',
                      'expose_getset',
                      'expose_unary','expose_binary',
                      'expose_vanilla_cmp','expose_vanilla_pow',
                      'expose_key_getitem',
                      'expose_index_getitem',
                      'expose_cmeth',
                      'expose_meth',
                      'expose_wide_meth',
                      'expose_new_mutable',
                      'expose_new_immutable',
                      'rest']

    def __init__(self,bindings=None,priority_order=None):
        if bindings is None:
            self.global_bindings = { 'csub': java_templating.csub,
                                     'concat': java_templating.concat,
                                     'strfy': java_templating.strfy }
        else:
            self.global_bindings = bindings
            
        if priority_order:
            self.priority_order = priority_order

        self.call_meths_cache = {}
        self.auxiliary = None

        self.no_setup = False
        self.statements = []

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
            directives.execute(directives.load(os.path.join(scriptdir,'gexpose-defs')),aux_gen)
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
        naked = False
        if templ_kind is None:
            templ_kind = 'Fragment'

        templ = JavaTemplate(body,
                                             parms=':'.join(parms[1:]),
                                             bindings = self.global_bindings,
                                             start = templ_kind)
        self.global_bindings[templ_name] = templ

    def dire_type_as(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        parms = parm.split()
        if len(parms) not in (1,2):
            self.invalid(name,parm)
        self.type_as = JavaTemplate(parms[0])
        if len(parms) == 2:
            if parms[1] == 'no-setup':
                self.no_setup = True
            else:
                self.invalid(name,parm)

    def dire_type_name(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        self.type_name_plain = parm.strip()
        self.type_name = make_name(self.type_name_plain)
        self.global_bindings['typname'] = self.type_name
            
    def dire_type_class(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        self.type_class = JavaTemplate(parm.strip())
        self.global_bindings['typ'] = self.type_class

    def dire_type_base_class(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        self.type_base_class = JavaTemplate(parm.strip())

    def dire_incl(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        directives.execute(directives.load(parm.strip()+'.expose'),self)

    def dire_expose_getset(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')

        parms = parm.strip().split()

        if len(parms) not in (2,3):
            self.invalid(name, parm)
        
        name = parms[0]
        get = '"%s"' % parms[1]
        if len(parms) == 3:
            set = '"%s"' % parms[2]
        else:
            set = "null"

        getset_bindings = self.global_bindings.copy()

        getset_bindings['name'] = JavaTemplate(make_id(name))
        getset_bindings['get'] = JavaTemplate(make_literal(get))
        getset_bindings['set'] = JavaTemplate(make_literal(set))

        getset = self.get_aux('getset')

        self.statements.append(getset.tbind(getset_bindings))
        
    NOARGS = JavaTemplate("void()")
    EMPTYALL = JavaTemplate(jast_make(jast.Expressions))

    def parse_sig(self,name,sig):
        argspecs = []
        some_opt = 0
        for m in ktg_re.finditer(sig):
            k = m.group('k')
            opt = m.group('opt') and 1 or 0
            if opt:
                some_opt = 1
            if opt != some_opt:
                self.invalid(name,"cannot interleave opt and non-opt arguments")
            dfl = m.group('dfl')
            if opt and dfl is None:
                dfl = ''
            tg = m.group('tg')
            argspecs.append((k,opt,dfl,tg))
        everything = [(k,tg) for k,opt,dfl,tg in argspecs]
        dfls = [ dfl for k,opt,dfl,tg in argspecs if opt]
        return everything,dfls

    def arg_i(self, argj, j, tg):
        if tg:
            err = "%s must be an integer" % tg
        else:
            err = "expected an integer"
        return JavaTemplate("%s.asInt(%s)" % (argj, j)), err # !!!

    def arg_l(self, argj, j, tg):
        if tg:
            err = "%s must be a long" % tg
        else:
            err = "expected a long"
        return JavaTemplate("%s.asLong(%s)" % (argj, j)), err # !!!

    def arg_b(self, argj, j, tg):
        return JavaTemplate("%s.__nonzero__()" % (argj)),None

    def arg_o(self,argj,j,tg):
        return JavaTemplate(argj),None

    def arg_S(self,argj,j,tg):
        if tg:
            err = "%s must be a string or None" % tg
        else:
            err = "expected a string or None"
        return JavaTemplate("%s.asStringOrNull(%s)" % (argj,j)),err # !!!

    def arg_s(self,argj,j,tg):
        if tg:
            err = "%s must be a string" % tg
        else:
            err = "expected a string"
        return JavaTemplate("%s.asString(%s)" % (argj,j)),err # !!!

    def arg_n(self,argj,j,tg):
        if tg:
            err = "%s must be a string" % tg
        else:
            err = "expected a string"
        return JavaTemplate("%s.asName(%s)" % (argj,j)),err # !!!

    def make_call_meths(self,n,bindings):
        try:
            return self.call_meths_cache[n].tbind(bindings)
        except KeyError:
            templ = "`call_meths`(`args%d,`body%d);"
            defs = []
            for i in range(n):
                defs.append(templ % (i,i))
            defs = '\n'.join(defs)
            jtempl = JavaTemplate(defs,start='ClassBodyDeclarations')
            self.call_meths_cache[n] = jtempl
            return jtempl.tbind(bindings)
            
    def handle_expose_meth_sig(self,sig,call_meths_bindings,body,body_bindings):
        proto_body_jt = JavaTemplate(body)
        everything,dfls = self.parse_sig('expose_meth',sig)

        dfls = map(JavaTemplate,dfls)

        tot = len(everything)
        rng = len(dfls)+1
        
        for dflc in range(rng):
            new_body_bindings = body_bindings.copy()
            args = self.NOARGS
            all = self.EMPTYALL
            j = 0
            conv_errors = {}
            for k,tg in everything[:tot-dflc]:
                argj = "arg%d" % j
                args += JavaTemplate("void(PyObject %s)" % argj)
                new_body_bindings[argj],err = getattr(self,'arg_%s' % k)(argj,j,tg)
                all += JavaTemplate(jast_make(jast.Expressions,[new_body_bindings[argj].fragment]))
                if err:
                    conv_errors.setdefault(err,[]).append(j)
                j += 1
            new_body_bindings['all'] = all
            for dv in dfls[rng-1-dflc:]:
                new_body_bindings["arg%d" % j] = dv
                j += 1
            for deleg_templ_name in ('void','deleg','vdeleg','rdeleg','ideleg','ldeleg','bdeleg','sdeleg', 'udeleg'):
                deleg_templ = self.get_aux(deleg_templ_name)
                new_body_bindings[deleg_templ_name] = deleg_templ.tbind(new_body_bindings)
            body_jt = proto_body_jt.tbind(new_body_bindings)
            if conv_errors:
                cases = JavaTemplate(jast_make(jast.SwitchBlockStatementGroups))
                for err,indexes in conv_errors.items():
                    suite = JavaTemplate('msg = "%s"; break; ' % err).fragment.BlockStatements
                    cases += java_templating.switchgroup(indexes,suite)
                bindings = {'cases': cases, 'unsafe_body': body_jt }
                body_jt = self.get_aux('conv_error_handling').tbind(bindings)

            call_meths_bindings['body%d' % dflc] = body_jt
            call_meths_bindings['args%d' % dflc] = args

        inst_call_meths = self.make_call_meths(rng,call_meths_bindings)

        return inst_call_meths,tot-rng+1,tot

    def expose_meth_body(self, name, parm, body):
        parm = parm.strip()
        if parm.find('>') != -1:
            prefix, parm = parm.split('>', 1)
            parm = parm.strip()
        else:
            prefix = self.type_name_plain+'_'
        if body is not None:
            return parm, prefix, body
        if parm.startswith(':'):
            retk,rest = parm.split(None,1)
            body = {
                ":i" : "`ideleg;",
                ":l" : "`ldeleg;",
                ":b" : "`bdeleg;",
                ":s" : "`sdeleg;",
                ":u" : "`udeleg;",
                ":-" : "`vdeleg; `void; ",
                ":o" : "`rdeleg;"
                }.get(retk, None)
            if not body:
                self.invalid(name,retk)
            return rest, prefix, body
        else:
            return parm, prefix, "`rdeleg;"


    def dire_expose_meth(self,name,parm,body): # !!!
        parm, prefix, body = self.expose_meth_body(name, parm, body)
        expose = self.get_aux('expose_narrow_meth')

        type_class = getattr(self,'type_class',None)
        type_name = getattr(self,'type_name',None)
        if type_class is None or type_name is None:
            raise Exception,"type_class or type_name not defined"
        parms = parm.strip().split(None,1)
        if len(parms) not in (1,2):
            self.invalid(name,parm)
        if len(parms) == 1:
            parms.append('')
        expose_bindings = {}
        expose_bindings['typ'] = type_class
        expose_bindings['name'] = JavaTemplate(parms[0])
        expose_bindings['deleg_prefix'] = make_name(prefix)

        # !!!
        call_meths_bindings = expose_bindings.copy()

        body_bindings = self.global_bindings.copy()
        body_bindings.update(expose_bindings)
        
        call_meths_bindings['call_meths'] = self.get_aux('call_meths').tbind({'typ': type_class})

        inst_call_meths,minargs,maxargs = self.handle_expose_meth_sig(parms[1],call_meths_bindings,body,body_bindings)

        expose_bindings['call_meths'] = inst_call_meths
        expose_bindings['minargs'] = JavaTemplate(str(minargs))
        expose_bindings['maxargs'] = JavaTemplate(str(maxargs))
        
        self.statements.append(expose.tbind(expose_bindings))

    #XXX: First pass is a c&p and modify of dire_expose_meth: do better.
    def dire_expose_cmeth(self,name,parm,body):
        parm, prefix, body = self.expose_meth_body(name, parm, body)
        expose = self.get_aux('expose_narrow_cmeth')

        type_class = getattr(self,'type_class',None)
        type_name = getattr(self,'type_name',None)
        if type_class is None or type_name is None:
            raise Exception,"type_class or type_name not defined"
        parms = parm.strip().split(None,1)
        if len(parms) not in (1,2):
            self.invalid(name,parm)
        if len(parms) == 1:
            parms.append('')
        expose_bindings = {}
        expose_bindings['typ'] = type_class
        expose_bindings['name'] = JavaTemplate(parms[0])
        expose_bindings['deleg_prefix'] = make_name(prefix)

        # !!!
        call_meths_bindings = expose_bindings.copy()

        body_bindings = self.global_bindings.copy()
        body_bindings.update(expose_bindings)
        
        call_meths_bindings['call_meths'] = self.get_aux('call_meths').tbind({'typ': type_class})

        inst_call_meths,minargs,maxargs = self.handle_expose_meth_sig(parms[1],call_meths_bindings,body,body_bindings)

        expose_bindings['call_meths'] = inst_call_meths
        expose_bindings['minargs'] = JavaTemplate(str(minargs))
        expose_bindings['maxargs'] = JavaTemplate(str(maxargs))
        
        self.statements.append(expose.tbind(expose_bindings))

    def dire_expose_unary(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        meth_names = parm.split()
        if meth_names[0].endswith('>'):
            meth_names = ["%s %s" % (meth_names[0], n) for n in meth_names[1:]]
        unary_body = self.get_aux('unary').fragment
        for meth_name in meth_names:
            self.dire_expose_meth('expose_unary_1',meth_name,unary_body)

    def dire_expose_binary(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        meth_names = parm.split()
        if meth_names[0].endswith('>'):
            meth_names = ["%s %s" % (meth_names[0], n) for n in meth_names[1:]]
        binary_body = self.get_aux('binary').fragment
        for meth_name in meth_names:
            self.dire_expose_meth('expose_binary_1',"%s o" % meth_name,binary_body)

    def dire_expose_key_getitem(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        prefix = ""
        if parm.endswith('>'):
            prefix = parm
        key_getitem_body = self.get_aux('key_getitem').fragment
        self.dire_expose_meth('expose_key_getitem',"%s __getitem__ o" % prefix,key_getitem_body)

    def dire_expose_index_getitem(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        prefix = ""
        if parm.endswith('>'):
            prefix = parm
        index_getitem_body = self.get_aux('index_getitem').fragment
        self.dire_expose_meth('expose_index_getitem',"%s __getitem__ o" % prefix,index_getitem_body)
        
    def dire_expose_vanilla_cmp(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        prefix = ""
        if parm.endswith('>'):
            prefix = parm
        vanilla_cmp_body = self.get_aux('vanilla_cmp').fragment
        self.dire_expose_meth('expose_vanilla_cmp',"%s __cmp__ o" % prefix,vanilla_cmp_body)

    def dire_expose_vanilla_pow(self,name,parm,body):
        if body is not None:
            self.invalid(name,'non-empty body')
        prefix = ""
        if parm.endswith('>'):
            prefix = parm
        vanilla_pow_body = self.get_aux('vanilla_pow').fragment
        self.dire_expose_meth('expose_vanilla_pow',"%s __pow__ oo?(null)" % prefix,vanilla_pow_body)

    def dire_expose_wide_meth(self,name,parm,body): # !!!
        parm, prefix, body = self.expose_meth_body(name, parm, body)
        parms = parm.split()
        args = JavaTemplate("void(PyObject[] args,String[] keywords)")
        all = JavaTemplate("args, keywords",start='Expressions')
        bindings = self.global_bindings.copy()
        if len(parms) not in (1,3):
            self.invalid(name,parm)
        bindings['name'] = JavaTemplate(parms[0])
        bindings['deleg_prefix'] = make_name(prefix)
        bindings['all'] = all
        bindings['args'] = args
        for deleg_templ_name in ('void','deleg','vdeleg','rdeleg'):
            deleg_templ = self.get_aux(deleg_templ_name)
            bindings[deleg_templ_name] = deleg_templ.tbind(bindings)
        body = JavaTemplate(body).tbind(bindings)
        bindings['body'] = body
        call_meths = self.get_aux('call_meths').tbind(bindings)
        bindings['call_meths'] = call_meths
        parms = (parms+[-1,-1])[1:3]
        bindings['minargs'] = JavaTemplate(parms[0])
        bindings['maxargs'] = JavaTemplate(parms[1])       
        expose = self.get_aux('expose_wide_meth').tbind(bindings)
        self.statements.append(expose)
  
    def dire_expose_new_mutable(self,name,parm,body):
        expose_new = self.get_aux('expose_new')
        parms = parm.split()
        body_bindings = self.global_bindings.copy()
        if body is None:
            body = self.get_aux('mutable_new_body')
        else:
            body = JavaTemplate(body)
        if not parms:
            parms = ['-1','-1']
        else:
            if len(parms) != 2:
                self.invalid(name,parm)
        body_bindings['minargs'] = JavaTemplate(parms[0])
        body_bindings['maxargs'] = JavaTemplate(parms[1])
        body = body.tbind(body_bindings)
        templ = expose_new.tbind(body_bindings)
        self.statements.append(templ.tbind({'body': body}))

    def dire_expose_new_immutable(self,name,parm,body):
        expose_new = self.get_aux('expose_new')
        parms = parm.split()
        body_bindings = self.global_bindings.copy()
        if body is not None:
            self.invalid(name,"non-empty body")
        body = self.get_aux('immutable_new_body')
        if not parms:
            parms = ['-1','-1']
        else:
            if len(parms) != 2:
                self.invalid(name,parm)
        body_bindings['minargs'] = JavaTemplate(parms[0])
        body_bindings['maxargs'] = JavaTemplate(parms[1])
        body = body.tbind(body_bindings)
        templ = expose_new.tbind(body_bindings)
        self.statements.append(templ.tbind({'body': body}))


    def dire_rest(self,name,parm,body):
        if parm:
            self.invalid(name,'non-empty parm')
        if body is None:
            return
        self.statements.append(JavaTemplate(body,start='BlockStatements')) 
    
    def generate(self):
        typeinfo0 = self.get_aux('typeinfo0')

        basic = JavaTemplate("",start='ClassBodyDeclarations')
        
        bindings = self.global_bindings.copy()
        if hasattr(self,'type_as'):
            basic = (basic + 
              JavaTemplate("public static final Class exposed_as = `as.class;",
                           start = 'ClassBodyDeclarations'))
            bindings['as'] = self.type_as
        else:
            basic = (basic + 
              JavaTemplate(
                "public static final String exposed_name = `strfy`(`name);",
                start='ClassBodyDeclarations'))
            bindings['name'] = self.type_name
            if hasattr(self,'type_base_class'):
               basic = (basic + 
                 JavaTemplate(
                   "public static final Class exposed_base = `base.class;",
                   start='ClassBodyDeclarations'))
               bindings['base'] = self.type_base_class

        typeinfo = typeinfo0
        
        setup = JavaTemplate("",start='BlockStatements')
        if not self.no_setup:
            typeinfo1 = self.get_aux('typeinfo1')
            
            pair = self.get_aux('pair')
            for statement in self.statements:
                setup = pair.tbind({'trailer': setup, 'last': statement})
            
            typeinfo = typeinfo.tfree() + typeinfo1.tfree()

        return typeinfo.tnaked().texpand({'basic': basic.tbind(bindings),
                                         'setup': setup},nindent=1)

def process(fn, mergefile=None):
    gen = Gen()
    directives.execute(directives.load(fn),gen)
    result = gen.generate()
    if mergefile is None:
        print result
    else:
        result = merge(mergefile, result)
    #gen.debug()
    
def usage():
    print "Usage: python %s infile [mergefile]" % sys.argv[0]

def merge(filename, generated):
    in_generated = False
    start_found = False
    end_found = False
    start_pattern = '    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py'
    end_pattern =   '    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py'
    output = []
    f = file(filename, 'r')
    for line in f:
        if line.startswith(start_pattern):
            in_generated = True
            start_found = True
        elif line.startswith(end_pattern):
            in_generated = False
            end_found = True
            output.append('%s\n%s\n%s\n' % (start_pattern, generated, end_pattern))
        elif in_generated:
            continue
        else:
            output.append(line)
    f.close()
    if not start_found:
        raise 'pattern [%s] not found in %s' % (start_pattern, filename)
    if not end_found:
        raise 'pattern [%s] not found in %s' % (end_pattern, filename)
    f = file(filename, 'w')
    f.write("".join(output))
    f.close()

if __name__ == '__main__':
    if (len(sys.argv) < 2 or len(sys.argv) > 3):
        usage()
    elif (len(sys.argv) == 2):
        process(sys.argv[1])
    elif (len(sys.argv) == 3):
        process(sys.argv[1], sys.argv[2])
