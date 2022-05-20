public class Individual {

    private final String name;
    private final Long id;
    private Individual father;

    public Individual(String name, Long id) {
        this.name = name;
        this.id = id;
    }

    public Individual(String name, Long id, Individual father) {
        this.name = name;
        this.id = id;
        this.father = father;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public Individual getFather() {
        return father;
    }

    public void setFather(Individual father) {
        this.father = father;
    }
}
