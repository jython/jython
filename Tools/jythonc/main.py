# Copyright © Corporation for National Research Initiatives

"""Usage: jpythonc [options] [module]*

where options include:

  --package package
  -p package
      put all compiled code into the named Java package

  --jar jarfile
  -j jarfile
      Compile into jarfile (implies --deep)

  --deep
  -d
      Compile all Python dependencies of the module 

  --core
  -c
      Include the core JPython libraries (implies --deep)
      
  --all
  -a
      Include all of the JPython libraries (implies --deep)

  --bean jarfile
  -b jarfile
      Compile into jarfile, include manifest for bean
      
  --addpackages packages
  -A packages
      Include Java dependencies from this list of packages.  Default is
      org.python.modules and com.oroinc.text.regex
      
  --workdir directory
  -w directory
      Specify working directory for compiler (default is ./jpywork)
      
  --skip modules
  -s modules
      Don't include any of these modules in compilation
      
  --compiler path
  -C path
      Use a different compiler than `standard' javac.  If this is set to
      `NONE' then compile ends with .java.  Alternatively, you can set the
      property python.jpythonc.compiler in the registry.

  --compileropts options
  -J options
      Options passed directly to the Java compiler.  Alternatively, you can
      set the property python.jpythonc.compileropts in the registry.

  --falsenames names
  -f names
      A comma-separated list of names that are always false.  Can be used to
      short-circuit if clauses.

  --help
  -h
      Print this message and exit
"""

from compile import Compiler
import sys, string, os
import getopt
                


def addCore(extraPackages):
    skiplist = ['org.python.core.parser',
                'org.python.core.BytecodeLoader',
                'org.python.core.jpython',
                ]
    extraPackages.append(('org.python.core', skiplist))


def addAll(extraPackages):
    for name in ['core', 'compiler', 'parser']:
        extraPackages.append(('org.python.'+name, []))
                
                      

def usage(errcode, msg=''):
    print __doc__ % globals()
    if msg:
        print msg
    sys.exit(errcode)


def getOptions():
    class Opts:
        jar = None
        workdir = 'jpywork'
        core = 0
        all = 0
        deep = 0
        bean = None
        skip = ''
        package = None
        addfiles = ''
        falsenames = []
        compiler = None
        jopts = None
        addpackages = ''
        
    options = Opts()

    oldopts = ['-jar', '-workdir', '-core', '-all', '-deep',
               '-bean', '-skip', '-package', '-addfiles', '-falsenames',
               '-compiler', '-addpackages']

    # For backwards compatibility, long options used to take only a single
    # dash.  Convert them to GNU style (e.g. as supported by getopt)
    sysargs = []
    for arg in sys.argv[1:]:
        if arg in oldopts:
            newarg = '-'+arg
            print 'Warning:', arg, 'is deprecated, use', newarg
            sysargs.append(newarg)
        else:
            sysargs.append(arg)

    try:
        opts, args = getopt.getopt(
            sysargs, 'p:j:dcab:A:w:s:C:f:J:h',
            ['package=', 'jar=', 'deep', 'core', 'all', 'bean=',
             'addpackages=', 'workdir=', 'skip=', 'compiler=',
             'falsenames=', 'compileropts=', 'help'])
    except getopt.error, msg:
        usage(1, msg)

    for opt, arg in opts:
        if opt in ('-h', '--help'):
            usage(0)
        elif opt in ('-p', '--package'):
            options.package = arg
        elif opt in ('-j', '--jar'):
            options.jar = arg
            options.deep = 1
        elif opt in ('-d', '--deep'):
            options.deep = 1
        elif opt in ('-c', '--core'):
            options.core = 1
            options.deep = 1
        elif opt in ('-a', '--all'):
            options.all = 1
            options.deep = 1
        elif opt in ('-b', '--bean'):
            options.bean = arg
        elif opt in ('-A', '--addpackages'):
            options.addpackages = arg
        elif opt in ('-w', '--workdir'):
            options.workdir = arg
        elif opt in ('-s', '--skip'):
            options.skip = arg
        elif opt in ('-C', '--compiler'):
            options.compiler = arg
        elif opt in ('-J', '--compileropts'):
            options.jopts = arg.split()
        elif opt in ('-f', '--falsenames'):
            options.falsenames = arg.split(',')

    # there should be at least one module to compile
    if len(args) == 0:
        usage(0, 'nothing to compile')

    # post processing
    options.args = args

    if not os.path.isabs(options.workdir):
        options.workdir = os.path.join(".", options.workdir)  

    return options



mainclass = basepath = None

def doCompile(opts):
    skiplist = string.split(opts.skip, ",")
    optpkgs = string.split(opts.addpackages, ',')
    addpackages = ['org.python.modules',
                   'com.oroinc.text.regex'] + optpkgs

    comp = Compiler(javapackage=opts.package,
                    deep=opts.deep,
                    include=addpackages,
                    skip=skiplist,
                    options=opts)
    global mainclass
    global basepath

    for target in opts.args:
        if target.endswith('.py'):
            classname = os.path.splitext(os.path.basename(target))[0]
            filename = target
            if basepath is None:
                basepath = os.path.split(target)[0]
                sys.path.insert(0, basepath)
        else:
            classname = target
            import ImportName
            m = ImportName.lookupName(classname)
            if not m:
                print 'Could not find class:', classname
                sys.exit(1)
            filename = m.file
        if mainclass is None:
            mainclass = classname
            if opts.package is not None:
                mainclass = opts.package+'.'+mainclass
        comp.compilefile(filename, classname)

    comp.dump(opts.workdir)
    return comp



def copyclass(jc, fromdir, todir):
    import jar
    from java.io import FileInputStream, FileOutputStream

    name = apply(os.path.join, jc.split('.'))+'.class'
    fromname = os.path.join(fromdir, name)
    toname = os.path.join(todir, name)
    tohead = os.path.split(toname)[0]
    if not os.path.exists(tohead):
        os.makedirs(tohead)
    istream = FileInputStream(fromname)
    ostream = FileOutputStream(toname)
    jar.copy(istream, ostream)
    istream.close()
    ostream.close()



def writeResults(comp, opts):
    global mainclass
    global basepath

    javaclasses = comp.javaclasses

    if opts.bean is not None:
        jarfile = opts.bean
    else:
        jarfile = opts.jar              

    if jarfile is None:
        if not opts.deep and opts.package is not None:
            for jc in javaclasses:
                if isinstance(jc, type( () )):
                    jc = jc[0]
                if basepath is None:
                    basepath = '.'
                copyclass(jc, opts.workdir, basepath)
        sys.exit(0)

    print 'Building archive:', jarfile
    from jar import JavaArchive

    extraPackages = []

    if opts.core:
        addCore(extraPackages)
    if opts.all:
        addAll(extraPackages)

    ja = JavaArchive(extraPackages)
    ja.addToManifest({'Main-Class':mainclass})
    for jc in javaclasses:
        if isinstance(jc, type( () )):
            ja.addClass(opts.workdir, jc[0], jc[1])
        else:
            ja.addClass(opts.workdir, jc)

    for dep in comp.trackJavaDependencies():
        ja.addEntry(dep)

    if opts.core or opts.all:
        ja.addSingleClass('org.python.modules.Setup')

    ja.dump(jarfile)
        


def main():
    opts = getOptions()
    comp = doCompile(opts)
    writeResults(comp, opts)
