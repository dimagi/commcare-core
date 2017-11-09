package org.javarosa.xpath;

import org.javarosa.core.model.instance.TreeReference;

/**
 * An exception indicating either that:
 * a) An instance is declared, but doesn't actually have any data in it
 * b) An instance was required to be present to evaluate a ref, but was not
 */
public class XPathMissingInstanceException extends XPathException {
    String instanceName;
    TreeReference ref;

    public XPathMissingInstanceException(String instanceName, String message) {
        super(message);
        this.instanceName = instanceName;
    }

    public XPathMissingInstanceException(TreeReference refThatNeededInstance) {
        super("No instance was found with which to resolve reference: " + refThatNeededInstance);
        this.ref = refThatNeededInstance;
    }


}
