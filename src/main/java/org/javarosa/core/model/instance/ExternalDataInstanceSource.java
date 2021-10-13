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

    public ExternalDataInstanceSource(String sourceUri) {
        this.sourceUri = sourceUri;
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

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        sourceUri = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, sourceUri);
    }

    public String getSourceUri() {
        return sourceUri;
    }
}
