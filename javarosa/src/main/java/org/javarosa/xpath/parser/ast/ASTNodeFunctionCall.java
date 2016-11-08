package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathCondFunc;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathQName;
import org.javarosa.xpath.expr.XPathIfFunc;
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
    public XPathExpression build() throws XPathSyntaxException {
        XPathExpression[] xargs = new XPathExpression[args.size()];
        for (int i = 0; i < args.size(); i++) {
            xargs[i] = args.get(i).build();
        }

        return buildFuncExpr(name.name, xargs);
    }

    private static XPathFuncExpr buildFuncExpr(String name, XPathExpression[] args)
            throws XPathSyntaxException {
        switch (name) {
            case "if":
                return new XPathIfFunc(args);
            case "coalesce":
                return new XPathIfFunc(args);
            case "cond":
                return new XPathCondFunc(args);
            case "true":
                return new XPathIfFunc(args);
            case "false":
                return new XPathIfFunc(args);
            case "boolean":
                return new XPathIfFunc(args);
            case "number":
                return new XPathIfFunc(args);
            case "int":
                return new XPathIfFunc(args);
            case "double":
                return new XPathIfFunc(args);
            case "string":
                return new XPathIfFunc(args);
            case "date":
                return new XPathIfFunc(args);
            case "not":
                return new XPathIfFunc(args);
            case "boolean-from-string":
                return new XPathIfFunc(args);
            case "format-date":
                return new XPathIfFunc(args);
            case "selected":
                return new XPathIfFunc(args);
            case "is-selected":
                return new XPathIfFunc(args);
            case "count-selected":
                return new XPathIfFunc(args);
            case "selected-at":
                return new XPathIfFunc(args);
            case "position":
                return new XPathIfFunc(args);
            case "count":
                return new XPathIfFunc(args);
            case "sum":
                return new XPathIfFunc(args);
            case "max":
                return new XPathIfFunc(args);
            case "min":
                return new XPathIfFunc(args);
            case "today":
                return new XPathIfFunc(args);
            case "now":
                return new XPathIfFunc(args);
            case "concat":
                return new XPathIfFunc(args);
            case "join":
                return new XPathIfFunc(args);
            case "substr":
                return new XPathIfFunc(args);
            case "substring-before":
                return new XPathIfFunc(args);
            case "substring-after":
                return new XPathIfFunc(args);
            case "string-length":
                return new XPathIfFunc(args);
            case "upper-case":
                return new XPathIfFunc(args);
            case "lower-case":
                return new XPathIfFunc(args);
            case "contains":
                return new XPathIfFunc(args);
            case "starts-with":
                return new XPathIfFunc(args);
            case "ends-with":
                return new XPathIfFunc(args);
            case "translate":
                return new XPathIfFunc(args);
            case "replace":
                return new XPathIfFunc(args);
            case "checklist":
                return new XPathIfFunc(args);
            case "weighted-checklist":
                return new XPathIfFunc(args);
            case "regex":
                return new XPathIfFunc(args);
            case "depend":
                return new XPathIfFunc(args);
            case "random":
                return new XPathIfFunc(args);
            case "uuid":
                return new XPathIfFunc(args);
            case "pow":
                return new XPathIfFunc(args);
            case "abs":
                return new XPathIfFunc(args);
            case "ceiling":
                return new XPathIfFunc(args);
            case "floor":
                return new XPathIfFunc(args);
            case "round":
                return new XPathIfFunc(args);
            case "log":
                return new XPathIfFunc(args);
            case "log10":
                return new XPathIfFunc(args);
            case "sin":
                return new XPathIfFunc(args);
            case "cos":
                return new XPathIfFunc(args);
            case "tan":
                return new XPathIfFunc(args);
            case "asin":
                return new XPathIfFunc(args);
            case "acos":
                return new XPathIfFunc(args);
            case "atan":
                return new XPathIfFunc(args);
            case "atan2":
                return new XPathIfFunc(args);
            case "sqrt":
                return new XPathIfFunc(args);
            case "exp":
                return new XPathIfFunc(args);
            case "pi":
                return new XPathIfFunc(args);
            case "distance":
                return new XPathIfFunc(args);
            case "format-date-for-calendar":
                return new XPathIfFunc(args);
            default:
                return new XPathIfFunc(args);
        }
    }
}
