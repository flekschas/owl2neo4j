// Constraints
// http://neo4j.com/docs/stable/query-constraints.html
//
// The URI should always be unique. In some cases it makes sense to force a
// unique name as well.
//
// Class names follow the schema below:
// Schema: <Ontology Abbreviation>:<Ontology ID>
// E.g.: owl:thing

CREATE CONSTRAINT on (n:Class) ASSERT n.uri IS UNIQUE;

CREATE CONSTRAINT on (n:Individual) ASSERT n.uri IS UNIQUE;

CREATE CONSTRAINT on (n:Ontology) ASSERT n.uri IS UNIQUE;
CREATE CONSTRAINT on (n:Ontology) ASSERT n.name IS UNIQUE;

CREATE CONSTRAINT on (n:Relationship) ASSERT n.uri IS UNIQUE;
CREATE CONSTRAINT on (n:Relationship) ASSERT n.name IS UNIQUE;

CREATE CONSTRAINT on (n:Property) ASSERT n.uri IS UNIQUE;
CREATE CONSTRAINT on (n:Property) ASSERT n.name IS UNIQUE;
