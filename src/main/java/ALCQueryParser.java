import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import java.util.Set;

public class ALCQueryParser {

    private final OWLOntology rootOntology;
    private final BidirectionalShortFormProvider bidirectionalShortFormProvider;
    private final ManchesterOWLSyntaxParser manchesterOWLSyntaxParser;

    public ALCQueryParser(OWLOntology rootOntology) {
        this.rootOntology = rootOntology;
        OWLOntologyManager manager = rootOntology.getOWLOntologyManager();
        Set<OWLOntology> importsClosure;
        importsClosure = rootOntology.getImportsClosure();
        bidirectionalShortFormProvider = new BidirectionalShortFormProviderAdapter(manager,
                importsClosure, new SimpleShortFormProvider());
        this.manchesterOWLSyntaxParser = new ManchesterOWLSyntaxParserImpl(rootOntology.getOWLOntologyManager().getOntologyConfigurator(),
                rootOntology.getOWLOntologyManager().getOWLDataFactory());
        OWLEntityChecker shortFormEntityChecker = new ShortFormEntityChecker(bidirectionalShortFormProvider);
        manchesterOWLSyntaxParser.setDefaultOntology(rootOntology);
        manchesterOWLSyntaxParser.setOWLEntityChecker(shortFormEntityChecker);
    }

    public OWLClassExpression parseClassExpression(String classExpression){
        return manchesterOWLSyntaxParser.parseClassExpression(classExpression);
    }

    public static void main(String[] args) throws OWLOntologyCreationException {
        assert args.length == 2;
        String fileName = args[0];
        String query = args[1];
        ALCOntologyLoader alcOntologyLoader = new ALCOntologyLoader(fileName);
        OWLOntology owlOntology = alcOntologyLoader.loadOntology();
        ALCQueryParser alcQueryParser = new ALCQueryParser(owlOntology);
        OWLClassExpression owlQuery = alcQueryParser.parseClassExpression(query);
        ALCReasoner alcReasoner = new ALCReasoner(owlOntology, owlOntology.getOWLOntologyManager().getOWLDataFactory());
        System.out.println(alcReasoner.isSatisfiable(owlQuery));
    }

}
