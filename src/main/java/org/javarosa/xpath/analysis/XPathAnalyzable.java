package org.javarosa.xpath.analysis;

/**
 * Represents any object which may be subject to static analysis by an XPathAnalyzer
 * (XPathExpressions and TreeReferences)
 *
 * @author Aliza Stone
 */
public interface XPathAnalyzable {

    void applyAndPropagateAnalyzer(XPathAnalyzer analyzer) throws AnalysisInvalidException;

}
