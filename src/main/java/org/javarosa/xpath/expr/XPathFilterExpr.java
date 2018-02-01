package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.XPathAnalyzer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * This construct, whose syntax is of the form '(filter-expr pred1 pred2 ...)',
 * is currently unsupported by JavaRosa
 */
public class XPathFilterExpr extends XPathExpression {
    public XPathExpression x;
    public XPathExpression[] predicates;

    public XPathFilterExpr() {
    } //for deserialization

    public XPathFilterExpr(XPathExpression x, XPathExpression[] predicates) {
        this.x = x;
        this.predicates = predicates;
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        throw new XPathUnsupportedException("filter expression");
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("{filt-expr:");
        sb.append(x.toString());
        sb.append(",{");
        for (int i = 0; i < predicates.length; i++) {
            sb.append(predicates[i].toString());
            if (i < predicates.length - 1)
                sb.append(",");
        }
        sb.append("}}");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathFilterExpr) {
            XPathFilterExpr fe = (XPathFilterExpr)o;

            return x.equals(fe.x) && ExtUtil.arrayEquals(predicates, fe.predicates, false);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int predHash = 0;
        for (XPathExpression pred : predicates) {
            predHash ^= pred.hashCode();
        }
        return x.hashCode() ^ predHash;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        x = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        Vector v = (Vector)ExtUtil.read(in, new ExtWrapListPoly(), pf);

        predicates = new XPathExpression[v.size()];
        for (int i = 0; i < predicates.length; i++) {
            predicates[i] = (XPathExpression)v.elementAt(i);
        }
        computedCacheability = ExtUtil.readBool(in);
        isCacheable = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        Vector<XPathExpression> v = new Vector<>();
        for (XPathExpression predicate : predicates) {
            v.addElement(predicate);
        }

        ExtUtil.write(out, new ExtWrapTagged(x));
        ExtUtil.write(out, new ExtWrapListPoly(v));
        ExtUtil.writeBool(out, computedCacheability);
        ExtUtil.writeBool(out, isCacheable);
    }

    @Override
    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        throw new UnpivotableExpressionException();
    }

    @Override
    public String toPrettyString() {
        return "Unsupported Predicate";
    }

    @Override
    public void applyAndPropagateAnalyzer(XPathAnalyzer analyzer) throws AnalysisInvalidException {
        if (analyzer.shortCircuit()) {
            return;
        }
        analyzer.doAnalysis(XPathFilterExpr.this);
        this.x.applyAndPropagateAnalyzer(analyzer);
        for (XPathExpression expr : this.predicates) {
            expr.applyAndPropagateAnalyzer(analyzer);
        }
    }
}
