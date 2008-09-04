#!/bin/bash

die() {
    echo 1>&2 $@
    exit 1
}

if [ x"$INROOT_DIR" = x ] ; then
    die "INROOT_DIR environment variable must be specified"
fi

SYSTEM_JARS="jna objectweb-asm/asm objectweb-asm/asm-{util,analysis,tree}"
if [ x"$RHINO_JAR" = x ] ; then
    JARS=
    SYSTEM_JARS="$SYSTEM_JARS js.jar"
else
    JARS=$RHINO_JAR
fi

JGIRSRC=${INROOT_DIR}/share/girepository
for j in ${JGIRSRC}/*.jar; do
  JARS="$JARS:$j"
done
JARS="${JARS}:${INROOT_DIR}/share/java/jgir.jar"
CLASSPATH=$(build-classpath $SYSTEM_JARS):${JARS}
export CLASSPATH
echo "CLASSPATH=$CLASSPATH"
exec env /usr/bin/java org.mozilla.javascript.tools.shell.Main "$@"