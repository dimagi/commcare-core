package org.javarosa.core.model;

import org.javarosa.core.model.instance.TreeReference;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LetRefBinding {
    public final TreeReference ref;
    public final TreeReference var;

    public LetRefBinding(TreeReference ref, TreeReference var) {
        this.ref = ref;
        this.var = var;
    }
}
