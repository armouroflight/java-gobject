#!/bin/bash

die() {
    echo 1>&2 $@
    exit 1
}

if [ x"$INROOT_DIR" = x ] ; then
    die "INROOT_DIR environment variable must be specified"
fi

JGIRSRC=${INROOT_DIR}/share/girepository
JARS=
for j in ${JGIRSRC}/*.jar; do
  JARS="$JARS:$j"
done
JARS="${JARS}:${INROOT_DIR}/share/java/jgir.jar"
CLASSPATH=$(build-classpath jna):${JARS}
export CLASSPATH
echo "CLASSPATH=$CLASSPATH"
exec env "$@"