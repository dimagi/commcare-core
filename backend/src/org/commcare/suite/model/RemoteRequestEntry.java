package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Suite entry for performing a synchronous query/post request to an external
 * server. Lifecycle is: gather query params, query the server, process
 * response data, and complete the transaction with a post to the server.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteRequestEntry extends Entry {
    private PostRequest post;

    @SuppressWarnings("unused")
    public RemoteRequestEntry() {

    }

    public RemoteRequestEntry(String commandId, DisplayUnit display,
                              Vector<SessionDatum> data,
                              Hashtable<String, DataInstance> instances,
                              Vector<StackOperation> stackOperations,
                              AssertionSet assertions,
                              PostRequest post) {
        super(commandId, display, data, instances, stackOperations, assertions);

        this.post = post;
    }

    public PostRequest getSyncPost() {
        return post;
    }

    @Override
    public boolean isSync() {
        return true;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);

        post = (PostRequest)ExtUtil.read(in, PostRequest.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);

        ExtUtil.write(out, post);
    }
}
