**v0.7.2**

- Fix minor time logging issue
- Test Java 11

**v0.7.1**

- Force ontology file path to start with either `/` or `./`
- Test Java 8

**v0.7.0**

- Enable importing ontologies with an extensive class hierarchy

**v0.6.1**

- Fix a bug when the relative path to the ontology directory is "."

**v0.6.0**

- Scan local directory for other OWL files prior to importing whose files form a remote source. Cleaned up a bit.

**v0.5.0**

- Added an option to include OWL import closures during Neo4J import.

**v0.4.0**

- Added option for batch imports.

**v0.3.5**

- Fixed an issue which broke node labelling.

**v0.3.4**

- Fixed issue when merging an existing node with a new label and a bit of code clean-up.

**v0.3.3**

- Force uppercase ID space for ontology IDs. E.g. OWL:Thing instead of owl:Thing. Check super classes for top nodes, i.e. owl:Thing and OWL classes which are equivalent to owl:Thing, and add a proper relation.

**v0.3.2**

- Now extracting versionIri if available. Otherwise only minor enhancements.

**v0.3.1**

- Fixed critical bug that prevented the tool from execution when no --eqp was passed. Furthermore, fixed minor bugs related to the verbose output, remove code related to OWLIndividuals (not supported) and fixed wrong version number when calling --version.

**v0.3.0**

- Added extraction of existential quantification properties via flag `--eqp <URI>`. Bug fixes. Prettification.

**v0.2.0**

- Mostly bug fixes and enhancements.

**v0.1.0**

- Intial working version.
