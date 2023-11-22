#!/bin/bash
set -e;

mkdir -p maven/com;
rm -rv maven/com;
./gradlew publishAllPublicationsToLocalRepository -PdisableSigning;
tree maven/com;
