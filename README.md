# OWL 2 Neo4J [![Build Status](https://travis-ci.org/flekschas/owl2neo4j.svg?branch=master)](https://travis-ci.org/flekschas/owl2neo4j)

Convert [OWL](owl) to labeled property graph and import into [Neo4J](neo4j).

## Build

Each release comes with a precompiled JAR, created automatically by Travis-CI. To build the JAR file manually follow the three steps below. The JAR file will be created in `dist`.

**Requirements**:

* [Java RE 7](jre7)
* [Gradle](gradle)

```
$ git clone https://github.com/flekschas/owl2neo4j
$ cd owl2neo4j
$ gradle build
```

## Import ontology

**Requirements**:

* [Neo4J](neo4j)

A typical import is executed as follows:

```
$ java -jar ./dist/owl2neo4j.jar -o pizza.owl -n "Pizza Ontology" -a pizza
```

**Command line options**:

```
usage: java -jar owl2neo4j.jar -a <String> [--eqp <String>] [-h] -n
       <String> -o <Path> [-p <String>] [-s <URL>] [-u <String>] [-v]
       [--version]
Import OWL into Neo4J as a labeled property graph.

 -a,--abbreviation <String>   Ontology abbreviation (E.g. go)
    --eqp <String>            Existential quantification property (E.g.
                              http://www.co-ode.org/ontologies/pizza/pizza
                              .owl#hasTopping)
 -h,--help                    Shows this help
 -n,--name <String>           Ontology name (E.g. Gene Ontology)
 -o,--owl <Path>              Path to OWL file
 -p,--password <String>       Neo4J user password
 -s,--server <URL>            Neo4J server root URL [Default:
                              http://localhost:7474]
 -u,--user <String>           Neo4J user name
 -v,--verbosity               Verbose output
    --version                 Show version
```

For detailed instructions and help regarding the different options please refer to the [wiki](./wiki).

**Importing large ontologies**:

By default the OWLAPI XML loader has a 64,000 triple limit. To increase the limit and being able to import larger ontologies start `owl2neo4j.jar` with `DentityExpansionLimit=<LARGE_NUMBER>` flag like so:

```
$ java -jar -DentityExpansionLimit=1000000 ./dist/owl2neo4j.jar -o extra-large-pizza.owl -n "Extra Large Pizza Ontology" -a elpo
```

Be sure that you have enough RAM to theoretical load _1000000_ (or any other number), otherwise your system will complain.

[gradle]:https://gradle.org/
[jre7]:http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html
[neo4j]:http://neo4j.com/
[owl]:www.w3.org/2004/OWL/
