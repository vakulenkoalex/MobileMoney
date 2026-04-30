#!/bin/sh

##############################################################################
#
#   Gradle start up script for Ubuntu/Linux
#
##############################################################################

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
cd "`dirname "$PRG"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -z "$JAVA_HOME" ] ; then
  if [ -d /usr/lib/jvm/java-17-openjdk-amd64 ] ; then
    JAVACMD=/usr/lib/jvm/java-17-openjdk-amd64/bin/java
  elif [ -d /usr/lib/jvm/java-21-openjdk-amd64 ] ; then
    JAVACMD=/usr/lib/jvm/java-21-openjdk-amd64/bin/java
  elif command -v java >/dev/null 2>&1; then
    JAVACMD=java
  else
    echo "JAVA_HOME not set and no java found"
    exit 1
  fi
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
