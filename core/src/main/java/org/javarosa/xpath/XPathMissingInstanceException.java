package org.javarosa.xpath;

public class XPathMissingInstanceException extends XPathException {
    String instanceName;

    public XPathMissingInstanceException(String instanceName, String message) {
        super(message);
        this.instanceName = instanceName;
    }
}
