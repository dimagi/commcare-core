package org.javarosa.xpath.parser.ast;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathQName;
import org.javarosa.xpath.expr.functions.IfFunc;
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

    private static XPathFuncExpr buildFuncExpr(String name, XPathExpression[] args) {
        switch (name) {
            case "if":
                return new IfFunc(args);
            case "coalesce":
                return new IfFunc(args);
            case "cond":
                return new IfFunc(args);
            case "true":
                return new IfFunc(args);
            case "false":
                return new IfFunc(args);
            case "boolean":
                return new IfFunc(args);
            case "number":
                return new IfFunc(args);
            case "int":
                return new IfFunc(args);
            case "double":
                return new IfFunc(args);
            case "string":
                return new IfFunc(args);
            case "date":
                return new IfFunc(args);
            case "not":
                return new IfFunc(args);
            case "boolean-from-string":
                return new IfFunc(args);
            case "format-date":
                return new IfFunc(args);
            case "selected":
                return new IfFunc(args);
            case "is-selected":
                return new IfFunc(args);
            case "count-selected":
                return new IfFunc(args);
            case "selected-at":
                return new IfFunc(args);
            case "position":
                return new IfFunc(args);
            case "count":
                return new IfFunc(args);
            case "sum":
                return new IfFunc(args);
            case "max":
                return new IfFunc(args);
            case "min":
                return new IfFunc(args);
            case "today":
                return new IfFunc(args);
            case "now":
                return new IfFunc(args);
            case "concat":
                return new IfFunc(args);
            case "join":
                return new IfFunc(args);
            case "substr":
                return new IfFunc(args);
            case "substring-before":
                return new IfFunc(args);
            case "substring-after":
                return new IfFunc(args);
            case "string-length":
                return new IfFunc(args);
            case "upper-case":
                return new IfFunc(args);
            case "lower-case":
                return new IfFunc(args);
            case "contains":
                return new IfFunc(args);
            case "starts-with":
                return new IfFunc(args);
            case "ends-with":
                return new IfFunc(args);
            case "translate":
                return new IfFunc(args);
            case "replace":
                return new IfFunc(args);
            case "checklist":
                return new IfFunc(args);
            case "weighted-checklist":
                return new IfFunc(args);
            case "regex":
                return new IfFunc(args);
            case "depend":
                return new IfFunc(args);
            case "random":
                return new IfFunc(args);
            case "uuid":
                return new IfFunc(args);
            case "pow":
                return new IfFunc(args);
            case "abs":
                return new IfFunc(args);
            case "ceiling":
                return new IfFunc(args);
            case "floor":
                return new IfFunc(args);
            case "round":
                return new IfFunc(args);
            case "log":
                return new IfFunc(args);
            case "log10":
                return new IfFunc(args);
            case "sin":
                return new IfFunc(args);
            case "cos":
                return new IfFunc(args);
            case "tan":
                return new IfFunc(args);
            case "asin":
                return new IfFunc(args);
            case "acos":
                return new IfFunc(args);
            case "atan":
                return new IfFunc(args);
            case "atan2":
                return new IfFunc(args);
            case "sqrt":
                return new IfFunc(args);
            case "exp":
                return new IfFunc(args);
            case "pi":
                return new IfFunc(args);
            case "distance":
                return new IfFunc(args);
            case "format-date-for-calendar":
                return new IfFunc(args);
            default:
                return new IfFunc(args);
        }
    }
}
