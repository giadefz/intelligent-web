import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.*;
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
        LazyUnfoldingVisitor lazyUnfoldingVisitor = new LazyUnfoldingVisitor();
        logicalAxioms.forEach(a -> a.accept(lazyUnfoldingVisitor));
        OWLDependencyGraph dependencyGraph = lazyUnfoldingVisitor.getDependencyGraph();

        checkLazyUnfolding(logicalAxioms, dependencyGraph, OWLEquivalentClassesAxiom.class);
        checkLazyUnfolding(logicalAxioms, dependencyGraph, OWLSubClassOfAxiom.class);

        logicalAxioms.removeAll(unfoldableAxioms);

        return new ImmutablePair<>(logicalAxioms, unfoldableAxioms);
    }

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

    private void checkLazyUnfolding(Set<OWLLogicalAxiom> logicalAxioms, OWLDependencyGraph dependencyGraph,
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


}
