package org.commcare.cases.instance;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.IOException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseDataInstance extends ExternalDataInstance {
    private static TreeElement template;
    private static final String CASEDB_WILD_CARD = "CASEDB_WILD_CARD";

    static {
        try {
            template = FormLoadingUtils.xmlToTreeElement("/casedb_instance_structure.xml");
        } catch (InvalidStructureException  e) {
            template = null;
        } catch (IOException e) {
            template = null;
        }
    }

    public CaseDataInstance() {
    }

    protected CaseDataInstance(String reference, String instanceid) {
        super(reference, instanceid);
    }

    public CaseDataInstance buildExternalDataInstance(String reference, String instanceId) {
        return new CaseDataInstance(reference, instanceId);
    }

    /**
     * Overrides JavaRosa's DataInstance implementation
     *
     * @param ref the reference path to be followed
     * @return was a valid path found for the reference?
     */
    public boolean hasTemplatePath(TreeReference ref) {
        if (template != null) {
            return pathsMatch(ref, template, 0);
        } else {
            return super.hasTemplatePath(ref);
        }
    }

    private static boolean pathsMatch(TreeReference ref, TreeElement currTemplateNode, int depth) {
        if (currTemplateNode == null) {
            return false;
        }

        if (depth == ref.size()) {
            return true;
        }

        String name = ref.getName(depth);

        if (ref.getMultiplicity(depth) == TreeReference.INDEX_ATTRIBUTE) {
            return pathsMatch(ref, currTemplateNode.getAttribute(null, name), depth + 1);
        } else {
            TreeElement nextTemplateNode = currTemplateNode.getChild(name, 0);
            if (nextTemplateNode ==  null) {
                nextTemplateNode = currTemplateNode.getChild(CASEDB_WILD_CARD, 0);
            }
            return pathsMatch(ref, nextTemplateNode, depth + 1);
        }
    }
}
