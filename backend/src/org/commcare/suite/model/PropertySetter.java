/**
 *
 */
package org.commcare.suite.model;

import org.commcare.util.SignatureVerifier;
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
    String signedValue;

    /**
     * Serialization Only!!!
     */
    public PropertySetter() {

    }

    protected PropertySetter(String key, String value, boolean force, String signedValue) {
        this.key = key;
        this.value = value;
        this.force = force;
        if (signedValue != null) {
            this.signedValue = signedValue;
        } else {
            this.signedValue = "";
        }
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getValueWithSignatureCheck(SignatureVerifier verifier) {
        if (hasSignature()) {
            if (verifier.verify(value, signedValue)) {
                return value;
            }
            return Profile.getPropertyDefaultValue(key);
        }
        return value;
    }

    public boolean isForce() {
        return force;
    }

    private boolean hasSignature() {
        return !"".equals(signedValue);
    }

    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        key = ExtUtil.readString(in);
        value = ExtUtil.readString(in);
        force = ExtUtil.readBool(in);
        signedValue = ExtUtil.readString(in);
    }

    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, key);
        ExtUtil.writeString(out, value);
        ExtUtil.writeBool(out, force);
        ExtUtil.writeString(out, signedValue);
    }

    public boolean equals(Object o) {
        if(!(o instanceof PropertySetter)) {
            return false;
        }
        
        PropertySetter p = (PropertySetter)o;
        return this.key.equals(p.key) &&
                this.value.equals(p.value) &&
                force == p.force &&
                this.signedValue.equals(p.signedValue);
    }
}