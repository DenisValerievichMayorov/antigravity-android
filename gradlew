#!/usr/bin/env sh

# Resolves the path to the gradlew script
#
# Specific to Unix-like platforms

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HO=`pwd`
cd "$SAVED" >/dev/null

APP_OUT=`basename "$0"`

# Use the maximum available, otherwise default to 512M
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Find java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVACMD="$JAVA_HOME/bin/java"
    else
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        echo "Please set the JAVA_HOME variable in your environment to match the" >&2
        echo "location of your Java installation." >&2
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        echo "Please set the JAVA_HOME variable in your environment to match the" >&2
        echo "location of your Java installation." >&2
        exit 1
    }
fi

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
JVM_OPTS=""

# Collect all arguments for the java command
# Split the JVM_OPTS and JAVA_OPTS by spaces, then build the command line
eval set -- $DEFAULT_JVM_OPTS $JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath \""$APP_HO/gradle/wrapper/gradle-wrapper.jar"\" org.gradle.wrapper.GradleWrapperMain '"$@"'

exec "$JAVACMD" "$@"
