import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Getter
@AllArgsConstructor
public class NodeInfo {

    private final TableauxIndividual individual;
    private final Stream<OWLClassExpression> classExpressions;
    private final Set<OWLObjectUnionOf> alreadyVisitedUnions;
    private final OWLClassExpression newClassExpression;

    public static NodeInfo getNewNode(NodeInfo oldNode, OWLObjectUnionOf visitingUnionOf, int indexOfVisitingOperand,
                                      Stream<OWLClassExpression> newClassExpressions){
        List<OWLClassExpression> operands = visitingUnionOf.operands().toList();
        return NodeInfo.builder()
                .alreadyVisitedUnions(
                        Stream.concat(oldNode.getAlreadyVisitedUnions().stream(), Stream.of(visitingUnionOf))
                                .collect(Collectors.toSet()))
                .individual(oldNode.getIndividual().clone())
                .classExpressions(newClassExpressions)
                .newClassExpression(operands.get(indexOfVisitingOperand))
                .build();
    }

}
