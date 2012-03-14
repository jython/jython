#!/usr/bin/env python
"""
This script copies the relevant files from CPythonLib and applies the patches previously
made with genpatches.py. You will probably need to tweak some patches by hand, or delete
them entirely and do the migration for those modules manually.
"""

import os.path
import sys
import shutil


if not os.path.exists('patches'):
    print >>sys.stderr, 'Run genpatches.py first.'
    sys.exit(1)

succeeded = []
failed = []
for dirpath, dirnames, filenames in os.walk('patches'):
    for filename in filenames:
        realfilename = filename[:-6]
        patchpath = os.path.join(dirpath, filename)
        srcpath = os.path.join('CPythonLib', dirpath[8:], realfilename)
        dstpath = srcpath.replace('CPythonLib', 'Lib')
        print '\nCopying %s -> %s' % (srcpath, dstpath) 
        sys.stdout.flush()
        shutil.copyfile(srcpath, dstpath)

        retcode = os.system('patch -p1 -N <%s' % patchpath)
        if retcode != 0:
            failed.append(dstpath)
        else:
            succeeded.append(dstpath)

if succeeded:
    print '\nThe following files were successfully patched:'
    for path in sorted(succeeded):
        print path

if failed:
    print '\nPatching failed for the following files:'
    for path in sorted(failed):
        print path
    print '\nYou will need to migrate these modules manually.'
