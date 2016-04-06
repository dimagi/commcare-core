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
 * Created by amstone326 on 4/6/16.
 */
public class SignedPermission implements Externalizable {

    public static final String KEY_MULTIPLE_APPS_COMPATIBILITY = "multiple-apps-compatible";
    public static final String MULT_APPS_ENABLED_VALUE = "enabled";
    public static final String MULT_APPS_DISABLED_VALUE = "disabled";
    public static final String MULT_APPS_IGNORE_VALUE = "ignore";

    private String key;
    private String value;
    private String signature;
    private String verifiedValue;
    private boolean verificationOccurred;

    public SignedPermission(String key, String value, String signature) {
        this.key = key;
        this.value = value;
        this.signature = signature;
    }

    public void verifyValue(SignatureVerifier verifier) {
        if (verifier.verify(value, signature)) {
            verifiedValue = value;
        } else {
            verifiedValue = getDefaultValue();
        }
        verificationOccurred = true;
    }

    private String getDefaultValue() {
        switch(key) {
            case KEY_MULTIPLE_APPS_COMPATIBILITY:
                return MULT_APPS_DISABLED_VALUE;
            default:
                return "";
        }
    }

    public String getVerifiedValue() {
        if (!verificationOccurred) {
            System.out.print("WARNING: Attempting to get the verified value for a signed " +
                    "permission before verification has occurred; using default value");
            return getDefaultValue();
        }
        return verifiedValue;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        key = ExtUtil.readString(in);
        value = ExtUtil.readString(in);
        signature = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, key);
        ExtUtil.writeString(out, value);
        ExtUtil.writeString(out, signature);
    }
}
