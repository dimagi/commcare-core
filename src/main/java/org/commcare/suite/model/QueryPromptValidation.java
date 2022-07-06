package org.commcare.suite.model;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Model for <validation> node in {@link QueryPrompt}
 */
public class QueryPromptValidation implements Externalizable {

    private XPathExpression test;
    private Text message;

    @SuppressWarnings("unused")
    public QueryPromptValidation() {
    }

    public QueryPromptValidation(XPathExpression test, Text message) {
        this.test = test;
        this.message = message;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        test = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        message = (Text)ExtUtil.read(in, Text.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(test));
        ExtUtil.write(out, message);
    }

    public XPathExpression getTest() {
        return test;
    }

    public Text getMessage() {
        return message;
    }
}
