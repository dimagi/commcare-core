package org.commcare.suite.model;

import org.commcare.session.RemoteQuerySessionManager;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

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
    private XPathExpression relevantExpr;
    private String iconForActionBarReference;

    /**
     * Serialization only!!!
     */
    @SuppressWarnings("unused")
    public Action() {

    }

    /**
     * Creates an Action model with the associated display details and stack
     * operations set.
     */
    public Action(DisplayUnit display, Vector<StackOperation> stackOps,
                  XPathExpression relevantExpr, String iconForActionBar) {
        this.display = display;
        this.stackOps = stackOps;
        this.relevantExpr = relevantExpr;
        this.iconForActionBarReference = iconForActionBar == null ? "" : iconForActionBar;
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

    public boolean isRelevant(EvaluationContext evalContext) {
        if (relevantExpr == null) {
            return true;
        } else {
            String result = RemoteQuerySessionManager.evalXpathExpression(relevantExpr, evalContext);
            return "true".equals(result);
        }
    }

    public boolean hasActionBarIcon() {
        return !"".equals(iconForActionBarReference);
    }

    public String getActionBarIconReference() {
        return iconForActionBarReference;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        display = (DisplayUnit)ExtUtil.read(in, DisplayUnit.class, pf);
        stackOps = (Vector<StackOperation>)ExtUtil.read(in, new ExtWrapList(StackOperation.class), pf);
        relevantExpr = (XPathExpression)ExtUtil.read(in, new ExtWrapNullable(new ExtWrapTagged()), pf);
        iconForActionBarReference = ExtUtil.readString(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, display);
        ExtUtil.write(out, new ExtWrapList(stackOps));
        ExtUtil.write(out, new ExtWrapNullable(relevantExpr == null ? null : new ExtWrapTagged(relevantExpr)));
        ExtUtil.writeString(out, iconForActionBarReference);
    }
}
