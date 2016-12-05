package org.javarosa.xpath.parser.ast;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ASTNodePredicate extends ASTNode {
    public ASTNode expr;

    @Override
    public List<? extends ASTNode> getChildren() {
        List<ASTNode> v = new ArrayList<>();
        v.add(expr);
        return v;
    }

    @Override
    public XPathExpression build(HashRefResolver hashRefResolver) throws XPathSyntaxException {
        return expr.build(hashRefResolver);
    }
}
