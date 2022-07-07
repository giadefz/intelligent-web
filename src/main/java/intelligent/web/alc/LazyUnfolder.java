package intelligent.web.alc;

import intelligent.web.individual.TableauxIndividual;
import intelligent.web.visitor.GetLeftClassVisitor;
import intelligent.web.visitor.LazyUnfoldingRuleApplier;
import intelligent.web.visitor.LazyUnfoldingVisitor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LazyUnfolder {

    private final Set<OWLLogicalAxiom> unfoldableAxioms = new HashSet<>();
    private final Set<OWLLogicalAxiom> logicalAxioms;
    private final GetLeftClassVisitor getLeftClassVisitor = new GetLeftClassVisitor();
    private final OWLDataFactory dataFactory;

    public LazyUnfolder(Set<OWLLogicalAxiom> logicalAxioms, OWLDataFactory dataFactory) {
        this.logicalAxioms = logicalAxioms;
        this.dataFactory = dataFactory;
    }

    public Pair<Set<OWLLogicalAxiom>, Set<OWLLogicalAxiom>> lazyUnfolding() {
        normalizeAxioms();
        //build the dependency graph
        LazyUnfoldingVisitor lazyUnfoldingVisitor = new LazyUnfoldingVisitor();
        logicalAxioms.forEach(a -> a.accept(lazyUnfoldingVisitor));
        OWLDependencyGraph dependencyGraph = lazyUnfoldingVisitor.getDependencyGraph();

        checkLazyUnfolding(dependencyGraph, OWLEquivalentClassesAxiom.class);
        checkLazyUnfolding(dependencyGraph, OWLSubClassOfAxiom.class);

        logicalAxioms.removeAll(unfoldableAxioms);

        return new ImmutablePair<>(logicalAxioms, unfoldableAxioms);
    }

    /**
     * transforms multiple subClassOfAxioms with same left operand into a single subClassOfAxiom with all right
     * operands found put in "and"
     */
    private void normalizeAxioms(){
        Set<OWLLogicalAxiom> allSubClassesOf = logicalAxioms.stream()
                .filter(a -> a instanceof OWLSubClassOfAxiom)
                .collect(Collectors.toSet());

        logicalAxioms.removeAll(allSubClassesOf);

        Set<OWLSubClassOfAxiom> normalizedSubclassesOfAxioms = allSubClassesOf.stream()
                .collect(Collectors.groupingBy(a -> a.accept(getLeftClassVisitor)))
                .entrySet()
                .stream()
                .map(this::normalizeSubClassOfAxiom)
                .collect(Collectors.toSet());

        logicalAxioms.addAll(normalizedSubclassesOfAxioms);

    }

    private OWLSubClassOfAxiom normalizeSubClassOfAxiom(Map.Entry<OWLClassExpression, List<OWLLogicalAxiom>> entry){
        Stream<OWLClassExpression> superClassesStream = entry.getValue().stream()
                .map(a -> (OWLSubClassOfAxiom) a)
                .map(OWLSubClassOfAxiom::getSuperClass);
        return this.dataFactory.getOWLSubClassOfAxiom(
                entry.getKey(), this.dataFactory.getOWLObjectIntersectionOf(superClassesStream)
        );
    }

    private void checkLazyUnfolding(OWLDependencyGraph dependencyGraph,
                                    Class<? extends OWLLogicalAxiom> axiomClass) {
        logicalAxioms.stream()
                .filter(a -> axiomClass.isAssignableFrom(a.getClass()))
                .forEach(a -> {
                            OWLClassExpression leftOperand = a.accept(getLeftClassVisitor);
                            if (dependencyGraph.isClassUnfoldable(leftOperand, unfoldableAxioms)){
                                unfoldableAxioms.add(a);
                            }
                        }
                );
    }

    public boolean lazyUnfoldingRulesCauseClash(TableauxIndividual individual, NodeInfo nodeInfo, Set<OWLLogicalAxiom> axioms) {
        long notClashedAxioms = axioms.stream()
                .takeWhile(a -> !axiomCausesClash(a, individual, nodeInfo))
                .count();

        return notClashedAxioms != axioms.size(); //if no axioms caused clash, return true
    }

    private Boolean axiomCausesClash(OWLLogicalAxiom a, TableauxIndividual individual, NodeInfo nodeInfo) {
        return a.accept(new LazyUnfoldingRuleApplier(individual, nodeInfo, dataFactory));
    }


}
