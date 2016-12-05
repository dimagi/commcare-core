package org.javarosa.core.log;

/**
 * @author Clayton Sims
 */
public interface IFullLogSerializer<T> {
    T serializeLogs(LogEntry[] logs);
}
