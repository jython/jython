Some classes have generated code to enable their usage within Jython.  Each 
such file will have a generated section that is created with the gexpose.py 
script.  For the PyInteger class it is created thus:

  python gexpose.py int.expose ../../jython/src/org/python/core/PyInteger.java

For each class there is an xxx.expose file describing what should be exposed.

In addition there is an xxxDerived.java class that is completely generated 
with the script gderived.py.  For the PyInteger class it is created thus:

  python gderived.py int.derived >../../jython/src/org/python/core/PyIntegerDerived.java

There is an ant target to generate these automatically.  See the template
target in the top-level build file, or the org.python.util.TemplateAntTask
ant task.  In the future, the template generation will be linked into the
main build targets.
