Welcome to Jython 2.2a0
=======================

This is a snapshot release - it contains a mixture of 2.1 and 2.2 features.

There has been no validation of which parts of the Python 2.2 library
are functional with this release. Expect pickle and related modules to be very broken, others as well.

Use at your own discretion, and provide feedback (jython-kbutler@sabaydi.com) of which modules are broken.

It was compiled with Sun JDK 1.4, on Windows.  It thus will not be
compatible with previous JDK versions, and lacks the readline library.


The installer image did not work correctly, so this is just an archive of
the CVS tree.


Major changes since Jython 2.1
========================
- iterators

- __future__ division

- multiple sql enhancements

- scoping enhancements

- os module support for system, environment, and popen functions

- jythonc basically unchanged, meaning it does not support Python 2.2 features

- generators are mostly finished in interpreter

- int-long unification (overflows return PyLong)

- many bugfixes


Test status
===========
- Passes tests in Lib/test/testall.py
- Unable to run zxjdbctests at this point
- Have not run CPython test suite
- Have not run PyXML test suite (and basically just included the distribution)



To do for 2.2a1
===============
- compile with JDK1.3 -target 1.1

- compile on linux, build readline library

- be selective on PyXML files

- identify Python 2.2 library files to include

- The installer .class file built by this process doesn't work yet - it starts up, prompts for the OS (!),
then fails to load os.properties




Build procedure
===============
3rd-party package sources:

JavaCC
License: Custom, does *NOT* allow redistribution
Only needed if you change the Jython grammar.
http://www.webgain.com/products/java_cc/


PyXML
License: Python license
Needed to copy PyXML files to jython.
http://pyxml.sourceforge.net/

Jikes
License: IBM Public License (allows redistribution)
Alternate compiler for Java code, can use classic javac compiler instead.
Selected by build.compiler = classic or build.compiler = jikes
http://oss.software.ibm.com/developerworks/opensource/jikes/

Python2.2
License: Python license
Used for python library (?) and for building documentation and installing
PyXML source.
http://www.python.org

HT2HTML
License: Python license
Used for building documentation
http://ht2html.sourceforge.net/

servlet.jar
License: Apache Software License, BSD-style
Used for building PyServlet support. Can obtain from Tomcat project, installed tomcat/common/lib/servlet.jar
http://jakarta.apache.org/tomcat/index.html

java_readline / libreadline-java.jar
License: LGPL, note that the Unix libreadline.so is GPL
Used for building Readline support (Unix only).  Build as JAVAC=javac make, don't worry about errors if missing C libraries, etc., as Jython only needs the jar file to meet the Java dependencies.
http://java-readline.sourceforge.net/

Database modules, needed for building zxJDBC DataHandlers

JDBC (javax.sql)
License: Custom, distribute unmodified .jar bundled with your software
Download from java.sun.com ("JDBC 2.0 Optional Package Binary"), or just get the MySql connector below,
as it contains the jdbc2_0-stdext.jar
http://java.sun.com/products/jdbc/download.html#spec

Oracle
License: Custom, export restrictions, allows distribute to customers
Choose JDBC drivers, pick a recent Oracle version, and download classes12.zip for JDK1.2 or 1.3, classes111.zip for JDK1.1, or odjbc.jar for jdk1.4
http://otn.oracle.com/software/tech/java/sqlj_jdbc/content.html

J/Connector for MySql
License: GPL
Selected 3.0, the most recent Production release.
http://www.mysql.com/downloads/api-jdbc-stable.html

PostgreSQL
License: BSD
Get the pg73jdbc1.jar (Want to preserve JDK1.1 compatibility)
http://jdbc.postgresql.org/


Informix
License: Custom, strange - can make "copies to support the level of use authorized" whatever that means
Download the JDBC drivers, extract, run java -jar setup.jar
http://www-3.ibm.com/software/data/informix/tools/jdbc/

LiftOff Installer
License: GPL
Download via CVS
cvs -d:pserver:anonymous@cvs.sourceforge.net:/cvsroot/liftoff login
cvs -z3 -d:pserver:anonymous@cvs.sourceforge.net:/cvsroot/liftoff co liftoff

--------------------
Building jython:
ant dist

# if you ever do ant clean, you'll need to do
# cvs upd -APd 
# to get back the PythonGrammar* classes (clean removes them)
# unless you've installed javacc

--------------------
Jython tests:
# you'll need ~200 MB of RAM + swap for some of these tests
# If using a JRE, need to add a path to a JDK's tools.jar
# set JYTHON_JAVA_ARGS -Dtoolsjar=d:/pipeline/products/jdk/lib/tools.jar
# The javashell test is sensitive to other output from your jython script,
# and to multiple layers of script wrapping.  Best to ensure that
# $JYTHONCVS/jython is the first 'jython' in yuor PATH.
export JYTHONCVS=d:/kb/jython_src
export PATH="$JYTHONCVS:$PATH"
cd $JYTHONCVS/Lib/test/javatests
javac -d .. *.java
cd ..
jythoncvs testall.py 2&>1 | tee testouput

#### before complaining that tests don't pass, make sure that you're running
#### with the CVS version of Jython! (especially if the pow() test fails to throw TypeError)

--------------------
Initialize Postgres on NT notes (skip to just run test): 
export LANG=
export PGDATA=/var/lib/pgsql/data
export PATH="/usr/local/bin:$PATH"
#install cygipc 1.13 (deprecated, but necessary) from
#  http://www.neuro.gatech.edu/users/cwilson/cygutils/cygipc/
ipc-daemon &
   

