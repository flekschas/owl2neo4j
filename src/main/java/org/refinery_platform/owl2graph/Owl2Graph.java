package org.refinery_platform.owl2graph;

/** OWL API */
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.apibinding.OWLManager;

/** Reasoner */
import org.semanticweb.HermiT.Reasoner;

/** Apache commons */
import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Base64;

/** Jersey RESTful client */
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;


public class Owl2Graph {

    private static String REST_ENDPOINT = "/db/data";
    private static String TRANSACTION_ENDPOINT = "/db/data/transaction";

    public static String ROOT_ONTOLOGY = "owl";
    public static String ROOT_CLASS = "Thing";
    public static String ROOT_CLASS_NAME = ROOT_ONTOLOGY + ":" + ROOT_CLASS;

    // Graph related nodes
    private static String CLASS_NODE_LABEL = "Class";
    private static String INDIVIDUAL_NODE_LABEL = "Individual";
    // Meta data related nodes
    private static String ONTOLOGY_NODE_LABEL = "Ontology";
    private static String RELATIONSHIP_NODE_LABEL = "Relationship";
    private static String PROPERTY_NODE_LABEL = "Property";

    private String path_to_owl;
    private String ontology_name;
    private String ontology_acronym;
    private String server_root_url;
    private String neo4j_authentication_header;
    private String transaction;

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private IRI documentIRI;

