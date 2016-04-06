package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CalculateDatum extends SessionDatum {
    private String id;
    private String value;

    public CalculateDatum(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getDataId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        value = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, value);
    }
}
