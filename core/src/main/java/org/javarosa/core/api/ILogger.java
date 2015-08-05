package org.javarosa.core.api;

import org.javarosa.core.log.IFullLogSerializer;
import org.javarosa.core.log.StreamLogSerializer;

import java.io.IOException;
import java.util.Date;

/**
 * IIncidentLogger's are used for instrumenting applications to identify usage
 * patterns, usability errors, and general trajectories through applications.
 *
 * @author Clayton Sims
 * @date Apr 10, 2009
 */
public interface ILogger {

    public void log(String type, String message, Date logDate);

    public void clearLogs();

    public <T> T serializeLogs(IFullLogSerializer<T> serializer);

    public void serializeLogs(StreamLogSerializer serializer) throws IOException;

    public void serializeLogs(StreamLogSerializer serializer, int limit) throws IOException;

    public void panic();

    public int logSize();

    public void halt();
}
