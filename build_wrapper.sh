#!/bin/bash

# Wraps the JAR with `owl2neo4j_wrapper.sh` to make it look like a normal
# executable binary file.

gradle build

( cat ./owl2neo4j_wrapper.sh
  cat ./dist/owl2neo4j.jar ) > ./dist/owl2neo4j

chmod +x ./owl2neo4j
