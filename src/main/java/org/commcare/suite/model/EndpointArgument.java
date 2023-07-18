package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Model class to represent an argument to Endpoint
 */
public class EndpointArgument implements Externalizable {

    private String id;

    @Nullable
    private String instanceId;

    @Nullable
    private String instanceSrc;

    // for serialization
    public EndpointArgument() {
    }

    public EndpointArgument(String id, @Nullable String instanceId, @Nullable String instanceSrc) {
        this.id = id;
        this.instanceId = instanceId;
        this.instanceSrc = instanceSrc;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        instanceId = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        instanceSrc = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(instanceId));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(instanceSrc));
    }

    public String getId() {
        return id;
    }

    @Nullable
    public String getInstanceId() {
        return instanceId;
    }

    @Nullable
    public String getInstanceSrc() {
        return instanceSrc;
    }

    /**
     * If the argument should be processed into a external data instance
     *
     * @return true if the argument defines instance attributes, false otherwise
     */
    public boolean isInstanceArgument() {
        return instanceId != null;
    }
}
