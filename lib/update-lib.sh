#!/bin/bash

# Variable names renamed because of a (transient?) bug in Travis

# This script populates the lib directory with a number of commercial
# jar files necessary to compile extension steps.
if [ "$XMLD_BASE" != "" ] && [ "$XMLD_NAME" != "" ] && [ "XMLD_PASS" != "" ]
then
    echo "Updating lib directory..."
    cd lib
    curl -o filelist -u "$XMLD_NAME:$XMLD_PASS" "$XMLD_BASE/filelist"
    for f in `cat filelist`; do
        curl -s -o $f -u "$XMLD_NAME:$XMLD_PASS" "$XMLD_BASE/$f"
    done
    mkdir -p AH
    cd AH
    curl -s -o archive.tar.gz -u "$XMLD_NAME:$XMLD_PASS" "$XMLD_BASE/AH/archive.tar.gz"
    tar zxf archive.tar.gz
    rm archive.tar.gz
fi
