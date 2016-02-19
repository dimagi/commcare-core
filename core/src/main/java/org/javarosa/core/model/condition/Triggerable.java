package org.javarosa.core.model.condition;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTrace;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A triggerable represents an action that should be processed based
 * on a value updating in a model. Trigerrables are comprised of two
 * basic components: An expression to be evaluated, and a reference
 * which represents where the resultant value will be stored.
 *
 * A triggerable will dispatch the action it's performing out to
 * all relevant nodes referenced by the context against these current
 * models.
 *
 * @author ctsims
 */
public abstract class Triggerable implements Externalizable {
    /**
     * The expression which will be evaluated to produce a result
     */
    public IConditionExpr expr;

    /**
     * References to all of the (non-contextualized) nodes which should be
     * updated by the result of this triggerable
     */
    public Vector<TreeReference> targets;

    /**
     * Current reference which is the "Basis" of the trigerrables being
     * evaluated. This is the highest common root of all of the targets being
     * evaluated.
     */
    public TreeReference contextRef;  //generic ref used to turn triggers into absolute references

    /**
     * The first context provided to this triggerable before reducing to the common root.
     */
    public TreeReference originalContextRef;

    private int stopContextualizingAt;

    /**
     * Whether this trigger is collecting debug traces *
     */
    boolean mIsDebugOn = false;

    /**
     * Debug traces collecting during trigger execution. See the
     * getTriggerTraces method for details.
     */
    Hashtable<TreeReference, EvaluationTrace> mTriggerDebugs;


    public Triggerable() {

    }

    public Triggerable(IConditionExpr expr, TreeReference contextRef) {
        this.expr = expr;
        this.contextRef = contextRef;
        this.originalContextRef = contextRef;
        this.targets = new Vector<TreeReference>();
        this.stopContextualizingAt = -1;
    }

    protected abstract Object eval(FormInstance instance, EvaluationContext ec);

    protected abstract void apply(TreeReference ref, Object result, FormInstance instance, FormDef f);

    public abstract boolean canCascade();

    /**
     * @return A key string describing the triggerable type used to aggregate and
     * request specific debugging results.
     */
    public abstract String getDebugLabel();

    /**
     * @param mDebugMode Whether this triggerable should be collecting trace information
     *                   during execution.
     */
    public void setDebug(boolean mDebugMode) {
        this.mIsDebugOn = mDebugMode;
        if (mIsDebugOn) {
            mTriggerDebugs = new Hashtable<TreeReference, EvaluationTrace>();
        } else {
            mTriggerDebugs = null;
        }
    }

    /**
     * Retrieves evaluation traces collected during execution of this
     * triggerable in debug mode.
     *
     * @return A mapping from tree refernences impacted by this triggerable, to
     * the root of the evaluation trace that was triggered.
     * @throws IllegalStateException If debugging has not been enabled.
     */
    public Hashtable<TreeReference, EvaluationTrace> getEvaluationTraces() throws IllegalStateException {
        if (!mIsDebugOn) {
            throw new IllegalStateException("Evaluation traces requested from triggerable not in debug mode.");
        }
        if (mTriggerDebugs == null) {
            return new Hashtable<TreeReference, EvaluationTrace>();
        }
        return this.mTriggerDebugs;
    }

    /**
     * Not for re-implementation, dispatches all of the evaluation
     */
    public final void apply(FormInstance instance, EvaluationContext parentContext,
                            TreeReference context, FormDef f) {
        // The triggering root is the highest level of actual data we can
        // inquire about, but it _isn't_ necessarily the basis for the actual
        // expressions, so we need genericize that ref against the current
        // context
        TreeReference ungenericised = originalContextRef.contextualize(context);
        EvaluationContext ec = new EvaluationContext(parentContext, ungenericised);
        EvaluationContext triggerEval = ec;
        if (mIsDebugOn) {
            triggerEval = new EvaluationContext(ec, ec.getContextRef());
            triggerEval.setDebugModeOn();
        }

        Object result = eval(instance, triggerEval);

        for (int i = 0; i < targets.size(); i++) {
            TreeReference targetRef =
                    ((TreeReference)targets.elementAt(i)).contextualize(ec.getContextRef());
            Vector v = ec.expandReference(targetRef);

            for (int j = 0; j < v.size(); j++) {
                TreeReference affectedRef = (TreeReference)v.elementAt(j);
                if (mIsDebugOn) {
                    mTriggerDebugs.put(affectedRef, triggerEval.getEvaluationTrace());
                }
                apply(affectedRef, result, instance, f);
            }
        }
    }

