package intelligent.web.alc;

import intelligent.web.individual.TableauxIndividual;
import intelligent.web.individual.TableauxIndividualFactory;
import intelligent.web.rdf.RDFBuilder;
import intelligent.web.visitor.NNFMod;
import org.apache.commons.lang3.tuple.Pair;
import org.semanticweb.owlapi.model.*;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public class ALCReasoner {
    private final OWLDataFactory dataFactory;
    private final TableauxIndividualFactory tableauxIndividualFactory = TableauxIndividualFactory.getInstance();
    private final OWLObjectIntersectionOf concept;
    private final Set<OWLLogicalAxiom> unfoldableSet;
    private final LazyUnfolder lazyUnfolder;
    private final Logger LOGGER = getLogger(this.getClass());


    public ALCReasoner(OWLOntology ontology) {
        this.dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        this.lazyUnfolder = new LazyUnfolder(ontology.getLogicalAxioms(), ontology.getOWLOntologyManager().getOWLDataFactory());
        Pair<Set<OWLLogicalAxiom>, Set<OWLLogicalAxiom>> lazyUnfolding = lazyUnfolding();
        this.concept = extractConcept(lazyUnfolding);
        this.unfoldableSet = lazyUnfolding.getRight();
        LOGGER.info("ONTOLOGY: " + ontology.getLogicalAxioms());
        LOGGER.info("LAZY UNFOLDING Tu: " + lazyUnfolding.getRight());
        LOGGER.info("LAZY UNFOLDING Tg: " + lazyUnfolding.getLeft());
    }

    private Pair<Set<OWLLogicalAxiom>, Set<OWLLogicalAxiom>> lazyUnfolding() {
        return this.lazyUnfolder.lazyUnfolding();
    }

    public boolean isSatisfiable(OWLClassExpression classExpression) {
        long startTime = System.nanoTime();
        OWLClassExpression nnfQuery = classExpression.getNNF();
        TableauxIndividual a = tableauxIndividualFactory.getNewIndividual();
        final NodeInfo nodeInfo = NodeInfo.builder()
                .individual(a)
                .classExpressions(concept != null ? concept.conjunctSet() : Stream.empty())
                .newClassExpression(nnfQuery)
                .alreadyVisitedUnions(Collections.emptySet())
                .build();
        boolean isClashFree = isClashFree(nodeInfo);
        long endTime = System.nanoTime();
        long totalIsClashFreeExecution = (endTime - startTime)/1000000;
        LOGGER.info("isSatisfiable execution time: " + totalIsClashFreeExecution + "ms");
        long rdfCreationStartTime = System.nanoTime();
        RDFBuilder.writeTableauxAsString();
        try {
            RDFBuilder.createTableauxImage();
        }catch(Exception e){
            System.out.println("Error creating tableaux image!");
        }
        long rdfCreationEndTime = System.nanoTime();
        long totalRdfCreationExecutionTime = (rdfCreationEndTime - rdfCreationStartTime)/1000000;
        LOGGER.info("RDFTableauxCreation execution time: " + totalRdfCreationExecutionTime + "ms");
        RDFBuilder.flush();
        tableauxIndividualFactory.flush();
        long totalExecutionTime=totalIsClashFreeExecution + totalRdfCreationExecutionTime;
        LOGGER.info("Total execution time: " + totalExecutionTime + "ms");
        LOGGER.info("Is Satisfiable: " + isClashFree);
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
        if (isClashFound(nodeInfo, currentIndividual) || lazyUnfoldingRulesCauseClash(currentIndividual, nodeInfo)) {
            RDFBuilder.addClash(nodeInfo);
            return false;
        }

        RDFBuilder.addToRDFModel(nodeInfo);
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
        final NodeInfo newNodeInfo = NodeInfo.builder()
                .father(nodeInfo)
                .individual(son)
                .classExpressions(getSonClassExpressions(isSonBlocked))
                .newClassExpression(getSonNewClassExpressions(sonBasicClassExpressions, isSonBlocked))
                .alreadyVisitedUnions(Collections.emptySet())
                .propertyAssertionAxiom(property)
                .build();
        return isClashFree(newNodeInfo);
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
     * @return true if "or" is clash free
     */
    private boolean applyOr(NodeInfo nodeInfo, Set<OWLClassExpression> newClassExpressions) {
        Optional<OWLObjectUnionOf> unionOf = getUnvisitedUnionOf(newClassExpressions, nodeInfo.getAlreadyVisitedUnions());
        if (unionOf.isPresent()) {
            OWLObjectUnionOf owlObjectUnionOf = unionOf.get();
            long clashedOperands = owlObjectUnionOf.operands()
                    .takeWhile(p -> !addNodeToTableauxAndCheckIfClashFree(NodeInfo.getNewNode(nodeInfo, newClassExpressions.stream(), p, owlObjectUnionOf)))
                    .count();
            //if all operands were traversed, it means that all branches result in clash -> return false
            return clashedOperands != owlObjectUnionOf.operands().count();
        }
        return applySomeValuesFrom(nodeInfo, newClassExpressions);
    }

    private boolean addNodeToTableauxAndCheckIfClashFree(NodeInfo nodeInfo){
        RDFBuilder.addOr(nodeInfo);
        return isClashFree(nodeInfo);
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

    public void flush(){
        RDFBuilder.flush();
    }

}
