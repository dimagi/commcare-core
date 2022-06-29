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

public class QueryPromptValidation implements Externalizable {

    private XPathExpression xpath;
    private String message;

    @SuppressWarnings("unused")
    public QueryPromptValidation() {
    }

    public QueryPromptValidation(XPathExpression xpath, String message) {
        this.xpath = xpath;
        this.message = message;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        xpath = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        message = (String)ExtUtil.read(in, String.class, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(xpath));
        ExtUtil.write(out, message);
    }
}
