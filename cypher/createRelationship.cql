// Relationship should be namespaced using their original ontology's
// abbreviation.
// Example: rdfs:label
//
// Helper relationships which are not related to any ontology should not be
// prefixed
// Example: supClassOf
//
// Node: Backticks (`) escape relationship types

MATCH (src:Class {name:'owl:cell'}), (dest:Class {name:'owl:thing'})
MERGE (src)-[:`rdfs:subClassOf`]->(dest)
MERGE (dest)-[:`supClassOf`]->(src)
