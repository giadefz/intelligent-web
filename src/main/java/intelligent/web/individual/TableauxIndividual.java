package intelligent.web.individual;

import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class TableauxIndividual extends OWLNamedIndividualImpl implements Cloneable {

    private final Long id;
    private final TableauxIndividual father;
    private Set<OWLClassExpression> labels = new HashSet<>();

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

    public TableauxIndividual(String name, Long id, TableauxIndividual father, Set<OWLClassExpression> labels) {
        super(IRI.create(name));
        this.id = id;
        this.father = father;
        this.labels = labels;
    }

    public TableauxIndividual(String name, Long id, Set<OWLClassExpression> labels) {
        super(IRI.create(name));
        this.id = id;
        this.father = null;
        this.labels = labels;
    }

    public Long getId() {
        return id;
    }

    public Optional<TableauxIndividual> getFather() {
        return Optional.ofNullable(father);
    }

    /**
     * @param label: the new label
     * @return true if label causes clash, false otherwise
     */
    public boolean addingLabelCausesClash(OWLClassExpression label) {
        Set<OWLClassExpression> labelClassExpressions = label.asConjunctSet();
        labels.addAll(labelClassExpressions);
        if (!label.isClassExpressionLiteral()) return false;
        if (label.isOWLNothing()) return true;
        return labels.contains(label.getComplementNNF());
    }

    public boolean addingLabelCausesClash(Stream<OWLClassExpression> labelConjunctSet) {
        return labelConjunctSet.anyMatch(this::addingLabelCausesClash);
    }

    @Override
    public TableauxIndividual clone() {
        try {
            TableauxIndividual clone = (TableauxIndividual) super.clone();
            clone.labels = new HashSet<>(this.labels);
            return clone;
        } catch (CloneNotSupportedException e) {
            return new TableauxIndividual(this.getIRI().getIRIString(), this.getId(), this.getFather().orElse(null));
        }
    }

    public Set<OWLClassExpression> getLabels() {
        return labels;
    }

    public boolean isBlocked() {
        TableauxIndividual currentIndividual = this;
        Optional<TableauxIndividual> currentFatherOptional = currentIndividual.getFather();
        while (currentFatherOptional.isPresent()) {
            TableauxIndividual currentFather = currentFatherOptional.get();
            if (father.getLabels().containsAll(currentIndividual.getLabels())) return true;
            else currentFatherOptional = currentFather.getFather();
        }
        return false;
    }

}
