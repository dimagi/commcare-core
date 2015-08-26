package org.commcare.cases.instance;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeReference;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseDataInstance extends ExternalDataInstance {
    public CaseDataInstance() {
    }

    protected CaseDataInstance(String reference, String instanceid) {
        super(reference, instanceid);
    }

    public CaseDataInstance buildDataInstance(String reference, String instanceId) {
        return new CaseDataInstance(reference, instanceId);
    }

    /**
     * Overrides JavaRosa's DataInstance implementation
     *
     * @param ref the reference path to be followed
     * @return was a valid path found for the reference?
     */
    public boolean hasTemplatePath(TreeReference ref) {
        return ref.isAbsolute();
                // && hasTemplatePathRec(ref, getBase(), 0);
    }

}