rm -rf $PGDATA
initdb #(slow)
/bin/postmaster -i -D $PGDATA &
createdb ziclix
--------------------
Initialize MySQL on NT notes (skip to just run test):
net start mysql
mysql/bin/MySqlAdministrator
double-click on MySql, enter connection info, OK
as a SQL query, enter:
create database ziclix
then press the "play" button.  Don't expect any feedback...
(could also just create a ziclix directory under the mysql/data)
--------------------

#################### Run zxjdbc test ####################
cd $JYTHONCVS/Lib/test/zxjdbc
# customize test.xml, setting:
#  - hosts & ports as needed
#  - usernames & passwords as needed
#  - !!! Change org.gjt paths to paths in new MySQL drivers

export LANG=
export PGDATA=/var/lib/pgsql/data
export PATH="/usr/local/bin:$PATH"

ipc-daemon &
/bin/postmaster -i -D $PGDATA &
export CLASSPATH="d:/kb/jython_3rdparty/jdbc2_0-stdext.jar;d:/kb/jython_3rdparty/pg73jdbc1.jar"
jythoncvs runner.py test.xml postgresql 2&>1 | tee ../jdbctestoutput.new


net start mysql
export CLASSPATH="d:/kb/jython_3rdparty/jdbc2_0-stdext.jar;d:/kb/jython_3rdparty/mysql-connector-java-3.0.7-stable/mysql-connector-java-3.0.7-stable-bin.jar"
jythoncvs runner.py test.xml mysql  2&>1 | tee -a ../jdbctestoutput.new
--------------------
Building liftoff

Make sure you get liftoff CVS.
Here's how to build it:

liftoff=d:/kb/jython_3rdparty/liftoff
cd $liftoff/src

CLASSPATH="../lib/ant.jar;../lib/xml.jar" ant -f bootstrap.xml
CLASSPATH="../lib/ant.jar;../lib/xml.jar;../lib/LiftOffAnt.jar" ant

# MODIFY ../data/builder.properties to change the /home/andi/projekte/
# to d:/kb/jython_3rdparty/ (or wherever your liftoff tree lives)

# MODIFY build.xml to copy .gif files in compile-builder
<copydir  src="${src.builder}"
              dest="${build.builder}"
              includes="**/*.gif"/>


CLASSPATH="../lib/ant.jar;../lib/xml.jar;../lib/LiftOffAnt.jar" ant


--------------------
Building Jython's installer

cd $JYTHONCVS
python installer/mklist.py > installer/liftoff.filelist

# UPDATE the product.version in liftoff.props (to something -custom)
# UPDATE the destination.package_prefix (to something appropriate)
# UPDATE PySystemState.version (to the something -custom)

java -Ddatadir=$liftoff/data -cp $liftoff/lib/LiftOff.jar net.sourceforge.liftoff.builder.Main

File|Open  # may have to do it twice - I get NullPointerException the first time
installer/liftoff.props

Create|Class

OK

The mouse doesn't "click" some buttons in the "Builder" GUI (use Enter/Space), and if you
get a NullPointerException trying to do File|Open, just try again. (!?)




ChangeLog from Release_2_1 to head
========================================================================
2003/04/10    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.31
  * com/ziclix/python/sql/Fetch.java:1.14
  * com/ziclix/python/sql/resource/zxJDBCMessages.properties:1.5
    handle AbstractMethodErrors when using an older version driver

  * Lib/test/zxjdbc/test_zxjdbc_dbapi20.py:1.1
  * com/ziclix/python/sql/PyCursor.java:1.32
  * com/ziclix/python/sql/Fetch.java:1.15
  * com/ziclix/python/sql/zxJDBC.java:1.12
  * Lib/test/zxjdbc/zxtest.py:1.19
    fixes to pass compatability tests

2003/04/09    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/JavaDateFactory.java:1.3
  * com/ziclix/python/sql/PyStatement.java:1.4
  * com/ziclix/python/sql/handler/UpdateCountDataHandler.java:1.3
  * com/ziclix/python/sql/handler/OracleDataHandler.java:1.5
  * com/ziclix/python/sql/handler/SQLServerDataHandler.java:1.3
  * com/ziclix/python/sql/handler/MySQLDataHandler.java:1.3
  * com/ziclix/python/sql/handler/InformixDataHandler.java:1.3
  * com/ziclix/python/sql/PyConnection.java:1.12
  * com/ziclix/python/sql/JDBC20DataHandler.java:1.3
  * com/ziclix/python/sql/PyCursor.java:1.30
  * com/ziclix/python/sql/Fetch.java:1.13
  * com/ziclix/python/sql/Procedure.java:1.9
  * com/ziclix/python/sql/FilterDataHandler.java:1.3
  * com/ziclix/python/sql/zxJDBC.java:1.11
  * com/ziclix/python/sql/DataHandler.java:1.9
  * com/ziclix/python/sql/DBApiType.java:1.3
  * com/ziclix/python/sql/PyExtendedCursor.java:1.11
    optimize imports

  * org/python/core/PyObject.java:2.29
    null guard for __findattr__(Py.None)

  * com/ziclix/python/sql/handler/RowIdHandler.java:1.1
  * com/ziclix/python/sql/handler/PostgresqlDataHandler.java:1.3
    flexible rowid fetching

  * Lib/javaos.py:2.18
    added getlogin()

