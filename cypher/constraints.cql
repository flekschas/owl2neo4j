// Constraints
// http://neo4j.com/docs/stable/query-constraints.html
//
// The URI should always be unique. In some cases it makes sense to force a
// unique name as well.
//
// Class names follow the schema below:
// Schema: <Ontology Abbreviation>:<Ontology ID>
// E.g.: owl:thing

CREATE INDEX on :Class(name);
CREATE CONSTRAINT on (n:Class) ASSERT n.uri IS UNIQUE;

CREATE CONSTRAINT on (n:Ontology) ASSERT n.acronym IS UNIQUE;
CREATE CONSTRAINT on (n:Ontology) ASSERT n.uri IS UNIQUE;

CREATE CONSTRAINT on (n:User) ASSERT n.id IS UNIQUE;

CREATE CONSTRAINT on (n:DataSet) ASSERT n.id IS UNIQUE;
