import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TableauxIndividual extends OWLNamedIndividualImpl implements Cloneable{
    
    private final Long id;
    private final TableauxIndividual father;
    private Set<OWLClassExpression> literals = new HashSet<>();

    public TableauxIndividual(String name, Long id) {
        super(IRI.create(name));
        this.father = null;
        this.id = id;
    }

    public TableauxIndividual(String name, Long id, TableauxIndividual father) {
        super(IRI.create(name));
        this.id = id;
        this.father = father;
    }

    public TableauxIndividual(String name, Long id, TableauxIndividual father, Set<OWLClassExpression> literals) {
        super(IRI.create(name));
        this.id = id;
        this.father = father;
        this.literals = literals;
    }

    public TableauxIndividual(String name, Long id, Set<OWLClassExpression> literals) {
        super(IRI.create(name));
        this.id = id;
        this.father = null;
        this.literals = literals;
    }

    public Long getId() {
        return id;
    }

    public Optional<TableauxIndividual> getFather() {
        return Optional.ofNullable(father);
    }

    /**
     *
     * @param literal: the new literal
     * @return true if literal causes clash, false otherwise
     */
    public boolean addLiteral(OWLClassExpression literal){
        assert literal.isClassExpressionLiteral();
        literals.add(literal);
        return literals.contains(literal.getComplementNNF());
    }

    @Override
    public TableauxIndividual clone() {
        try {
            TableauxIndividual clone = (TableauxIndividual) super.clone();
            clone.literals = new HashSet<>(this.literals);
            return clone;
        } catch (CloneNotSupportedException e) {
            return new TableauxIndividual(this.getIRI().getIRIString(), this.getId(), this.getFather().orElse(null));
        }
    }
}
