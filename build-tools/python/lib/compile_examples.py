# Reading compiled Python files

import sys, os.path
import marshal, py_compile, dis

# Normally you don't get a .pyc file if you just run a program.
# You do get a .pyc file from compiling a module.
# It is written in ./__pycache__ and called NAME.cpython-38.pyc

CACHE = '__pycache__'
COMPILER = 'cpython-38'


HELP =  """Command: compile_examples srcdir dstdir

        where:
        srcdir  is the root of the Python examples in the project source
                typically ./src/test/pythonExample
        dstdir  is the root of the Python examples in the build tree
                typically ./build/generated/sorces/pythonExample/test
        """



def getcode(filename):
    "Read a compiled file and return the code object"
    with open(filename, 'rb') as f:
        # Skip header. See run_pyc_file() in pythonrun.c
        f.read(16)
        return marshal.load(f)


def getobj(filename):
    "Read an object from a file"
    with open(filename, 'rb') as f:
        return marshal.load(f)


def filetime(path_elements, name_elements):
    """Compose a file path and report when last modified

    path_elements: file path elements (will be os.path.join'd)
    name_elements: file name elements (will be '.'.join'd)

    The arguments are used to locate a file (which need not exist)
    and the function returns the name (always) and the last modified
    time (or zero if the file does not exist).
    """

    file = os.path.join(*path_elements, '.'.join(name_elements))
    try:
        time = os.path.getmtime(file)
    except OSError:
        time = 0
    #print(f"{file:>40s}: {time:15.3f}")
    return file, time


def copy(srcfile, dstfile):
    "Copy one text file to another"
    print(f"  Copy: {os.path.basename(srcfile)}")
    with open(srcfile, 'rt', encoding='utf-8') as s:
        ensure_dir(os.path.dirname(dstfile))
        with open(dstfile, 'wt', encoding='utf-8') as d:
            for line in s:
                d.write(line)


def execute(pycfile, varfile, disfile):
    "Execute a program and save the local variables"
    print(f"  Generate: {os.path.basename(disfile)}")
    co = getcode(pycfile)
    with open(disfile, 'wt', encoding='utf-8') as f:
        # Dumps code blocks of nested functions
        dis.dis(co, file=f)
    print(f"  Generate: {os.path.basename(varfile)}")
    gbl = dict()
    exec(co, gbl)
    # Remove items forced in by exec
    del gbl['__builtins__']
    # try:
    #     print("   ", list(gbl.keys()))
    # except UnicodeEncodeError:
    #     pass
    with open(varfile, 'wb') as f:
        marshal.dump(gbl, f)


def generate(reldir, name, source, generated):
    """Generate test reldir/name.py and results

    reldir:     the relative directory path (from source/generated)
    name:       just the name part of the Python file
    source:     directory of the source files
    generated:  directory of the compiled/generated files
    """
    srcfile, srctime = filetime([source, reldir], [name, 'py'])
    #print(f"   {name}.py")
    #print(f"      source: {srctime:15.3f}")

    dstfile, dsttime = filetime([generated, reldir], [name, 'py'])
    #print(f"       build: {dsttime:15.3f}")

    pycfile, pyctime = filetime([generated, reldir, CACHE],
                  [name, COMPILER, 'pyc'])
    #print(f"        .pyc: {pyctime:15.3f}")

    varfile, vartime = filetime([generated, reldir, CACHE],
                  [name, COMPILER, 'var'])
    #print(f"        .var: {vartime:15.3f}")

    disfile, distime = filetime([generated, reldir, CACHE],
                  [name, COMPILER, 'dis'])
    #print(f"        .dis: {distime:15.3f}")

    if dsttime < srctime:
        # Copy, compile, run and store
        copy(srcfile, dstfile)
        dsttime = srctime

    if pyctime < dsttime:
        # Compile, run and store
        print(f"  Compile: {os.path.basename(pycfile)}")
        py_compile.compile(dstfile)
        pyctime = os.path.getmtime(pycfile)

    if vartime < pyctime or distime < pyctime:
        # Run and store
        execute(pycfile, varfile, disfile)


def ensure_dir(d):
    if not os.path.exists(d):
        os.makedirs(d)
    if not (ok:=os.path.isdir(d)):
        print(f"Not a directory '{d}'", file=sys.stderr)
    return ok


def main(source, generated):

    for dirpath, _, files in os.walk(source):
        #print(f"{dirpath}:")
        reldir = os.path.relpath(dirpath, source)
        for file in files:
            parts = file.rsplit('.', 1)
            if len(parts) > 1 and parts[1] == "py":
                name = parts[0]
                #print(f"  {name}:")
                generate(reldir, name, source, generated)

def show_help():
    print(HELP, file=sys.stderr)

# --------------------------------------------------------------------

if len(sys.argv) == 3:
    source, generated = sys.argv[1:]
    if ensure_dir(source) and ensure_dir(generated):
        cwd = os.getcwd()
        source = os.path.relpath(source)
        generated = os.path.relpath(generated)
        main(source, generated)
    else:
        show_help()
else:
    show_help()