2003/01/03  kevinbutler

  * Lib/test/test_jreload.py:1.1
  * org/python/core/FileUtil.java:2.1
  * org/python/core/SyspathJavaLoader.java:2.6
  * org/python/core/imp.java:2.64
  * Lib/jxxload_help/PathVFSJavaLoader.java:1.2
    [511493] Prevent truncation of large file loads, especially in jreload, but also in other locations that loaded only the available bytes.

2003/01/03    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/DateFactory.java:1.2
  * com/ziclix/python/sql/JavaDateFactory.java:1.2
  * com/ziclix/python/sql/PyCursor.java:1.29
  * com/ziclix/python/sql/DataHandler.java:1.8
    move system datahandler creation to DataHandler; copyrights

  * com/ziclix/python/sql/DateFactory.java:1.1
  * com/ziclix/python/sql/JavaDateFactory.java:1.1
  * com/ziclix/python/sql/zxJDBC.java:1.10
    abstract date creation

2002/12/31    <bzimmer@hyakutake.duncllc.com>

  * Lib/isql.py:1.5
    allow escaping of commands

  * Lib/dbexts.py:1.11
    allow dynamic cursors in begin()

2002/12/24    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.28
    fixed warnings bug #609505; tabs->spaces

  * org/python/core/PyLong.java:2.17
  * org/python/core/PyInteger.java:2.19
  * org/python/modules/operator.java:2.10
    feature complete operator module

2002/12/19  kevinbutler

  * Lib/popen2.py:2.5
    [ 573791 ] close stdin file for 'system' function

  * Lib/javaos.py:2.17
    Remove unnecessary from __future__ import division, for Jython 2.1 compatibility

2002/11/27    <bzimmer@hyakutake.duncllc.com>

  * Lib/dbexts.py:1.10
    changed empty results from None to []

2002/11/06  bckfnn

  * Lib/javashell.py:2.3
    Removed unneeded import of 'threading'.

  * org/python/modules/math.java:2.5
    Get the right OverflowError for longs by doing the float conversion
directly. Should probably be done for all the methods.

  * org/python/core/PyString.java:2.60
    Fix a SIOOB exception when specifing a large 'end' value.

  * Lib/popen2.py:2.4
    Delay import of 'threading' until actually needed. 'threading' imports
atexit and atexit has sideeffects and since popen2 is imported
by site.py (by 'os') the sideeffects could not be controlled by
user scripts.

  * org/python/core/PyJavaClass.java:2.41
    Apply patch "[ 632983 ] Simplification in PyJavaClass.java".

  * org/python/core/PySystemState.java:2.79
    Added maxunicode field.

  * org/python/core/PySystemState.java:2.78
  * org/python/core/PyInstance.java:2.32
  * org/python/core/ThreadState.java:2.6
    Add recursion limit to instance __call__.

2002/11/05  bckfnn

  * org/python/core/PyInteger.java:2.17
    int-long unification. Handle overflows by returning a PyLong.

  * org/python/core/PyInteger.java:2.18
    Turn OverflowWarnings from the warnings module into OverflowErrors.

  * org/python/modules/cPickle.java:1.22
    Generate memo-ids that matches CPython's cPickle.

  * Lib/popen2.py:2.3
    Fix missing definition of bufsize.

  * org/python/core/__builtin__.java:2.55
    A temporary hack to define the 'file' builtin.

  * org/python/core/PyXRange.java:2.5
    - Added deprication warnings.
- Justify the stop value.

  * org/python/core/PyString.java:2.59
    - Add optional seperator argument to [lr]strip.
- Fix a SIOOB java exception i zfill.

2002/11/01  bckfnn

  * org/python/core/PySequenceIter.java:2.3
  * org/python/core/__builtin__.java:2.54
  * org/python/core/PyFile.java:2.28
  * org/python/core/Py.java:2.68
  * org/python/core/PyDictionary.java:2.21
  * org/python/modules/operator.java:2.9
    Missing iterator support.

  * org/python/core/__builtin__.java:2.53
    Fix the exception message, the count was wrong.

2002/10/31  bckfnn

  * org/python/core/PyString.java:2.58
    Support generators as argument to join by removing an old check for
sequences.

  * org/python/core/PyLong.java:2.16
  * org/python/core/PyInteger.java:2.16
    Pass the test_pow testcase by adding support for negative modulo.

  * org/python/core/PyFile.java:2.27
    Fix for "[ 631430 ] read(-1) uses wrong fileposition.".

2002/10/30  bckfnn

  * Lib/inspect.py:1.1
    Initial revision of inspect.

  * org/python/core/PyString.java:2.57
    Fix for "[ 631035 ] Negative repeat cause java exception.".

  * Tools/jythonc/SrcGenCompiler.py:2.3
  * org/python/compiler/CodeCompiler.java:2.31
    Fix for "[ 631017 ] Private fields mismangled".

2002/10/29    <bzimmer@hyakutake.duncllc.com>

  * Lib/dbexts.py:1.9
    add exception subclasses to instance; don't silently ignore class import problems

2002/10/29  bckfnn

  * org/python/core/__builtin__.java:2.52
    Fix for "[ 608628 ] long(java.math.BigInteger) does not work"

  * org/python/modules/struct.java:2.7
    Finn Bock's struct module
Patch [ 577728 ] struct.java now accepts 64bits ints
The patch is a step in the right direction, but it does not implement
the funtionallity correctly. For example, the test_struct still fails
for some values of long.

  * org/python/core/PyJavaClass.java:2.40
    Apply patch "[ 612223 ] allows Java setters that return not void".
