import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.Set;

public class TableauxIndividualFactory {

    private static TableauxIndividualFactory instance;

    private Long maxId;

    private TableauxIndividualFactory() {
        this.maxId = 0L;
    }

    public static TableauxIndividualFactory getInstance(){
        if(instance == null){
            instance = new TableauxIndividualFactory();
        }
        return instance;
    }

    public TableauxIndividual getNewIndividual(){
        this.maxId++;
        //TODO: GENERATE NAME
        return new TableauxIndividual(maxId.toString(), maxId);
    }

    public TableauxIndividual getNewIndividual(TableauxIndividual father){
        this.maxId++;
        //TODO: GENERATE NAME
        return new TableauxIndividual(maxId.toString(), maxId, father);
    }

}
