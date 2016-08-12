package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * An action defines a user interface element that can be
 * triggered by the user to fire off one or more stack operations
 * in the current session
 *
 * @author ctsims
 */
public class Action implements Externalizable {

    private DisplayUnit display;
    private Vector<StackOperation> stackOps;

    /**
     * Serialization only!!!
     */
    public Action() {

    }

    /**
     * Creates an Action model with the associated display details and stack
     * operations set.
     */
    public Action(DisplayUnit display, Vector<StackOperation> stackOps) {
        this.display = display;
        this.stackOps = stackOps;
    }

    /**
     * @return The Display model for showing this action to the user
     */
    public DisplayUnit getDisplay() {
        return display;
    }

    /**
     * @return A vector of the StackOperation models which
     * should be processed sequentially upon this action
     * being triggered by the user.
     */
    public Vector<StackOperation> getStackOperations() {
        return stackOps;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
        this.stackOps = (Vector<StackOperation>)ExtUtil.read(in, new ExtWrapList(StackOperation.class), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, display);
        ExtUtil.write(out, new ExtWrapList(stackOps));
    }
}
