This example shows one way to incorporate a JPython class into a Java program.

To do this you should follow the following steps:

Note: path names are given for a Windows machine.  Make the obvious translation for Unix.

1. run "jpython Graph.py" in this directory
    This is just to make sure the JPython code works on your machine

2. run "jpythonc Graph.py" in this directory
    This should produce the Java class Graph.class.  Because this is only
    a shallow freeze of the code in Graph.py, you can modify the actual
    Python code (and any libraries it depends on) without needed to perform
    the freeze process again.  You will need to repeat this freeze process
    any time you add new methods to the Graph class that override Java
    methods in its superclass.

3. run "javac PythonGraph.java"
    You must have both the current directory ('.') and the JPython library
    directory (<install_dir>\jpython.jar) in your CLASSPATH for this to work.

4. run "java PythonGraph"
    You need the same classpath as given above