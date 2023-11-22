#!/bin/bash
set -e;

./gradlew publishAllPublicationsToSonatypeRepository --no-parallel -PossrhUsername=$1 -PossrhPassword=$2

echo "Login here, check, and then release: https://s01.oss.sonatype.org/#welcome";
