import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.List;

public class LazyUnfoldingVisitor implements OWLAxiomVisitor {

    private final OWLDependencyGraph dependencyGraph = new OWLDependencyGraph();

    @Override
    public void visit(OWLSubClassOfAxiom axiom) {
        OWLClassExpression subClass = axiom.getSubClass();
        OWLClassExpression superClass = axiom.getSuperClass();
        superClass.classesInSignature()
                .forEach(c -> dependencyGraph.addDependencyToGraph(subClass, c));
    }

    @Override
    public void visit(OWLEquivalentClassesAxiom axiom) {
        List<OWLClassExpression> equivalentClasses = axiom.classExpressions().toList();
        OWLClassExpression firstOperand = equivalentClasses.get(0);
        OWLClassExpression secondOperand = equivalentClasses.get(1);
        secondOperand.classesInSignature()
                .forEach(c -> dependencyGraph.addDependencyToGraph(firstOperand, c));
    }

    public OWLDependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

}
