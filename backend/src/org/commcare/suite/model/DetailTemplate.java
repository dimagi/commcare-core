package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * Interface for any class that can be used as a template in a detail field.
 *
 * @author jschweers
 */
public interface DetailTemplate {
    /**
     * Generate the actual data for this template, given a specific EvaluationContext.
     */
    public Object evaluate(EvaluationContext context) throws XPathSyntaxException;
}
