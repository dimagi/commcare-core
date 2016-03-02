package org.javarosa.core.model.trace;

/**
 * Serializes an evaluation trace into a structured format.
 *
 * @param <T> The expected serialization outcome
 * @author ctsims
 */
public interface EvaluationTraceSerializer<T> {
    T serializeEvaluationLevels(EvaluationTrace input);
}
