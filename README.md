# OWL 2 Neo4J [![Build Status](https://travis-ci.org/flekschas/owl2neo4j.svg?branch=master)](https://travis-ci.org/flekschas/owl2neo4j)

Convert [OWL](owl) schema ontologies to labeled property graph and import into [Neo4J](neo4j).

_Note: currently the tool only converts the class hierarchy; instances are ignored for now._

## Build

Each release comes with a precompiled JAR, created automatically by Travis-CI. To build the JAR file manually follow the three steps below. The JAR file will be created in `dist`.

**Requirements**:

* [Java RE 7](jre7) or [Java RE 8](jre8)
* [Gradle](gradle)

```
git clone https://github.com/flekschas/owl2neo4j && cd owl2neo4j
gradle build
```

## Import ontology

**Requirements**:

* [Neo4J](neo4j)

A single file import is executed as follows:

```
java -jar ./dist/owl2neo4j.jar -o ./pizza.owl -n "Pizza Ontology" -a pizza
```

(To get `pizza.owl` run `curl -O https://protege.stanford.edu/ontologies/pizza/pizza.owl`.)

In order to import multiple ontologies at once create a JSON file:

```
{
	"server": "http://my-server:7474",
    "ontologies": [
        {
            "o": "./chebi.owl",
            "n": "Chemical Entities of Biological Interest",
            "a": "CHEBI"
        },
        {
            "o": "./cl.owl",
            "n": "Cell Ontology",
            "a": "CL",
            "i": true
        },
        {
            "o": "./efo.owl",
            "n": "Experimental Factor Ontology",
            "a": "EFO"
        }
    ]
}
```

`a`, `i`, `n` and `o` correspond to the CLI options. The only difference is that n, i.e. the path to the OWL file to be imported, should be relative to the JSON file. `server` is optional; when it's not defined, the default server url, i.e. `http://127.0.0.1:7474`, is used.
Next you can import the everything with the following call:

```
$ java -jar ./dist/owl2neo4j.jar -b ./import.json
```

(Assuming that the file above is `./import.json`.)

**Command line options**:

```
usage: java -jar owl2neo4j.jar -a <String> -b <Path> [--eqp <String>] [-h]
       [-i] [-l] -n <String> -o <Path> [-p <String>] [-s <URL>] [-u
       <String>] [-v] [--version]
Import OWL into Neo4J as a labeled property graph.

 -a,--abbreviation <String>   Ontology abbreviation (E.g. go)
 -b,--batch <Path>            Path to JSON file
    --eqp <String>            Existential quantification property (E.g.
                              http://www.co-ode.org/ontologies/pizza/pizza
                              .owl#hasTopping)
 -h,--help                    Shows this help
 -i,--incl-imports            Include import closure
 -l,--no-local                Don't scan for locally available OWL files
                              to ensure loading remote files.
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
java -jar -DentityExpansionLimit=1000000 ./dist/owl2neo4j.jar -o extra-large-pizza.owl -n "Extra Large Pizza Ontology" -a elpo
```

Be sure that you have enough RAM to theoretical load _1000000_ (or any other number), otherwise your system will complain.

**Handshake error**:

In case you see a _handshake_ error of this form `Remote host closed connection during handshake` add the following parameter when calling java:

```
java -jar -Dhttps.protocols=TLSv1.1,TLSv1.2 ...
```

[gradle]:https://gradle.org/
[jre7]:http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html
[jre8]:http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html
[neo4j]:http://neo4j.com/
[owl]:www.w3.org/2004/OWL/
