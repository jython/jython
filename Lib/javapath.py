"""Common pathname manipulations, JDK version.

Instead of importing this module directly, import os and refer to this
module as os.path.

"""

# Incompletely implemented:
# islink -- How?
# ismount -- How?
# splitdrive -- How?
# normcase -- How?

# Missing:
# expandvars -- can't be bothered right now
# sameopenfile -- Java doesn't have fstat nor file descriptors?
# samestat -- How?

import java
from java.io import File
from java.lang import System
import os

def dirname(path):
    """Return the directory component of a pathname"""
    result = File(path).getParent()
    if not result:
	if isabs(path):
	    result = path # Must be root
	else:
	    result = ""
    return result

def basename(path):
    """Return the final component of a pathname"""
    return File(path).getName()

def split(path):
    """Split a pathname.

    Return tuple "(head, tail)" where "tail" is everything after the
    final slash.  Either part may be empty.

    """
    return (dirname(path), basename(path))

def splitext(path):
    """Split the extension from a pathname.

    Extension is everything from the last dot to the end.  Return
    "(root, ext)", either part may be empty.

    """
    i = 0
    n = -1
    for c in path:
        if c == '.': n = i
        i = i+1
    if n < 0:
        return (path, "")
    else:
        return (path[:n], path[n:])

def splitdrive(path):
    """Split a pathname into drive and path.

    On JDK, drive is always empty.
    XXX This isn't correct for JDK on DOS/Windows!

    """
    return ("", path)

def exists(path):
    """Test whether a path exists.

    Returns false for broken symbolic links.

    """
    return File(path).exists()

def isabs(path):
    """Test whether a path is absolute"""
    return File(path).isAbsolute()

def isfile(path):
    """Test whether a path is a regular file"""
    return File(path).isFile()

def isdir(path):
    """Test whether a path is a directory"""
    return File(path).isDirectory()

def join(path, *args):
    """Join two or more pathname components, inserting os.sep as needed"""
    f = File(path)
    for a in args:
	g = File(a)
	if g.isAbsolute() or len(f.getPath()) == 0:
	    f = g
	else:
	    f = File(f, a)
    return f.getPath()

def normcase(path):
    """Normalize case of pathname.

    XXX Not done right under JDK.

    """
    return File(path).getPath()

def commonprefix(m):
    "Given a list of pathnames, return the longest common leading component"
    if not m: return ''
    prefix = m[0]
    for item in m:
        for i in range(len(prefix)):
            if prefix[:i+1] <> item[:i+1]:
                prefix = prefix[:i]
                if i == 0: return ''
                break
    return prefix

def islink(path):
    """Test whether a path is a symbolic link.

    XXX This incorrectly always returns false under JDK.

    """
    return 0

def samefile(path, path2):
    """Test whether two pathnames reference the same actual file"""
    f = File(path)
    f2 = File(path2)
    return f.getCanonicalPath() == f2.getCanonicalPath()

def ismount(path):
    """Test whether a path is a mount point.

    XXX This incorrectly always returns false under JDK.

    """
    return 0


def walk(top, func, arg):
    """Walk a directory tree.

    walk(top,func,args) calls func(arg, d, files) for each directory
    "d" in the tree rooted at "top" (including "top" itself).  "files"
    is a list of all the files and subdirs in directory "d".

    """
    try:
        names = os.listdir(top)
    except os.error:
        return
    func(arg, top, names)
    for name in names:
	name = join(top, name)
	if isdir(name) and not islink(name):
	    walk(name, func, arg)

def expanduser(path):
    if path[:1] == "~":
	c = path[1:2]
	if not c:
	    return gethome()
	if c == os.sep:
	    return File(gethome(), path[2:]).getPath()
    return path

def getuser():
    return System.getProperty("user.name")

def gethome():
    return System.getProperty("user.home")


# normpath() from Python 1.5.2, with Java appropriate generalizations

# Normalize a path, e.g. A//B, A/./B and A/foo/../B all become A/B.
# It should be understood that this may change the meaning of the path
# if it contains symbolic links!
def normpath(path):
    """Normalize path, eliminating double slashes, etc."""
    sep = os.sep
    if sep == '\\':
        path = path.replace("/", sep)
    curdir = os.curdir
    pardir = os.pardir
    import string
    # Treat initial slashes specially
    slashes = ''
    while path[:1] == sep:
        slashes = slashes + sep
        path = path[1:]
    comps = string.splitfields(path, sep)
    i = 0
    while i < len(comps):
        if comps[i] == curdir:
            del comps[i]
            while i < len(comps) and comps[i] == '':
                del comps[i]
        elif comps[i] == pardir and i > 0 and comps[i-1] not in ('', pardir):
            del comps[i-1:i+1]
            i = i-1
        elif comps[i] == '' and i > 0 and comps[i-1] <> '':
            del comps[i]
        else:
            i = i+1
    # If the path is now empty, substitute '.'
    if not comps and not slashes:
        comps.append(curdir)
    return slashes + string.joinfields(comps, sep)

# Return an absolute path.
def abspath(path):
    return File(path).getAbsolutePath()


def getsize(path):
    f = File(path)
    size = f.length()
    # Sadly, if the returned length is zero, we don't really know if the file
    # is zero sized or does not exist.
    if size == 0 and not f.exists():
        raise OSError(0, 'No such file or directory', path)
    return size

def getmtime(path):
    f = File(path)
    return f.lastModified() / 1000.0

def getatime(path):
    # We can't detect access time so we return modification time. This
    # matches the behaviour in os.stat().
    f = File(path)
    return f.lastModified() / 1000.0

