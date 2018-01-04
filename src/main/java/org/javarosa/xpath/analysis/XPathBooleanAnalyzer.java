package org.javarosa.xpath.analysis;

/**
 * A type of XPathAnalyzer that can evaluate an XPath expression for whether a given condition
 * is true or false for the expression as a whole
 *
 * @author Aliza Stone
 */
public abstract class XPathBooleanAnalyzer extends XPathAnalyzer {

    protected boolean determination = false;

}
