package org.javarosa.xpath;

import org.javarosa.core.log.FatalException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathBinaryOpExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.expr.XPathUnaryOpExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

public class XPathConditional implements IConditionExpr {
    private XPathExpression expr;
    public String xpath; //not serialized!
    private boolean hasNow; //indicates whether this XpathConditional contains the now() function (used for timestamping)

    public XPathConditional(String xpath) throws XPathSyntaxException {
        hasNow = xpath.contains("now()");
        this.expr = XPathParseTool.parseXPath(xpath);
        this.xpath = xpath;
    }

    public XPathConditional(XPathExpression expr) {
        this.expr = expr;
    }

    @SuppressWarnings("unused")
    public XPathConditional() {

    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        try {
            return XPathFuncExpr.unpack(expr.eval(model, evalContext));
        } catch (XPathUnsupportedException e) {
            if (xpath != null) {
                throw new XPathUnsupportedException(xpath);
            } else {
                throw e;
            }


        }
    }

    @Override
    public boolean eval(DataInstance model, EvaluationContext evalContext) {
        return XPathFuncExpr.toBoolean(evalRaw(model, evalContext)).booleanValue();
    }

    @Override
    public String evalReadable(DataInstance model, EvaluationContext evalContext) {
        return XPathFuncExpr.toString(evalRaw(model, evalContext));
    }

    @Override
    public Vector<TreeReference> evalNodeset(DataInstance model, EvaluationContext evalContext) {
        if (expr instanceof XPathPathExpr) {
            return ((XPathPathExpr)expr).evalRaw(model, evalContext).getReferences();
        } else {
            throw new FatalException("evalNodeset: must be path expression");
        }
    }

    /**
     * Gather the references that affect the outcome of evaluating this
     * conditional expression.
     *
     * @param originalContextRef context reference pointing to the nodeset
     *                           reference; used for expanding 'current()'
     * @return References of which this conditional expression depends on. Used
     * for retriggering the expression's evaluation if any of these references
     * value or relevancy calculations once.
     */
    @Override
    public Vector<TreeReference> getExprsTriggers(TreeReference originalContextRef) {
        Vector<TreeReference> triggers = new Vector<>();
        getExprsTriggersAccumulator(expr, triggers, null, originalContextRef);
        return triggers;
    }

    /**
     * Recursive helper to getExprsTriggers with an accumulator trigger vector.
     *
     * @param expr               Current expression we are collecting triggers from
     * @param triggers           Accumulates the references that this object's
     *                           expression value depends upon.
     * @param contextRef         Use this updated context; used, for instance,
     *                           when we move into handling predicates
     * @param originalContextRef Context reference pointing to the nodeset
     *                           reference; used for expanding 'current()'
     */
    private static void getExprsTriggersAccumulator(XPathExpression expr,
                                                    Vector<TreeReference> triggers,
                                                    TreeReference contextRef,
                                                    TreeReference originalContextRef) {
        if (expr instanceof XPathPathExpr) {
            TreeReference ref = ((XPathPathExpr)expr).getReference();
            TreeReference contextualized = ref;

            if (ref.getContext() == TreeReference.CONTEXT_ORIGINAL ||
                    (contextRef == null && !ref.isAbsolute())) {
                // Expr's ref begins with 'current()' or is relative and the
                // context ref is missing.
                contextualized = ref.contextualize(originalContextRef);
            } else if (contextRef != null) {
                contextualized = ref.contextualize(contextRef);
            }

            // find the references this reference depends on inside of predicates
            for (int i = 0; i < ref.size(); i++) {
                Vector<XPathExpression> predicates = ref.getPredicate(i);
                if (predicates == null) {
                    continue;
                }

                // contextualizing with ../'s present means we need to
                // calculate an offset to grab the appropriate predicates
                int basePredIndex = contextualized.size() - ref.size();

                TreeReference predicateContext = contextualized.getSubReference(basePredIndex + i);

                for (XPathExpression predicate : predicates) {
                    getExprsTriggersAccumulator(predicate, triggers,
                            predicateContext, originalContextRef);
                }
            }
            if (!triggers.contains(contextualized)) {
                triggers.addElement(contextualized);
            }
        } else if (expr instanceof XPathBinaryOpExpr) {
            getExprsTriggersAccumulator(((XPathBinaryOpExpr)expr).a, triggers,
                    contextRef, originalContextRef);
            getExprsTriggersAccumulator(((XPathBinaryOpExpr)expr).b, triggers,
                    contextRef, originalContextRef);
        } else if (expr instanceof XPathUnaryOpExpr) {
            getExprsTriggersAccumulator(((XPathUnaryOpExpr)expr).a, triggers,
                    contextRef, originalContextRef);
        } else if (expr instanceof XPathFuncExpr) {
            XPathFuncExpr fx = (XPathFuncExpr)expr;
            for (int i = 0; i < fx.args.length; i++)
                getExprsTriggersAccumulator(fx.args[i], triggers,
                        contextRef, originalContextRef);
        }
    }

    @Override
    public int hashCode() {
        return expr.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathConditional) {
            XPathConditional cond = (XPathConditional)o;
            return expr.equals(cond.expr);
        } else {
            return false;
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        expr = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        hasNow = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(expr));
        ExtUtil.writeBool(out, hasNow);
    }

    @Override
    public String toString() {
        return "xpath[" + expr.toString() + "]";
    }

    @Override
    public Vector<Object> pivot(DataInstance model, EvaluationContext evalContext) throws UnpivotableExpressionException {
        return expr.pivot(model, evalContext);
    }
}
