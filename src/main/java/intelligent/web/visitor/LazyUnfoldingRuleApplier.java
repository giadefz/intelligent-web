package intelligent.web.visitor;

import intelligent.web.alc.NodeInfo;
import intelligent.web.individual.TableauxIndividual;
import org.semanticweb.owlapi.model.*;

import java.util.stream.Stream;

public class LazyUnfoldingRuleApplier implements OWLAxiomVisitorEx<Boolean> {

    private final TableauxIndividual individual;
    private final NodeInfo nodeInfo;
    private final OWLDataFactory dataFactory;

    public LazyUnfoldingRuleApplier(TableauxIndividual individual, NodeInfo nodeInfo, OWLDataFactory dataFactory) {
        this.individual = individual;
        this.nodeInfo = nodeInfo;
        this.dataFactory = dataFactory;
    }

    @Override
    public Boolean visit(OWLSubClassOfAxiom axiom) {
        return applySubClassOfRuleCausesClash(axiom);
    }

    @Override
    public Boolean visit(OWLEquivalentClassesAxiom axiom) {
        if (!applyPositiveEquivalenceRuleCausesClash(axiom)) {
            return applyNegativeEquivalenceRuleCausesClash(axiom);
        }
        return true;
    }

    public boolean applyPositiveEquivalenceRuleCausesClash(OWLEquivalentClassesAxiom axiom) {
        OWLClassExpression leftOperand = axiom.classExpressions().toList().get(0);
        OWLClassExpression rightOperand = axiom.classExpressions().toList().get(1).getNNF();
        return checkClashAndAddLabels(leftOperand, rightOperand);
    }

    public boolean applyNegativeEquivalenceRuleCausesClash(OWLEquivalentClassesAxiom axiom) {
        OWLClassExpression leftOperand = axiom.classExpressions().toList().get(0).getComplementNNF();
        OWLClassExpression rightOperand = axiom.classExpressions().toList().get(1).getComplementNNF();
        return checkClashAndAddLabels(leftOperand, rightOperand);
    }

    private boolean applySubClassOfRuleCausesClash(OWLSubClassOfAxiom axiom) {
        OWLClassExpression subClass = axiom.getSubClass();
        OWLClassExpression superClass = axiom.getSuperClass().getNNF();
        return checkClashAndAddLabels(subClass, superClass);
    }

    private boolean checkClashAndAddLabels(OWLClassExpression subClass, OWLClassExpression superClass) {
        if (individual.getLabels().contains(subClass) && !individual.getLabels().containsAll(superClass.asConjunctSet())) {
            OWLObjectIntersectionOf newClassExpressions = this.dataFactory.getOWLObjectIntersectionOf(
                    Stream.concat(nodeInfo.getNewClassExpression().conjunctSet(), superClass.conjunctSet())
            );
            nodeInfo.setNewClassExpression(newClassExpressions);
            return individual.addingLabelCausesClash(superClass.conjunctSet());
        }
        return false;
    }
}
