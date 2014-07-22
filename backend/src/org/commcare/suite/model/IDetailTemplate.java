package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;

public interface IDetailTemplate {
	public Object evaluate(EvaluationContext context);
}
