// Creates a new node or matches existing node, referenced by _n_, with label
// _Class_.
//
// The nodes must have a unique name which is compose of the Onology's
// abbreviation, an optional namespace separator, followed by a colon and the
// class name without the Ontology's name and optional namespace separator.
//
// Example:
// Ontology name: http://www.w3.org/2002/07/owl
// Ontology abbr: owl
// Ontology namespaceSeparartor: #
// Class URI: http://www.w3.org/2002/07/owl#thing
// Class name --> owl:thing

MERGE (n:Class {name:'<prefix>:<ontId>'})