If this patch cause any kind of problem, I'll likely take it out again.

  * Tools/jythonc/PythonModule.py:2.21
    Apply patch "[ 583040 ] java.lang.System in jythonc".

  * org/python/util/ReadlineConsole.java:1.7
    Apply patch "[ 502151 ] Extend ReadlineConsole to cmd.Cmd".
Uses newJavaFunc instead of defining a separate PyObject subclass.

  * org/python/util/InteractiveInterpreter.java:2.9
    Apply "[ 630057 ] use py.printException() [fixes 625364]".

  * Lib/socket.py:1.14
    Apply "[ 628326 ] Dump socket.getaddrinfo and sendall impl".

  * org/python/core/PySystemState.java:2.77
    Add en easy way of disabling package scan.
The idea comes from patch "[ 525092 ] disable package scan in registry".

2002/10/15  bckfnn

  * org/python/core/PyGenerator.java:2.1
  * org/python/core/PyTableCode.java:2.20
  * org/python/core/__builtin__.java:2.51
  * org/python/core/parser.java:2.17
  * org/python/core/PyFrame.java:2.14
  * org/python/core/CompilerFlags.java:2.8
  * org/python/modules/types.java:2.9
  * org/python/compiler/ScopesCompiler.java:2.9
  * org/python/compiler/Code.java:2.7
  * org/python/compiler/JavaMaker.java:2.16
  * org/python/compiler/ScopeInfo.java:2.8
  * org/python/compiler/ProxyMaker.java:2.17
  * org/python/compiler/Future.java:2.9
  * org/python/compiler/Module.java:2.13
  * org/python/compiler/CodeCompiler.java:2.30
    Support for generators.

  * org/python/parser/python.jjt:2.21
    Support for generators and __future__ enabling of the yield keyword.

  * org/python/core/PySequence.java:2.20
    Deal with outofbound start values when using negative step.
This fixes expressions like "[][::-1]".

  * org/python/parser/PythonGrammarTreeConstants.java:2.11
  * org/python/parser/PythonGrammarTokenManager.java:2.18
  * org/python/parser/PythonGrammar.java:2.19
    Commit generated code.

  * org/python/compiler/ScopesCompiler.java:2.8
    Cleanup some unused mode code.

  * org/python/core/PyInteger.java:2.15
    Better CPython match for the exception text.

2002/10/13  pedronis

  * org/python/util/jython.java:2.26
    Force exit even when there are non-deamon running threads (e.g. AWT) when quitting
interactive mode.

2002/10/10    <bzimmer@hyakutake.duncllc.com>

  * Misc/make_errno.py:1.2
  * org/python/modules/errno.java:2.2
    run on linux to get the full complement of errors; EAGAIN and EWOULDBLOCK both appear, just like CPython

2002/10/09  bckfnn

  * org/python/modules/Setup.java:2.21
    Taken from PySystemState.java and made analogous to CPython's
Modules/Setup.
Added the 'errno' module.

  * Misc/make_errno.py:1.1
    Initial revision.

  * org/python/compiler/CodeCompiler.java:2.29
    Fix for [ 577395 ] Outer finally not executed at return.

  * org/python/modules/errno.java:2.1
    Added the 'errno' module.

2002/10/07    <bzimmer@hyakutake.duncllc.com>

  * Lib/dbexts.py:1.8
    allow multiple datahandlers in config

  * Lib/test/zxjdbc/zxtest.py:1.18
    allow custom datahandler for date/time tests

2002/09/19  bckfnn

  * org/python/core/__builtin__.java:2.50
    Support tuple argument to isinstance.

2002/09/18  bckfnn

  * org/python/core/PyException.java:2.8
    If the type is a tuple, unpack the first element recusively.
Fix for test367, also found in anydbm.

  * Lib/javapath.py:1.10
    Fix for [ 562943 ] os.path.getmtime misbehaves on nonfile.

  * org/python/util/PythonObjectInputStream.java:1.9
    Fix for [ 529242 ] Python singletons deserialization bug
Replace None, Ellipsis and NotImplemented with the singleton instances.

2002/09/17  bckfnn

  * org/python/core/PyList.java:2.27
    Swithed to the stable and far faster merge sort.

  * org/python/core/MergeState.java:1.1
    Initial version of Tim Peters stable sort algorithm.

  * org/python/util/JythoncAntTask.java:1.1
    Initial version of Cyrille Morvan's ant task.
http://nagoya.apache.org/bugzilla/show_bug.cgi?id=8080
The class and package name has been renamed.

  * org/python/parser/TreeBuilder.java:2.3
    Fix an exception when presented with an empty list: []

  * org/python/core/__builtin__.java:2.49
    Added True and False.

2002/08/29  bckfnn

  * org/python/util/ReadlineConsole.java:1.6
    Fix for "[ 572769 ] Blank input lines break readline console"

2002/07/15  kevinbutler

  * Lib/javashell.py:2.2
  * Lib/popen2.py:2.2
    Convert line-endings

2002/07/10  kevinbutler

  * Lib/javashell.py:2.1
  * Lib/popen2.py:2.1
  * Lib/javaos.py:2.16
    Add popen* support, move shell environment functionality into javashell.py to clean up javaos.py

