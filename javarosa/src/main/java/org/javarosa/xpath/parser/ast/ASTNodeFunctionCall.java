package org.javarosa.xpath.parser.ast;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathQName;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class ASTNodeFunctionCall extends ASTNode {
    public final XPathQName name;
    public List<? extends ASTNode> args;

    public ASTNodeFunctionCall(XPathQName name) {
        this.name = name;
        args = new ArrayList<>();
    }

    @Override
    public List<? extends ASTNode> getChildren() {
        return args;
    }

    @Override
    public XPathExpression build(HashRefResolver hashRefResolver) throws XPathSyntaxException {
        XPathExpression[] xargs = new XPathExpression[args.size()];
        for (int i = 0; i < args.size(); i++) {
            xargs[i] = args.get(i).build(hashRefResolver);
        }

        return new XPathFuncExpr(name, xargs);
    }
}
