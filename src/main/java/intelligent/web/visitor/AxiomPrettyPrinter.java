package intelligent.web.visitor;

import org.semanticweb.owlapi.model.OWLAxiomVisitorEx;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.List;

public class AxiomPrettyPrinter implements OWLAxiomVisitorEx<String> {

    private final ClassPrettyPrinter classPrettyPrinter = new ClassPrettyPrinter(false);

    @Override
    public String visit(OWLSubClassOfAxiom axiom) {
        String superClassString = axiom.getSuperClass().accept(classPrettyPrinter);
        String subClassString = axiom.getSubClass().accept(classPrettyPrinter);
        return subClassString + " ⊆ " + superClassString;
    }

    @Override
    public String visit(OWLEquivalentClassesAxiom axiom) {
        List<OWLClassExpression> classExpressions = axiom.classExpressions().toList();
        String left = classExpressions.get(0).accept(classPrettyPrinter);
        String right = classExpressions.get(1).accept(classPrettyPrinter);
        return left + " ≡ " + right;
    }
}