2002/06/11  bckfnn

  * org/python/core/PyCallIter.java:2.2
  * org/python/core/PyIterator.java:2.1
  * org/python/core/PySequenceIter.java:2.2
  * org/python/core/PyList.java:2.26
  * org/python/core/PyDictionary.java:2.20
  * org/python/core/PyStringMap.java:2.15
    Use the new PyIterator helper class when implementing iterators.

  * org/python/parser/PythonGrammarTreeConstants.java:2.10
  * org/python/parser/PythonGrammarTokenManager.java:2.17
  * org/python/parser/PythonGrammar.java:2.18
    Commiting generated files.

  * org/python/parser/TreeBuilder.java:2.2
  * org/python/parser/ast/Interactive.java:1.2
  * org/python/parser/Visitor.java:2.8
  * org/python/compiler/Future.java:2.8
  * org/python/compiler/CodeCompiler.java:2.28
    Allow multiple statements in a Interactive node (aka single_input).

  * org/python/core/parser.java:2.16
  * org/python/core/CompilerFlags.java:2.7
  * org/python/parser/python.jjt:2.20
  * org/python/util/InteractiveInterpreter.java:2.8
    Allow multiple empty lines in the source when calling
compile(..., "<str>", "single") while maintaining that the interactive
prompt stops when an empty line is entered.
This done by adding a boolean 'interactive' flags to CompilerFlags and to
the JavaCC tokensource.

2002/05/31  bckfnn

  * Lib/socket.py:1.13
    Fix for "[ 544891 ] problems with socket.py".
Patch applied directly.

  * org/python/modules/sre/SRE_STATE.java:1.10
    Fix a bug in BIGCHARSET where the block list must be indexed as an
array of unsigned bytes. Bug exposed by test363.

  * org/python/core/PySystemState.java:2.76
    Added 'byteorder' attribute.

2002/05/30  bckfnn

  * Tools/jythonc/SrcGenCompiler.py:2.2
  * org/python/core/Py.java:2.67
  * org/python/core/imp.java:2.63
  * org/python/compiler/CodeCompiler.java:2.27
  * com/ziclix/python/sql/pipe/Pipe.java:1.3
    Renamed 'assert' to 'assert_' because it is a 1.4 reserved word.

  * Tools/jythonc/ImportName.py:2.7
    Removed reference to SimpleCompiler.

  * Tools/jythonc/SrcGenCompiler.py:2.1
  * org/python/parser/TreeBuilder.java:2.1
  * org/python/core/parser.java:2.15
  * org/python/core/Py.java:2.66
  * org/python/core/imp.java:2.62
  * org/python/core/CompilerFlags.java:2.6
  * org/python/parser/SimpleNode.java:2.19
  * org/python/parser/ParseException.java:1.4
  * org/python/parser/python.jjt:2.19
  * org/python/parser/Visitor.java:2.7
  * org/python/parser/JJTPythonGrammarState.java:2.3
  * org/python/parser/Node.java:2.2
  * org/python/modules/codeop.java:2.10
  * Tools/jythonc/Object.py:2.7
  * Tools/jythonc/PythonModule.py:2.20
  * Tools/jythonc/ObjectFactory.py:2.14
  * Tools/jythonc/compile.py:2.22
  * org/python/compiler/ScopesCompiler.java:2.7
  * org/python/compiler/SymInfo.java:2.3
  * org/python/compiler/ArgListCompiler.java:2.9
  * org/python/compiler/ScopeInfo.java:2.7
  * org/python/compiler/CompilationContext.java:2.4
  * org/python/compiler/Future.java:2.7
  * org/python/compiler/Module.java:2.12
  * org/python/compiler/CodeCompiler.java:2.26
  * Tools/jythonc/jast/Statement.py:2.7
    Implementation of the new AST tree.

  * org/python/modules/sre/SRE_STATE.java:1.9
    Fix for "[ 545235 ] unexpected match with re"

  * org/python/core/PyDictionary.java:2.19
  * org/python/core/PyStringMap.java:2.14
    Fix for "[ 532747 ] for i in iter(d)".
