#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*->\(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`cd "$(dirname "$PRG")" >/dev/null 2>&1 && pwd`"
APP_HOME="`cd "$(dirname "$SAVED")" >/dev/null 2>&1 && pwd`"
cd "$APP_HOME" >/dev/null 2>&1 || exit

# Determine the Java command to use in order to perform the actual
# submission, i.e., determine the command that will start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "Error: JAVA_HOME is not defined correctly." >&2
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || { echo "Error: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2; exit 1; }
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" ] && [ "$darwin" = "false" ] && [ "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" ] || [ "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            echo "Warning: Could not set maximum file descriptor limit: $MAX_FD" >&2
        fi
    else
        echo "Warning: Could not query maximum file descriptor limit: $MAX_FD_LIMIT" >&2
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = "true" ] || [ "$msys" = "true" ] ; then
    APP_HOME=`(cd "$APP_HOME" && pwd -P)`
    APP_HOME=`echo "$APP_HOME" | sed 's|/cygdrive/\(..\)|\1:|g'`
    CLASSPATH=`echo "$CLASSPATH" | sed 's|/cygdrive/\(..\)|\1:|g'`
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    for arg do
        if expr "$arg" : '[^/].*' > /dev/null ; then
            arg=`(cd "$arg" && pwd -P)`
            arg=`echo "$arg" | sed 's|/cygdrive/\(..\)|\1:|g'`
        fi
        APP_ARGS="$APP_ARGS \"$arg\""
    done
    # to finish the shell variable expansion
    eval "set -- $APP_ARGS"
fi

# Collect all arguments for the java command.
set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

# Stop when "xargs" is not available.
if ! command -v xargs >/dev/null 2>&1
then
    echo "Error: xargs is not available" >&2
    exit 1
fi

# Use "xargs" to parse quoted args.
# With -n1 it outputs one arg per line, with -0 it handles special chars (spaces and newlines) properly.
if ! echo "$*" | xargs -0 sh -c 'for arg; do
    case "$arg" in
      -*)  false ;;
      *)   true ;;
    esac
done' sh
then
    echo "Error: Unexpected failure parsing options" >&2
    exit 1
fi

exec "$JAVACMD" "$@"
