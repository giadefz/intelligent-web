import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import com.google.common.util.concurrent.CycleDetectingLockFactory;
import org.apache.jena.ext.com.google.common.graph.MutableValueGraph;
import org.apache.jena.ext.com.google.common.graph.ValueGraphBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.VCARD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;

public class LoadAndSaveOntologyTest {

    private OWLOntologyManager manager;
    private OWLOntology ontology;

    private OWLDataFactory dataFactory;

    @BeforeEach
    void setUp() throws OWLOntologyCreationException, FileNotFoundException {
        this.manager = OWLManager.createOWLOntologyManager();
//        this.ontology = loadFromFile(manager, "simpleontology.txt");
//        this.ontology = loadFromFile(manager, "otherontology.txt");
//        this.ontology = loadFromFile(manager, "ontont.txt");
        this.ontology = loadFromFile(manager, "pizza.txt");
//        this.ontology = loadFromFile(manager, "ontology.txt");
//        this.ontology = loadKoalaOntology(manager);
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
    }

    @Test
    void loadKoalaOntologyTest() throws OWLOntologyCreationException {
        OWLOntology ontologyKoala = this.loadKoalaOntology(this.manager);
        ontologyKoala.logicalAxioms().forEach(System.out::println);
    }

    @Test
    void getObjectPropertyAxiomTest() {
        TableauxIndividual newIndividual = TableauxIndividualFactory.getInstance().getNewIndividual();
        TableauxIndividual newIndividual2 = TableauxIndividualFactory.getInstance().getNewIndividual();
        Optional<OWLObjectProperty> any = this.ontology.objectPropertiesInSignature()
                .findAny();
        OWLObjectProperty owlObjectProperty = any.get();
        OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom =
                this.dataFactory.getOWLObjectPropertyAssertionAxiom(owlObjectProperty, newIndividual, newIndividual2);
        owlObjectPropertyAssertionAxiom.isInSimplifiedForm();
    }

