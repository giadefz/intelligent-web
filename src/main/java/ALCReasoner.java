import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ALCReasoner {

    protected final OWLOntology ontology;
    protected final OWLDataFactory dataFactory;
    protected final Stream<OWLAxiom> axiomsInNNF;
    protected final OWLClass temp;
    protected final OWLObjectIntersectionOf tBox;
    protected final TableauxIndividualFactory tableauxIndividualFactory = TableauxIndividualFactory.getInstance();

    public ALCReasoner(OWLOntology ontology, OWLDataFactory dataFactory) {
        this.ontology = ontology;
        this.dataFactory = dataFactory;
        this.temp = dataFactory.getOWLClass("temp");
        OWLDeclarationAxiom da = dataFactory.getOWLDeclarationAxiom(temp);
        ontology.add(da);
        this.axiomsInNNF = computeAxiomsInNNF();
        this.tBox = getTBox();
    }

    private Stream<OWLAxiom> computeAxiomsInNNF() {
        return ontology.logicalAxioms().map(l -> l.accept(new NNFMod(dataFactory, temp)));
    }

    private OWLObjectIntersectionOf getTBox() {
        Stream<OWLClassExpression> superClasses = axiomsInNNF.map(a -> (OWLSubClassOfAxiom) a)
                .map(OWLSubClassOfAxiom::getSuperClass);
        return dataFactory.getOWLObjectIntersectionOf(superClasses);
    }

    public boolean isSatisfiable(OWLClassExpression classExpression) {
        OWLClassExpression nnfQuery = classExpression.getNNF();
        TableauxIndividual a = tableauxIndividualFactory.getNewIndividual();
        return isClashFree(NodeInfo.builder()
                .individual(a)
                .classExpressions(Stream.empty())
                .newClassExpression(this.dataFactory.getOWLObjectIntersectionOf(nnfQuery, this.tBox))
                .alreadyVisitedUnions(Collections.emptySet())
                .build());
    }

    public boolean isClashFree(NodeInfo nodeInfo) {
        TableauxIndividual currentIndividual = nodeInfo.getIndividual();
        if (isClashFound(nodeInfo, currentIndividual)) return false;
        Set<OWLClassExpression> newClassExpressions =
                applyAnd(nodeInfo.getNewClassExpression(), nodeInfo.getClassExpressions())
                        .collect(Collectors.toSet());
        return applyOr(nodeInfo, newClassExpressions);
    }

    private boolean isClashFound(NodeInfo nodeInfo, TableauxIndividual currentIndividual) {
        return nodeInfo.getNewClassExpression()
                .conjunctSet()
                .anyMatch(currentIndividual::addingLabelCausesClash);
    }

    private boolean applyExistential(NodeInfo nodeInfo, Set<OWLClassExpression> newClassExpressions) {
        Set<OWLObjectSomeValuesFrom> owlObjectSomeValuesFroms = getSomeValuesFroms(newClassExpressions);
        if (!owlObjectSomeValuesFroms.isEmpty()) {
            return owlObjectSomeValuesFroms.stream()
                    .allMatch(p -> applyExistentialQuantification(p, nodeInfo));
        }
        return true;
    }

    private Set<OWLObjectSomeValuesFrom> getSomeValuesFroms(Set<OWLClassExpression> newClassExpressions) {
        return newClassExpressions.stream()
                .filter(p -> p instanceof OWLObjectSomeValuesFrom)
                .map(p -> (OWLObjectSomeValuesFrom) p)
                .collect(Collectors.toSet());
    }

    private boolean applyExistentialQuantification(OWLObjectSomeValuesFrom someValuesFrom, NodeInfo nodeInfo) {

        OWLObjectProperty owlObjectProperty = someValuesFrom.objectPropertiesInSignature()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No object property found in someValuesFrom" + someValuesFrom));
        OWLClassExpression filler = someValuesFrom.getFiller();
        TableauxIndividual father = nodeInfo.getIndividual();
        TableauxIndividual son = tableauxIndividualFactory.getNewIndividual();
        filler.conjunctSet()
                .forEach(son::addingLabelCausesClash); //ignore clash for now, if there is clash it will be found in recursive call
        OWLObjectPropertyAssertionAxiom property =
                this.dataFactory.getOWLObjectPropertyAssertionAxiom(owlObjectProperty, father, son);
        return isClashFree(NodeInfo.builder()
                .individual(son)
                .classExpressions(Stream.empty())
                .newClassExpression(getSonNewClassExpressions(father, son, filler))
                .alreadyVisitedUnions(Collections.emptySet())
                .propertyAssertionAxiom(property)
                .build()
        );
    }

    private OWLClassExpression getSonNewClassExpressions(TableauxIndividual father, TableauxIndividual son, OWLClassExpression filler) {
        //todo: check quantificatore per ogni
        if (son.isBlocked(father)) {
            return filler;
        } else {
            return this.dataFactory.getOWLObjectIntersectionOf(filler, this.tBox);
        }
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
        return applyExistential(nodeInfo, newClassExpressions);
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
