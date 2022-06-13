import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.stream.StreamSupport;

public class OWLDependencyGraph {

    private final MutableGraph<OWLClassExpression> dependencyGraph = GraphBuilder.directed().build();

    /**
     *
     * @param firstNode the tail of the edge
     * @param secondNode the head of the edge
     * @return if the dependency graph was modified as a result of this call
     */
    public boolean addDependencyToGraph(OWLClassExpression firstNode, OWLClassExpression secondNode){
        dependencyGraph.addNode(secondNode);
        dependencyGraph.addNode(firstNode);
        return dependencyGraph.putEdge(firstNode, secondNode);
    }

    public void prettyPrintGraph(OWLClassExpression root){
        Iterable<OWLClassExpression> traverser = Traverser.forGraph(dependencyGraph).breadthFirst(root);
        StreamSupport.stream(traverser.spliterator(), false)
                .map(c -> c.toString() + "->")
                .forEach(System.out::println);

    }

}