    @Test
    void reasonerTest() {
        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(this.ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        OWLClass A = this.dataFactory.getOWLClass("A");
        reasoner.getSubClasses(A);
        reasoner.isSatisfiable(A);
    }

    @Test
    void alcReasonerTest() {
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        OWLClass A = this.dataFactory.getOWLClass("A");
        OWLClass D = this.dataFactory.getOWLClass("D");
        OWLObjectProperty p = this.dataFactory.getOWLObjectProperty("P");
        OWLObjectSomeValuesFrom owlObjectSomeValuesFrom = this.dataFactory.getOWLObjectSomeValuesFrom(p, D);
        System.out.println(alcReasoner.isSatisfiable(this.dataFactory.getOWLObjectIntersectionOf(A, owlObjectSomeValuesFrom)));
    }

    @Test
    void conjunctSet() {
        OWLClass A = this.dataFactory.getOWLClass("A");
        OWLClass D = this.dataFactory.getOWLClass("D");
        OWLClass C = this.dataFactory.getOWLClass("C");
        OWLObjectIntersectionOf owlObjectIntersectionOf = this.dataFactory.getOWLObjectIntersectionOf(D, C);
        OWLObjectIntersectionOf owlObjectIntersectionOf1 = this.dataFactory.getOWLObjectIntersectionOf(A, owlObjectIntersectionOf);
        owlObjectIntersectionOf1.asConjunctSet();
    }

    private OWLOntology loadKoalaOntology(@Nonnull OWLOntologyManager manager) throws OWLOntologyCreationException {
        return manager.loadOntologyFromOntologyDocument(new StringDocumentSource(koala));
    }

    private OWLOntology loadFromFile(@Nonnull OWLOntologyManager manager, String fileName) throws OWLOntologyCreationException, FileNotFoundException {
        File file = new File(fileName);
        return manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), new OWLOntologyLoaderConfiguration());
    }

    @Test
    void alcQueryParserTest() {
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("C and D");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        System.out.println(alcReasoner.isSatisfiable(cl));
    }

    @Test
    void lazyUnfoldingTest() {
        LazyUnfoldingVisitor lazyUnfoldingVisitor = new LazyUnfoldingVisitor();
        this.ontology.logicalAxioms()
                .forEach(a -> a.accept(lazyUnfoldingVisitor));
        OWLDependencyGraph dependencyGraph = lazyUnfoldingVisitor.getDependencyGraph();
        dependencyGraph.prettyPrintGraph(this.dataFactory.getOWLClass("VeggiePizza"));

    }

    @Test
    void rdfTest() {
        Model model = ModelFactory.createDefaultModel();
        Resource testResource = model.createResource("http://locahost/root")
                .addProperty(VCARD.Family, model.createResource("http://locahost/test1").addProperty(VCARD.Family, "CoC"))
                .addProperty(VCARD.Family, "test2_label");
        model.write(System.out);
    }

    private final static String koala = "<?xml version=\"1.0\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns=\"http://protege.stanford.edu/plugins/owl/owl-library/koala.owl#\" xml:base=\"http://protege.stanford.edu/plugins/owl/owl-library/koala.owl\">\n"
            + "  <owl:Ontology rdf:about=\"\"/>\n"
            + "  <owl:Class rdf:ID=\"Female\"><owl:equivalentClass><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty><owl:hasValue><Gender rdf:ID=\"female\"/></owl:hasValue></owl:Restriction></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Marsupials\"><owl:disjointWith><owl:Class rdf:about=\"#Person\"/></owl:disjointWith><rdfs:subClassOf><owl:Class rdf:about=\"#Animal\"/></rdfs:subClassOf></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Student\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Person\"/><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasValue></owl:Restriction><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"#University\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"KoalaWithPhD\"><owl:versionInfo>1.2</owl:versionInfo><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Restriction><owl:hasValue><Degree rdf:ID=\"PhD\"/></owl:hasValue><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasDegree\"/></owl:onProperty></owl:Restriction><owl:Class rdf:about=\"#Koala\"/></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"University\"><rdfs:subClassOf><owl:Class rdf:ID=\"Habitat\"/></rdfs:subClassOf></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Koala\"><rdfs:subClassOf><owl:Restriction><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">false</owl:hasValue><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"#DryEucalyptForest\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Animal\"><rdfs:seeAlso>Male</rdfs:seeAlso><rdfs:subClassOf><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasHabitat\"/></owl:onProperty><owl:minCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:minCardinality></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf><owl:Restriction><owl:cardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:cardinality><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><owl:versionInfo>1.1</owl:versionInfo></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Forest\"><rdfs:subClassOf rdf:resource=\"#Habitat\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Rainforest\"><rdfs:subClassOf rdf:resource=\"#Forest\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"GraduateStudent\"><rdfs:subClassOf><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasDegree\"/></owl:onProperty><owl:someValuesFrom><owl:Class><owl:oneOf rdf:parseType=\"Collection\"><Degree rdf:ID=\"BA\"/><Degree rdf:ID=\"BS\"/></owl:oneOf></owl:Class></owl:someValuesFrom></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Student\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Parent\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Animal\"/><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty><owl:minCardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</owl:minCardinality></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass><rdfs:subClassOf rdf:resource=\"#Animal\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"DryEucalyptForest\"><rdfs:subClassOf rdf:resource=\"#Forest\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Quokka\"><rdfs:subClassOf><owl:Restriction><owl:hasValue rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasValue><owl:onProperty><owl:FunctionalProperty rdf:about=\"#isHardWorking\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"TasmanianDevil\"><rdfs:subClassOf rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"MaleStudentWith3Daughters\"><owl:equivalentClass><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><owl:Class rdf:about=\"#Student\"/><owl:Restriction><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty><owl:hasValue><Gender rdf:ID=\"male\"/></owl:hasValue></owl:Restriction><owl:Restriction><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty><owl:cardinality rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</owl:cardinality></owl:Restriction><owl:Restriction><owl:allValuesFrom rdf:resource=\"#Female\"/><owl:onProperty><owl:ObjectProperty rdf:about=\"#hasChildren\"/></owl:onProperty></owl:Restriction></owl:intersectionOf></owl:Class></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Degree\"/>\n"
            + "  <owl:Class rdf:ID=\"Male\"><owl:equivalentClass><owl:Restriction><owl:hasValue rdf:resource=\"#male\"/><owl:onProperty><owl:FunctionalProperty rdf:about=\"#hasGender\"/></owl:onProperty></owl:Restriction></owl:equivalentClass></owl:Class>\n"
            + "  <owl:Class rdf:ID=\"Gender\"/>\n"
            + "  <owl:Class rdf:ID=\"Person\"><rdfs:subClassOf rdf:resource=\"#Animal\"/><owl:disjointWith rdf:resource=\"#Marsupials\"/></owl:Class>\n"
            + "  <owl:ObjectProperty rdf:ID=\"hasHabitat\"><rdfs:range rdf:resource=\"#Habitat\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:ObjectProperty>\n"
            + "  <owl:ObjectProperty rdf:ID=\"hasDegree\"><rdfs:domain rdf:resource=\"#Person\"/><rdfs:range rdf:resource=\"#Degree\"/></owl:ObjectProperty>\n"
            + "  <owl:ObjectProperty rdf:ID=\"hasChildren\"><rdfs:range rdf:resource=\"#Animal\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:ObjectProperty>\n"
            + "  <owl:FunctionalProperty rdf:ID=\"hasGender\"><rdfs:range rdf:resource=\"#Gender\"/><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#ObjectProperty\"/><rdfs:domain rdf:resource=\"#Animal\"/></owl:FunctionalProperty>\n"
            + "  <owl:FunctionalProperty rdf:ID=\"isHardWorking\"><rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#boolean\"/><rdfs:domain rdf:resource=\"#Person\"/><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#DatatypeProperty\"/></owl:FunctionalProperty>\n"
            + "  <Degree rdf:ID=\"MA\"/>\n" + "</rdf:RDF>";

    @Test
    void reasonerPizza(){
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(this.ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        // Query: VeggiePizza and MeatPizza
        // Sat: True
        OWLClass veggiepizza = this.dataFactory.getOWLClass("VeggiePizza");
        OWLClass meatpizza = this.dataFactory.getOWLClass("MeatPizza");
        System.out.println(reasoner.isSatisfiable(this.dataFactory.getOWLObjectIntersectionOf(veggiepizza, meatpizza)));
        System.out.println(alcReasoner.isSatisfiable(this.dataFactory.getOWLObjectIntersectionOf(veggiepizza, meatpizza)));
    }

    @Test
    void reasonerEx3_4(){
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);

        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(this.ontology);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

        //query( and( ( ̃forall( p, a)), ( ̃exist( p, ( ̃a))))).
        // Sat: False
        OWLClass a = this.dataFactory.getOWLClass("A");
        OWLObjectProperty p = this.dataFactory.getOWLObjectProperty("P");
        OWLObjectComplementOf first = this.dataFactory.getOWLObjectComplementOf(this.dataFactory.getOWLObjectAllValuesFrom(p, a));
        OWLObjectComplementOf second = this.dataFactory.getOWLObjectComplementOf(this.dataFactory.getOWLObjectSomeValuesFrom(p, this.dataFactory.getOWLObjectComplementOf(a)));
        System.out.println(reasoner.isSatisfiable(this.dataFactory.getOWLObjectIntersectionOf(first, second)));
        System.out.println(alcReasoner.isSatisfiable(this.dataFactory.getOWLObjectIntersectionOf(first, second)));
    }

    @Test
    void graph() {
        MutableGraph<Object> graph = GraphBuilder.directed().build();
        OWLClass a = this.dataFactory.getOWLClass("A");
        OWLClass b = this.dataFactory.getOWLClass("B");
        graph.addNode(a);
        graph.addNode(b);
        graph.putEdge(a, b);
        Graphs.hasCycle(graph);

    }
}
