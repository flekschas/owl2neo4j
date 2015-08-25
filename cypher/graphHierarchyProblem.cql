// The problem with a graph hierarchy is that a class can be a sub-class of two
// other classes. This makes the hierarchy a graph rather than a tree. For
// hierarchy visualizations this is a problem as they rely on the tree
// structure.

MERGE (root:graphHierarchyTest {name:'root'})
MERGE (a:graphHierarchyTest {name:'a'})
MERGE (b:graphHierarchyTest {name:'b'})
MERGE (c:graphHierarchyTest {name:'c'})
MERGE (d:graphHierarchyTest {name:'d'})
MERGE (e:graphHierarchyTest {name:'e'})
MERGE (a)<-[:subClassOf]-(c)
MERGE (b)<-[:subClassOf]-(d)
MERGE (b)<-[:subClassOf]-(e)
MERGE (root)<-[:subClassOf]-(a)
MERGE (root)<-[:subClassOf]-(b)
MERGE (a)<-[:subClassOf]-(d)
