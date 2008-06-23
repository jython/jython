#!/bin/sh

# test script for bin/jython

if [ -z "$1" ] ; then
    echo "$0: usage: test-jython.sh <Jython home>" >&2
    exit 99
fi

TEST_DIR="`mktemp -dt test-jython.XXXXXX`"
SPACE_DIR="$TEST_DIR/directory with spaces"

cp -Rp "$1" "$SPACE_DIR"

for JYTHON_HOME in "$SPACE_DIR" "$1" ; do
    JYTHON="$JYTHON_HOME/bin/jython"
    export JYTHON_HOME
    set -ex

    # -J passthrough
    "$JYTHON" -J-version 2>&1 | [ `egrep -c "^java version "` == 1 ]

    # Jython reports version
    "$JYTHON" --version 2>&1 | [ `egrep -c "^Jython "` == 1 ]

    # $JYTHON_OPTS
    JYTHON_OPTS="--version" "$JYTHON" 2>&1 | [ `egrep -c "^Jython "` == 1 ]

    # Jython help
    "$JYTHON" --help 2>&1 | [ `egrep -c "^usage: "` == 1 ]

    # Jython launcher help
    "$JYTHON" --help 2>&1 | [ `egrep -c "^Jython launcher options:"` == 1 ]

    # Jython exit status
    set +e
    "$JYTHON" -c 'import sys; sys.exit(5)' 
    [ $? == 5 ] || exit 1
    set -e
    
    # Jython executable (don't include newline in case it's \r\n)
    [ `"$JYTHON" -c 'import sys; print sys.executable is not None,'` == True ]

    # JDB
    echo run | "$JYTHON" --jdb -c "print '\ntest'" | [ `egrep -c "^test"` == 1 ]

    # Jython profiling
    "$JYTHON" --profile -c pass 2>&1 | \
	[ `egrep -c "^\| Thread depth limit:"` == 1 ]
    [ -f profile.txt ] && rm profile.txt

    # $CLASSPATH
    CLASSPATH="$JYTHON_HOME/Lib/test/blob.jar" \
	"$JYTHON" -c "print __import__('Blob')" | \
	  [ `egrep -c "^Blob"` == 1 ]

    # $CLASSPATH + profiling
    CLASSPATH="$JYTHON_HOME/Lib/test/blob.jar" \
	"$JYTHON" --profile -c "print __import__('Blob')" | \
	  [ `egrep -c "^Blob"` == 1 ]

    set +ex
done

rm -rf "$TEST_DIR"

echo "Test successful."
