package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AppDependency implements Externalizable {
    private String id;
    private String name;
    private boolean force;

    // Set later during dependency check
    private boolean installed = false;

    /**
     * Serialization Only!!!
     */
    public AppDependency() {
    }

    public AppDependency(String id, String name, boolean force) {
        this.id = id;
        this.name = name;
        this.force = force;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        name = ExtUtil.readString(in);
        force = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.writeString(out, name);
        ExtUtil.writeBool(out, force);
    }

    public String getId() {
        return id;
    }

    public boolean isForce() {
        return force;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "AppDependency{" +
                "id='" + id + '\'' +
                ", force=" + force +
                '}';
    }

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }
}
