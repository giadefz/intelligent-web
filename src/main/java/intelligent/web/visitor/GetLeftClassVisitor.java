package intelligent.web.visitor;

import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.List;

public class GetLeftClassVisitor implements OWLAxiomVisitorEx<OWLClassExpression> {

    @Override
    public OWLClassExpression visit(OWLSubClassOfAxiom axiom) {
        return axiom.getSubClass();
    }

    @Override
    public OWLClassExpression visit(OWLEquivalentClassesAxiom axiom) {
        List<OWLClassExpression> classExpressions = axiom.classExpressions().toList();
        return classExpressions.get(0);
    }
}
