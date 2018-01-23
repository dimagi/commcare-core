package org.javarosa.xpath.expr;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.analysis.AnalysisInvalidException;
import org.javarosa.xpath.analysis.XPathAnalyzer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class XPathUnaryOpExpr extends XPathOpExpr {
    public XPathExpression a;

    public XPathUnaryOpExpr() {
         // for deserialization of children
    }

    public XPathUnaryOpExpr(XPathExpression a) {
        this.a = a;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathUnaryOpExpr) {
            XPathUnaryOpExpr x = (XPathUnaryOpExpr)o;
            return a.equals(x.a);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return a.hashCode();
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        a = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(a));
    }

    @Override
    public void applyAndPropagateAnalyzer(XPathAnalyzer analyzer) throws AnalysisInvalidException {
        if (analyzer.shortCircuit()) {
            return;
        }
        analyzer.doAnalysis(XPathUnaryOpExpr.this);
        this.a.applyAndPropagateAnalyzer(analyzer);
    }
}