Adding a __iter__ method to the iterators.

  * org/python/parser/SimpleNode.java:2.20
    Don't use jdk1.4 features!

  * org/python/parser/PythonGrammarTreeConstants.java:2.9
  * org/python/parser/PythonGrammarTokenManager.java:2.16
  * org/python/parser/PythonGrammar.java:2.17
  * org/python/parser/PythonGrammarConstants.java:2.9
    Committing generated JavaCC files.

  * org/python/parser/ast/Assert.java:1.1
  * org/python/parser/ast/Assign.java:1.1
  * org/python/parser/ast/Attribute.java:1.1
  * org/python/parser/ast/AugAssign.java:1.1
  * org/python/parser/ast/BinOp.java:1.1
  * org/python/parser/ast/BoolOp.java:1.1
  * org/python/parser/ast/Break.java:1.1
  * org/python/parser/ast/Call.java:1.1
  * org/python/parser/ast/ClassDef.java:1.1
  * org/python/parser/ast/Compare.java:1.1
  * org/python/parser/ast/Continue.java:1.1
  * org/python/parser/ast/Delete.java:1.1
  * org/python/parser/ast/Dict.java:1.1
  * org/python/parser/ast/Ellipsis.java:1.1
  * org/python/parser/ast/Exec.java:1.1
  * org/python/parser/ast/Expr.java:1.1
  * org/python/parser/ast/Expression.java:1.1
  * org/python/parser/ast/ExtSlice.java:1.1
  * org/python/parser/ast/For.java:1.1
  * org/python/parser/ast/FunctionDef.java:1.1
  * org/python/parser/ast/Global.java:1.1
  * org/python/parser/ast/If.java:1.1
  * org/python/parser/ast/Import.java:1.1
  * org/python/parser/ast/ImportFrom.java:1.1
  * org/python/parser/ast/Index.java:1.1
  * org/python/parser/ast/Interactive.java:1.1
  * org/python/parser/ast/Lambda.java:1.1
  * org/python/parser/ast/List.java:1.1
  * org/python/parser/ast/ListComp.java:1.1
  * org/python/parser/ast/Module.java:1.1
  * org/python/parser/ast/Name.java:1.1
  * org/python/parser/ast/Num.java:1.1
  * org/python/parser/ast/Pass.java:1.1
  * org/python/parser/ast/Print.java:1.1
  * org/python/parser/ast/Raise.java:1.1
  * org/python/parser/ast/Repr.java:1.1
  * org/python/parser/ast/Return.java:1.1
  * org/python/parser/ast/Slice.java:1.1
  * org/python/parser/ast/Str.java:1.1
  * org/python/parser/ast/Subscript.java:1.1
  * org/python/parser/ast/Suite.java:1.1
  * org/python/parser/ast/TryExcept.java:1.1
  * org/python/parser/ast/TryFinally.java:1.1
  * org/python/parser/ast/Tuple.java:1.1
  * org/python/parser/ast/UnaryOp.java:1.1
  * org/python/parser/ast/VisitorBase.java:1.1
  * org/python/parser/ast/VisitorIF.java:1.1
  * org/python/parser/ast/While.java:1.1
  * org/python/parser/ast/Yield.java:1.1
  * org/python/parser/ast/aliasType.java:1.1
  * org/python/parser/ast/argumentsType.java:1.1
  * org/python/parser/ast/boolopType.java:1.1
  * org/python/parser/ast/cmpopType.java:1.1
  * org/python/parser/ast/excepthandlerType.java:1.1
  * org/python/parser/ast/exprType.java:1.1
  * org/python/parser/ast/expr_contextType.java:1.1
  * org/python/parser/ast/keywordType.java:1.1
  * org/python/parser/ast/listcompType.java:1.1
  * org/python/parser/ast/modType.java:1.1
  * org/python/parser/ast/operatorType.java:1.1
  * org/python/parser/ast/sliceType.java:1.1
  * org/python/parser/ast/stmtType.java:1.1
  * org/python/parser/ast/unaryopType.java:1.1
    AST nodes generated from python.asdl. Python.asdl is currently located
in CPython's nondist/sandbox/ast.

2002/05/27  bckfnn

  * org/python/core/__builtin__.java:2.48
    Fix copy/paste error in recent zip iteration code.

2002/05/26  bckfnn

  * org/python/core/__builtin__.java:2.46
    Throw TypeError when abs() argument isn't a number. I wish I had a better
solution.

  * org/python/core/PyArray.java:2.9
    Support negative step slices for java array.

  * org/python/core/PyFloat.java:2.12
    Restrict 3 arg pow to integers (new in 2.2).

  * org/python/core/PyNone.java:2.8
    Make None a non-number.

  * org/python/core/__builtin__.java:2.45
    Made zip() use the __iter__ protocol.

  * org/python/core/PyString.java:2.55
    Fix several bugs in string->complex conversion.
Fixes patch "[ 511321 ] Jython complex from string".

  * org/python/core/PyFunction.java:2.15
    Disallow None as function __dict__ value (new in 2.2).

  * org/python/core/PyFile.java:2.26
    Check for valid open mode flags.

  * org/python/core/__builtin__.java:2.44
    Added restrictions on complex() arguments.

  * org/python/core/Py.java:2.65
    Added newLong(BigInteger), needed by the int/long unification.

  * org/python/core/__builtin__.java:2.47
    Added a dummy object() builtin, just to make pickle.py happy.

  * org/python/modules/types.java:2.8
    Added StringTypes.

  * org/python/core/PyString.java:2.56
    Added .decode() methods.

2002/05/16    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/Fetch.java:1.12
    added accessor for description

2002/05/10    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/Fetch.java:1.11
    fixed indexing out of output parameters

  * com/ziclix/python/sql/procedure/SQLServerProcedure.java:1.1
  * com/ziclix/python/sql/handler/SQLServerDataHandler.java:1.2
  * com/ziclix/python/sql/PyConnection.java:1.11
  * com/ziclix/python/sql/PyCursor.java:1.27
  * com/ziclix/python/sql/Procedure.java:1.8
  * com/ziclix/python/sql/DataHandler.java:1.7
  * Lib/test/zxjdbc/test.xml:1.9
  * Lib/test/zxjdbc/dbextstest.py:1.6
  * Lib/test/zxjdbc/zxtest.py:1.17
  * Lib/test/zxjdbc/sptest.py:1.5
    extensible stored procedures

2002/05/09    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyStatement.java:1.3
  * com/ziclix/python/sql/PyCursor.java:1.26
  * com/ziclix/python/sql/Fetch.java:1.10
    api cleanup

  * com/ziclix/python/sql/WarningEvent.java:1.1
  * com/ziclix/python/sql/WarningListener.java:1.1
  * com/ziclix/python/sql/PyConnection.java:1.10
  * com/ziclix/python/sql/PyCursor.java:1.25
  * com/ziclix/python/sql/Fetch.java:1.9
  * Lib/test/zxjdbc/test.xml:1.8
  * Lib/test/zxjdbc/zxtest.py:1.16
  * Lib/test/zxjdbc/runner.py:1.4
  * Lib/test/zxjdbc/sptest.py:1.4
    added public Java API for Fetch; fetch[many|all] return empty list on completion

