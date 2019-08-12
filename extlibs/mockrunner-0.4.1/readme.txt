This contains the minimal set of jars from mockrunner-0.4.1 to run the modjy
tests against j2ee1.3 with jdk1.5.

These are run from the main Jython directory as part of "ant test",
or from tests/modjy with "ant". In the latter case, JYTHON_HOME must
be set to the project/dist folder and MOCKRUNNER_HOME to this folder.

xml-apis-*.jar is added to mockrunner because it is needed for these tests,
but not for Jython generally.
