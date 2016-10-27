package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author ctsims
 */
public class AssertionSet implements Externalizable {

    private Vector<String> xpathExpressions;
    private Vector<Text> messages;

    @SuppressWarnings("unused")
    public AssertionSet() {

    }

    /**
     * Create an set of assertion tests.
     * NOTE: The tests are _not parsed here_ to test their xpath expressions.
     * They should be tested _before_ being passed in (we don't do so here
     * to permit retaining the locality of which expression failed).
     */
    public AssertionSet(Vector<String> xpathExpressions, Vector<Text> messages) {
        //First, make sure things are set up correctly
        if (xpathExpressions.size() != messages.size()) {
            throw new IllegalArgumentException("Expression and message sets must be the same size");
        }

        this.xpathExpressions = xpathExpressions;
        this.messages = messages;
    }

    public Text getAssertionFailure(EvaluationContext ec) {
        try {
            for (int i = 0; i < xpathExpressions.size(); ++i) {
                XPathExpression expression = XPathParseTool.parseXPath(xpathExpressions.elementAt(i));
                try {
                    Object val = expression.eval(ec);
                    if (!XPathFuncExpr.toBoolean(val)) {
                        return messages.elementAt(i);
                    }
                } catch (Exception e) {
                    return messages.elementAt(i);
                }
            }
            return null;
        } catch (XPathSyntaxException xpe) {
            throw new XPathException("Assertion somehow failed to parse after validating : " + xpe.getMessage());
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        this.xpathExpressions = (Vector<String>)ExtUtil.read(in, new ExtWrapList(String.class));
        this.messages = (Vector<Text>)ExtUtil.read(in, new ExtWrapList(Text.class));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapList(xpathExpressions));
        ExtUtil.write(out, new ExtWrapList(messages));
    }
}