2002/04/21    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyStatement.java:1.2
  * Lib/dbexts.py:1.7
  * com/ziclix/python/sql/PyConnection.java:1.9
  * com/ziclix/python/sql/PyCursor.java:1.24
  * Lib/test/zxjdbc/test.xml:1.7
  * Lib/test/zxjdbc/dbextstest.py:1.5
    prepared statements can live outside the cursor

2002/04/19    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.23
  * com/ziclix/python/sql/zxJDBC.java:1.9
  * Lib/test/zxjdbc/dbextstest.py:1.4
  * Lib/test/zxjdbc/zxtest.py:1.15
    cleanup dynamic statements

  * com/ziclix/python/sql/PyStatement.java:1.1
  * Lib/dbexts.py:1.6
  * com/ziclix/python/sql/PyConnection.java:1.8
  * com/ziclix/python/sql/PyCursor.java:1.22
  * Lib/test/zxjdbc/zxtest.py:1.14
  * Lib/test/zxjdbc/runner.py:1.3
    added .prepare() to cursor

2002/04/12    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.21
  * com/ziclix/python/sql/Procedure.java:1.7
    added rstype and rsconcur for prepared statements

  * com/ziclix/python/sql/PyConnection.java:1.7
  * com/ziclix/python/sql/PyCursor.java:1.20
  * com/ziclix/python/sql/Fetch.java:1.8
  * com/ziclix/python/sql/zxJDBC.java:1.8
  * com/ziclix/python/sql/PyExtendedCursor.java:1.10
  * Lib/test/zxjdbc/test.xml:1.6
  * Lib/test/zxjdbc/zxtest.py:1.13
    added result set type and concurrency

  * Lib/test/zxjdbc/runner.py:1.2
    added option to run a single test

2002/03/29    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.19
  * Lib/test/zxjdbc/zxtest.py:1.12
  * com/ziclix/python/sql/pipe/db/DBSource.java:1.2
  * com/ziclix/python/sql/pipe/db/DBSink.java:1.3
    cursor is now file-like

2002/03/26    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyConnection.java:1.6
  * com/ziclix/python/sql/PyCursor.java:1.18
  * Lib/test/zxjdbc/zxtest.py:1.11
    close open cursors when the connection closes

  * com/ziclix/python/sql/connect/Connectx.java:1.3
    check for pooled datasources first

2002/03/11  bckfnn

  * org/python/modules/struct.java:2.6
    Fix for "[ 522828 ] struct.pack('>NNs', v) fails for NN > 20"

  * org/python/core/__builtin__.java:2.43
    Fix for "[ 522558 ] list() is broken"

  * org/python/modules/cStringIO.java:1.14
    Fix for "[ 522423 ] cStringIO has no reset() method"

  * org/python/core/PyLong.java:2.15
    Fix for "[ 517237 ] Binary ops with int and long fail".

2002/01/26    <bzimmer@hyakutake.duncllc.com>

  * Lib/isql.py:1.4
    quit from within the interpreter no longer dumps a stacktrace; use connects with the same dbexts class

2002/01/14    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.17
  * com/ziclix/python/sql/Fetch.java:1.7
  * Lib/test/zxjdbc/zxtest.py:1.10
  * com/ziclix/python/sql/connect/Connect.java:1.3
    added .scroll() and .rownumber

2002/01/13  bckfnn

  * org/python/core/Options.java:2.12
    Two new options: Qnew and divisionWarning. In support for pep-238.

  * org/python/util/jython.java:2.25
    Support the -Q option (pep-238).

  * org/python/parser/SimpleNode.java:2.18
  * org/python/parser/Visitor.java:2.6
    Support for the floor division operator (pep-238).

  * org/python/core/CompilerFlags.java:2.5
    Support for future division (pep-238).

  * org/python/compiler/ScopesCompiler.java:2.6
  * org/python/compiler/Future.java:2.6
  * org/python/compiler/Module.java:2.11
  * org/python/compiler/CodeCompiler.java:2.25
    Support for floor division and future division.
This patch also moves the Future instance from the CodeCompiler to the
module where I believe it should be defined. I'm sorry if that happens
to break something for nested_scope, but nested_scope should not need
future support anymore.

  * Tools/jythonc/PythonModule.py:2.19
    Moved the Future instance to the module (part of pep-238 changes).

  * org/python/parser/PythonGrammarTreeConstants.java:2.8
  * org/python/parser/PythonGrammarTokenManager.java:2.15
  * org/python/parser/PythonGrammar.java:2.16
  * org/python/parser/PythonGrammarConstants.java:2.8
    Generated files (floor division operator).

  * Lib/javaos.py:2.15
    Avoid a warnall warning.

  * org/python/parser/python.jjt:2.18
    Added floor division operator.

  * org/python/core/PyComplex.java:2.10
  * org/python/core/PyObject.java:2.28
  * org/python/core/PyInstance.java:2.31
  * org/python/core/PyFloat.java:2.11
  * org/python/core/PyLong.java:2.14
  * org/python/core/imp.java:2.61
  * org/python/core/PyInteger.java:2.14
    Added __truediv__ and __floordiv__ methods.
