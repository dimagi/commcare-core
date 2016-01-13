package org.commcare.suite.model;

import org.javarosa.core.model.instance.DataInstance;

import java.util.Hashtable;
import java.util.Vector;

/**
 * An entry action used for viewing data; main difference from an Entry is that
 * it doesn't end in launching form entry.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class View extends EntryBase {
    /**
     * Serialization only!
     */
    public View() {

    }

    public View(String commandId, DisplayUnit display,
                Vector<SessionDatum> data,
                Hashtable<String, DataInstance> instances,
                Vector<StackOperation> stackOperations,
                AssertionSet assertions) {
        super(commandId, display, data, instances, stackOperations, assertions);
    }
}
