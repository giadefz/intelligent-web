import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.NNF;

import java.util.List;
import java.util.stream.Collectors;

public class NNFMod extends NNF {

    private final OWLClass temp;

    /**
     * @param datafactory datafactory to use
     */
    public NNFMod(OWLDataFactory datafactory, OWLClass temp) {
        super(datafactory);
        this.temp = temp;
    }

    @Override
    public OWLAxiom visit(OWLSubClassOfAxiom axiom) {
        return df.getOWLSubClassOfAxiom(temp,
                df.getOWLObjectUnionOf(
                                df.getOWLObjectComplementOf(axiom.getSubClass()),
                                axiom.getSuperClass())
                        .accept(classVisitor)
        );
    }

    @Override
    public OWLAxiom visit(OWLEquivalentClassesAxiom axiom) {
        List<OWLClassExpression> classExpressions = axiom.classExpressions().toList();
        return df.getOWLSubClassOfAxiom(temp,
                df.getOWLObjectIntersectionOf(
                        df.getOWLObjectUnionOf(
                                df.getOWLObjectComplementOf(classExpressions.get(0)),
                                classExpressions.get(1)),
                        df.getOWLObjectUnionOf(
                                df.getOWLObjectComplementOf(classExpressions.get(1)),
                                classExpressions.get(0))
                ).accept(classVisitor)
        );
    }
}
