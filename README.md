# OWL 2 Neo4J [![Build Status](https://travis-ci.org/flekschas/owl2neo4j.svg?branch=develop)](https://travis-ci.org/flekschas/owl2neo4j)

Convert [OWL](owl) to labeled property graph and import into [Neo4J](neo4j).

## Build

**Requirements**:

* [Java RE 7](jre7)
* [Gradle](gradle)

```
$ clone https://github.com/flekschas/owl2neo4j
$ cd owl2neo4j
$ gradle build
```

## Import ontology

**Requirements**:

* [Neo4J](neo4j)

```
$ java -jar ./build/libs/Owl2Neo4J.jar -o pizza.owl -n "Pizza Ontology" -a pizza -s http://localhost:7474 -u neo4j -p neo4j
```

**Command line options**:

```
usage: java -jar Owl2Graph.jar -a <String> [-h] -n <String> -o <Path> -p
       <String> -s <URL> -u <String> [-v] [--version]
Import OWL into Neo4J as a labeled property graph.

 -a,--abbreviation <String>   Ontology abbreviation (E.g. go)
 -h,--help                    Shows this help
 -n,--name <String>           Ontology name (E.g. Gene Ontology)
 -o,--owl <Path>              Path to OWL file
 -p,--password <String>       Neo4J user password
 -s,--server <URL>            Neo4J server root URL
 -u,--user <String>           Neo4J user name
 -v,--verbosity               Verbose output
    --version                 Show version
```

**Importing large ontologies**:

By default the OWLAPI XML loader has a 64,000 triple limit. To increase the limit and being able to import larger ontologies start `Owl2Neo4J.jar` with `DentityExpansionLimit=<LARGE_NUMBER>` flag like so:

```
$ java -jar -DentityExpansionLimit=10000000 ./build/libs/Owl2Neo4J.jar -o extra-large-pizza.owl -n "Extra Large Pizza Ontology" -a elpo -s http://localhost:7474 -u neo4j -p neo4j
```

Be sure that you have enough RAM to theoretical load _10000000_ (or any other number), otherwise your system will complain.

[gradle]:https://gradle.org/
[jre7]:http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html
[neo4j]:http://neo4j.com/
[owl]:www.w3.org/2004/OWL/
