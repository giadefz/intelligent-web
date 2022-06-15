import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Getter
@AllArgsConstructor
public class NodeInfo implements RdfSerializable{

    private final NodeInfo father;
    private final TableauxIndividual individual;
    private final Stream<OWLClassExpression> classExpressions;
    private final Set<OWLObjectUnionOf> alreadyVisitedUnions;
    private final OWLClassExpression newClassExpression;
    private final OWLObjectPropertyAssertionAxiom propertyAssertionAxiom;
    private final boolean checkLazyUnfoldingRule;

    public static NodeInfo getNewNode(NodeInfo oldNode, Stream<OWLClassExpression> newClassExpressions,
                                      OWLClassExpression newClassExpression, OWLObjectUnionOf visitingUnionOf){
        return NodeInfo.builder()
                .father(oldNode)
                .alreadyVisitedUnions(
                        Stream.concat(oldNode.getAlreadyVisitedUnions().stream(), Stream.of(visitingUnionOf))
                                .collect(Collectors.toSet()))
                .individual(oldNode.getIndividual().clone())
                .classExpressions(newClassExpressions)
                .newClassExpression(newClassExpression)
                .checkLazyUnfoldingRule(true)
                .build();
    }

    @Override
    public String toRdf() {
        return null;
    }
}
