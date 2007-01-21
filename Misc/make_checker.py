import __builtin__

import sys
import __builtin__

opt = lambda n: getattr(__builtin__, n, None)

def f(): pass
try:
    raise Exception
except:
    _, _, tb = sys.exc_info() 

class C:
    f = f

m = C.f

types_list = [
object,
type,
unicode,
dict,
list,
slice,
super,
staticmethod,
float,
opt('enumerate'),
open,
opt('basestring'),
long,
tuple,
str,
property,
int,
xrange,
file,
complex,
opt('bool'),
classmethod,
#buffer,
# +
type(f),
type(m),
type(f.func_code),
type(sys._getframe()),
type(tb),
type(slice),
]

del f, tb
if 0:
    for n, x in __builtin__.__dict__.items():
        if isinstance(x, type):
            if x not in types:
                print "%s," % n

i = 0
#Make types a mapping from an item in types_list to its index
types = {}
for t in types_list:
    if t is not None:
        types.setdefault(t, i)
    i += 1

extra = {
    type(dict.__dict__.get('fromkeys')): types[classmethod], # xxx hack
    type(list.__dict__['append']): 'm',
    type(int.__dict__['__add__']): 'm',
    type(int.__dict__['__new__']): 'n',
    type(object.__dict__['__class__']): 'd',
    type(super.__dict__['__thisclass__']): 'd',
    type(None): '-',
}

def which(t):
    '''Get the index of t in types_list or its str value in extra if it's not in types_list'''
    try:
        return types[t]
    except KeyError:
        return extra[t]

def do_check(names, checks):
    def swhich(t):
        try:
            return which(t)
        except KeyError:
            return "%s?" % t.__name__
    def n(fnd):
	'''Gets the name of fnd if it's a single item, or its names if it's a tuple'''
        if isinstance(fnd, tuple):
            return tuple(map(n, fnd))
        r = names.get(fnd, fnd)
        if isinstance(r, int):
            return "%s?" % types_list[fnd].__name__
        return r
    missing = []
    bad_type = []
    different = []
    ok = []
    for check in checks:
        index, expected_type, expected_bases, expected_dict = check
        t = types_list[index]
        if t is None:
	    missing.append(t)
            continue
        which_type = swhich(type(t))
        if which_type != expected_type:
	    bad_type.append((t, names[index], n(expected_type)))
        elif expected_bases:
	    differences = {}
            which_bases = tuple(map(swhich, t.__bases__))
            if which_bases != expected_bases:
	        differences['bases'] = ['had %s but expected %s' % (n(which_bases), n(expected_bases))]
            d = t.__dict__
	    bad_types = []
            miss = []
            extra = []
            for name in d.keys():
                if name not in expected_dict:
                    extra.append(name)
            if extra:
		differences['extra'] = extra
            for name, expected in expected_dict.items():
                if name not in d:
                    miss.append(name)
                else:
                    which_type = swhich(type(d[name]))
                    if which_type != expected:
			bad_types.append("%r type %s isn't %s" % (name, n(which_type), n(expected)))
            if miss:
		differences['missing'] = miss
	    if bad_types:
		differences['bad_types'] = bad_types
	    if differences:
		different.append((t, names[index], differences))
	    else:
		ok.append(names[index])
    return ok, missing, bad_type, different

def report(ok, missing, bad_type, different):
    if ok:
        print 'OK: %s' % ', '.join(ok)
    if missing:
	print 'Missing: %s' % ', '.join(missing)
    if bad_type:
	print 'Bad Type:'
	for t, name, expected_type in bad_type:
	    print "    ", name, t, "type isn't", expected_type
    if different:
	print 'Different:'
	for t, name, differences in different:
	    print '    ', name, t
	    for k, v in differences.items():
		if not v: continue
		print '        ', k
		for val in v:
		    print '            ', val

if __name__ == '__main__':
    names = {}
    checks = []
    i = -1
    for t in types_list:
        i += 1
        if t is None:
            continue
        names[i] = t.__name__
        check = []
        checks.append(check)
        check.append(which(t)) # index
        assert type(t) in types, t
        check.append(which(type(t)))
        if not hasattr(t, '__bases__'):
            check.extend([None, None])
            continue
        bases = []
        for b in t.__bases__:
            assert b in types
            bases.append(which(b))
        check.append(tuple(bases))
        membs = {}
        for n,x in t.__dict__.items():
            membs[n] = which(type(x))
        check.append(membs)


    # sanity-check
    report(*do_check(names, checks))

    ver = sys.version.split()[0]
    simple_ver = ver[:3].replace('.', '')

    import pprint
    f = open('checker%s.py' % simple_ver, 'w')
    print >>f, "names = ",
    pprint.pprint(names, stream=f)
    print >>f, "checks = ",
    pprint.pprint(checks, stream=f)
    print >>f, '''source_version = '%s'
if __name__ == '__main__':
    print 'comparing with information from %%s' %% source_version
    import make_checker
    make_checker.report(*make_checker.do_check(names, checks))''' % ver
    f.close()

    