Emit warnings when classic __div__ is called.

  * Misc/make_binops.py:1.5
    Added __floordiv__ and __truediv__ (pep-238).

  * org/python/core/PyTableCode.java:2.19
    Added CO_FUTUREDIVISION. Value must match the value in __future__.py.

2002/01/11    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/zxJDBC.java:1.5
    print the java stacktrace if requested

  * com/ziclix/python/sql/PyConnection.java:1.5
  * com/ziclix/python/sql/PyCursor.java:1.16
  * com/ziclix/python/sql/Fetch.java:1.6
  * com/ziclix/python/sql/zxJDBC.java:1.7
  * com/ziclix/python/sql/DataHandler.java:1.6
  * com/ziclix/python/sql/PyExtendedCursor.java:1.9
  * com/ziclix/python/sql/connect/Connect.java:1.2
  * com/ziclix/python/sql/connect/Lookup.java:1.2
  * com/ziclix/python/sql/connect/Connectx.java:1.2
  * com/ziclix/python/sql/pipe/db/BaseDB.java:1.2
  * com/ziclix/python/sql/pipe/db/DBSink.java:1.2
  * com/ziclix/python/sql/pipe/Pipe.java:1.2
    enable the throwing of all exceptions

  * com/ziclix/python/sql/zxJDBC.java:1.6
    cleaner exception handling

2002/01/11  bckfnn

  * org/python/core/Options.java:2.11
    Fix a copy&paste bug in default value for caseok.
2.1.1 bugfix candidate.

2002/01/10  kevinbutler

  * Lib/javaos.py:2.14
    added OS/2 support (from Ype Kingma)

2002/01/10    <bzimmer@hyakutake.duncllc.com>

  * Lib/test/zxjdbc/dbextstest.py:1.3
  * Lib/test/zxjdbc/zxtest.py:1.9
    more robust testing

  * com/ziclix/python/sql/PyCursor.java:1.15
  * com/ziclix/python/sql/Fetch.java:1.5
  * com/ziclix/python/sql/Procedure.java:1.6
  * com/ziclix/python/sql/PyExtendedCursor.java:1.8
    documentation and some cleanup

2002/01/09    <bzimmer@hyakutake.duncllc.com>

  * Lib/isql.py:1.3
    added __tojava__() to raw_input = 0

2002/01/08    <bzimmer@hyakutake.duncllc.com>

  * com/ziclix/python/sql/PyCursor.java:1.14
  * Lib/test/zxjdbc/zxtest.py:1.8
    implemented iteration protocol

  * org/python/core/CollectionIter2.java:1.2
    dictionaries return the keys in an iteration, not the (key,value) mapping

2002/01/07    <bzimmer@hyakutake.duncllc.com>

  * Lib/test/zxjdbc/zxtest.py:1.6
    use the cursor's datahandler

  * com/ziclix/python/sql/PyCursor.java:1.13
  * Lib/test/zxjdbc/zxtest.py:1.7
    updatecount is None if stmt.getUpdateCount() < 0

  * Lib/dbexts.py:1.5
    removed dependence on string mod

  * com/ziclix/python/sql/JDBC20DataHandler.java:1.2
    delegate to DataHandler if getBigDecimal() fails

  * com/ziclix/python/sql/Procedure.java:1.5
    close the statement IF NOT null

  * Lib/isql.py:1.2
    added update count status

2002/01/07  bckfnn

  * org/python/core/CollectionIter.java:1.1
  * org/python/core/CollectionIter2.java:1.1
  * org/python/core/PyInstance.java:2.30
    Support iteration over java instances.

  * Lib/string.py:1.9
    Define the new 2.2 names.
This closes patch: "[ #500267 ] add missing attributes to string.py".

  * Lib/javaos.py:2.13
    Define "extsep".

2002/01/06  bckfnn

  * org/python/core/PyCallIter.java:2.1
  * org/python/core/PySequenceIter.java:2.1
  * org/python/core/__builtin__.java:2.42
  * org/python/core/PyString.java:2.54
  * org/python/core/PyObject.java:2.27
  * org/python/core/PyInstance.java:2.29
  * org/python/core/PySequence.java:2.19
  * org/python/core/PyArray.java:2.8
  * org/python/core/Py.java:2.64
  * org/python/core/PyDictionary.java:2.18
  * org/python/core/codecs.java:2.12
  * org/python/core/imp.java:2.60
  * org/python/core/PyStringMap.java:2.13
  * org/python/core/exceptions.java:1.12
  * org/python/core/PyClass.java:2.29
  * org/python/parser/PythonGrammarTreeConstants.java:2.7
  * org/python/parser/JJTPythonGrammarState.java:2.2
  * org/python/modules/cStringIO.java:1.13
  * org/python/modules/xreadlines.java:1.2
  * org/python/compiler/CodeCompiler.java:2.24
  * org/python/modules/sre/PatternObject.java:1.9
    Support for the __iter__ protocol. With this change, all loops over
sequences will use the __iter__() method to get a iterator object and all
builtin sequence objects will ofcourse define the __iter__() method.

  * org/python/core/PySystemState.java:2.75
    Post 2.1 version number.

  * org/python/modules/_sre.java:1.7
  * org/python/modules/sre/SRE_STATE.java:1.8
  * org/python/modules/sre/PatternObject.java:1.8
  * org/python/modules/sre/ScannerObject.java:1.4
    CPython-2.2's sre changes to MAGIC==20010701.

  * org/python/core/Py.java:2.63
  * org/python/core/exceptions.java:1.11
    Added 2.2 exceptions.



