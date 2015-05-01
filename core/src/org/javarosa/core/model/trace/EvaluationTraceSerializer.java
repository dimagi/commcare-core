package org.javarosa.core.model.trace;

/**
 * 
 * Serializes an evaluation trace into a structured format.
 * 
 * @author ctsims
 *
 * @param <T> The expected serialization outcome
 */
public interface EvaluationTraceSerializer<T> {
    public T serializeEvaluationLevels(EvaluationTrace input);
}
