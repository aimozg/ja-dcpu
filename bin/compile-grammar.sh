#!/bin/bash

ANTLR_VERSION=3.2

PROJECTDIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )"/.. && pwd )"
ANTLR_JAR="$PROJECTDIR/lib/antlr-$ANTLR_VERSION.jar"

cd "$PROJECTDIR"

echo -e "Generating java from grammar\n"
java -cp $ANTLR_JAR org.antlr.Tool -report -fo antlr-generated/dcpu grammar/dcpu/*.g
echo "Generation complete - refresh your IDE to compile source"
