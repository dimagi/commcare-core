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
public class FormIdDatum extends SessionDatum {
    private String value;

    public FormIdDatum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
    }
}
