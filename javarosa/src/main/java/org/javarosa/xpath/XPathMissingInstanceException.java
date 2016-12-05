package org.javarosa.xpath;

import java.text.MessageFormat;

public class XPathMissingInstanceException extends XPathException {
    final String instanceName;
    final String expression;
    final String contextRef;
    final String message;

    public XPathMissingInstanceException(String instanceName, String expression, String contextRef, String message) {
        super();
        this.instanceName = instanceName;
        this.expression = expression;
        this.contextRef = contextRef;
        this.message = message;
    }

    @Override
    public String getMessage() {
        Object[] args = new Object[] {instanceName, expression, contextRef, message};
        MessageFormat msg = new MessageFormat("The instance \"{0}\" in expression \"{1}\" used by \"{2}\" {3}. " +
                "Please correct your form or application.");
        return msg.format(args);
    }
}
