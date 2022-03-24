package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.Externalizable;

/**
 * Interface for classes that represent query data elements.
 */
public interface QueryData extends Externalizable {
    String getKey();
    String getValue(EvaluationContext context);
}
