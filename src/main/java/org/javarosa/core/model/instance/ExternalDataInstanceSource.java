package org.javarosa.core.model.instance;

import org.commcare.core.interfaces.RemoteInstanceFetcher;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExternalDataInstanceSource implements Externalizable {

    TreeElement root;
    private String sourceUri;
    private String instanceId;

    /**
     * Externalizable constructor
     */
    public ExternalDataInstanceSource() {

    }

    public ExternalDataInstanceSource(String instanceId, String sourceUri) {
        this.instanceId = instanceId;
        this.sourceUri = sourceUri;
    }

    public ExternalDataInstanceSource(String instanceId, TreeElement root, String sourceUri) {
        this.instanceId = instanceId;
        this.sourceUri = sourceUri;
        this.root = root;
    }

    public boolean needsInit() {
        if (root == null) {
            return true;
        }
        return false;
    }

    protected TreeElement getRoot() {
        if (needsInit()) {
            throw new RuntimeException("Uninstantiated external instance source");
        }
        return root;
    }

    public void init(TreeElement root) {
        if (this.root != null) {
            throw new RuntimeException("Initializing an already instantiated external instance source is not permitted");
        }
        this.root = root;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        sourceUri = ExtUtil.readString(in);
        instanceId = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, sourceUri);
        ExtUtil.write(out, instanceId);
    }

    public String getSourceUri() {
        return sourceUri;
    }
}
