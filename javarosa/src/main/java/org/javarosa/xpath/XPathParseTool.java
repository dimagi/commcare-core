package org.javarosa.xpath;

import org.javarosa.core.model.condition.HashRefResolver;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.Lexer;
import org.javarosa.xpath.parser.Parser;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathParseTool {
    public static final String[] xpathClasses = {
            "org.javarosa.xpath.expr.XPathArithExpr",
            "org.javarosa.xpath.expr.XPathBoolExpr",
            "org.javarosa.xpath.expr.XPathCmpExpr",
            "org.javarosa.xpath.expr.XPathEqExpr",
            "org.javarosa.xpath.expr.XPathFilterExpr",
            "org.javarosa.xpath.expr.XPathFuncExpr",
            "org.javarosa.xpath.expr.XPathNumericLiteral",
            "org.javarosa.xpath.expr.XPathNumNegExpr",
            "org.javarosa.xpath.expr.XPathPathExpr",
            "org.javarosa.xpath.expr.XPathStringLiteral",
            "org.javarosa.xpath.expr.XPathUnionExpr",
            "org.javarosa.xpath.expr.XPathVariableReference"
    };

    public static XPathExpression parseXPath(String xpath) throws XPathSyntaxException {
        return parseXPath(xpath, null);
    }

    public static XPathExpression parseXPath(String xpath, HashRefResolver hashRefResolver) throws XPathSyntaxException {
        return Parser.parse(Lexer.lex(xpath), hashRefResolver);
    }
}
