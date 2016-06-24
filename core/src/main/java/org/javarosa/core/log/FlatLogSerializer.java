package org.javarosa.core.log;

/**
 * @author Clayton Sims
 */
public class FlatLogSerializer implements IFullLogSerializer<String> {

    @Override
    public String serializeLogs(LogEntry[] logs) {
        StringBuilder stringBuilder = new StringBuilder("");
        for (LogEntry logEntry : logs) {
            stringBuilder.append(serializeLog(logEntry));
        }
        return stringBuilder.toString();
    }

    private String serializeLog(LogEntry log) {
        return "[" + log.getType() + "] " + log.getTime().toString() + ": " + log.getMessage() + "\n";
    }
}
