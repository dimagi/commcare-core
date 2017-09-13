package org.javarosa.xpath.analysis;

/**
 * Thrown when an XPathAnalyzer comes across an expression for which it may not be able to do a
 * complete/accurate analysis, to indicate that the results of the analysis should not  be used
 */
public class AnalysisInvalidException extends Exception {

    public AnalysisInvalidException(String msg) {
        super(msg);
    }
}
