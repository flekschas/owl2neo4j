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
import org.apache.commons.lang3.StringUtils;

/** Jersey RESTful client */
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;

/** JSON **/
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Owl2Graph {

    private static String REST_ENDPOINT = "/db/data";
    private static String TRANSACTION_ENDPOINT = "/db/data/transaction";

    public static String ROOT_ONTOLOGY = "owl";
    public static String ROOT_CLASS = "Thing";
    public static String ROOT_CLASS_ONT_ID = ROOT_ONTOLOGY + ":" + ROOT_CLASS;
    public static String ROOT_CLASS_URI = "http://www.w3.org/2002/07/owl#" + ROOT_CLASS;

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
    private OWLDataFactory datafactory;
    private String ontUri;

    private Logger cqlLogger;
    private FileHandler fh;
    private Boolean verbose_output = false;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public static final String VERSION = "0.1.0";

    // Inline class handling labels
    public class Label {
        private String text;
        private String lang;

        public Label (String text, String lang) {
            this.text = text;
            this.lang = lang;
        }

        public String getLang () {
            return this.lang;
        }

        public String getText () {
            return this.text;
        }

        public void setLang (String lang) {
            this.lang = lang;
        }

        public void setText (String text) {
            this.text = text;
        }
    }

    public static void main(String[] args) {
        Owl2Graph ont = new Owl2Graph(args);

        Unirest.setDefaultHeader("Content-type", "application/json");
        Unirest.setDefaultHeader("Accept", "application/json; charset=UTF-8");
        // Yields better performance and reduces memory load on the Neo4J server
        // http://neo4j.com/docs/stable/rest-api-streaming.html
        Unirest.setDefaultHeader("X-Stream", "true");
        Unirest.setDefaultHeader(
            "Authorization", ont.neo4j_authentication_header
        );

        // Test if server is available
        try {
            HttpResponse<JsonNode> response = Unirest.get(
                ont.server_root_url
            ).asJson();
            System.out.println(
                "Neo4J status: " + Integer.toString(response.getStatus())
            );
        } catch (Exception e) {
            print_error("Error querying Neo4J server root URL");
            print_error(e.getMessage());
            System.exit(1);
        }

        // Try authentication
        try {
            HttpResponse<JsonNode> response = Unirest.get(
                ont.server_root_url + REST_ENDPOINT
            ).asJson();
            System.out.println(
                "REST endpoint status: " +
                Integer.toString(response.getStatus())
            );
        } catch (Exception e) {
            print_error("Error querying Neo4J REST endpoint");
            print_error(e.getMessage());
            System.exit(1);
        }

        long loadTimeSec = -1;
        double loadTimeMin = -1.0;

        try {
            long start = System.nanoTime();
            ont.loadOntology();
            long end = System.nanoTime();
            loadTimeSec = TimeUnit.NANOSECONDS.toSeconds(end - start);
            loadTimeMin = TimeUnit.NANOSECONDS.toMinutes(end - start);
            System.out.println("Successfully loaded ontology");
        } catch (Exception e) {
            print_error("Error loading the ontology");
            print_error(e.getMessage());
            System.exit(1);
        }

        long importTimeSec = -1;
        double importTimeMin = -1;
        try {
            long start = System.nanoTime();
            ont.importOntology();
            long end = System.nanoTime();
            importTimeSec = TimeUnit.NANOSECONDS.toSeconds(end - start);
            importTimeMin = TimeUnit.NANOSECONDS.toMinutes(end - start);
            System.out.println("Successfully imported ontology");
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

        // Print some performance related numbers
        if (ont.verbose_output) {
            System.out.println("-----");
            System.out.println(
                "Load time:   " +
                Double.toString(loadTimeMin) +
                "min (" +
                Long.toString(loadTimeSec) +
                "s)"
            );
            System.out.println(
                "Import time: " +
                Double.toString(importTimeMin) +
                "min (" +
                Long.toString(importTimeSec) +
                "s)");
        }
    }

    public Owl2Graph(String[] args) {
        parseCommandLineArguments(args);
    }

    public void loadOntology() throws OWLException {
        this.manager = OWLManager.createOWLOntologyManager();
        this.documentIRI = IRI.create("file:" + this.path_to_owl);
        this.ontology = manager.loadOntologyFromOntologyDocument(documentIRI);
        this.datafactory = OWLManager.getOWLDataFactory();
        this.ontUri = this.ontology.getOntologyID().getOntologyIRI().toString();

        System.out.println("Ontology Loaded...");
        System.out.println("Document IRI: " + documentIRI);
        System.out.println("Ontology    : " + this.ontUri);
    }

    private void importOntology() throws Exception
    {
        OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration(
            progressMonitor
        );
        OWLReasoner reasoner = reasonerFactory.createReasoner(this.ontology, config);
        reasoner.precomputeInferences();

        if (!reasoner.isConsistent()) {
            throw new Exception("Ontology is inconsistent");
        }

        // Init Cypher logger
        this.cqlLogger = Logger.getLogger("Cypher:" + this.ontology_acronym);
        if (this.verbose_output) {
            try {
                // Create at most five 10MB logger.
                this.fh = new FileHandler("Cypher log for " + this.ontology_acronym + ".log", 10485760, 5);
                this.cqlLogger.addHandler(fh);
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // This blog is heavily inspired by:
        // http://neo4j.com/blog/and-now-for-something-completely-different-using-owl-with-neo4j/
        try {
            initTransaction();

            // Create a node for the ontology
            createNode(
                ONTOLOGY_NODE_LABEL,
                this.ontology_acronym,
                this.ontUri
            );
            setProperty(
                ONTOLOGY_NODE_LABEL,
                this.ontUri,
                "rdfs:label",
                this.ontology_name
            );
            setProperty(
                ONTOLOGY_NODE_LABEL,
                this.ontUri,
                "acronym",
                this.ontology_acronym
            );
            setProperty(
                ONTOLOGY_NODE_LABEL,
                this.ontUri,
                "uri",
                this.ontology.getOntologyID().getOntologyIRI().toString()
            );

            // Create root node "owl:Thing"
//            createNode(
//                CLASS_NODE_LABEL,
//                ROOT_CLASS_ONT_ID,
//                ROOT_CLASS_URI
//            );

            // Get all all ontologies being imported via `owl:import` plus the _root_ ontology itself.
            //Set<OWLOntology> ontologies = this.ontology.getImportsClosure();


            for (OWLClass c: this.ontology.getClassesInSignature()) {
                String classString = c.toString();
                String classUri = this.extractUri(classString);
                String classOntID = this.getOntID(classUri);

                createNode(CLASS_NODE_LABEL, classOntID, classUri);

                this.storeLabel(c, classUri);

                NodeSet<OWLClass> superclasses = reasoner.getSuperClasses(c, true);

                if (superclasses.isEmpty()) {
                    createRelationship(
                        CLASS_NODE_LABEL,
                        classUri,
                        CLASS_NODE_LABEL,
                        ROOT_CLASS_URI,
                        "rdfs:subClassOf"
                    );
                } else {
                    for (Node<OWLClass> parentOWLNode: superclasses) {
                        OWLClassExpression parent = parentOWLNode.getRepresentativeElement();
                        String parentString = parent.toString();
                        String parentUri = this.extractUri(parentString);
                        String parentOntID = this.getOntID(parentUri);

                        createNode(
                            CLASS_NODE_LABEL,
                            parentOntID,
                            parentUri
                        );
                        createRelationship(
                            CLASS_NODE_LABEL,
                            classUri,
                            CLASS_NODE_LABEL,
                            parentUri,
                            "rdfs:subClassOf"
                        );
                    }
                }

                Set<OWLClass> equivalentClasses = getEquivalentClasses(reasoner, c);

                for (OWLClass ec : equivalentClasses) {
                    String ecString = ec.toString();
                    String ecUri = this.extractUri(ecString);
                    String ecOntID = this.getOntID(ecUri);

                    if (!ecUri.equals(classUri)) {
                        createNode(
                            CLASS_NODE_LABEL,
                            ecOntID,
                            ecUri
                        );

                        createRelationship(
                            CLASS_NODE_LABEL,
                            ecUri,
                            CLASS_NODE_LABEL,
                            classUri,
                            "owl:equivalentClass"
                        );
                    }

                }

                for (Node<OWLNamedIndividual> in: reasoner.getInstances(c, true)) {
                    OWLNamedIndividual i = in.getRepresentativeElement();
                    String indString = i.toString();
                    String indUri = this.extractUri(indString);
                    String indOntID = this.getOntID(indUri);

                    createNode(
                        INDIVIDUAL_NODE_LABEL,
                        indOntID,
                        indUri
                    );
                    createRelationship(
                        INDIVIDUAL_NODE_LABEL,
                        indUri,
                        CLASS_NODE_LABEL,
                        classUri,
                        "rdf:type"
                    );

                    for (OWLObjectPropertyExpression objectProperty:
                        ontology.getObjectPropertiesInSignature()) {
                        for
                        (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual>
                        object: reasoner.getObjectPropertyValues(i,
                        objectProperty)) {
                            // Get Relationship name
                            String relString = objectProperty.toString();
                            String relUri = this.extractUri(relString);
                            String relOntID = this.getOntID(relUri);

                            // Create a meta node for the potentially new Relationship
                            createNode(
                                RELATIONSHIP_NODE_LABEL,
                                relOntID,
                                relUri
                            );

                            // Get related Individual
                            String relIndString = object.getRepresentativeElement().toString();
                            String relIndUri = this.extractUri(relIndString);

                            // Connect both individuals
                            createRelationship(
                                INDIVIDUAL_NODE_LABEL,
                                indUri,
                                INDIVIDUAL_NODE_LABEL,
                                relIndUri,
                                relOntID
                            );
                        }
                    }
                    for (OWLDataPropertyExpression dataProperty :
                        ontology.getDataPropertiesInSignature()) {
                        for (OWLLiteral object: reasoner.getDataPropertyValues(
                            i, dataProperty.asOWLDataProperty())) {
                            String propertyString =
                                dataProperty.asOWLDataProperty().toString();
                            String propertyUri = this.extractUri(propertyString);
                            String propertyOntID = this.getOntID(propertyUri);
                            String propertyValue = object.toString();

                            createNode(
                                PROPERTY_NODE_LABEL,
                                propertyOntID,
                                propertyUri
                            );
                            setProperty(
                                INDIVIDUAL_NODE_LABEL,
                                indUri,
                                propertyOntID,
                                propertyValue
                            );
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

    public String extractUri (String classString) {
        String classUri = classString;
        int openingAngleBracketPos = classString.indexOf("<");
        int closingAngleBracketPos = classString.lastIndexOf(">");
        try {
            if (openingAngleBracketPos >= 0 && closingAngleBracketPos >= 0) {
                classUri = classString.substring(
                    classString.indexOf("<") + 1,
                    classString.lastIndexOf(">")
                );
            }
        } catch (Exception e) {
            print_error("Couldn't extract URI of '" + classString + "'");
            print_error(e.getMessage());
            System.exit(1);
        }
        return classUri;
    }

    public String getOntID (String classUri) {
        String idSpace = "";
        String classOntID = classUri;
        // First extract the substring after the last slash to avoid possible
        // conflicts
        if (classOntID.contains("/")) {
            int lastSlash = classOntID.lastIndexOf("/");
            if (lastSlash >= 0) {
                String tmp = classOntID.substring(lastSlash);
                if (tmp.length() == 1) {
                    tmp = classOntID.substring(0, lastSlash);
                    lastSlash = tmp.lastIndexOf("/");
                    if (lastSlash >= 0) {
                        tmp = tmp.substring(lastSlash);
                    }
                }
                if (tmp.length() > 1) {
                    classOntID = tmp.substring(1);
                }
            }
        }
        // OWL IDs start with `#` so we extract everything after that.
        int hashPos = classOntID.indexOf("#");
        if (hashPos >= 0 && hashPos + 1 != classOntID.length()) {
            classOntID = classOntID.substring(
                hashPos + 1
            );
            if (this.ontUri.equals(classUri.substring(0, classUri.indexOf("#")))) {
                idSpace = this.ontology_acronym;
            }
        }
        // If the string contains an underscore than it is most likely an OBO ontology converted to OWL. The prefix is
        // different in this case. We will use the ID space of OBO.
        // For more details: http://www.obofoundry.org/id-policy.shtml
        int underscorePos = classOntID.indexOf("_");
        if (underscorePos >= 0 && underscorePos + 1 != classOntID.length()) {
            if (idSpace.length() == 0) {
                idSpace = classOntID.substring(
                    0,
                    underscorePos
                );
            }
            classOntID = classOntID.substring(underscorePos + 1);
        }
        if (idSpace.length() > 0) {
            idSpace = idSpace + ":";
        }
        return idSpace + classOntID;
    }

    private Label getLabel (OWLClass c, OWLOntology ont) {
        Label classLabel = new Label(null, null);
        for (OWLAnnotation annotation : c.getAnnotations(ont, this.datafactory.getRDFSLabel())) {
            if (annotation.getValue() instanceof OWLLiteral) {
                OWLLiteral val = (OWLLiteral) annotation.getValue();

                classLabel.setText(val.getLiteral().replace("'", "\\'"));
                classLabel.setLang(val.getLang());
            }
        }
        return classLabel;
    }

    private void storeLabel (OWLClass c, String classUri) {
        Label classLabel = this.getLabel(c, this.ontology);

        if (StringUtils.isEmpty(classLabel.text)) {
            Set<OWLOntology> importedOntologies = this.ontology.getImports();
            for (OWLOntology ont: importedOntologies) {
                classLabel = this.getLabel(c, ont);
                if (StringUtils.isNotEmpty(classLabel.text)) {
                    break;
                }
            }
        }

        if (StringUtils.isNotEmpty(classLabel.text)) {
            setProperty(
                CLASS_NODE_LABEL,
                classUri,
                "rdfs:label",
                classLabel.text
            );
        }

        if (StringUtils.isNotEmpty(classLabel.lang)) {
            setProperty(
                CLASS_NODE_LABEL,
                classUri,
                "labelLang",
                classLabel.lang
            );
        }
    }

    private Set<OWLClass> getEquivalentClasses (OWLReasoner reasoner, OWLClass c) {
        Node<OWLClass> equivalentClasses = reasoner.getEquivalentClasses(c);
        Set<OWLClass> results;
        if (!c.isAnonymous()) {
            results = equivalentClasses.getEntities();
        } else {
            results = equivalentClasses.getEntitiesMinus(c.asOWLClass());
        }
        return results;
    }

    private void initTransaction () {
        // Fire empty statement to initialize transaction
        try {
            HttpResponse<JsonNode> response = Unirest.post(
                this.server_root_url + TRANSACTION_ENDPOINT)
                    .body("{\"statements\":[]}")
                    .asJson();
            Headers headers = response.getHeaders();
            String location = "";
            if (headers.containsKey("location")) {
                location = headers.get("location").toString();
                this.transaction = location.substring(
                    location.lastIndexOf("/"),
                    location.length() -1
                );
                System.out.println(
                    "Transaction Sting: '" +
                    this.transaction +
                    "'"
                );
            }
            if (this.verbose_output) {
                System.out.println(
                    "Transaction initialized. Commit at " +
                    location +
                    " [Neo4J status:" +
                    Integer.toString(response.getStatus()) +
                    "]"
                );
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
            HttpResponse<JsonNode> response = Unirest.post(
                this.server_root_url + TRANSACTION_ENDPOINT + this.transaction + "/commit")
                    .body("{\"statements\":[]}")
                    .asJson();
            if (this.verbose_output) {
                System.out.println(
                    "Transaction committed. [Neo4J status:" +
                    Integer.toString(response.getStatus()) +
                    "]"
                );
            }
            JSONObject jsonResponse = response.getBody().getObject();
            JSONArray errors = (JSONArray) jsonResponse.get("errors");
            if (errors.length() > 0) {
                JSONObject error = (JSONObject) errors.get(0);
                String errorMsg = error.get("message").toString();
                if (this.verbose_output) {
                    errorMsg = response.getBody().toString();
                }
                throw new Exception(errorMsg);
            }
        } catch (Exception e) {
            print_error("Error committing transaction");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void createNode (String classLabel, String classOntID, String classUri) {
        // Uniqueness for Class nodes needs to be defined before
        // Look: cypher/constraints.cql
        // Example: cypher/createClass.cql
        try {
            String cql = "MERGE (n:" + classLabel + ":" + this.ontology_acronym + " {name:'" + classOntID + "',uri:'" + classUri + "'});";
            HttpResponse<JsonNode> response = Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction)
                .body("{\"statements\":[{\"statement\":\"" + cql + "\"}]}")
                    .asJson();
            if (this.verbose_output) {
                System.out.println("CQL: `" + cql + "` [Neo4J status:" + Integer.toString(response.getStatus()) + "]");
                this.cqlLogger.info(cql);
            }
        } catch (UnirestException e) {
            print_error("Error creating a node");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void createRelationship (String srcLabel, String srcUri, String destLabel, String destUri, String relationship) {
        // Example: cypher/createRelationship.cql
        try {
            String cql = "MATCH (src:" + srcLabel + " {uri:'" + srcUri + "'}), (dest:" + destLabel + " {uri:'" + destUri + "'}) MERGE (src)-[:`" + relationship + "`]->(dest);";
            HttpResponse<JsonNode> response = Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction)
                    .body("{\"statements\":[{\"statement\":\"" + cql + "\"}]}")
                    .asJson();
            if (this.verbose_output) {
                System.out.println("CQL: `" + cql + "`  [Neo4J status: " + Integer.toString(response.getStatus()) + "]");
                this.cqlLogger.info(cql);
            }
        } catch (UnirestException e) {
            print_error("Error creating a relationship");
            print_error(e.getMessage());
            System.exit(1);
        }
    }

    private void setProperty (String classLabel, String classUri, String propertyName, String propertyValue) {
        // Example: cypher/setProperty.cql
        try {
            String cql = "MATCH (n:" + classLabel + " {uri:'" + classUri + "'}) SET n.`" + propertyName + "` = '" + propertyValue + "';";
            HttpResponse<JsonNode> response = Unirest.post(this.server_root_url + TRANSACTION_ENDPOINT + this.transaction)
                    .body("{\"statements\":[{\"statement\":\"" + cql + "\"}]}")
                    .asJson();
            if (this.verbose_output) {
                System.out.println("CQL: `" + cql + "` [Neo4J status: " + Integer.toString(response.getStatus()) + "]");
                this.cqlLogger.info(cql);
            }
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

        Option version = Option.builder()
            .longOpt("version")
            .desc("Show version")
            .build();

        Option verbosity = Option.builder("v")
            .longOpt("verbosity")
            .desc("Verbose output")
            .build();

        Option owl = Option.builder("o")
            .argName("Path")
            .hasArg()
            .numberOfArgs(1)
            .type(String.class)
            .required()
            .longOpt("owl")
            .desc("Path to OWL file")
            .build();

        Option name = Option.builder("n")
            .argName("String")
            .hasArg()
            .numberOfArgs(1)
            .type(String.class)
            .required()
            .longOpt("name")
            .desc("Ontology name (E.g. Gene Ontology)")
            .build();

        Option acronym = Option.builder("a")
            .argName("String")
            .hasArg()
            .numberOfArgs(1)
            .type(String.class)
            .required()
            .longOpt("abbreviation")
            .desc("Ontology abbreviation (E.g. go)")
            .build();

        Option server = Option.builder("s")
            .argName("URL")
            .hasArg()
            .numberOfArgs(1)
            .type(String.class)
            .longOpt("server")
            .desc("Neo4J server root URL [Default: http://localhost:7474]")
            .build();

        Option user = Option.builder("u")
            .argName("String")
            .hasArg()
            .numberOfArgs(1)
            .type(String.class)
            .longOpt("user")
            .desc("Neo4J user name")
            .build();

        Option password = Option.builder("p")
            .argName("String")
            .hasArg()
            .numberOfArgs(1)
            .type(String.class)
            .longOpt("password")
            .desc("Neo4J user password")
            .build();

        all_options.addOption(help);
        all_options.addOption(version);
        all_options.addOption(verbosity);
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
        call_options.addOption(verbosity);

        try {
            // Parse only for meta options, e.g. `-h` and `-v`
            cl = new DefaultParser().parse(meta_options, args, true);
            if (cl.getOptions().length > 0) {
                if (cl.hasOption("h")) {
                    usage(all_options);
                }
                if (cl.hasOption("version")) {
                    System.out.println(VERSION);
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
            this.ontology_name = cl.getOptionValue("n");
            this.ontology_acronym = cl.getOptionValue("a");
            this.server_root_url = cl.getOptionValue("s", "http://localhost:7474");
            this.neo4j_authentication_header = "Basic: " + Base64.encodeBase64String((cl.getOptionValue("u") + ":" + cl.getOptionValue("p")).getBytes());

            if (cl.hasOption("v")) {
                this.verbose_output = true;
            }
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
        formatter.printHelp("java -jar owl2neo4j.jar", header, options, footer, true);
    }

    /**
     * Prints error message in red.
     */
    public static void print_error(String message) {
        System.err.println(ANSI_RED + message + ANSI_RESET);
    }
}
