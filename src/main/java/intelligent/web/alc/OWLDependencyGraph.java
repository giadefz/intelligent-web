package intelligent.web.alc;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import intelligent.web.visitor.GetLeftClassVisitor;
import org.semanticweb.owlapi.model.*;

import java.util.Set;
import java.util.stream.StreamSupport;

public class OWLDependencyGraph {

    private final MutableGraph<OWLClassExpression> dependencyGraph = GraphBuilder.directed().allowsSelfLoops(true).build();
    private final GetLeftClassVisitor leftClassVisitor = new GetLeftClassVisitor();

    /**
     *
     * @param firstNode the tail of the edge
     * @param secondNode the head of the edge
     * @return true if the dependency graph was modified as a result of this call
     */
    public boolean addDependencyToGraph(OWLClassExpression firstNode, OWLClassExpression secondNode){
        dependencyGraph.addNode(secondNode);
        dependencyGraph.addNode(firstNode);
        return dependencyGraph.putEdge(firstNode, secondNode);
    }

    public boolean isClassUnfoldable(OWLClassExpression classExpression, Set<OWLLogicalAxiom> unfoldableAxioms){
        if(isClassAlreadyPresent(classExpression, unfoldableAxioms))
            return false;
        Iterable<OWLClassExpression> traversedNodesFromClassExpression =
                Traverser.forGraph(dependencyGraph).breadthFirst(classExpression);
        MutableGraph<OWLClassExpression> subgraph = Graphs.inducedSubgraph(dependencyGraph, traversedNodesFromClassExpression);
        return !Graphs.hasCycle(subgraph);
    }

    private boolean isClassAlreadyPresent(OWLClassExpression classExpression, Set<OWLLogicalAxiom> unfoldableAxioms) {
        return unfoldableAxioms.stream()
                .filter(this::isSubClassOfOrEquivalent)
                .map(a -> a.accept(leftClassVisitor))
                .anyMatch(c -> c.equals(classExpression));
    }


    private boolean isSubClassOfOrEquivalent(OWLAxiom axiom) {
        return axiom instanceof OWLEquivalentClassesAxiom || axiom instanceof OWLSubClassOfAxiom;
    }

}
