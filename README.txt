Welcome to Jython 2.0 alpha 2
=============================

Jython is the successor to JPython. The Jython project was
created in accordance with the CNRI JPython 1.1.x license, in
order to ensure the continued existence and development of this
important piece of Python software.  


If you are a JPython-1.1 user, you'll want to take a look at some
of the differences between JPython-1.1 and Jython 2.0.

In particulary the few places where backward compatibility have 
been broken. 

    - The user configuration file is now called <user.home>/.jython

    - The jar file conytaining all jython is now called jython.jar.

    - Text files will pass data read and written through the default
      codecs for the JVM. Binary files will write only the lower eight
      bits of each unicode character.

    - arrays passed to java code will no longer autocoerce just 
      because the elements can be autocoerced.

    - The precedence of java loading have changed. Now the sys.path
      is searched for python modules before the CLASSPATH and sys.path
      is searched for java class and java packages.

    - The \x escape have changed, now it will eat two hex characters
      but never more. The behaviour matches CPython2.0

    - The python.path property is appended to sys.path instead of
      being inserted at position 1 in sys.path.

