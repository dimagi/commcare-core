package org.javarosa.xpath.analysis;

/**
 * Thrown when an XPathAnalyzer comes across an expression for which it may not be able to do a
 * complete/accurate analysis, to indicate that the results of the analysis should not  be used.
 */
public class AnalysisInvalidException extends Exception {

    // Static exception instances b/c generating them dynamically costs valuable time during analysis
    public static AnalysisInvalidException INSTANCE_NO_CONTEXT_REF =
            new AnalysisInvalidException("No context ref available when needed");
    public static AnalysisInvalidException INSTANCE_NO_ORIGINAL_CONTEXT_REF =
            new AnalysisInvalidException("No original context ref available when needed");
    public static AnalysisInvalidException INSTANCE_ITEMSET_ACCUM_FAILURE =
            new AnalysisInvalidException("Itemset accumulation failed");
    public static AnalysisInvalidException INSTANCE_TEXT_PARSE_FAILURE =
            new AnalysisInvalidException("Couldn't parse Text XPath Expression");

    public AnalysisInvalidException(String msg) {
        super(msg);
    }

}
