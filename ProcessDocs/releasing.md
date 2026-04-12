# How To Release Jython

These are the steps needed to make a public release of Jython.
In the case of a public release,
you can only do this once the hard work of development and debugging is done,
and there is a consensus in the project that adequate quality has been achieved.

These notes will also be a useful guide if you intend to make a snapshot (private) release.
The process may be tested, without making a real release,
for example to check that the scripts still work:
go through the steps but without pushing upstream or publishing.

Delete the workspace soon afterwards,
so as not to leave change sets tagged as a release,
that might be accidentally built upon or pushed at a later time.


## Before you start

### Is Jython Ready?

These things are better dealt with in the development environment,
by issues and PRs,
rather than during the release process:

* Are the JARs we reference or embed sufficiently up to date?
  The security vunverabilities (CVEs) reported against Jython come
  almost entirely from the JARs we cite or embed.
* Is the Unicode reference data sufficiently up to date?
  See `~/Misc/makeucnhashdat.py` and `unchash.dat`.
* `src/org/python/core/imp.java`: If there has been any compiler change,
  increment the magic number `APIVersion`.
  This magic declares old compiled files incompatible, forcing a fresh compilation for users.
  (Maybe do it anyway, if it's been a long time.)

Consider *not* doing some of these actions until a release candidate is imminent.
The way we manage dependencies means the repository history contains
a copy of every version of every JAR we ever used.
This is a lot of space.
`unchash.dat` is also large.
Refreshing a dependency will seldom impact our own code materially,
but if it will, it is fine to de-risk the change earlier.


### Local Necessities

To complete a public release you need the following things:

* Two JDBC driver JARs that we do not track with version control (licensing restrictions):

  * Informix (currently `jdbc-4.50.11.jar` for Java 8).
  * Oracle (currently `ojdbc8-23.4.0.24.05.jar` for Java 8).
* Commit rights to the Jython repository (to push the tagged version).
* The right to publish Jython at [Sonatype](https://oss.sonatype.org).
* A PGP signing key pair (generated with `gpg --gen-key`).
* Access to the channels where we announce releases (e.g. Twitter).

You can dry-run this process with only the first pre-requisite (driver JARs),
and a Git clone of the official repository.
In that case, be careful not to push any changes.

> [!TIP]
> If you clone from `https://github.com/jython/jython.git`,
> that will prevent an unintended push.)


## Making a Releasable Jython

### Start in the Right Place

`cd` to a directory where you have permission to make subdirectories.
The path to this directory can get embedded in published files,
so aim for:

* short (i.e. near a file system root), in the examples `D:\git`.
* impersonal (not containing company or personal names).
* represenable in ASCII (even though Jython is pretty good with Unicode now).

The examples in this text were mostly made in Windows PowerShell,
but Git remote operations are in Git Bash.


### Tool Check

We must build with the right version of Java.
(At the time of writing we target Java 8.)
At the same time, let's check that we have the tools we need on the path:

```posh
PS git> java -version
java version "1.8.0_321"
PS git> ant -version
Apache Ant(TM) version 1.10.14 compiled on August 16 2023
PS git> gpg --version
gpg (GnuPG) 2.3.3
libgcrypt 1.9.4
PS git> git --version
git version 2.39.0.windows.2
```


### Clone the Repository

Clone the repository to a named subdirectory and `cd` into it (Bash):

```bash
$ git clone git@github.com:jython/jython.git work
Cloning into 'work'...
$ cd work
$ git describe --all
heads/master
```

And in Powershell,
the last commits should be the same as in the project repository:

```posh
PS work> git log --oneline --graph -4
* d04ff7f62 (HEAD -> master, origin/master, origin/HEAD) Begin to identify as v2.7.4rc2
* 3562755e5 (tag: v2.7.4rc1) Prepare for 2.7.4rc1 release.
* 245deba51 Now with sensible timeouts.
* 66600ad7e Impose timeout on regrtest workflows
```


### Changes Preparing for a Release

The following files may need to be updated to match the version you are about to release:

* `build.xml`: The version number appears piece by piece in the target `common-config`.
  Update these properties:

  * `jython.major_version`,
  * `jython.minor_version`,
  * `jython.micro_version`,
  * `jython.release_level`, and
  * `jython.release_serial`.

  In the language of these properties,
  version 2.7.4 final is spelled `2`, `7`, `4`, `${PY_RELEASE_LEVEL_FINAL}`, `0`.
  Every other expression needing a version number is derived from these 5 values.
* `build.gradle`: The version number appears as a simple string property `version`,
  near the top of the file.
  Version 2.7.4 is simply set like this: `version = '2.7.4'`.
* `README.txt`: It is possible no change is needed at all,
  and if a change is needed, it will probably only be to the running text.
  A copy of this file is made during the build,
  in which information from `build.xml` replaces the place-holders.
  (The place-holders look like `@jython.version@`, etc..)
  The resulting text is what a user sees when installing interactively.
  It automatically includes a prominent banner when making a snapshot build.
* `NEWS`: First try to ensure we have listed all issues closed since the last release.
  The top of this file may look like:

  ```text
  Jython <something> Bugs fixed and features added
      - [ NNNN ] ...
  ```

  Replace the first line with the release you are building
  e.g. "Jython 2.7.4".
  For a final release,
  it will probably say it is the same as the release candidate,
  rather than listing bugs fixed.
  Add anything necessary to the section "New Features".
  After publication (not now),
  we will add a new, empty, section for the version then under development.

These version-settings may already have been made correctly,
to match the identity of the next release.
The build script ensures that, until we actually tag a change set as a release,
the version numbers set here will always appear with a "snapshot" suffix.

You should run the `ant javatest` and `ant regrtest` targets at this point.
These should run cleanly, or at least failures be explained and acceptable,
e.g. known to be attributable to limitations in your network environment.
If bugs are discovered that you need to fix,
it would be best to abandon work on this repository,
fix them in your usual development workbench,
and push or PR them into the project.

> [!TIP]
> You can run the `ant bugtest` target, but it is deprecated.
> (We haven't maintained it as Jython changed.)
> It produces some failures known to be spurious.
> It also creates files you have to clean up manually
> before you can build for a release.

If you changed anything, commit this set of changes locally:

```bash
$ git add --all
$ git status
On branch master
Your branch is up to date with 'origin/master'.

Changes to be committed:
  (use "git restore --staged <file>..." to unstage)
        modified:   NEWS
        modified:   build.gradle
        modified:   build.xml

$ git commit -m"Prepare for 2.7.4 release."
[master 3f256f4a7] Prepare for 2.7.4 release.
 3 files changed, 4 insertions(+), 6 deletions(-)
```


### Get the JARs

Find the database driver JARs from reputable sources.

* The Informix driver may be obtained from Maven Central.
  Version `jdbc-4.50.11.jar` is known to work on Java 8.

* The Oracle JDBC driver may also be found at Maven Central.
  (The Oracle JARs on Maven Central are now official.)
  For Java 8 use the `ojdbc8` JARs.

Let's assume we put the JARs in `D:\git\support`.
Create an `ant.properties` correspondingly:

```properties
# Ant properties defined externally to the release build.
informix.jar = ../support/jdbc-4.50.11.jar
oracle.jar = ../support/ojdbc8-23.4.0.24.05.jar
```

Note that this file is ephemeral and local:
it is ignored by Git because it is named in `.gitignore`.


### Check the Configuration of the Build

Run the `full-check` target, which does some simple checks on the repository:

```posh
PS work> ant full-check
Buildfile: D:\git\work\build.xml

force-snapshot-if-polluted:
     [echo]
     [echo] Change set 3f256f4a7 is not tagged 'v2.7.4' - build is a snapshot.

dump:
     [echo] --- build Jython version ---
     [echo] jython.version.short      = '2.7.4'
     [echo] jython.release            = '2.7.4'
     [echo] jython.version            = '2.7.4-SNAPSHOT'
     [echo] --- optional libraries ---
     [echo] informix                  = '../support/jdbc-4.50.11.jar'
     [echo] oracle                    = '../support/ojdbc8-23.4.0.24.05.jar'
```

It makes an extensive dump,
in which lines like those above matter particularly.
See that `build.xml` has worked out the version string correctly,
and that it is a snapshot build,
as it must be because you haven't tagged the release yet.
Check that the rest of this dump looks like what you ordered
(version of Java correct?)
and that it ends with `BUILD SUCCESSFUL`.

You could do a complete dry-run at this point.
It would create a snapshot build that identifies itself by the version string above.
If you want something other than "SNAPSHOT" as the qualifier,
define the property `snapshot.name` on the `ant` command line or in `ant.properties`.

If you see a message along the lines "Workspace contains uncontrolled files"
then the files listed must be removed (or possibly added to version control) before continuing.
They may be test-droppings or the by-product of your last-minute changes.


### Tag the Release

Ensure you have committed any outstanding changes (none in this example)
and tag the final state as the release,
being careful to observe the conventional pattern
(there *is* a "v" and there are *two* dots):

```posh
PS work> git tag -a -s v2.7.4 -m"Jython 2.7.4 final"
```

This may open a pop-up from GPG
that requires a password to unlock your signing key
(see [PGP Signing](#pgp-signing)).

Note that `git tag -a` creates a sort of commit.
It will need to be pushed eventually,
but the current state of your repository is still at the change set tagged.
If something goes wrong after this point,
but before the eventual push to the repository,
that requires changes and a fresh commit,
it is possible to delete the tag with `git tag -d v2.7.4`,
and make it again at the new tip when you're ready.
The Git book explains why you should not
[delete a tag after the push](https://git-scm.com/docs/git-tag#_discussion).

We follow CPython in signing the tag with GPG as indicated in PEP 101
and the [CPython release-tools](https://github.com/python/release-tools).
See the section [PGP Signing](#pgp-signing) for how to generate a key.
(If you are doing a dry-run you can avoid signing it
by dropping the `-s` option.)

As explained in [signing Git commits with GPG](
https://jamesmckay.net/2016/02/signing-git-commits-with-gpg-on-windows/),
`gpg` as supplied with *Git for Windows*
and *GnuPG for Windows* disagree about the location of your keys.
In order for signing to work,
it may be necessary to prepare your installation of Git (one time only)
to select the full version of *GnuPG for Windows* as follows.

```posh
git config --global gpg.program $env:localappdata\gnupg\bin\gpg.exe
```


### Ant Build for Release

Run the `full-check` target again:

```posh
PS work> ant full-check
Buildfile: D:\git\work\build.xml

     [echo] Build is for release of 2.7.4.

     [echo] jython.version            = '2.7.4'
```
This time the script confirms it is a release
and the version appears without the "SNAPSHOT" qualifier.

If all remains well with the properties dumped, run the `full-build` target.
This outputs the same dump as `full-check` and goes on to build the release artifacts.
`build.xml` does not force a snapshot build on you now
because the source tree is clean and the tag corresponds to the version.

The artifacts of interest are produced in the `./dist` directory and they are:

1. `jython.jar`
1. `jython-installer.jar`
1. `jython-standalone.jar`
1. `sources.jar`
1. `javadoc.jar`

> [!NOTE] At the time of writing, the `javadoc` sub-target produces many warnings.
> Java 8 is much stricter than Java 7 about correct Javadoc.
> These are not fatal to the build:
> they are a sign that our Javadoc is a bit shabby (and always was secretly).


### Gradle Build for Release

We can also build a slim JAR (one *not* containing its dependencies) using Gradle.
The Gradle build was released experimentally in Jython 2.7.2.
Now users have a little experience using this JAR for applications,
we consider it a normal part of the build.

Gradle operates a build entirely parallel to the Ant build,
where everything is regenerated from source,
working in folder `./build2`.

```posh
PS work> .\gradlew --console=plain publish
> Task :generateVersionInfo
This build is for v2.7.4.

> Task :generateGrammarSource
...
> Task :compileJava
> Task :expose
> Task :mergeExposed
> Task :mergePythonLib
> Task :copyLib
> Task :processResources
> Task :classes
> Task :pycompile
> Task :jar
> Task :generateMetadataFileForMainPublication
> Task :generatePomFileForMainPublication
> Task :javadoc
...
> Task :javadocJar
> Task :sourcesJar
> Task :publishMainPublicationToStagingRepoRepository
> Task :publish

BUILD SUCCESSFUL in 6m 41s
16 actionable tasks: 16 executed
```

Don't worry, despite the name, this doesn't actually *publish* Jython.
When the build finishes, a JAR that is potentially fit to publish,
and its subsidiary artifacts (source, javadoc, checksums),
will have been created in `./build2/stagingRepo/org/python/jython-slim/2.7.4`.

It can also be "published" to your local Maven cache (usually `~/.m2/repository`)
with the task `publishMainPublicationToMavenLocal`.
This need not be done as part of a release,
but can be useful in verification using a Gradle or Maven build that references it
(see the section [Slim (Gradle) regrtest](#slim-gradle-regrtest).


### Test what you built

At this point,
take the stand-alone and installer JARs to an empty directory elsewhere,
and try to use them in a new shell session.
In the example,
the local directory `inst` is chosen as the target in the installer.
Let's use Java 11, different from the version we built with.

```posh
PS 274-trial> mkdir kit
PS 274-trial> copy "D:\git\work\dist\jython*.jar" .\kit
PS 274-trial> java -jar kit\jython-installer.jar
WARNING: An illegal reflective access operation has occurred
...
DEPRECATION: A future version of pip will drop support for Python 2.7.
...
Successfully installed pip-19.1 setuptools-41.0.1
```

It is worth checking the manifests:

```posh
PS 274-trial> jar -xf .\kit\jython-standalone.jar META-INF
PS 274-trial> cat .\META-INF\MANIFEST.MF
Manifest-Version: 1.0
Ant-Version: Apache Ant 1.10.14
Created-By: 1.8.0_321-b07 (Oracle Corporation)
Main-Class: org.python.util.jython
Built-By: Jeff
Automatic-Module-Name: org.python.jython2.standalone
Implementation-Vendor: Python Software Foundation
Implementation-Title: Jython fat jar with stdlib
Implementation-Version: 2.7.4

Name: Build-Info
version: 2.7.4
git-build: true
oracle: true
informix: true
build-compiler: modern
jdk-target-version: 1.8
debug: true
```

And similarly in other JARs `inst\jython.jar`, `kit\jython-installer.jar`.


#### Installation `regrtest`

The real test consists in running the regression tests:

```posh
PS 274-trial> inst\bin\jython -m test.regrtest -e
== 2.7.4 (tags/v2.7.4:3f256f4a7, Aug 18 2024, 10:30:53)
== [Java HotSpot(TM) 64-Bit Server VM (Oracle Corporation)]
== platform: java11.0.22
== encodings: stdin=ms936, stdout=ms936, FS=utf-8
== locale: default=('en_GB', 'windows-1252'), actual=(None, None)
test_grammar
test_opcodes
test_dict
...
4 fails unexpected:
    test___all__ test_gc_jy test_import_jy test_ssl_jy
```

These failures are false alarms.

* `test___all__`, `test_gc_jy`  and `test_import_jy` fail,
  and others are skipped,
  because we (deliberately) do not include certain test resources.
* `test_ssl_jy` fails because of [BJO issue 2858](https://bugs.jython.org/issue2858).
* `test_sort` also fails intermittently on later versions of Java.


#### Stand-alone `regrtest`

The stand-alone JAR does not include the tests,
but one may run them by supplying a copy of the test modules as below.
The point of copying (only) the test directory to `TestLib/test`,
rather than putting `inst/Lib` on the path,
is to ensure that other modules are tested from the stand-alone JAR itself.
There will be many failures.
When the author last tried, they were these:

```posh
PS 274-trial> copy -r inst\Lib\test TestLib\test
PS 274-trial> $env:JYTHONPATH = ".\TestLib"
PS 274-trial> java -jar kit\jython-standalone.jar -m test.regrtest -e
== 2.7.4 (tags/v2.7.4:3f256f4a7, Aug 18 2024, 10:30:53)
== [Java HotSpot(TM) 64-Bit Server VM (Oracle Corporation)]
== platform: java11.0.22
== encodings: stdin=ms936, stdout=ms936, FS=utf-8
== locale: default=('en_GB', 'windows-1252'), actual=(None, None)
test_grammar
test_opcodes
...
test_zlib
test_zlib_jy
338 tests OK.
17 tests skipped:
    test_codecmaps_hk test_coerce_jy test_curses test_dict2java
    test_exceptions_jy test_java_integration test_java_subclasses
    test_java_visibility test_jbasic test_joverload test_jy_internals
    test_set_jy test_smtpnet test_socketserver test_subprocess
    test_urllib2net test_urllibnet
10 skips unexpected:
    test_coerce_jy test_dict2java test_exceptions_jy
    test_java_integration test_java_subclasses test_java_visibility
    test_jbasic test_joverload test_jy_internals test_set_jy
33 tests failed:
    test_argparse test_classpathimporter test_cmd_line
    test_cmd_line_script test_codecs_jy test_compile_jy test_email_jy
    test_email_renamed test_gc_jy test_httpservers test_import
    test_import_jy test_json test_jython_initializer
    test_jython_launcher test_lib2to3 test_linecache test_marshal
    test_os_jy test_pdb test_platform test_popen test_quopri test_repr
    test_site test_site_jy test_ssl_jy test_sys test_sys_jy
    test_threading test_urllib2 test_warnings test_zipimport_support
33 fails unexpected:
    test_argparse test_classpathimporter test_cmd_line
    test_cmd_line_script test_codecs_jy test_compile_jy test_email_jy
    test_email_renamed test_gc_jy test_httpservers test_import
    test_import_jy test_json test_jython_initializer
    test_jython_launcher test_lib2to3 test_linecache test_marshal
    test_os_jy test_pdb test_platform test_popen test_quopri test_repr
    test_site test_site_jy test_ssl_jy test_sys test_sys_jy
    test_threading test_urllib2 test_warnings test_zipimport_support
```

Most of these failures are in tests that assume
the library is a real file system.
Others arise because we do not include certain JARs needed for the test.
It is necessary to pick through the failures carefully
to detect which are real.

> [!TIP]
> We could probably do this better through skips in the tests,
> sensitive to running stand-alone,
> or (widely useful) a broader interpretation of "file path" in Jython,
> reflecting the importance of the JAR file system in Java.
>
> We should do this occasionally, and not just when trying to release.
> Some of the failures are genuine problems,
> by chance revealed only in the stand-alone version.


#### Slim (Gradle) `regrtest`

There is not currently a pre-prepared way to test
the Gradle-built JAR (`jython-slim`),
but it is not difficult to create something.
For this, it is necessary to publish to a local repository,
such as your personal Maven cache:

```posh
PS work> .\gradlew --console=plain publishMainPublicationToMavenLocal
```

This will deliver build artifacts to
`~/.m2/repository/org/python/jython-slim/2.7.4`.
One can construct an application to run with that as a dependency,
by giving it a Gradle build file like this:

```Gradle
// Application importing the jython-slim JAR.
plugins {
    id 'application'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'org.python:jython-slim:2.7.4'
}

application {
    mainClass = 'uk.co.farowl.jython.slimdemo.RegressionTest'
}
```


The following executes `test.regrtest`
using the same local copy of the tests
prepared for the stand-alone Jython.

```java
package uk.co.farowl.jython.slimdemo;
import org.python.util.PythonInterpreter;
public class RegressionTest {
    public static void main(String[] args) {
        try (PythonInterpreter interp = new PythonInterpreter()) {
            interp.exec("import sys, os");
            interp.exec("sys.path[0] = os.sep.join(['.', 'TestLib'])");
            interp.exec("sys.argv[1:] = ['-e']");
            interp.exec("from test import regrtest as rt");
            interp.exec("rt.main()");
        }
    }
}
```

Tests have about the same success rate as for the stand-alone Jython JAR.
Notably `test_ssl_jy` passes here because a genuine (not wrapped)
Bouncy Castle JAR is on the path.

Tests end with a failure status under Gradle, even when all tests pass,
because `regrtest` calls `sys.exit`,
which raises `SystemExit`.
It looks like:

```text
333 tests OK.
...
33 tests failed:
...
Exception in thread "MainThread" Traceback (most recent call last):
  File "<string>", line 1, in <module>
  File ".\TestLib\test\regrtest.py", line 521, in main
    sys.exit(surprises > 0)
SystemExit: True
```

One could improve the driver program, but it is complicated to do properly.


### Build the Bundles to Publish

Back in the release working directory,
the artifacts for Maven are built using a separate script `maven/build.xml`.

```posh
PS work> ant -f maven\build.xml
Buildfile: D:\git\work\maven\build.xml
...
validate-template-pom:
[xmlvalidate] 1 file(s) have been successfully validated.
...
BUILD SUCCESSFUL
Total time: 2 minutes 27 seconds
```

During the build, `gpg` may prompt you (in a dialogue box)
for the pass-phrase that protects your private signing key.
This leaves the following new artifacts in `./publications`:

* `jython-2.7.4-bundle.jar`
* `jython-standalone-2.7.4-bundle.jar`
* `jython-installer-2.7.4-bundle.jar`
* `jython-slim-2.7.4-bundle.jar`


## Publication

### Account

In order to publish the bundles created in `./publications`,
it is necessary to have an account with access to `groupId` `org.python`,
which Sonatype will grant given the support of an existing owner.
(This is a human process administered through JIRA.)
There is an extensive
[Sonatype OSSRH Guide](https://central.sonatype.org/pages/ossrh-guide.html)
about getting and using an account.


### PGP Signing

You need a PGP signing key pair (generated with `gpg --gen-key`)
on the computer where you are working.
This must be published through the pool of PGP key servers
for Sonatype to pick up,
and so reassure users that
this release of Jython is really from the project.

The infrastructure of PGP has been overhauled
since the previous version of these notes was written.
Follow the Sonatype guide
[Working with PGP Signatures](https://central.sonatype.org/publish/requirements/gpg/),
which now appears to have been updated with the changes.

```posh
PS work> gpg --list-secret-keys
C:\Users\Jeff\AppData\Roaming\gnupg\pubring.kbx
-----------------------------------------------
sec   rsa2048 2019-10-20 [SC] [expires: 2028-02-26]
      C8C4B9DC1E031F788B12882B875C3EF9DC4638E3
uid           [ultimate] Jeff Allen <ja.py@farowl.co.uk>
ssb   rsa2048 2019-10-20 [E] [expires: 2028-02-26]
```

The [OpenPGP key server](https://keys.openpgp.org)
provides an interface to query a PGP public key.
PGP servers form a pool.
It may take a few hours for your key to wash up at the machine
Sonatype consults.

Generation and publication of a key are one-time actions,
except that the key has a finite lifetime with possible extensions.
(The key here has been extended twice.)
See
[Working with PGP Signatures](https://central.sonatype.org/publish/requirements/gpg/)
for how to extend the life of a key.

> [!IMPORTANT]
> You may decide to create a new key for signing future releases.
> The key that was used to sign past releases should remain valid
> so that users can still validate those past releases.
> Renewing an old key is a valid and useful thing to do.
> (An exception to this rule is when the old *private* key is thought
> to have been lost.)


### Publication via Sonatype

You are now ready to upload bundles acceptable to Sonatype.

* Go to the [Sonatype](https://oss.sonatype.org) 
  repository manager and log in.
* Under "Build Promotion" select "Staging Upload".
* On the "Staging Upload" tab, and the Upload Mode drop-down,
  select "Artifact Bundle".
* Navigate to the `./publications` folder and upload in turn:

  * `jython-slim-2.7.4-bundle.jar`
  * `jython-2.7.4-bundle.jar`
  * `jython-standalone-2.7.4-bundle.jar`
  * `jython-installer-2.7.4-bundle.jar`

  For some reason (privacy?) the display shows a fake file path
  but the name is correct.
  Each upload creates a "staging repository".

> [!NOTE]You may get a report (e-mail) from Sonatype Lift at this point
> reporting potential vulnerabilities in dependencies.
> (It seems only to work on the `-slim` JAR, which is why we upload it first.)
> If any vulnerability is sufficiently serious to warrant upgrading JARs,
> treat this as a late test failure:
> fix it in your normal development environment with a PR and repeat the process.  
> 
> Assuming you have deferred pushing the tag no publicly visible harm has been done.
> (See [Push with tag](#only-now-is-it-safe-to-git-push) below.)
> If you already pushed the tag,
> repeat the release process with an appropriate increment on the version number.

You may discard (drop) Repositories that you decide not to publish
from the "Staging Repositories" tab in the repository manager.

* Under "Build Promotion" select the "Staging Repositories" tab.
* Check (on the "Activity" tab)
  that the upload reached "Close" with good status,
  If not, it should tell you what is lacking, and you have to go back and fix it.
* In a fresh directory,
  download the (as yet unreleased) artifacts from Sonatype and test them,
  repeating the section [Test what you built](#test-what-you-built).
  A staging URL has form:
  `https://oss.sonatype.org/content/repositories/orgpython-1105`
  where the final number increments with each upload.
* When you are absolutely satisfied ... "Release" the bundles.
  This will cause them to appear in the Maven
  [Central Repository](https://search.maven.org/)
  (takes an hour or two).

> [!CAUTION]
> Release at Sonatype is irreversible.


### Only now is it safe to `git push`

If testing convinces you this is a build we should let loose
on an unsuspecting public,
it is time to push these changes and the tag you made
upstream to the Jython repository.
Back in the place where the release was built (and using Bash):

```bash
$ git push --follow-tags
```

Try very hard not to push a tag you later regret
(e.g. on the wrong change set or a version still needing a fix).
It is problematic to delete a tag after the push.
It is better to increment the version,
which is painless if it is an `a`, `b`, or `rc` release,
and painfully obvious if this is a final release.


### Announcement

> [!NOTE]
> This section is untested since recent changes.

* update files in (or make a PR against) the
  [website repository](https://github.com/jython/jython.github.io)
  that reference the current release:

  * Add to the [website news page](https://www.jython.org/news)
  * Ensure links on the [website front page](https://www.jython.org/index)
    and [website download page](https://www.jython.org/download) reflect:

    * the latest stable release
    * the current alpha, beta, or candidate release (if any to be advertised)

  Exactly what you do here will depend on the kind of release you just made.

* announce on Twitter (as jython), mailing lists, blog ...


## Ready for new work

After a release,
Jython in the development environment
should no longer identify itself as the version just released,
so we increment the version string.
We do not know for sure the version next to be publicly released,
so we use the smallest increment that results in a valid version number.

After an alpha, beta or release candidate,
assume the successor version to be a one-up serial of the *same* release level,
incrementing `jython.release_serial`.
After a final release,
assume the successor to be an alpha of the next micro-release.
For example, `2.7.2b2` is followed by `2.7.2b3`,
and `2.7.2` by `2.7.3a1`.

If the version under development is ostensibly `2.7.4b3`,
the build system will label the code as `2.7.4b3-DEV` in builds.
If you build an installer, or dry-run a release, it will be `2.7.4b3-SNAPSHOT`.
You can read this as a version that "may eventually become" `2.7.4b3` etc..

The version under development in this scheme will often be one that never sees a release.
E.g. when we are apparently working on `2.7.4b3`,
the next release is quite likely to be `2.7.4rc1` instead.
It's a harmless idiosyncrasy of the process that
the version may only be chosen accurately when the time comes to release it.

Make this change in both `build.xml` and `build.gradle`.
See the section on
[Changes preparing for a release](#changes-preparing-for-a-release)
for details.

In `NEWS`, add a new, empty, section in the development history that looks like this:
```text
Jython <successor version> Bugs fixed
```

If you've just built a final release, the new material will look like:
```text
==============================================================================
Jython <successor version>
==============================================================================

New Features

Jython <successor version>a1 Bugs fixed
```

Commit and push this change upstream.

> [!IMPORTANT]
> The description of a new feature is associated with
> the prospective final release,
> not the alpha or beta that introduced it.
