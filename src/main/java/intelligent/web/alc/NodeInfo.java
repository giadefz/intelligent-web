package intelligent.web.alc;

import intelligent.web.individual.TableauxIndividual;
import intelligent.web.rdf.RdfSerializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class NodeInfo implements RdfSerializable {

    private final NodeInfo father;
    private final TableauxIndividual individual;
    private Stream<OWLClassExpression> classExpressions;
    private final Set<OWLObjectUnionOf> alreadyVisitedUnions;
    private OWLClassExpression newClassExpression;
    private final OWLObjectPropertyAssertionAxiom propertyAssertionAxiom;

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
                .build();
    }

    @Override
    public String toRdf() {
        return null;
    }
}
