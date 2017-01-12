package org.javarosa.xpath.parser.ast;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFilterExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class ASTNodeFilterExpr extends ASTNode {
    public ASTNodeAbstractExpr expr;
    public final Vector<ASTNode> predicates;

    public ASTNodeFilterExpr() {
        predicates = new Vector<>();
    }

    @Override
    public List<? extends ASTNode> getChildren() {
        List<ASTNode> list = new ArrayList<>();
        list.add(expr);
        for (Enumeration<ASTNode> e = predicates.elements(); e.hasMoreElements(); ) {
            list.add(e.nextElement());
        }
        return list;
    }

    @Override
    public XPathExpression build(HashRefResolver hashRefResolver) throws XPathSyntaxException {
        XPathExpression[] preds = new XPathExpression[predicates.size()];
        for (int i = 0; i < preds.length; i++)
            preds[i] = predicates.elementAt(i).build(hashRefResolver);

        return new XPathFilterExpr(expr.build(hashRefResolver), preds);
    }
}
