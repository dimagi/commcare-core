package org.javarosa.core.model.condition;

import org.javarosa.core.model.instance.TreeReference;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public interface HashRefResolver {
    TreeReference resolveLetRef(TreeReference reference);
}
