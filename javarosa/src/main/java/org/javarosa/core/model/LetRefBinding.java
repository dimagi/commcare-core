package org.javarosa.core.model;

import org.javarosa.model.xform.XPathReference;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LetRefBinding {
    public final XPathReference ref;
    public final String var;

    public LetRefBinding(XPathReference ref, String var) {
        this.ref = ref;
        this.var = var;
    }
}
