This example shows one way to incorporate a Jython class into a Java
program.

To do this you should follow the following steps:

Note: path names are given for a Unix machine.  Make the obvious
translation for Windows.

1. run "jython Graph.py" in this directory

   This is just to make sure the Jython code works on your machine

2. run "jythonc -package pygraph Graph.py" in this directory

   This should produce the Java class pygraph.Graph.  Because this is
   only a shallow freeze of the code in Graph.py, you can modify the
   actual Python code (and any libraries it depends on) without needed
   to perform the freeze process again.  You will need to repeat this
   freeze process any time you add new methods to the Graph class that
   override Java methods in its superclass.

   Notice the strange "@sig ..." doc comments on the __init__ and the
   setExpression methods.  These cause convenient methods to be
   created on the Java proxy class.

3. run "javac pygraph/PythonGraph.java"

   You must have both the current directory ('.') and the Jython
   library directory (<install_dir>\jython.jar) in your CLASSPATH for
   this to work.

4. run "java pygraph.PythonGraph"

   You need the same classpath as given above
