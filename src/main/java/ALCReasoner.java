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
    protected final Map<TableauxIndividual, Set<OWLClassExpression>> blockingMap = new HashMap<>();
    protected final TableauxIndividualFactory tableauxIndividualFactory = TableauxIndividualFactory.getInstance();
    protected final HashMap<TableauxIndividual, Set<OWLClassExpression>> literals = new HashMap<>();

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
        Set<OWLClassExpression> classExpressions = new HashSet<>();
        classExpressions.add(tBox);
        blockingMap.put(a, classExpressions);
        return tableaux(a, new HashSet<>(), nnfQuery);
    }

    /**
     *
     * @param tableauxIndividual
     * @param alreadyVisitedUnions
     * @return isClashFree
     */
    public boolean tableaux(TableauxIndividual tableauxIndividual, Set<OWLObjectUnionOf> alreadyVisitedUnions, OWLClassExpression newClassExpression) {
        boolean ret;
        initializeLiteralsSet(literals, tableauxIndividual);
        Set<OWLClassExpression> classExpressions = blockingMap.get(tableauxIndividual);
        applyAnd(newClassExpression, classExpressions);
        if (clashFound(literals.get(tableauxIndividual), newClassExpression)){
            removeClassExpressions(classExpressions, newClassExpression.conjunctSet());
            return false;
        }
        ret = applyOr(tableauxIndividual, alreadyVisitedUnions, literals.get(tableauxIndividual), classExpressions);
        //handling EXISTENTIAL
        //before going up, we must remove literals added to L(x)
        removeLiterals(literals.get(tableauxIndividual), newClassExpression.asConjunctSet());
        removeClassExpressions(classExpressions, newClassExpression.conjunctSet());
        return ret;
    }

    private void removeClassExpressions(Set<OWLClassExpression> classExpressions, Stream<OWLClassExpression> classExpressionsToRemove) {
        classExpressionsToRemove
                .forEach(classExpressions::remove);
    }

    private void initializeLiteralsSet(HashMap<TableauxIndividual, Set<OWLClassExpression>> literals, TableauxIndividual tableauxIndividual) {
        literals.computeIfAbsent(tableauxIndividual, k -> new HashSet<>());
    }

    private boolean applyOr(TableauxIndividual tableauxIndividual, Set<OWLObjectUnionOf> alreadyVisitedUnions, Set<OWLClassExpression> literals, Set<OWLClassExpression> classExpressions) {

        Optional<OWLObjectUnionOf> owlUnionOf = classExpressions.stream()
                .filter(p -> p instanceof OWLObjectUnionOf)
                .filter(p -> !alreadyVisitedUnions.contains(p))
                .map(p -> (OWLObjectUnionOf) p)
                .findAny();

        if(owlUnionOf.isPresent()){
            OWLObjectUnionOf owlObjectUnionOf = owlUnionOf.get();
            List<OWLClassExpression> operands = owlObjectUnionOf.operands().toList();
            alreadyVisitedUnions.add(owlObjectUnionOf);
            boolean clashFree = tableaux(tableauxIndividual, alreadyVisitedUnions, operands.get(0));
            if(!clashFree){ //try other branch
                clashFree = tableaux(tableauxIndividual, alreadyVisitedUnions, operands.get(1));
                return clashFree; //if this branch also is not clash free, returns false
            }
        }
        return true;
    }

    /**
     * When going up in the chain, we must remove literals from L(x)
     * @param literals
     * @param classExpressions
     */
    private void removeLiterals(Set<OWLClassExpression> literals, Set<OWLClassExpression> classExpressions) {
        classExpressions.stream()
                .filter(OWLClassExpression::isClassExpressionLiteral)
                .forEach(literals::remove);
    }

    private void applyAnd(OWLClassExpression newClassExpression, Set<OWLClassExpression> classExpressions) {
        //TODO: QUESTO VA QUA?? TEORICAMENTE AGGIUNGE COSE SOLO ALLA PRIMA ESECUZIONE, OVVERO QUANDO SI SPACCHETTA LA TBOX
        classExpressions.addAll(
                classExpressions.stream().flatMap(OWLClassExpression::conjunctSet)
                        .collect(Collectors.toSet())
        );
        classExpressions.addAll(newClassExpression.asConjunctSet());
    }

    private boolean clashFound(Set<OWLClassExpression> literals, OWLClassExpression newClassExpression) {
        boolean clashFound = newClassExpression.conjunctSet()
                .map(OWLClassExpression::getComplementNNF)
                .filter(OWLClassExpression::isClassExpressionLiteral)
                .anyMatch(literals::contains);
        if(clashFound) return  true;
        else { //if no clash is found, we add every literal to L(x)
            newClassExpression.conjunctSet()
            .filter(OWLClassExpression::isClassExpressionLiteral)
                    .forEach(literals::add);
            return false;
        }
    }

}
