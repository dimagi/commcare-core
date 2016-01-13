package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;

import java.util.Hashtable;
import java.util.Vector;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class RemoteSync extends EntryBase {
    private String syncUrl;
    /**
     * Serialization only!
     */
    public RemoteSync() {

    }

    public RemoteSync(String commandId, DisplayUnit display,
                      String syncUrl,
                      Vector<SessionDatum> data,
                      Hashtable<String, DataInstance> instances,
                      Vector<StackOperation> stackOperations,
                      AssertionSet assertions) {
        super(commandId, display, data, instances, stackOperations, assertions);

        this.syncUrl = syncUrl;
    }
}
