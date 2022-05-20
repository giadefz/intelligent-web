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
    protected Map<Individual, Set<OWLClassExpression>> blockingMap;

    public ALCReasoner(OWLOntology ontology, OWLDataFactory dataFactory) {
        this.ontology = ontology;
        this.dataFactory = dataFactory;
        this.temp = dataFactory.getOWLClass("temp");
        this.blockingMap = new HashMap<>();
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
        Stream<OWLClassExpression> classExpressions = Stream.of(this.tBox, nnfQuery);
        Individual a = new Individual("a", 1l);
        blockingMap.put(a, classExpressions.collect(Collectors.toSet()));
        return tableaux(a, blockingMap, new HashSet<>());
    }

    /**
     *
     * @param individual
     * @param blockingMap
     * @param alreadyVisitedUnions
     * @return isClashFree
     */
    public boolean tableaux(Individual individual, Map<Individual, Set<OWLClassExpression>> blockingMap, Set<OWLObjectUnionOf> alreadyVisitedUnions) {
        boolean ret = true;
        Set<OWLClassExpression> classExpressions = blockingMap.get(individual);
        classExpressions.addAll(
                classExpressions.stream().flatMap(OWLClassExpression::conjunctSet)
                        .collect(Collectors.toSet())
        );
        //check blocking
        Optional<OWLObjectUnionOf> owlUnionOf = classExpressions.stream()
                .filter(p -> p instanceof OWLObjectUnionOf)
                .filter(p -> !alreadyVisitedUnions.contains(p))
                .map(p -> (OWLObjectUnionOf) p)
                .findAny();

        if(owlUnionOf.isPresent()){
            OWLObjectUnionOf owlObjectUnionOf = owlUnionOf.get();
            List<OWLClassExpression> operands = owlObjectUnionOf.operands().toList();
            classExpressions.add(operands.get(0));
            alreadyVisitedUnions.add(owlObjectUnionOf);
            ret = tableaux(individual, blockingMap, alreadyVisitedUnions);
            if(!ret){
                classExpressions.remove(operands.get(0));
                classExpressions.add(operands.get(1));
                ret = tableaux(individual,blockingMap, alreadyVisitedUnions);
            }
        }
        //check esistenziale
        return ret;
    }

}
