package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.analysis.XPathAccumulatingAnalyzer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathNumericLiteral extends XPathExpression {
    public double d;

    @SuppressWarnings("unused")
    public XPathNumericLiteral() {
    } //for deserialization

    public XPathNumericLiteral(Double d) {
        this.d = d;
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return new Double(d);
    }

    @Override
    public String toString() {
        return "{num:" + Double.toString(d) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathNumericLiteral) {
            XPathNumericLiteral x = (XPathNumericLiteral)o;
            return (Double.isNaN(d) ? Double.isNaN(x.d) : d == x.d);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (new Long(Double.doubleToLongBits(d))).hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        if (in.readByte() == (byte)0x00) {
            d = ExtUtil.readNumeric(in);
        } else {
            d = ExtUtil.readDecimal(in);
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        if (d == (int)d) {
            out.writeByte(0x00);
            ExtUtil.writeNumeric(out, (int)d);
        } else {
            out.writeByte(0x01);
            ExtUtil.writeDecimal(out, d);
        }
    }

    @Override
    public String toPrettyString() {
        return Double.toString(d);
    }

    @Override
    public void applyAndPropagateAccumulatingAnalyzer(XPathAccumulatingAnalyzer analyzer) {
        analyzer.extractTargetValues(XPathNumericLiteral.this);
    }
}
