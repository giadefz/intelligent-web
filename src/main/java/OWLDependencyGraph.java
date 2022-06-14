import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import org.semanticweb.owlapi.model.*;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class OWLDependencyGraph {

    private final MutableGraph<OWLClassExpression> dependencyGraph = GraphBuilder.directed().build();
    private final GetLeftClassVisitor leftClassVisitor = new GetLeftClassVisitor();

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

    public boolean isClassUnfoldable(OWLClassExpression classExpression, Set<OWLLogicalAxiom> unfoldableAxioms){
        boolean classAlreadyPresent = unfoldableAxioms.stream()
                .filter(this::isSubClassOfOrEquivalent)
                .map(a -> a.accept(leftClassVisitor))
                .anyMatch(c -> c.equals(classExpression));
        if(classAlreadyPresent) return false;
        Iterable<OWLClassExpression> traverser = Traverser.forGraph(dependencyGraph).breadthFirst(classExpression);
        MutableGraph<OWLClassExpression> subgraph = Graphs.inducedSubgraph(dependencyGraph, traverser);
        return !Graphs.hasCycle(subgraph);
    }



    private boolean isSubClassOfOrEquivalent(OWLAxiom axiom) {
        return axiom instanceof OWLEquivalentClassesAxiom || axiom instanceof OWLSubClassOfAxiom;
    }

}
