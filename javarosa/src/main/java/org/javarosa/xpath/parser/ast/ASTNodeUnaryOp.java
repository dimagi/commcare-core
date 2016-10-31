package org.javarosa.xpath.parser.ast;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathNumNegExpr;
import org.javarosa.xpath.expr.XPathUnaryOpExpr;
import org.javarosa.xpath.parser.Token;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeUnaryOp extends ASTNode {
    public ASTNode expr;
    public int op;

    @Override
    public List<? extends ASTNode> getChildren() {
        List<ASTNode> v = new ArrayList<>();
        v.add(expr);
        return v;
    }

    @Override
    public XPathExpression build(HashRefResolver hashRefResolver) throws XPathSyntaxException {
        XPathUnaryOpExpr x;
        if (op == Token.UMINUS) {
            x = new XPathNumNegExpr(expr.build(hashRefResolver));
        } else {
            throw new XPathSyntaxException();
        }
        return x;
    }
}
