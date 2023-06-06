package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapMap;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import io.reactivex.Single;

/**
 * Describes a user-initiated action, what information needs to be collected
 * before that action can begin, and what the UI should present to the user
 * regarding this action.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public abstract class Entry implements Externalizable, MenuDisplayable {
    Vector<SessionDatum> data;
    DisplayUnit display;
    private String commandId;
    Hashtable<String, DataInstance> instances;
    Vector<StackOperation> stackOperations;
    AssertionSet assertions;

    /**
     * Serialization only!
     */
    public Entry() {
    }

    public Entry(String commandId, DisplayUnit display,
                 Vector<SessionDatum> data,
                 Hashtable<String, DataInstance> instances,
                 Vector<StackOperation> stackOperations,
                 AssertionSet assertions) {
        this.commandId = commandId == null ? "" : commandId;
        this.display = display;
        this.data = data;
        this.instances = instances;
        this.stackOperations = stackOperations;
        this.assertions = assertions;
    }

    public boolean isView() {
        return getXFormNamespace() == null && getPostRequest() == null && stackOperations.size() == 0;
    }

    public boolean isRemoteRequest() {
        return getXFormNamespace() == null && getPostRequest() != null;
    }

    public String getXFormNamespace() {
        return null;
    }

    public PostRequest getPostRequest() {
        return null;
    }

    /**
     * @return the ID of this entry command. Used by Menus to determine
     * where the command should be located.
     */
    public String getCommandId() {
        return commandId;
    }

    /**
     * @return A text whose evaluated string should be presented to the
     * user as the entry point for this operation
     */
    public Text getText() {
        return display.getText();
    }

    public Vector<SessionDatum> getSessionDataReqs() {
        return data;
    }

    public Hashtable<String, DataInstance> getInstances(Set<String> instancesToInclude) {
        return GetLimitedInstances.getLimitedInstances(null, instances);
    }
//    /**
//     *
//     * @param limitingList a list of instance names to restrict the returning set to; null
//     *                     if no limiting is being used
//     * @return a hashtable representing the data instances that are in scope for this Entry,
//     * potentially limited by @limitingList
//     */
//    // getLimitedInstances move into its own class
//    // make static
//    public Hashtable<String, DataInstance> getInstances(Set<String> limitingList) {
//        Hashtable<String, DataInstance> copy = new Hashtable<>();
//        for (Enumeration en = instances.keys(); en.hasMoreElements(); ) {
//            String key = (String)en.nextElement();
//
//            //This is silly, all of these are external data instances. TODO: save their
//            //construction details instead.
//            DataInstance cur = instances.get(key);
//            if (limitingList == null || limitingList.contains(cur.getInstanceId())) {
//                // Make sure we either aren't using a limiting list, or the instanceid is in the list
//                if (cur instanceof ExternalDataInstance) {
//                    //Copy the EDI so when it gets populated we don't keep it dependent on this object's lifecycle!!
//                    copy.put(key, new ExternalDataInstance(((ExternalDataInstance)cur).getReference(), cur.getInstanceId()));
//                } else {
//                    copy.put(key, cur);
//                }
//            }
//        }
//
//        return copy;
//    }

    public AssertionSet getAssertions() {
        return assertions == null ? new AssertionSet(new Vector<String>(), new Vector<Text>()) : assertions;
    }

    /**
     * Retrieve the stack operations that should be processed after this entry
     * session has successfully completed.
     *
     * @return a Vector of Stack Operation models.
     */
    public Vector<StackOperation> getPostEntrySessionOperations() {
        return stackOperations;
    }

    @Override
    public String getImageURI() {
        if (display.getImageURI() == null) {
            return null;
        }
        return display.getImageURI().evaluate();
    }

    @Override
    public String getAudioURI() {
        if (display.getAudioURI() == null) {
            return null;
        }
        return display.getAudioURI().evaluate();
    }

    @Override
    public String getDisplayText(EvaluationContext ec) {
        if (display.getText() == null) {
            return null;
        }
        return display.getText().evaluate(ec);
    }

    @Override
    public Single<String> getTextForBadge(EvaluationContext ec) {
        if (display.getBadgeText() == null) {
            return Single.just("");
        }
        return display.getBadgeText().getDisposableSingleForEvaluation(ec);
    }

    @Override
    public Text getRawBadgeTextObject() {
        return display.getBadgeText();
    }

    @Override
    public Text getRawText() {
        return display.getText();
    }


    @Override
    public String getCommandID() {
        return commandId;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        this.commandId = ExtUtil.readString(in);
        this.display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);

        data = (Vector<SessionDatum>)ExtUtil.read(in, new ExtWrapListPoly(), pf);
        instances = (Hashtable<String, DataInstance>)ExtUtil.read(in, new ExtWrapMap(String.class, new ExtWrapTagged()), pf);
        stackOperations = (Vector<StackOperation>)ExtUtil.read(in, new ExtWrapList(StackOperation.class), pf);
        assertions = (AssertionSet)ExtUtil.read(in, new ExtWrapNullable(AssertionSet.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, commandId);
        ExtUtil.write(out, display);
        ExtUtil.write(out, new ExtWrapListPoly(data));
        ExtUtil.write(out, new ExtWrapMap(instances, new ExtWrapTagged()));
        ExtUtil.write(out, new ExtWrapList(stackOperations));
        ExtUtil.write(out, new ExtWrapNullable(assertions));
    }

    @Override
    public String toString() {
        return "Entry with id " + this.getCommandId();
    }
}
