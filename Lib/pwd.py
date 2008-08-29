"""
This module provides access to the Unix password database.

Password database entries are reported as 7-tuples containing the
following items from the password database (see `<pwd.h>'), in order:
pw_name, pw_passwd, pw_uid, pw_gid, pw_gecos, pw_dir, pw_shell.  The
uid and gid items are integers, all others are strings. An exception
is raised if the entry asked for cannot be found.
"""

__all__ = ['getpwuid', 'getpwnam', 'getpwall']

from os import _name, _posix
from java.lang import NullPointerException

if _name == 'nt':
    raise ImportError, 'pwd module not supported on Windows'

class struct_passwd(tuple):
    """
    pwd.struct_passwd: Results from getpw*() routines.

    This object may be accessed either as a tuple of
      (pw_name,pw_passwd,pw_uid,pw_gid,pw_gecos,pw_dir,pw_shell)
    or via the object attributes as named in the above tuple.
    """

    attrs = ['pw_name', 'pw_passwd', 'pw_uid', 'pw_gid', 'pw_gecos',
             'pw_dir', 'pw_shell']

    def __new__(cls, pwd):
        return tuple.__new__(cls, (getattr(pwd, attr) for attr in cls.attrs))

    def __getattr__(self, attr):
        try:
            return self[self.attrs.index(attr)]
        except ValueError:
            raise AttributeError

def getpwuid(uid):
    """
    getpwuid(uid) -> (pw_name,pw_passwd,pw_uid,
                      pw_gid,pw_gecos,pw_dir,pw_shell)
    Return the password database entry for the given numeric user ID.
    See pwd.__doc__ for more on password database entries.
    """
    try:
        return struct_passwd(_posix.getpwuid(uid))
    except NullPointerException:
        raise KeyError, uid

def getpwnam(name):
    """
    getpwnam(name) -> (pw_name,pw_passwd,pw_uid,
                        pw_gid,pw_gecos,pw_dir,pw_shell)
    Return the password database entry for the given user name.
    See pwd.__doc__ for more on password database entries.
    """
    try:
        return struct_passwd(_posix.getpwnam(name))
    except NullPointerException:
        raise KeyError, name

def getpwall():
    """
    getpwall() -> list_of_entries
    Return a list of all available password database entries,
    in arbitrary order.
    See pwd.__doc__ for more on password database entries.
    """
    entries = []
    try:
        while True:
            entries.append(struct_passwd(_posix.getpwent()))
    except NullPointerException:
        return entries
