import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class RDFBuilder {
    private static final String DEFAULT_URI = "http://locahost/";
    private static Model MODEL = ModelFactory.createDefaultModel();
    private static final Property CHILD = MODEL.createProperty(DEFAULT_URI + "child");
    private static final Property EXPRESSION = MODEL.createProperty(DEFAULT_URI + "expression");
    private static final Property CLASH = MODEL.createProperty(DEFAULT_URI + "clash");

    public static void addToRDFModel(NodeInfo nodeInfo){
        if(MODEL.isEmpty())
            addExpressionToResource(createNewResource(nodeInfo), nodeInfo);
        else
            addChildToFather(nodeInfo);
    }
    
    private static void addChildToFather(NodeInfo nodeInfo){
        Resource father = MODEL.getResource(DEFAULT_URI+ nodeInfo.getFather().hashCode());
        father.addProperty(CHILD, addExpressionToResource(createNewResource(nodeInfo), nodeInfo));
    }
    
    private static Resource createNewResource(NodeInfo nodeInfo){
        return MODEL.createResource(DEFAULT_URI + nodeInfo.hashCode());
    }
    
    private static Resource addExpressionToResource(Resource resource, NodeInfo nodeInfo){
        return resource.addProperty(EXPRESSION, "X" + nodeInfo.getIndividual() + ": " + PrettyPrinter.printOwlExpression(nodeInfo.getNewClassExpression()));
    }

    public static void addClash(NodeInfo nodeInfo){
        Resource resource = MODEL.getResource(DEFAULT_URI + nodeInfo.hashCode());
        resource.addProperty(CLASH, "CLASH: " + nodeInfo.hashCode());
    }

    public static Model getModel(){
        return MODEL;
    }

    public static void flush(){
        MODEL = ModelFactory.createDefaultModel();
    }
}
