#!/bin/sh
[ -f tags ] && rm -f tags
find -name \*.java -exec etags -a -o tags {} \;