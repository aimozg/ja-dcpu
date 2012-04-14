#!/bin/bash -eu
# TODO: assumes compilation is done by IDE. should probably do it here instead

VERSION=$1
ARCHIVE=ja-dcpu-demo-$VERSION.jar

ROOTDIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )"/.. && pwd )"

jar cvfm $ROOTDIR/build/$ARCHIVE $ROOTDIR/ExtAsmDemo.META-INF/MANIFEST.MF -C $ROOTDIR/build/classes dcpu
