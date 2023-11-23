#!/bin/bash
find . -iname '*.swift' -type f -exec sed -i '' 's/[[:space:]]\{1,\}$//' {} \+;
