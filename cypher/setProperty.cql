// Set a property
//
// nodeLabel: [Class, Individual, Ontology, Property, Relationship]
//  Example: Class
//
// nodeName: <prefix>[:<prefix>]:<name>
//  Example: efo:cellLine:h9
//
// propertyName: <prefix>[:<prefix>]:<name>
//  Example: rdfs:label
//
// value: String, Integer, Float
//  Example: 'whatever'

MATCH (n:<nodeLabel> {uri:'<nodeName>'})
SET n.<propertyName> = '<value>';
