package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Describes a user-initiated form entry action. Includes information that
 * needs to be collected before that action can begin and what the UI should
 * present to the user regarding this action.
 *
 * @author ctsims
 */
public class FormEntry extends Entry {

    private String xFormNamespace;
    private PostRequest post;

    /**
     * Serialization only!
     */
    public FormEntry() {

    }

    public FormEntry(String commandId, DisplayUnit display,
                     Vector<SessionDatum> data, String formNamespace,
                     Hashtable<String, DataInstance> instances,
                     Vector<StackOperation> stackOperations, AssertionSet assertions, PostRequest post) {
        super(commandId, display, data, instances, stackOperations, assertions);

        this.xFormNamespace = formNamespace;
        this.post = post;
    }

    /**
     * @return The XForm Namespace of the form which should be filled out in
     * the form entry session triggered by this action.
     */
    @Override
    public String getXFormNamespace() {
        return xFormNamespace;
    }

    @Override
    public PostRequest getPostRequest() {
        return post;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        this.xFormNamespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        this.post = (PostRequest)ExtUtil.read(in, new ExtWrapNullable(PostRequest.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(xFormNamespace));
        ExtUtil.write(out, new ExtWrapNullable(post));
    }
}
