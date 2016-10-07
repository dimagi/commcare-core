package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathNumNegExpr;
import org.javarosa.xpath.expr.XPathUnaryOpExpr;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Vector;

public class ASTNodeUnaryOp extends ASTNode {
    public ASTNode expr;
    public int op;

    @Override
    public Vector getChildren() {
        Vector<ASTNode> v = new Vector<>();
        v.addElement(expr);
        return v;
    }

    @Override
    public XPathExpression build() throws XPathSyntaxException {
        XPathUnaryOpExpr x;
        if (op == Token.UMINUS) {
            x = new XPathNumNegExpr(expr.build());
        } else {
            throw new XPathSyntaxException();
        }
        return x;
    }
}