    public static void main(String[] args) {
        Owl2Graph ont = new Owl2Graph(args);

        Unirest.setDefaultHeader("Content-type", "application/json");
        Unirest.setDefaultHeader("Accept", "application/json; charset=UTF-8");
        // Yields better performance and reduces memory load on the Neo4J server
        // http://neo4j.com/docs/stable/rest-api-streaming.html
        Unirest.setDefaultHeader("X-Stream", "true");
        Unirest.setDefaultHeader("Authorization", ont.neo4j_authentication_header);

        // Test if server is available
        try {
            HttpResponse<JsonNode> response = Unirest.get(ont.server_root_url).asJson();
            System.out.println("Neo4J status: " + Integer.toString(response.getStatus()));
        } catch (UnirestException e) {
            print_error("Error querying Neo4J server root URL");
            print_error(e.getMessage());
            System.exit(1);
        }

        // Try authentication
        try {
            HttpResponse<JsonNode> response = Unirest.get(ont.server_root_url + REST_ENDPOINT).asJson();
            System.out.println("REST endpoint status: " + Integer.toString(response.getStatus()));
        } catch (UnirestException e) {
            print_error("Error querying Neo4J REST endpoint");
            print_error(e.getMessage());
            System.exit(1);
        }

        try {
            ont.loadOntology();
        } catch (OWLException e) {
            print_error("Error loading the ontology");
            print_error(e.getMessage());
            System.exit(1);
        }
        try {
            ont.importOntology();
        } catch (Exception e) {
            print_error("Error importing the ontology");
            print_error(e.getMessage());
            System.exit(1);
        }

        // Unirest has be closed explicitly
        try {
            Unirest.shutdown();
        } catch (Exception e) {
            print_error("Error shutting down Unirest");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    public Owl2Graph(String[] args) {
        parseCommandLineArguments(args);
    }

    public void loadOntology() throws OWLException {
        this.manager = OWLManager.createOWLOntologyManager();
        this.documentIRI = IRI.create(this.path_to_owl);
        this.ontology = manager.loadOntologyFromOntologyDocument(documentIRI);

        System.out.println("Ontology Loaded...");
        System.out.println("Document IRI: " + documentIRI);
        System.out.println("Ontology    : " + ontology.getOntologyID());
        System.out.println("Format      : " + manager.getOntologyFormat(ontology));
    }

    private void importOntology() throws Exception
    {
        OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, config);
        reasoner.precomputeInferences();

        // OWLReasoner reasoner = new Reasoner(ontology);

        if (!reasoner.isConsistent()) {
            //throw your exception of choice here
            throw new Exception("Ontology is inconsistent");
        }

        try {
            initTransaction();

            // Create a node for the ontology
            createNode(ONTOLOGY_NODE_LABEL, this.ontology_acronym);
            setProperty(ONTOLOGY_NODE_LABEL, this.ontology_acronym, "rdfs:label", this.ontology_name);
            setProperty(ONTOLOGY_NODE_LABEL, this.ontology_acronym, "uri", ontology.getOntologyID().getOntologyIRI().toString());

            // Create root node "owl:Thing"
            createNode(CLASS_NODE_LABEL, ROOT_CLASS_NAME);

            for (OWLClass c :ontology.getClassesInSignature(true)) {
                String classString = c.toString();
                String className = "";
                if (classString.contains("#")) {
                    classString = classString.substring(
                        classString.indexOf("#") + 1,classString.lastIndexOf(">"));
                    className = this.uniqueClassName(this.ontology_acronym, classString);
                }

                createNode(CLASS_NODE_LABEL, className);

                NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(c, true);

                if (superclasses.isEmpty()) {
                    createRelationship(CLASS_NODE_LABEL, className, CLASS_NODE_LABEL, ROOT_CLASS_NAME, "rdfs:subClassOf");
                } else {
                    for (org.semanticweb.owlapi.reasoner.Node<OWLClass>
                        parentOWLNode: superclasses) {
                        OWLClassExpression parent =
                            parentOWLNode.getRepresentativeElement();
                        String parentString = parent.toString();
                        String parentName = "";
                        if (parentString.contains("#")) {
                            parentString = parentString.substring(
                                parentString.indexOf("#")+1,
                                parentString.lastIndexOf(">"));
                            parentName = this.uniqueClassName(this.ontology_acronym, parentString);
                        }
                        createNode(CLASS_NODE_LABEL, parentName);
                        createRelationship(CLASS_NODE_LABEL, className, CLASS_NODE_LABEL, parentName, "rdfs:subClassOf");
                    }
                }

                for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> in
                    : reasoner.getInstances(c, true)) {
                    OWLNamedIndividual i = in.getRepresentativeElement();
                    String indString = i.toString();
                    String indName = "";
                    if (indString.contains("#")) {
                        indString = indString.substring(
                            indString.indexOf("#")+1,indString.lastIndexOf(">"));
                        indName = this.uniqueClassName(className, indString);
                    }
                    createNode(INDIVIDUAL_NODE_LABEL, indName);
                    createRelationship(INDIVIDUAL_NODE_LABEL, indName, CLASS_NODE_LABEL, className, "a");

                    for (OWLObjectPropertyExpression objectProperty:
                        ontology.getObjectPropertiesInSignature()) {
                        for
                        (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual>
                        object: reasoner.getObjectPropertyValues(i,
                        objectProperty)) {
                            // Get Relationship name
                            String relString = objectProperty.toString();
                            relString = relString.substring(relString.indexOf("#")+1,
                                    relString.lastIndexOf(">"));
                            String relName = uniqueClassName(this.ontology_acronym, relString);

                            // Create a meta node for the potentially new Relationship
                            createNode(RELATIONSHIP_NODE_LABEL, relName);

                            // Get related Individual
                            String relIndString = object.getRepresentativeElement().toString();
                            relIndString = relIndString.substring(relIndString.indexOf("#") + 1,
                                    relIndString.lastIndexOf(">"));
                            String relIndName = uniqueClassName(INDIVIDUAL_NODE_LABEL, relIndString);

                            // Connect both individuals
                            createRelationship(INDIVIDUAL_NODE_LABEL, indName, INDIVIDUAL_NODE_LABEL, relIndName, relName);

                            // Node objectNode = getOrCreateClass(s);
                            // individualNode.createRelationshipTo(objectNode,
                            //     DynamicRelationshipType.withName(reltype));
                        }
                    }
                    for (OWLDataPropertyExpression dataProperty:
                        ontology.getDataPropertiesInSignature()) {
                        for (OWLLiteral object: reasoner.getDataPropertyValues(
                            i, dataProperty.asOWLDataProperty())) {
                            String propertyString =
                                dataProperty.asOWLDataProperty().toString();
                            propertyString = propertyString.substring(propertyString.indexOf("#")+1,
                                    propertyString.lastIndexOf(">"));
                            String propertyValue = object.toString();
                            String propertyName = uniqueClassName(this.ontology_acronym, propertyString);

                            createNode(PROPERTY_NODE_LABEL, propertyName);
                            setProperty(INDIVIDUAL_NODE_LABEL, indName, propertyName, propertyValue);
                        }
                    }
                }
            }
            commitTransaction();
        } catch (Exception e) {
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    public String uniqueClassName (String ontAcronym, String className) {
        return ontAcronym + ":" + className;
    }

    private void initTransaction () {
        HttpResponse<JsonNode> jsonResponse;
        Headers headers;

        // Fire empty statement to initialize transaction
        try {
            jsonResponse = Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT)
                    .body("{\"statements\":[]}")
                    .asJson();
            headers = jsonResponse.getHeaders();
            if (headers.containsKey("location")) {
                String location = headers.get("location").toString();
                this.transaction = location.substring(location.lastIndexOf("/"));
            }
        } catch (Exception e) {
            print_error("Error starting transaction");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void commitTransaction () {
        // Fire empty statement to initialize transaction
        try {
            Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction + "/commit")
                    .body("{\"statements\":[]}")
                    .asJson();
        } catch (Exception e) {
            print_error("Error committing transaction");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void createNode (String classLabel, String className) {
        // Uniqueness for Class nodes needs to be defined before
        // Look: cypher/constraints.cql
        // Example: cypher/createClass.cql
        try {
            Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction)
                    .body("{\"statements\":[{\"statement\":\"CREATE (n:" + classLabel + " {name:'" + className + "'})\"}]}")
                    .asJson();
        } catch (UnirestException e) {
            print_error("Error creating a node");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void createRelationship (String srcClassName, String srcName, String destClassName, String destName, String relationship) {
        // Example: cypher/createRelationship.cql
        try {
            Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction)
                    .body("{\"statements\":[" +
                            "{\"statement\":\"" +
                              "MATCH (src:" + srcClassName + " {name:'" + srcName + "'}), (dest:" + destClassName + " {name:'" + destName + "'})" +
                              "CREATE (src)-[:`" + relationship + "`]->(dest)" +
                            "\"}" +
                            "]}")
                    .asJson();
        } catch (UnirestException e) {
            print_error("Error creating a relationship");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void setProperty (String classLabel, String className, String propertyName, String propertyValue) {
        // Example: cypher/setProperty.cql
        try {
            Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction)
                    .body("{\"statements\":[" +
                            "{\"statement\":\"" +
                              "MATCH (n:" + classLabel + " {uri:" + className + "}) SET n." + propertyName + " = '" + propertyValue + "'" +
                            "\"}" +
                            "]}")
                    .asJson();
        } catch (UnirestException e) {
            print_error("Error creating a node property");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Command line parser
     * @param args
     */
    private void parseCommandLineArguments(String[] args)
    {
        CommandLine cl;

        Options meta_options = new Options();
        Options call_options = new Options();
        Options all_options = new Options();

        Option help = Option.builder("h")
                .longOpt("help")
                .desc("Shows this help")
                .build();

        Option version = Option.builder("v")
                .longOpt("version")
                .desc("Show version")
                .build();

        Option owl = Option.builder("o")
                .hasArg()
                .numberOfArgs(1)
                .type(String.class)
                .required()
                .longOpt("owl")
                .desc("Path to OWL file")
                .build();

        Option name = Option.builder("n")
                .hasArg()
                .numberOfArgs(1)
                .type(String.class)
                .required()
                .longOpt("name")
                .desc("Ontology name (E.g. Gene Ontology)")
                .build();

        Option acronym = Option.builder("a")
                .hasArg()
                .numberOfArgs(1)
                .type(String.class)
                .required()
                .longOpt("abbreviation")
                .desc("Ontology abbreviation (E.g. go)")
                .build();

        Option server = Option.builder("s")
                .hasArg()
                .numberOfArgs(1)
                .type(String.class)
                .required()
                .longOpt("server")
                .desc("Neo4J server root URL")
                .build();

        Option user = Option.builder("u")
                .hasArg()
                .numberOfArgs(1)
                .type(String.class)
                .required()
                .longOpt("user")
                .desc("Neo4J user")
                .build();

        Option password = Option.builder("p")
                .hasArg()
                .numberOfArgs(1)
                .type(String.class)
                .required()
                .longOpt("password")
                .desc("Neo4J user password")
                .build();

        all_options.addOption(help);
        all_options.addOption(version);
        all_options.addOption(owl);
        all_options.addOption(name);
        all_options.addOption(acronym);
        all_options.addOption(server);
        all_options.addOption(user);
        all_options.addOption(password);

        meta_options.addOption(help);
        meta_options.addOption(version);

        call_options.addOption(owl);
        call_options.addOption(name);
        call_options.addOption(acronym);
        call_options.addOption(server);
        call_options.addOption(user);
        call_options.addOption(password);

        try {
            // Parse only for meta options, e.g. `-h` and `-v`
            cl = new DefaultParser().parse(meta_options, args, true);
            if (cl.getOptions().length > 0) {
                if (cl.hasOption("h")) {
                    usage(all_options);
                }
                if (cl.hasOption("v")) {
                    System.out.println("0.0.1");
                }
                // Exit the program whenever a meta option was found as meta and call options should be mutually exclusive
                System.exit(0);
            }
        }  catch (ParseException e) {
            print_error("Error parsing command line meta options");
            print_error(e.getMessage());
            System.out.println("\n");
            usage(all_options);
            System.exit(1);
        }

        try {
            cl = new DefaultParser().parse(call_options, args);
            this.path_to_owl = cl.getOptionValue("o");
            this.server_root_url = cl.getOptionValue("s").substring(0, cl.getOptionValue("s").length() - (cl.getOptionValue("s").endsWith("/") ? 1 : 0));
            this.ontology_name = cl.getOptionValue("n");
            this.ontology_acronym = cl.getOptionValue("a");
            this.neo4j_authentication_header = "Basic: " + Base64.encodeBase64String((cl.getOptionValue("u") + ":" + cl.getOptionValue("p")).getBytes());
        }  catch (ParseException e) {
            print_error("Error parsing command line call options");
            print_error(e.getMessage());
            System.out.println("\n");
            usage(all_options);
            System.exit(1);
        }
    }

    /**
     * Prints a usage message to the console.
     */
    public static void usage(Options options) {
        String header = "Import OWL into Neo4J as a labeled property graph.\n\n";
        String footer = "\nPlease report issues at http://github.com/flekschas/owl2neo4j/issues";

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar Owl2Graph.jar", header, options, footer, true);
    }

    /**
     * Prints error message in red.
     */
    public static void print_error(String message) {
        System.err.println((char)27 + "[31m" + message + (char)27 + "[0m");
    }
}
