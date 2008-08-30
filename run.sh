#!/bin/bash
JGIRSRC=/src/build/gi/share/gitypelibs
JARS=
for j in ${JGIRSRC}/*.jar; do
  JARS="$JARS:$j"
done
CLASSPATH=$(build-classpath jna objectweb-asm/asm objectweb-asm/asm-{util,analysis,tree}):${JGIRSRC}/bin:${JARS}
export CLASSPATH
echo "CLASSPATH=$CLASSPATH"
exec env GIREPOPATH=${JGIRSRC}/tests jhbuild run "$@"