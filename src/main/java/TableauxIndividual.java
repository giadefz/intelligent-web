import org.semanticweb.owlapi.model.IRI;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

public class TableauxIndividual extends OWLNamedIndividualImpl {
    
    private final Long id;
    private TableauxIndividual father;

    public TableauxIndividual(String name, Long id) {
        super(IRI.create(name));
        this.id = id;
    }

    public TableauxIndividual(String name, Long id, TableauxIndividual father) {
        super(IRI.create(name));
        this.id = id;
        this.father = father;
    }
    
    public Long getId() {
        return id;
    }

    public TableauxIndividual getFather() {
        return father;
    }

    public void setFather(TableauxIndividual father) {
        this.father = father;
    }
}
