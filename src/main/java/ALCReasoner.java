import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ALCReasoner {
    private final OWLOntology ontology;
    private final OWLDataFactory dataFactory;
    private final TableauxIndividualFactory tableauxIndividualFactory = TableauxIndividualFactory.getInstance();
    private final OWLObjectIntersectionOf concept;
    private final Set<OWLLogicalAxiom> unfoldableSet;
    private final LazyUnfolder lazyUnfolder;


    public ALCReasoner(OWLOntology ontology, OWLDataFactory dataFactory) {
        this.ontology = ontology;
        this.dataFactory = dataFactory;
        this.lazyUnfolder = new LazyUnfolder(this.ontology.getLogicalAxioms(), this.ontology.getOWLOntologyManager().getOWLDataFactory());
        Pair<Set<OWLLogicalAxiom>, Set<OWLLogicalAxiom>> lazyUnfolding = lazyUnfolding();
        this.concept = extractConcept(lazyUnfolding);
        this.unfoldableSet = lazyUnfolding.getRight();
        System.out.println("ONTOLOGY: " + ontology.getLogicalAxioms());
        System.out.println("LAZY UNFOLDING Tu: " + lazyUnfolding.getRight());
        System.out.println("LAZY UNFOLDING Tg: " + lazyUnfolding.getLeft());
    }

    private Pair<Set<OWLLogicalAxiom>, Set<OWLLogicalAxiom>> lazyUnfolding() {
        return this.lazyUnfolder.lazyUnfolding();
    }

    public boolean isSatisfiable(OWLClassExpression classExpression) {
        OWLClassExpression nnfQuery = classExpression.getNNF();
        TableauxIndividual a = tableauxIndividualFactory.getNewIndividual();
        boolean isClashFree = isClashFree(NodeInfo.builder()
                .individual(a)
                .classExpressions(concept != null ? concept.conjunctSet() : Stream.empty())
                .newClassExpression(nnfQuery)
                .alreadyVisitedUnions(Collections.emptySet())
                .build());
        RDFBuilder.getModel().write(System.out);
        return isClashFree;
    }

    private OWLObjectIntersectionOf extractConcept(Pair<Set<OWLLogicalAxiom>, Set<OWLLogicalAxiom>> tgLeftTuRight) {
        Supplier<Stream<OWLClassExpression>> streamSupplier = () -> tgLeftTuRight.
                getLeft()
                .stream()
                .map(l -> l.accept(new NNFMod(this.dataFactory)))
                .map(a -> (OWLSubClassOfAxiom) a)
                .map(OWLSubClassOfAxiom::getSuperClass);

        if (streamSupplier.get().findAny().isPresent()) {
            return this.dataFactory.getOWLObjectIntersectionOf(streamSupplier.get());
        } else return null;
    }

    private boolean isClashFree(NodeInfo nodeInfo) {
        TableauxIndividual currentIndividual = nodeInfo.getIndividual();
        RDFBuilder.addToRDFModel(nodeInfo);
        if (isClashFound(nodeInfo, currentIndividual) || lazyUnfoldingRulesCauseClash(currentIndividual, nodeInfo)) { //also adds labels from new class expression to individual
            RDFBuilder.addClash(nodeInfo);
            return false;
        }
        Set<OWLClassExpression> newClassExpressions =
                applyAnd(nodeInfo.getNewClassExpression(), nodeInfo.getClassExpressions())
                        .collect(Collectors.toSet());
        return applyOr(nodeInfo, newClassExpressions);
    }

    private boolean lazyUnfoldingRulesCauseClash(TableauxIndividual currentIndividual, NodeInfo nodeInfo) {
        return lazyUnfolder.lazyUnfoldingRulesCauseClash(currentIndividual, nodeInfo, unfoldableSet);
    }

    private boolean isClashFound(NodeInfo nodeInfo, TableauxIndividual currentIndividual) {
        return currentIndividual.addingLabelCausesClash(nodeInfo.getNewClassExpression().conjunctSet());
    }

    private boolean applySomeValuesFrom(NodeInfo nodeInfo, Set<OWLClassExpression> newClassExpressions) {
        Set<OWLObjectSomeValuesFrom> owlObjectSomeValuesFroms = getSomeValuesFroms(newClassExpressions);
        if (!owlObjectSomeValuesFroms.isEmpty()) {
            return owlObjectSomeValuesFroms.stream()
                    .allMatch(p -> applyExistentialQuantification(p, nodeInfo, newClassExpressions));
        }
        return true;
    }

    private Set<OWLObjectSomeValuesFrom> getSomeValuesFroms(Set<OWLClassExpression> newClassExpressions) {
        return newClassExpressions.stream()
                .filter(p -> p instanceof OWLObjectSomeValuesFrom)
                .map(p -> (OWLObjectSomeValuesFrom) p)
                .collect(Collectors.toSet());
    }

    private boolean applyExistentialQuantification(OWLObjectSomeValuesFrom someValuesFrom, NodeInfo nodeInfo, Set<OWLClassExpression> newClassExpressions) {
        OWLObjectProperty owlObjectProperty = getOwlObjectProperty(someValuesFrom);
        OWLClassExpression filler = someValuesFrom.getFiller();
        TableauxIndividual father = nodeInfo.getIndividual();
        TableauxIndividual son = tableauxIndividualFactory.getNewIndividual(father);
        OWLObjectPropertyAssertionAxiom property =
                this.dataFactory.getOWLObjectPropertyAssertionAxiom(owlObjectProperty, father, son);
        Stream<OWLClassExpression> classExpressionsInAllValuesFrom = applyAllValuesFrom(newClassExpressions, property);
        OWLObjectIntersectionOf sonBasicClassExpressions =
                this.dataFactory.getOWLObjectIntersectionOf(Stream.concat(classExpressionsInAllValuesFrom, filler.conjunctSet()));
        //we add labels now because of block checking in getSonNewClassExpressions
        sonBasicClassExpressions.conjunctSet()
                .forEach(son::addingLabelCausesClash); //ignore clash for now, if there is clash it will be found in recursive call
        boolean isSonBlocked = son.isBlocked();
        return isClashFree(NodeInfo.builder()
                .father(nodeInfo)
                .individual(son)
                .classExpressions(getSonClassExpressions(isSonBlocked))
                .newClassExpression(getSonNewClassExpressions(sonBasicClassExpressions, isSonBlocked))
                .alreadyVisitedUnions(Collections.emptySet())
                .propertyAssertionAxiom(property)
                .build()
        );
    }

    private Stream<OWLClassExpression> getSonClassExpressions(boolean blocked) {
        if (!blocked || this.concept == null) return Stream.empty();
        return this.concept.conjunctSet();
    }

    private OWLObjectProperty getOwlObjectProperty(OWLObjectSomeValuesFrom someValuesFrom) {
        return someValuesFrom.objectPropertiesInSignature()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No object property found in someValuesFrom" + someValuesFrom));
    }

    private OWLClassExpression getSonNewClassExpressions(OWLClassExpression sonBasicClassExpressions, boolean blocked) {
        if (blocked) {
            return sonBasicClassExpressions;
        } else {
            return this.dataFactory.getOWLObjectIntersectionOf(sonBasicClassExpressions, this.concept);
        }
    }

    private Stream<OWLClassExpression> applyAllValuesFrom(Set<OWLClassExpression> newClassExpressions, OWLObjectPropertyAssertionAxiom property) {
        return newClassExpressions.stream()
                .filter(p -> p instanceof OWLObjectAllValuesFrom)
                .map(p -> (OWLObjectAllValuesFrom) p)
                .filter(p -> p.objectPropertiesInSignature().findFirst().orElseThrow(() -> new IllegalStateException("No property found in AllValuesFrom"))
                        .equals(property.getProperty())
                )
                .map(OWLObjectAllValuesFrom::getFiller);
    }


    /**
     * @param nodeInfo
     * @param newClassExpressions
     * @return true if or is clash free
     */
    private boolean applyOr(NodeInfo nodeInfo, Set<OWLClassExpression> newClassExpressions) {
        Optional<OWLObjectUnionOf> unionOf = getUnvisitedUnionOf(newClassExpressions, nodeInfo.getAlreadyVisitedUnions());
        if (unionOf.isPresent()) {
            OWLObjectUnionOf owlObjectUnionOf = unionOf.get();
            long clashedOperands = owlObjectUnionOf.operands()
                    .takeWhile(p -> !isClashFree(NodeInfo.getNewNode(nodeInfo, newClassExpressions.stream(), p, owlObjectUnionOf)))
                    .count();
            //if all operands were traversed, it means that all branches result in clash -> return false
            return clashedOperands != owlObjectUnionOf.operands().count();
        }
        return applySomeValuesFrom(nodeInfo, newClassExpressions);
    }

    private Optional<OWLObjectUnionOf> getUnvisitedUnionOf(Set<OWLClassExpression> newClassExpressions, Set<OWLObjectUnionOf> alreadyVisitedUnions) {
        return newClassExpressions
                .stream()
                .filter(p -> p instanceof OWLObjectUnionOf)
                .filter(p -> !alreadyVisitedUnions.contains(p))
                .map(p -> (OWLObjectUnionOf) p)
                .filter(p -> p.operands().noneMatch(newClassExpressions::contains)) //or could be already satisfied
                .findAny();

    }

    private Stream<OWLClassExpression> applyAnd(OWLClassExpression newClassExpression, Stream<OWLClassExpression> classExpressions) {
        return Stream.concat(newClassExpression.conjunctSet(), classExpressions).distinct();
    }

}
