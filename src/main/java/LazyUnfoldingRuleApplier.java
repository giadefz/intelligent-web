import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

public class LazyUnfoldingRuleApplier implements OWLAxiomVisitorEx<Boolean> {

    private final TableauxIndividual individual;

    public LazyUnfoldingRuleApplier(TableauxIndividual individual) {
        this.individual = individual;
    }

    @Override
    public Boolean visit(OWLSubClassOfAxiom axiom) {
        return applySubClassOfRuleCausesClash(axiom);
    }

    @Override
    public Boolean visit(OWLEquivalentClassesAxiom axiom) {
        if(!applyPositiveEquivalenceRuleCausesClash(axiom)){
            return applyNegativeEquivalenceRuleCausesClash(axiom);
        }
        return true;
    }

    public boolean applyPositiveEquivalenceRuleCausesClash(OWLEquivalentClassesAxiom axiom){
        OWLClassExpression leftOperand = axiom.classExpressions().toList().get(0);
        OWLClassExpression rightOperand = axiom.classExpressions().toList().get(1).getNNF();
        if(individual.getLabels().contains(leftOperand) && !individual.getLabels().containsAll(rightOperand.asConjunctSet())){
            return individual.addingLabelCausesClash(rightOperand.conjunctSet());
        }
        return false;
    }

    public boolean applyNegativeEquivalenceRuleCausesClash(OWLEquivalentClassesAxiom axiom){
        OWLClassExpression leftOperand = axiom.classExpressions().toList().get(0).getComplementNNF();
        OWLClassExpression rightOperand = axiom.classExpressions().toList().get(1).getComplementNNF();
        if(individual.getLabels().contains(leftOperand) && !individual.getLabels().containsAll(rightOperand.asConjunctSet())){
            return individual.addingLabelCausesClash(rightOperand.conjunctSet());
        }
        return false;
    }

    public boolean applySubClassOfRuleCausesClash(OWLSubClassOfAxiom axiom){
        OWLClassExpression subClass = axiom.getSubClass();
        OWLClassExpression superClass = axiom.getSuperClass().getNNF();
        if(individual.getLabels().contains(subClass) && !individual.getLabels().containsAll(superClass.asConjunctSet())){
            return individual.addingLabelCausesClash(superClass.conjunctSet());
        }
        return false;
    }
}
