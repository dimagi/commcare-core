package org.javarosa.xpath;

import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.Lexer;
import org.javarosa.xpath.parser.Parser;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathParseTool {
    public static XPathExpression parseXPath(String xpath) throws XPathSyntaxException {
        return Parser.parse(Lexer.lex(xpath));
    }
}
