package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * An Entry definition describes a user initiated form entry action, what
 * information needs to be collected before that action can begin, and what the
 * User Interface should present to the user regarding these actions
 *
 * @author ctsims
 */
public class Entry extends EntryBase {

    private String xFormNamespace;

    /**
     * Serialization only!
     */
    public Entry() {

    }

    public Entry(String commandId, DisplayUnit display,
                 Vector<SessionDatum> data, String formNamespace,
                 Hashtable<String, DataInstance> instances,
                 Vector<StackOperation> stackOperations, AssertionSet assertions) {
        super(commandId, display, data, instances, stackOperations, assertions);

        this.xFormNamespace = formNamespace;
    }

    /**
     * @return The XForm Namespce of the form which should be filled out in
     * the form entry session triggered by this action. null if no entry
     * should occur [HACK].
     */
    public String getXFormNamespace() {
        return xFormNamespace;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        super.readExternal(in, pf);
        this.xFormNamespace = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(xFormNamespace));
    }
}
