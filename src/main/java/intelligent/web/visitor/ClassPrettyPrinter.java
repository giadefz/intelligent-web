package intelligent.web.visitor;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.model.*;

import java.util.List;

public class ClassPrettyPrinter implements OWLClassExpressionVisitorEx<String> {
    private final static ClassPrettyPrinter CLASS_PRETTY_PRINTER = new ClassPrettyPrinter(true);
    private boolean andSymbolAsComma;
    private String andSymbol;

    public ClassPrettyPrinter(boolean andSymbolAsComma) {
        this.andSymbolAsComma = andSymbolAsComma;
        if(andSymbolAsComma) andSymbol = ", ";
        else andSymbol = " ∧ ";
    }

    @Override
    public String visit(OWLObjectIntersectionOf ce) {
        List<String> collect = ce.operands()
                .map(o -> o.accept(this)).toList();
        return StringUtils.join(collect, andSymbol);
    }

    @Override
    public String visit(OWLObjectUnionOf ce) {
        List<String> collect = ce.operands()
                .map(o -> o.accept(this))
                .toList();
        return "(" + StringUtils.join(collect, " ∨ ") + ")";
    }

    @Override
    public String visit(OWLObjectComplementOf ce) {
        return "¬".concat(ce.getOperand().accept(this));
    }

    @Override
    public String visit(OWLObjectSomeValuesFrom ce) {
        return "∃".concat(((OWLObjectProperty)ce.getProperty()).getIRI().getShortForm())
                .concat(".")
                .concat(ce.getFiller().accept(this));
    }

    @Override
    public String visit(OWLObjectAllValuesFrom ce) {
        return "∀".concat(((OWLObjectProperty)ce.getProperty()).getIRI().getShortForm())
                .concat(".")
                .concat(ce.getFiller().accept(this));
    }

    @Override
    public String visit(OWLClass ce) {
        return ce.getIRI().getShortForm();
    }

    public static String printOwlExpression(OWLClassExpression owlClassExpression){
        return owlClassExpression.accept(CLASS_PRETTY_PRINTER);
    }
}
