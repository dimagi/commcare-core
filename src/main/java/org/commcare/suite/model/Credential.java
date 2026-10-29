package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Apps use this model to convey the types of credentials it issues
 */
public class Credential implements Externalizable {

    private String level;
    private String type;

    /**
     * Serialization Only!!!
     */
    public Credential() {
    }

    public Credential(String level, String type) {
        this.level = level;
        this.type = type;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        level = ExtUtil.readString(in);
        type = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, level);
        ExtUtil.writeString(out, type);
    }

    public String getLevel() {
        return level;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Credential{" +
                "level='" + level + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
