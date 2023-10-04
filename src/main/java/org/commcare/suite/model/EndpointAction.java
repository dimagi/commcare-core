package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EndpointAction implements Externalizable {

    private String endpointId;
    private boolean isBackground;

    public EndpointAction() {
    }

    public EndpointAction(String endpointId, boolean isBackground) {
        this.endpointId = endpointId;
        this.isBackground = isBackground;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        endpointId = ExtUtil.readString(in);
        isBackground = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, endpointId);
        ExtUtil.writeBool(out, isBackground);
    }

    public String getEndpointId() {
        return endpointId;
    }

    public boolean isBackground() {
        return isBackground;
    }
}