    public void addTarget(TreeReference target) {
        if (targets.indexOf(target) == -1) {
            targets.addElement(target);
        }
    }

    public Vector<TreeReference> getTargets() {
        return targets;
    }

    /**
     * This should return true if this triggerable's targets will implicity modify the
     * value of their children. IE: if this triggerable makes a node relevant/irrelevant,
     * expressions which care about the value of this node's children should be triggered.
     *
     * @return True if this condition should trigger expressions whose targets include
     * nodes which are the children of this node's targets.
     */
    public boolean isCascadingToChildren() {
        return false;
    }

    public Vector<TreeReference> getTriggers() {
        // grab the relative trigger references from expression
        Vector<TreeReference> relTriggers = expr.getExprsTriggers(originalContextRef);

        // construct absolute references by anchoring against the original context reference
        Vector<TreeReference> absTriggers = new Vector<TreeReference>();
        for (int i = 0; i < relTriggers.size(); i++) {
            TreeReference ref = ((TreeReference)relTriggers.elementAt(i)).anchor(originalContextRef);
            absTriggers.addElement(ref);
        }
        return absTriggers;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Triggerable) {
            Triggerable t = (Triggerable)o;
            if (this == t) {
                return true;
            }

            if (expr.equals(t.expr)) {
                // check triggers
                Vector<TreeReference> Atriggers = this.getTriggers();
                Vector<TreeReference> Btriggers = t.getTriggers();

                // order doesn't matter, but triggers in A must be in B and vice versa
                return (subsetOfAndAbsolute(Atriggers, Btriggers) &&
                        subsetOfAndAbsolute(Btriggers, Atriggers));
            }
        }

        return false;
    }

    /**
     * @param potentialSubset Ensure set elements are absolute and in the master set
     * @return True if all elements in the potential set are absolute and in
     * the master set.
     */
    private boolean subsetOfAndAbsolute(Vector<TreeReference> potentialSubset,
                                        Vector<TreeReference> masterSet) {
        for (TreeReference ref : potentialSubset) {
            // csims@dimagi.com - 2012-04-17
            // Added last condition here. We can't actually say whether two triggers
            // are the same purely based on equality if they are relative.
            if (!ref.isAbsolute() || masterSet.indexOf(ref) == -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = expr.hashCode();
        for (TreeReference trigRef : getTriggers()) {
            hash ^= trigRef.hashCode();
        }
        return hash;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        expr = (IConditionExpr)ExtUtil.read(in, new ExtWrapTagged(), pf);
        contextRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        originalContextRef = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        targets = (Vector<TreeReference>)ExtUtil.read(in, new ExtWrapList(TreeReference.class), pf);
        stopContextualizingAt = ExtUtil.readInt(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(expr));
        ExtUtil.write(out, contextRef);
        ExtUtil.write(out, originalContextRef);
        ExtUtil.write(out, new ExtWrapList(targets));
        ExtUtil.writeNumeric(out, stopContextualizingAt);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < targets.size(); i++) {
            sb.append(((TreeReference)targets.elementAt(i)).toString());
            if (i < targets.size() - 1)
                sb.append(",");
        }
        return "trig[expr:" + expr.toString() + ";targets[" + sb.toString() + "]]";
    }

    public TreeReference contextualizeContext(TreeReference anchorRef) {
        TreeReference contextulizedUsingAnchor = contextRef.contextualize(anchorRef);
        if (stopContextualizingAt != -1) {
            return contextulizedUsingAnchor.genericizeAfter(stopContextualizingAt);
        } else {
            return contextulizedUsingAnchor;
        }
    }

    public void updateStopContextualizingAt(TreeReference refInExpr) {
        int smallestIntersectionForRef = smallestIntersectingLevelWithPred(refInExpr);

        if (smallestIntersectionForRef != -1) {
            if (stopContextualizingAt == -1) {
                stopContextualizingAt = smallestIntersectionForRef;
            } else {
                stopContextualizingAt = Math.min(stopContextualizingAt, smallestIntersectionForRef);
            }
        }
    }

    private int smallestIntersectingLevelWithPred(TreeReference refInExpr) {
        TreeReference intersectionRef = contextRef.intersect(refInExpr.removePredicates());
        for (int refLevel = 0; refLevel < Math.min(refInExpr.size(), intersectionRef.size()); refLevel++) {
            Vector<XPathExpression> predicates = refInExpr.getPredicate(refLevel);
            if (predicates != null && predicates.size() > 0) {
                return refLevel;
            }
        }
        return -1;
    }
}
