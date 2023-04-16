#!/usr/bin/env bash

echo
echo "Using Java Version"
java -version
echo "$JMH_PARAMS"
exec bash -c "java -ea -jar jmh-ignite-benchmark.jar  $JMH_PARAMS"