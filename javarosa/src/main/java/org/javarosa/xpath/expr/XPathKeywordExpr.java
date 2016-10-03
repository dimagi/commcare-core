package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Keywords (':some-keyword') are a syntactical aid with no semantic meaning.
 *
 * Currently only used by the 'cond()' to designate the 'else' branch:
 * > cond(1 = 0, 'inconsistent world', :else, 'consistent world')
 */
public class XPathKeywordExpr extends XPathExpression {
    public XPathQName id;

    @SuppressWarnings("unused")
    public XPathKeywordExpr() {
    } //for deserialization

    public XPathKeywordExpr(XPathQName id) {
        this.id = id;
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return evalContext.getVariable(id.toString());
    }

    @Override
    public String toString() {
        return "{keyword :" + id.toString() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathKeywordExpr) {
            XPathKeywordExpr x = (XPathKeywordExpr)o;
            return id.equals(x.id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = (XPathQName)ExtUtil.read(in, XPathQName.class);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, id);
    }

    @Override
    public String toPrettyString() {
        return ":" + id.toString();
    }
}