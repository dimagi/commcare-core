package org.javarosa.core.model.condition;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

public class Condition extends Triggerable {
    public static final int ACTION_NULL = 0;
    public static final int ACTION_SHOW = 1;
    public static final int ACTION_HIDE = 2;
    public static final int ACTION_ENABLE = 3;
    public static final int ACTION_DISABLE = 4;
    public static final int ACTION_LOCK = 5;
    public static final int ACTION_UNLOCK = 6;
    public static final int ACTION_REQUIRE = 7;
    public static final int ACTION_DONT_REQUIRE = 8;

    public int trueAction;
    public int falseAction;

    public Condition() {
        // for externalization
    }

    public Condition(IConditionExpr expr, int trueAction, int falseAction,
                     TreeReference contextRef) {
        this(expr, trueAction, falseAction, contextRef, new Vector());
    }

    public Condition(IConditionExpr expr, int trueAction, int falseAction,
                     TreeReference contextRef, Vector targets) {
        super(expr, contextRef);
        this.trueAction = trueAction;
        this.falseAction = falseAction;
        this.targets = targets;
    }

    @Override
    public Object eval(FormInstance model, EvaluationContext evalContext) {
        try {
            return expr.eval(model, evalContext);
        } catch (XPathException e) {
            e.setMessagePrefix("Display Condition Error: Error in calculation for " + contextRef.toString(true));
            throw e;
        }
    }

    public boolean evalBool(FormInstance model, EvaluationContext evalContext) {
        return (Boolean)eval(model, evalContext);
    }

    @Override
    public void apply(TreeReference ref, Object rawResult, FormInstance model, FormDef f) {
        boolean result = (Boolean)rawResult;
        performAction(model.resolveReference(ref), result ? trueAction : falseAction);
    }

    @Override
    public boolean canCascade() {
        return (trueAction == ACTION_SHOW || trueAction == ACTION_HIDE);
    }

    @Override
    public boolean isCascadingToChildren() {
        return (trueAction == ACTION_SHOW || trueAction == ACTION_HIDE);
    }

    private void performAction(TreeElement node, int action) {
        switch (action) {
            case ACTION_NULL:
                break;
            case ACTION_SHOW:
                node.setRelevant(true);
                break;
            case ACTION_HIDE:
                node.setRelevant(false);
                break;
            case ACTION_ENABLE:
                node.setEnabled(true);
                break;
            case ACTION_DISABLE:
                node.setEnabled(false);
                break;
            case ACTION_LOCK:         /* not supported */
                break;
            case ACTION_UNLOCK:       /* not supported */
                break;
            case ACTION_REQUIRE:
                node.setRequired(true);
                break;
            case ACTION_DONT_REQUIRE:
                node.setRequired(false);
                break;
        }
    }

    /**
     * Conditions are equal if they have the same actions, expression, and
     * triggers, but NOT targets or context ref.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Condition) {
            Condition c = (Condition)o;
            return (this == c ||
                    (this.trueAction == c.trueAction &&
                            this.falseAction == c.falseAction &&
                            super.equals(c)));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return trueAction ^ falseAction ^ super.hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        super.readExternal(in, pf);
        trueAction = ExtUtil.readInt(in);
        falseAction = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeNumeric(out, trueAction);
        ExtUtil.writeNumeric(out, falseAction);
    }

    @Override
    public String getDebugLabel() {
        return "relevant";
    }
}
