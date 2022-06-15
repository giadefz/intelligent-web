import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import javax.annotation.Nonnull;
import java.io.File;

public class LoadAndSaveOntologyTest {

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLDataFactory dataFactory;
    private OWLReasonerFactory reasonerFactory;
    private OWLReasoner standardReasoner;

    @BeforeEach
    void setUp() {
        this.manager = OWLManager.createOWLOntologyManager();
        this.dataFactory = this.manager.getOWLDataFactory();
        this.reasonerFactory = new ReasonerFactory();
    }
    @Test
    void ontology3_0Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology3_0.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("B and (R only owl:Nothing)");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology3_1Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology3_1.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("B and (R only owl:Nothing)");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology1Test1() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology1.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(A1 or A2) and ((R only (A1 and A2)) or (not A1)) and (B1 or B2) and ((R some (not A1)) or (R some (not A2)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology1Test2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology1.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("((R some (A1 and A2)) or (R some (not B1))) and (B1 or B2) and (B3 or B4) and ((R only (not A1)) or (R only (not A2)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology2Test1() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("((S some (R only A)) or B) and ((S some C) or D) and (not D or not B) and ((S only (R some (not A))) or (S only (not C)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology2Test2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(A or B) and ((S only (R some (not A))) or (S only B)) and (not B or (R only A)) and (not A or (S some (R only A)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology2Test3() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("((R only A) or (R only (not A))) and (A or B) and ((R only (S only A)) or (not B)) and (R some (not A))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology2Test4() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(A or B) and (C or D) and ((R only (S only A)) or (not B)) and (R some (S only (not A)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ex3_4Test1() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ex3_4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("D");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ex3_4Test3() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ex3_4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("not D");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ex3_4Test2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ex3_4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(not (P some A)) and (not (P only (not A)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void ontology3_2Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology3_2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("B and (R only owl:Nothing)");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    @Test
    void pizzaTest() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "pizza.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("VeggiePizza and MeatPizza");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology, this.dataFactory);
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), standardReasoner.isSatisfiable(cl));
    }

    private OWLOntology loadFromFile(@Nonnull OWLOntologyManager manager, String fileName) throws OWLOntologyCreationException {
        File file = new File(fileName);
        return manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), new OWLOntologyLoaderConfiguration());
    }
}
