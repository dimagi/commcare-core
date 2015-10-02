package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFilterExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Enumeration;
import java.util.Vector;

public class ASTNodeFilterExpr extends ASTNode {
    public ASTNodeAbstractExpr expr;
    public Vector predicates;

    public ASTNodeFilterExpr() {
        predicates = new Vector();
    }

    @Override
    public Vector getChildren() {
        Vector v = new Vector();
        v.addElement(expr);
        for (Enumeration e = predicates.elements(); e.hasMoreElements(); )
            v.addElement(e.nextElement());
        return v;
    }

    @Override
    public XPathExpression build() throws XPathSyntaxException {
        XPathExpression[] preds = new XPathExpression[predicates.size()];
        for (int i = 0; i < preds.length; i++)
            preds[i] = ((ASTNode)predicates.elementAt(i)).build();

        return new XPathFilterExpr(expr.build(), preds);
    }
}
