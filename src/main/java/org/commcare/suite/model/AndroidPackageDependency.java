package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Model for defining a CommCare app dependencies on other Android apps
 */
public class AndroidPackageDependency implements Externalizable {
    private String id;

    /**
     * Serialization Only!!!
     */
    public AndroidPackageDependency() {
    }

    public AndroidPackageDependency(String id) {
        this.id = id;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "AndroidPackageDependency{" +
                "id='" + id + '\'' +
                '}';
    }
}
