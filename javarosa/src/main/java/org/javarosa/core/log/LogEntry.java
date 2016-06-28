package org.javarosa.core.log;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * @author Clayton Sims
 */
public class LogEntry implements Externalizable {
    public static final String STORAGE_KEY = "LOG";

    protected Date time;
    protected String type;
    protected String message;

    public LogEntry() {
        // for externalization
    }

    public LogEntry(String type, String message, Date time) {
        this.time = time;
        this.type = type;
        this.message = message;
    }

    public Date getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        time = ExtUtil.readDate(in);
        type = ExtUtil.readString(in);
        message = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeDate(out, time);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(type));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(message));
    }
}
