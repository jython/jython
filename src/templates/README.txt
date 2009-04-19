Derived classes (classes that allow for user extension) are created as follows:

1. Create a template file xxx.derived
2. Modify mappings, which associates a template with a specific class in
   the source tree to be generated
3. Run (with CPython) gderived.py against the the template file

Example: creating a derivable version of int

from the file int.derived:

  base_class: PyInteger
  want_dict: true
  ctr: int v
  incl: object

from mappings, the relevant entry (please keep sorted):

  int.derived:org.python.core.PyIntegerDerived

To generate the source of the class, src/org/python/core/PyInteger.java:

  python gderived.py int.derived

