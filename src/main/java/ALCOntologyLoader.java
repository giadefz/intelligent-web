import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;

public class ALCOntologyLoader {

    private final String fileName;
    private final OWLOntologyManager manager;

    public ALCOntologyLoader(String fileName) {
        this.fileName = fileName;
        this.manager = OWLManager.createOWLOntologyManager();
    }

    public OWLOntology loadOntology() throws OWLOntologyCreationException {
        File file = new File(fileName);
        return manager.loadOntologyFromOntologyDocument(new FileDocumentSource(file), new OWLOntologyLoaderConfiguration());
    }

}
