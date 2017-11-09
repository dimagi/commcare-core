package org.javarosa.xpath;

import org.javarosa.core.model.instance.TreeReference;

public class XPathMissingInstanceException extends XPathException {
    String instanceName;
    TreeReference ref;

    /**
     * Indicates that an instance is declared, but doesn't actually have any data in it
     * @param instanceName - the name of the empty instance
     * @param message
     */
    public XPathMissingInstanceException(String instanceName, String message) {
        super(message);
        this.instanceName = instanceName;
    }

    /**
     * Indicates that an instance was required to be present to evaluate a ref, but could not be
     * found at all
     * @param refThatNeededInstance - the ref that we were attempting to resolve, but could not
     *                              because no instance was found
     */
    public XPathMissingInstanceException(TreeReference refThatNeededInstance) {
        super("No instance was found with which to resolve reference: " + refThatNeededInstance);
        this.ref = refThatNeededInstance;
    }


}
