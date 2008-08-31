#!/bin/bash
JGIRSRC=${INROOT_DIR}/share/girepository
JARS=
for j in ${JGIRSRC}/*.jar; do
  JARS="$JARS:$j"
done
JARS="${JARS}:${INROOT_DIR}/share/java/jgir.jar"
CLASSPATH=$(build-classpath jna objectweb-asm/asm objectweb-asm/asm-{util,analysis,tree}):${JARS}
export CLASSPATH
echo "CLASSPATH=$CLASSPATH"
exec env "$@"