package org.javarosa.core.log;

/**
 * @author Clayton Sims
 * @date Apr 10, 2009
 */
public interface IFullLogSerializer<T> {
    public T serializeLogs(LogEntry[] logs);
}
