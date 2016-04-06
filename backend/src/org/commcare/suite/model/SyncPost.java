package org.commcare.suite.model;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Entry config for posting data to a remote server as part of synchronous
 * request transaction.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class SyncPost implements Externalizable {
    private String postUrl;
    private Hashtable<String, TreeReference> postKeys;

    @SuppressWarnings("unused")
    public SyncPost() {
    }

    public SyncPost(String postUrl, Hashtable<String, TreeReference> postKeys) {
        this.postUrl = (postUrl == null) ? "" : postUrl;
        this.postKeys = postKeys;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        postKeys = (Hashtable<String, TreeReference>)ExtUtil.read(in, new ExtWrapMap(String.class, TreeReference.class));
        postUrl = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapMap(postKeys));
        ExtUtil.writeString(out, postUrl);
    }
}
