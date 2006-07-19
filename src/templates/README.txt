Some classes have generated code to enable their usage within Jython.  Each 
such file will have a generated section that is created with the gexpose.py 
script.  For the PyIntger class it is created thus:

  python gexpose.py int.expose ../../jython/src/org/python/core/PyInteger.java

For each class there is an xxx.expose file describing what should be exposed.

In addition there is an xxxDerived.java class that is completely generated 
with the script gderived.py.  For the PyInteger class it is created thus:

  python gderived.py int.derived >../../jython/src/org/python/core/PyIntegerDerived.java

Note:  The above examples assume that the whole jython trunk is checked out so 
that the trunk/sandbox directory is a sibling of the trunk/jython directory.
