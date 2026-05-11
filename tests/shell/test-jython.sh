#!/usr/bin/env bash

# test script for bin/jython

if [ -z "$1" ] ; then
    echo "$0: usage: test-jython.sh <Jython home>" >&2
    exit 99
fi

TEST_DIR="`mktemp -dt test-jython.XXXXXX`"
SPACE_DIR="$TEST_DIR/directory with spaces"

cp -Rp "$1" "$SPACE_DIR"

java_major_version() {
    local output version major
    output="$("$1" -J-version 2>&1)"
    version=`expr "$output" : '.*version "\([^"]*\)"'`
    case "$version" in
        1.*)
            major=${version#1.}
            major=${major%%.*}
            ;;
        *)
            major=${version%%[!0-9]*}
            ;;
    esac
    echo "$major"
}

for JYTHON_HOME in "$SPACE_DIR" "$1" ; do
    JYTHON="$JYTHON_HOME/bin/jython"
    export JYTHON_HOME
    set -ex

    # -J passthrough
    "$JYTHON" -J-version 2>&1 | [ `egrep -c "^(java|openjdk) version "` == 1 ]

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

    JAVA_MAJOR=`java_major_version "$JYTHON"`

    # JDB
    JDB_OUTPUT="$TEST_DIR/jdb.out"
    set +e
    (echo run; sleep 3) | "$JYTHON" --jdb -c "print '\ntest'" > "$JDB_OUTPUT" 2>&1
    JDB_STATUS=$?
    set -e
    if [ `egrep -c "^test" "$JDB_OUTPUT"` == 1 ] ; then
        :
    else
        cat "$JDB_OUTPUT" >&2
        [ "$JDB_STATUS" -ne 0 ] && exit "$JDB_STATUS"
        exit 1
    fi

    # Jython profiling
    if [ "$JAVA_MAJOR" -lt 13 ] ; then
        "$JYTHON" --profile -c pass 2>&1 | \
	    [ `egrep -c "^\| Most expensive methods"` -ge 1 ]
        [ -f profile.txt ] && rm profile.txt
    else
        "$JYTHON" --profile -c pass 2>&1 | \
	    [ `egrep -c "^--profile is not supported on Java $JAVA_MAJOR;"` == 1 ]
    fi

    # $CLASSPATH
    CLASSPATH="$JYTHON_HOME/Lib/test/blob.jar" \
	"$JYTHON" -c "print __import__('Blob')" | \
	  [ `egrep -c "Blob"` == 1 ]

    # $CLASSPATH + profiling
    if [ "$JAVA_MAJOR" -lt 13 ] ; then
        CLASSPATH="$JYTHON_HOME/Lib/test/blob.jar" \
	    "$JYTHON" --profile -c "print __import__('Blob')" | \
	      [ `egrep -c "Blob"` == 1 ]
    else
        CLASSPATH="$JYTHON_HOME/Lib/test/blob.jar" \
	    "$JYTHON" --profile -c "print __import__('Blob')" 2>&1 | \
	      [ `egrep -c "^--profile is not supported on Java $JAVA_MAJOR;"` == 1 ]
    fi

    set +ex
done

rm -rf "$TEST_DIR"

echo "Test successful."
