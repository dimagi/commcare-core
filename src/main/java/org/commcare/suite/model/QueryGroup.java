package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// Model for <group> node
public class QueryGroup implements Externalizable {

    private String key;
    private DisplayUnit display;

    @SuppressWarnings("unused")
    public QueryGroup() {
    }

    public QueryGroup(String key, DisplayUnit display) {
        this.key = key;
        this.display = display;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        key = (String)ExtUtil.read(in, String.class, pf);
        display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, key);
        ExtUtil.write(out, display);
    }

    public String getKey() {
        return key;
    }

    public DisplayUnit getDisplay() {
        return display;
    }
}
