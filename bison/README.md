# Setup Eclipse Workspace
- set JDK 21 as default
- import existing eclipse project
- switch Text File Encoding to UTF-8
- run the `antlr-gen` (Ant) launch configuration
- [need to be adjusted after installing a new eclipse version:] 
     Java -> Compiler -> Errors/Warnings -> Incomplete 'switch' cases on enum: Warning (instead of Error)
- [overridden by Oomph settings in a bison eclipse:] Java -> Code Style -> Formatter: import `bison/formatting/Jython-like.xml`
- [overridden by Oomph settings in a bison eclipse:] Java -> Editor -> Save Actions: format edited lines


# Hint how to run unit tests from within Eclipse
- add `exposed-2.7.x.jar` topmost to the run configuration of those unit tests which need initialization
- at the moment this is the full `jython-2.7.x.jar`

# Differences to upstream
- `jakarta` migration
- improvement of `string.py` for `java.lang.String` arguments
- consistent calling/overwriting of protected final superclass methods
- build with JDK 21 - source and target compatibility are `JavaVersion.VERSION_21`
- artifact name is `jython` (instead of `jython.slim`)
- add an automatic module name `org.python.jython.bison`
- newer gradle version
- newer external libraries
- remove jline import from Py.java (to allow jline being excluded from ear)
- replace `AccessControlException`
- remove `getSecurityManager()`
- PyString now can contain 8 bit Unicode characters (in both conversion directions)
- fix (or mute) some regression tests
- prevent System.exit(n) and sys.exit() from being called (the latter only in embedded mode)

# Running regrtest
- run the following commands in a shell with JDK 21
- `ant clean`
- `ant`
- `./dist/bin/jython -m test.regrtest -e`

## Running a single regrtest
To execute - for example - `test_string.py`, the command line is as follows:
- `./dist/bin/jython -m test.test_string`

## `master` on JDK 21 Results
```
383 tests OK.
6 tests skipped:
    test_codecmaps_hk test_curses test_smtpnet test_subprocess
    test_urllib2net test_urllibnet
```

## `2.7.bison` Tip Results
See `bison/regrtest.log`

# Artifactory publishing of a SNAPSHOT (Note: maven snapshot publishing currently not perfect)
- make sure that `-Xlint:unchecked` only spits out warnings in `PythonParser.java`
- `./gradlew clean publish`
- copy `build2/stagingRepo/org/python/jython/2.7.x/jython-2.7.x-yyymmdd.hhmmss-1.pom` to `build2/stagingRepo/org/python/jython/2.7.x/jython-2.7.x-SNAPSHOT.pom`
- rename `/build2/libs/jython-2.7.x.jar` to `/build2/libs/jython-2.7.x-SNAPSHOT.jar`
- rename `/build2/libs/jython-2.7.x-sources.jar` to `/build2/libs/jython-2.7.x-SNAPSHOT-sources.jar`
- rename `/build2/libs/jython-2.7.x-SNAPSHOT-javadoc.jar` to `/build2/libs/jython-2.7.x-SNAPSHOT-javadoc.jar`
- deploy `jython-2.7.x-SNAPSHOT.pom`
- deploy `/build2/libs/jython-2.7.x-SNAPSHOT.jar` (adjust the `Group ID` to `org.python`)
- deploy `/build2/libs/jython-2.7.x-SNAPSHOT-sources.jar` (adjust the `Group ID` to `org.python`, set `Classifier` to `sources`)
- deploy `/build2/libs/jython-2.7.x-SNAPSHOT-javadoc.jar` (adjust the `Group ID` to `org.python`, set `Classifier` to `javadoc`)

# Artifactory publishing of a final version
- make sure that `-Xlint:unchecked` only spits out warnings in `PythonParser.java`
- make sure your local copy is clean (`git status` should display `nothing to commit, working tree clean`)
- `git tag -a v2.7.x -m "Jython version 2.7.x"`
- `git push origin v2.7.x`
- `./gradlew clean publish` (this creates all files correctly named in `build2/stagingRepo/org/python/jython/2.7.x`)
- deploy `jython-2.7.x.pom`
- deploy `jython-2.7.x.jar` (adjust the `Group ID` to `org.python`)
- deploy `jython-2.7.x-sources.jar` (adjust the `Group ID` to `org.python`, set `Classifier` to `sources`)
- deploy `jython-2.7.x-javadoc.jar` (adjust the `Group ID` to `org.python`, set `Classifier` to `javadoc`)
- prepare for the next version by increasing the patch version in `build.gradle`

# TODO
- rewrite the `build.gradle` for running with Gradle 9.x
- replace `finalize()`
