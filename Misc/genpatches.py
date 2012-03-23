#!/usr/bin/env python
"""
This script generates patches containing the Jython-specific deviations from the stdlib
modules. It looks for the string "svn.python.org" in the changeset summary and generates
a patch for all the changes made after that in each module.

The script expects to be run in the jython root directory and there should be a "cpython"
directory in the parent directory. That directory should be a clone of the CPython release
against which the modules in Lib/ have currently been patched.
Only modules that are common to Lib/, lib-python/ and ../cpython/Lib will be included in
the patches.
"""
from StringIO import StringIO
import os.path
import subprocess
import sys
import shutil


def get_modules(path):
    modules = set()
    for dirpath, dirnames, filenames in os.walk(path):
        for filename in filenames:
            if filename.endswith('.py'):
                cutoff = len(path) + 1
                fullpath = os.path.join(dirpath[cutoff:], filename)
                modules.add(fullpath)
    return modules

if not os.path.exists('lib-python'):
    print >>sys.stderr, 'You need to run this script from the Jython root directory.'
    sys.exit(1)

if not os.path.exists('../cpython'):
    print >>sys.stderr, 'You need to have the CPython clone in ../cpython.'
    sys.exit(1)

jymodules = get_modules(u'Lib')
cpymodules = get_modules(u'lib-python')
cpy25modules = get_modules(u'../cpython/Lib')
common_modules = jymodules.intersection(cpy25modules).intersection(cpymodules)

# Run mercurial to get the changesets where each file was last synced with CPython stdlib
print 'Parsing mercurial logs for the last synchronized changesets'
changesets = {}
for mod in common_modules:
    path = 'Lib/' + mod
    pipe = subprocess.Popen(['hg', 'log', '-v', path], stdout=subprocess.PIPE)
    stdoutdata, stderrdata = pipe.communicate()
    if pipe.returncode != 0:
        print >>sys.stderr, stderrdata
        sys.exit(1)

    buf = StringIO(stdoutdata)
    changeset = None
    found = False
    iterator = iter(list(buf))
    for line in iterator:
        if line.startswith('changeset:'):
            changeset = line.split(':')[1].strip()
        if line.startswith('description:'):
            for descline in iterator:
                if descline == '\n':
                    break
                if 'svn.python.org' in descline:
                    found = True
                    break
            if found:
                break

    if not found:
        print >>sys.stderr,'No sync changeset found for %s' % path
    else:
        changesets[path] = changeset

if os.path.exists('patches'):
    shutil.rmtree('patches')
os.mkdir('patches')

print 'Generating patches'
for path, changeset in changesets.iteritems():
    patchname = 'patches/%s.patch' % path[4:]
    patch_dir = os.path.dirname(patchname)
    if not os.path.exists(patch_dir):
        os.makedirs(patch_dir)

    retcode = os.system('hg diff -r {} {} > {}'.format(changeset, path, patchname))
    if retcode != 0:
        print >>sys.stderr, "Error creating patch for %s" % path
        sys.exit(3)

print 'All done. You can now run applypatches.py to update and patch the modules.'
