@echo off
rem ---------------------------------------------------------------------------
rem jython.bat - start script for Jython (adapted from jruby.bat)
rem
rem Environment variables (optional)
rem
rem   JAVA_HOME      Java installation directory
rem
rem   JYTHON_HOME    Jython installation directory
rem
rem   JYTHON_OPTS    Default Jython command line arguments
rem
rem ---------------------------------------------------------------------------

setlocal enabledelayedexpansion

rem ----- Verify and set required environment variables -----------------------

set _JYTHON_HOME="%JYTHON_HOME%"
if not "%JYTHON_HOME%" == "" goto gotHome
pushd "%~dp0%\.."
set _JYTHON_HOME="%CD%"
popd

:gotHome
if exist %_JYTHON_HOME%/jython.jar goto homeOK
echo Cannot find jython.jar in %_JYTHON_HOME%
echo Try running this batch file from the 'bin' directory of an installed Jython
echo or setting JYTHON_HOME.
goto cleanup

:homeOK
set _JAVA_CMD=java
if not "%JAVA_HOME%" == "" (
   set _JAVA_CMD=%JAVA_HOME%\bin\java
)

set _CP=%_JYTHON_HOME%\jython-complete.jar
if not exist %_JYTHON_HOME%\jython.jar goto run
rem prefer built version
set _CP=%_JYTHON_HOME%\jython.jar
for %%j in (%_JYTHON_HOME%\javalib\*.jar) do (
    set _CP=!_CP!;"%%j"
)

:run
%_JAVA_CMD% %JAVA_OPTS% -Xss512k -Xbootclasspath/a:%_CP% -Dpython.home=%_JYTHON_HOME% -Dpython.executable="%~f0" -classpath "%CLASSPATH%" org.python.util.jython %*
set E=%ERRORLEVEL%

:cleanup
set _JYTHON_HOME=%JYTHON_HOME%
set _JAVA_CMD=
set _CP=

:finish
exit /b %E%
