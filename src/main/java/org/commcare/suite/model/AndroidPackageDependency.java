package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AndroidPackageDependency implements Externalizable {
    private String id;
    private String name;

    /**
     * Serialization Only!!!
     */
    public AndroidPackageDependency() {
    }

    public AndroidPackageDependency(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        name = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.writeString(out, name);
    }

    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AndroidPackageDependency{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
