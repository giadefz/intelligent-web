import org.semanticweb.owlapi.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ALCReasoner {

    private final OWLOntology ontology;
    private final OWLDataFactory dataFactory;
    private final Stream<OWLAxiom> axiomsInNNF;
    //private final OWLClass temp;
    private final OWLObjectIntersectionOf tBox;
    private final TableauxIndividualFactory tableauxIndividualFactory = TableauxIndividualFactory.getInstance();

    public ALCReasoner(OWLOntology ontology, OWLDataFactory dataFactory) {
        this.ontology = ontology;
        System.out.println("ONTOLOGY: " + ontology);
        this.dataFactory = dataFactory;
        //this.temp = dataFactory.getOWLClass("temp");
        //OWLDeclarationAxiom da = dataFactory.getOWLDeclarationAxiom(temp);
        //ontology.add(da);
        if (this.ontology.getLogicalAxiomCount() == 0){
            this.axiomsInNNF = null;
            this.tBox = null;
        }else{
            this.axiomsInNNF = computeAxiomsInNNF();
            this.tBox = getTBox();
        }
    }

    private Stream<OWLAxiom> computeAxiomsInNNF() {
        //return ontology.logicalAxioms().map(l -> l.accept(new NNFMod(dataFactory, temp)));
        return ontology.logicalAxioms().map(l -> l.accept(new NNFMod(dataFactory,  dataFactory.getOWLThing())));
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

    private boolean isClashFree(NodeInfo nodeInfo) {
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
        return isClashFree(NodeInfo.builder()
                .individual(son)
                .classExpressions(Stream.empty())
                .newClassExpression(getSonNewClassExpressions(father, son, sonBasicClassExpressions))
                .alreadyVisitedUnions(Collections.emptySet())
                .propertyAssertionAxiom(property)
                .build()
        );
    }

    private OWLObjectProperty getOwlObjectProperty(OWLObjectSomeValuesFrom someValuesFrom) {
        return someValuesFrom.objectPropertiesInSignature()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No object property found in someValuesFrom" + someValuesFrom));
    }

    private OWLClassExpression getSonNewClassExpressions(TableauxIndividual father, TableauxIndividual son, OWLClassExpression sonBasicClassExpressions) {
        if (son.isBlocked() | this.tBox==null) {
            return sonBasicClassExpressions;
        } else {
            return this.dataFactory.getOWLObjectIntersectionOf(sonBasicClassExpressions, this.tBox);
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
