#!/bin/sh

##############################################################################
#
#   Gradle start up script for Ubuntu/Linux
#
##############################################################################

# Resolve links: $0 may be a link
PRG="$0"
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
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

warn () { echo "$*"; }
die () { echo "$*"; exit 1; }

cygwin=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

JAVACMD="$JAVA_HOME/bin/java"

if [ -z "$JAVA_HOME" ] ; then
  if [ -d /usr/lib/jvm/java-17-openjdk-amd64 ] ; then
    JAVACMD=/usr/lib/jvm/java-17-openjdk-amd64/bin/java
  else
    die "JAVA_HOME not set. Cannot determine Java path."
  fi
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
