import intelligent.web.alc.ALCQueryParser;
import intelligent.web.alc.ALCReasoner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;

import static org.slf4j.LoggerFactory.getLogger;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class LoadAndSaveOntologyTest {

    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLReasonerFactory reasonerFactory;
    private OWLReasoner standardReasoner;
    private final Logger LOGGER = getLogger(this.getClass());

    @BeforeEach
    void setUp() {
        this.manager = OWLManager.createOWLOntologyManager();
        this.reasonerFactory = new ReasonerFactory();
    }
    @Test //NON SODDISFACIBILE, TU E TG ENTRAMBI NON VUOTI
    void ontology3_0Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology3_0.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("B and (R only owl:Nothing)");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //NON SODDISFACIBILE
    void ontology3_1Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology3_1.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("B and (R only owl:Nothing)");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);

    }

    @Test //BLOCKING E SODDISFACIBILE, TBOX VUOTA
    void ontology1Test1() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology1.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(A1 or A2) and ((R only (A1 and A2)) or (not A1)) and (B1 or B2) and ((R some (not A1)) or (R some (not A2)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //SODDISFACIBILE, TBOX VUOTA
    void ontology1Test2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology1.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("((R some (A1 and A2)) or (R some (not B1))) and (B1 or B2) and (B3 or B4) and ((R only (not A1)) or (R only (not A2)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //SODDISFACIBILE
    void ontology2Test1() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("((S some (R only A)) or B) and ((S some C) or D) and (not D or not B) and ((S only (R some (not A))) or (S only (not C)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //SODDISFACIBILE
    void ontology2Test2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(A or B) and ((S only (R some (not A))) or (S only B)) and (not B or (R only A)) and (not A or (S some (R only A)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //SODDISFACIBILE, TBOX VUOTA
    void ontology2Test3() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("((R only A) or (R only (not A))) and (A or B) and ((R only (S only A)) or (not B)) and (R some (not A))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //SODDISFACIBILE, TBOX VUOTA
    void ontology2Test4() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(A or B) and (C or D) and ((R only (S only A)) or (not B)) and (R some (S only (not A)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //VA IN CONTRADDIZIONE LA QUERY APPLICANDO LAZY UNFOLDING
    void ex3_4Test1() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ex3_4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("D");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //QUI CON IL NOT D NON VA IN CONTRADDIZIONE
    void ex3_4Test3() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ex3_4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("not D");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //NON SODDISFACIBILE GENERA FIGLIO CHE VA IMMEDIATAMENTE IN CLASH
    void ex3_4Test2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ex3_4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("(not (P some A)) and (not (P only (not A)))");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //TU VUOTO, FIGLIO NON BLOCCATO VA IMMEDIATAMENTE IN CLASH, NON SODDISFACIBILE
    void ontology3_2Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology3_2.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("B and (R only owl:Nothing)");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //BLOCKING
    void ontology4Test() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "ontology4.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("C");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //IMMEDIATAMENTE SODDISFACIBILE DOPO APPLICAZIONE LAZY UNFOLDING
    void pizzaTest() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "pizza.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("VeggiePizza and MeatPizza");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    @Test //APPLICAZIONE LAZY UNFOLDING A FIGLIO
    void pizzaTest2() throws OWLOntologyCreationException {
        this.ontology = loadFromFile(this.manager, "pizza.txt");
        this.standardReasoner = reasonerFactory.createReasoner(this.ontology);
        ALCQueryParser alcQueryParser = new ALCQueryParser(this.ontology);
        OWLClassExpression cl = alcQueryParser.parseClassExpression("hasTopping some Veggie");
        ALCReasoner alcReasoner = new ALCReasoner(this.ontology);
        calculateHermitTimeAndAssertEquals(cl, alcReasoner);
    }

    private void calculateHermitTimeAndAssertEquals(OWLClassExpression cl, ALCReasoner alcReasoner) {
        long hermitStartTime = System.nanoTime();
        boolean satisfiableHermit = standardReasoner.isSatisfiable(cl);
        long hermitEndTime = System.nanoTime();
        long totalHermitTime = (hermitEndTime - hermitStartTime)/1000000;
        LOGGER.info("Hermit execution time: " + totalHermitTime + "ms");
        Assertions.assertEquals(alcReasoner.isSatisfiable(cl), satisfiableHermit);
    }

    private OWLOntology loadFromFile(@Nonnull OWLOntologyManager manager, String fileName) throws OWLOntologyCreationException {
        File file = new File(fileName);
        return manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), new OWLOntologyLoaderConfiguration());
    }
}
