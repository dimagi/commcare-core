package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathQName;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Vector;

public class ASTNodeFunctionCall extends ASTNode {
    public final XPathQName name;
    public Vector args;

    public ASTNodeFunctionCall(XPathQName name) {
        this.name = name;
        args = new Vector();
    }

    public Vector getChildren() {
        return args;
    }

    public XPathExpression build() throws XPathSyntaxException {
        XPathExpression[] xargs = new XPathExpression[args.size()];
        for (int i = 0; i < args.size(); i++)
            xargs[i] = ((ASTNode)args.elementAt(i)).build();

        return new XPathFuncExpr(name, xargs);
    }
}
