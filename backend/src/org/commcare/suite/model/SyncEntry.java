package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class SyncEntry extends Entry {
    private Vector<RemoteQuery> queries;
    private SyncPost post;

    /**
     * Serialization only!
     */
    public SyncEntry() {

    }

    public SyncEntry(String commandId, DisplayUnit display,
                     Vector<SessionDatum> data, Hashtable<String, DataInstance> instances,
                     Vector<StackOperation> stackOperations, AssertionSet assertions,
                     SyncPost post, Vector<RemoteQuery> queries) {
        super(commandId, display, data, instances, stackOperations, assertions);

        this.queries = queries;
        this.post = post;
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
    }

    public static class SyncPost implements Externalizable {
        private String postUrl;
        private Hashtable<String, TreeReference> postKeys;

        @Override
        public void readExternal(DataInputStream in, PrototypeFactory pf)
                throws IOException, DeserializationException {
        }

        @Override
        public void writeExternal(DataOutputStream out) throws IOException {
        }
    }
}
