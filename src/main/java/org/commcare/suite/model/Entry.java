package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import io.reactivex.Observable;

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
        return false;
    }

    public boolean isRemoteRequest() {
        return false;
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

    public Hashtable<String, DataInstance> getInstances() {
        Hashtable<String, DataInstance> copy = new Hashtable<>();
        for (Enumeration en = instances.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();

            //This is silly, all of these are externaldata instances. TODO: save their
            //construction details instead.
            DataInstance cur = instances.get(key);
            if (cur instanceof ExternalDataInstance) {
                //Copy the EDI so when it gets populated we don't keep it dependent on this object's lifecycle!!
                copy.put(key, new ExternalDataInstance(((ExternalDataInstance)cur).getReference(), cur.getInstanceId()));
            } else {
                copy.put(key, cur);
            }
        }

        return copy;
    }

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
    public String getDisplayText() {
        if (display.getText() == null) {
            return null;
        }
        return display.getText().evaluate();
    }

    @Override
    public Observable<String> getTextForBadge(EvaluationContext ec) {
        if (display.getBadgeText() == null) {
            return Observable.just("");
        }
        return Observable.just(display.getBadgeText().evaluate(ec));
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
        return "Entry with id " + this.getCommandId() + " display text " + this.getDisplayText();
    }
}
