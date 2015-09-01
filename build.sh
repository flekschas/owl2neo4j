#!/bin/bash

gradle build

( cat ./owl2neo4j_wrapper.sh
  cat ./build/libs/Owl2Neo4J.jar ) > ./owl2neo4j

chmod +x ./owl2neo4j
