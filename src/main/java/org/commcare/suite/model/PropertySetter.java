package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This is just a tiny little struct to make it reasonable to maintain
 * the properties until they are installed. Unfortunately, the serialization
 * framework requires it to be public.
 *
 * @author ctsims
 */
public class PropertySetter implements Externalizable {
    String key;
    String value;
    boolean force;

    /**
     * Serialization Only!!!
     */
    public PropertySetter() {
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isForce() {
        return force;
    }

    protected PropertySetter(String key, String value, boolean force) {
        this.key = key;
        this.value = value;
        this.force = force;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        key = ExtUtil.readString(in);
        value = ExtUtil.readString(in);
        force = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, key);
        ExtUtil.writeString(out, value);
        ExtUtil.writeBool(out, force);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PropertySetter)) {
            return false;
        }

        PropertySetter p = (PropertySetter)o;
        return this.key.equals(p.getKey()) &&
                this.value.equals(p.getValue()) &&
                force == p.force;
    }

    @Override
    public int hashCode() {
        int result = 11;
        result = 31 * result + key.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (force ? 0 : 1);
        return result;
    }
}