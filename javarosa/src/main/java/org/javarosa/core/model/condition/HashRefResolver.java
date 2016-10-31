package org.javarosa.core.model.condition;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathStep;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public interface HashRefResolver {
    TreeReference resolveLetRef(TreeReference reference);
    XPathStep[] resolveLetRefPathSteps(XPathStep varStep);
}
