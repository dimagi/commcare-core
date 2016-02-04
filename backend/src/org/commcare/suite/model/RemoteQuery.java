package org.commcare.suite.model;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteQuery implements Externalizable {
    Hashtable<String, TreeReference> hiddenQueryValues;
    Hashtable<String, DisplayUnit> userQueryPrompts;
    private String storageInstance;
    private String url;

    @SuppressWarnings("unused")
    public RemoteQuery() {
    }

    public RemoteQuery(String url, String storageInstance,
                       Hashtable<String, TreeReference> hiddenQueryValues,
                       Hashtable<String, DisplayUnit> userQueryPrompts) {
        this.url = url;
        this.storageInstance = storageInstance;
        this.hiddenQueryValues = hiddenQueryValues;
        this.userQueryPrompts = userQueryPrompts;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
    }
}
