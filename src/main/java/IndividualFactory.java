public class IndividualFactory {

    private static IndividualFactory instance;

    private Long maxId;

    private IndividualFactory() {
        this.maxId = 0L;
    }

    public static IndividualFactory getInstance(){
        if(instance == null){
            instance = new IndividualFactory();
        }
        return instance;
    }

    public Individual getNewIndividual(){
        this.maxId++;
        //TODO: GENERATE NAME
        return new Individual(maxId.toString(), maxId);
    }

    public Individual getNewIndividual(Individual father){
        this.maxId++;
        //TODO: GENERATE NAME
        return new Individual(maxId.toString(), maxId, father);
    }

}
