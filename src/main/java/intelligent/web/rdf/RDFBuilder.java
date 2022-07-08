package intelligent.web.rdf;

import intelligent.web.alc.NodeInfo;
import intelligent.web.visitor.ClassPrettyPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RDFBuilder {
    private static final String DEFAULT_URI = "http://locahost/";
    private static Model MODEL = ModelFactory.createDefaultModel();
    private static final Property CHILD = MODEL.createProperty(DEFAULT_URI + "child");
    private static final Property OR = MODEL.createProperty(DEFAULT_URI + "or");
    private static final Property EXPRESSION = MODEL.createProperty(DEFAULT_URI + "expression");
    private static final Property NEW_EXPRESSION = MODEL.createProperty(DEFAULT_URI + "new_expression");
    private static final Property CLASH = MODEL.createProperty(DEFAULT_URI + "clash");
    private static final Set<Integer> addedNodes = new HashSet<>();

    public static void addToRDFModel(NodeInfo nodeInfo){
        if(MODEL.isEmpty())
            addExpressionToResource(createNewResource(nodeInfo), nodeInfo);
        else
            addChildToFather(nodeInfo);
    }

    public static void addOr(NodeInfo nodeInfo){
        Resource father = MODEL.getResource(DEFAULT_URI+ nodeInfo.getFather().hashCode());
        father.addProperty(OR, addExpressionToResource(createNewResource(nodeInfo), nodeInfo));
    }
    
    private static void addChildToFather(NodeInfo nodeInfo){
        Resource resource;
        final NodeInfo father = nodeInfo.getFather();
        if(father != null) {
            resource = MODEL.getResource(DEFAULT_URI + nodeInfo.getFather().hashCode());
            if(!addedNodes.contains(nodeInfo.hashCode()))
                resource.addProperty(CHILD, addExpressionToResource(createNewResource(nodeInfo), nodeInfo));
        }else {
            if (!addedNodes.contains(nodeInfo.hashCode()))
                addExpressionToResource(createNewResource(nodeInfo), nodeInfo);
        }
    }
    
    private static Resource createNewResource(NodeInfo nodeInfo){
        addedNodes.add(nodeInfo.hashCode());
        return MODEL.createResource(DEFAULT_URI + nodeInfo.hashCode());
    }
    
    private static Resource addExpressionToResource(Resource resource, NodeInfo nodeInfo){
        final List<OWLClassExpression> classExpressions = nodeInfo.getClassExpressions().toList();
        nodeInfo.setClassExpressions(classExpressions.stream());
        String classExp;
        resource.addProperty(NEW_EXPRESSION, "X" + nodeInfo.getIndividual() + ": " + ClassPrettyPrinter.printOwlExpression(nodeInfo.getNewClassExpression()));
        if(!classExpressions.isEmpty()) {
            final List<String> collect = classExpressions.stream().map(ClassPrettyPrinter::printOwlExpression).toList();
            classExp = StringUtils.join(collect, ", ");
            resource.addProperty(EXPRESSION, "X" + nodeInfo.getIndividual() + ": " + classExp);
        }

        return resource;
    }

    public static void addClash(NodeInfo nodeInfo){
        Resource resource = MODEL.getResource(DEFAULT_URI + nodeInfo.hashCode());
        resource.addProperty(CLASH, "CLASH");
        if(!addedNodes.contains(nodeInfo.hashCode()))
            addChildToFather(nodeInfo);
    }

    public static Model getModel(){
        return MODEL;
    }

    public static String writeTableauxAsString(){
        StringWriter stringWriter = new StringWriter();
        MODEL.write(stringWriter);
        return stringWriter.toString();
    }

    public static String createTableauxImage() throws IOException {
        String tableaux = writeTableauxAsString();
        BufferedWriter writer = new BufferedWriter(new FileWriter("tableaux.rdf"));
        writer.write(tableaux);
        writer.close();
        String fileName = new SimpleDateFormat("yyyyMMddHHmm'.png'").format(new Date());
        Process process = Runtime.getRuntime().exec("python3 rdfToPng.py");
        return fileName;
    }

    public static void flush(){
        addedNodes.clear();
        MODEL = ModelFactory.createDefaultModel();
    }
}
