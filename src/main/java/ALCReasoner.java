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
    protected final Map<Individual, Set<OWLClassExpression>> blockingMap = new HashMap<>();
    protected final Set<OWLClassExpression> literals = new HashSet<>();
    protected final IndividualFactory individualFactory = IndividualFactory.getInstance();

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
        Individual a = individualFactory.getNewIndividual();
        Set<OWLClassExpression> classExpressions = new HashSet<>();
        classExpressions.add(tBox);
        blockingMap.put(a, classExpressions);

        return tableaux(a, blockingMap, new HashSet<>(), new HashSet<>(), nnfQuery);
    }

    /**
     *
     * @param individual
     * @param blockingMap
     * @param alreadyVisitedUnions
     * @return isClashFree
     */
    //TODO: REFACTOR, CREATE CLASS FOR TABLEAUX
    public boolean tableaux(Individual individual, Map<Individual, Set<OWLClassExpression>> blockingMap, Set<OWLObjectUnionOf> alreadyVisitedUnions, Set<OWLClassExpression> literals, OWLClassExpression newClassExpression) {
        boolean ret;
        if (clashFound(literals, newClassExpression)) return false;
        Set<OWLClassExpression> classExpressions = blockingMap.get(individual);
        applyAnd(newClassExpression, classExpressions);
        ret = applyOr(individual, blockingMap, alreadyVisitedUnions, literals, classExpressions);

        //handling EXISTENTIAL
        return ret;
    }

    private boolean applyOr(Individual individual, Map<Individual, Set<OWLClassExpression>> blockingMap, Set<OWLObjectUnionOf> alreadyVisitedUnions, Set<OWLClassExpression> literals, Set<OWLClassExpression> classExpressions) {
        boolean ret = true;
        Optional<OWLObjectUnionOf> owlUnionOf = classExpressions.stream()
                .filter(p -> p instanceof OWLObjectUnionOf)
                .filter(p -> !alreadyVisitedUnions.contains(p))
                .map(p -> (OWLObjectUnionOf) p)
                .findAny();

        if(owlUnionOf.isPresent()){
            OWLObjectUnionOf owlObjectUnionOf = owlUnionOf.get();
            List<OWLClassExpression> operands = owlObjectUnionOf.operands().toList();
            alreadyVisitedUnions.add(owlObjectUnionOf);
            ret = tableaux(individual, blockingMap, alreadyVisitedUnions, literals, operands.get(1));
            if(!ret){
                classExpressions.add(operands.get(1));
                ret = tableaux(individual, blockingMap, alreadyVisitedUnions, literals, operands.get(0));
            }
        }
        return ret;
    }

    private void applyAnd(OWLClassExpression newClassExpression, Set<OWLClassExpression> classExpressions) {
        classExpressions.addAll(
                classExpressions.stream().flatMap(OWLClassExpression::conjunctSet)
                        .collect(Collectors.toSet())
        );
        classExpressions.addAll(newClassExpression.asConjunctSet());
    }

    private boolean clashFound(Set<OWLClassExpression> literals, OWLClassExpression newClassExpression) {
        if(newClassExpression.isClassExpressionLiteral()){
            if(literals.contains(newClassExpression.getComplementNNF())){
                return true;
            }
            literals.add(newClassExpression);
        }
        return false;
    }

}
