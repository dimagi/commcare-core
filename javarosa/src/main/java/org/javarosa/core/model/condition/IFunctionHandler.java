package org.javarosa.core.model.condition;

import org.javarosa.xpath.XPathArityException;

import java.util.Vector;

public interface IFunctionHandler {
    /**
     * @return The name of function being handled
     */
    String getName();

    /**
     * @return Vector of allowed prototypes for this function. Each prototype is
     * an array of Class, corresponding to the types of the expected
     * arguments. The first matching prototype is used.
     */
    Vector getPrototypes();

    /**
     * @return true if this handler should be fed the raw argument list if no
     * prototype matches it
     */
    boolean rawArgs();

    /**
     * Evaluate the function
     */
    Object eval(Object[] args, EvaluationContext ec) throws XPathArityException;
}
