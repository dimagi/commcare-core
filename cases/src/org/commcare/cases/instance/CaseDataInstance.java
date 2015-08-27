package org.commcare.cases.instance;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.core.services.Logger;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.IOException;

/**
 * An external data instance that respects CaseDB template specifications.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseDataInstance extends ExternalDataInstance {
    private static TreeElement caseDbSpecTemplate;
    private static final String CASEDB_WILD_CARD = "CASEDB_WILD_CARD";

    static {
        try {
            caseDbSpecTemplate =
                    FormLoadingUtils.xmlToTreeElement("/casedb_instance_structure.xml");
        } catch (InvalidStructureException e) {
            caseDbSpecTemplate = null;
        } catch (IOException e) {
            caseDbSpecTemplate = null;
        }
    }

    public CaseDataInstance() {
        // For serialization
    }

    private CaseDataInstance(String reference, String instanceid) {
        super(reference, instanceid);
    }

    public CaseDataInstance buildExternalDataInstance(String reference, String instanceId) {
        return new CaseDataInstance(reference, instanceId);
    }

    /**
     * Does the reference follow the statically defined CaseDB spec?
     */
    public boolean hasTemplatePath(TreeReference ref) {
        if (caseDbSpecTemplate != null) {
            return followsTemplateSpec(ref, caseDbSpecTemplate, 0);
        } else {
            // failed to load casedb spec template, default to super implementation
            Logger.log("CaseDb Warning",
                    "Using default hasTemplatePath implementation: reference resolution will break!");
            return super.hasTemplatePath(ref);
        }
    }

    private static boolean followsTemplateSpec(TreeReference refToCheck,
                                               TreeElement currTemplateNode,
                                               int currRefDepth) {
        if (currTemplateNode == null) {
            return false;
        }

        if (currRefDepth == refToCheck.size()) {
            return true;
        }

        String name = refToCheck.getName(currRefDepth);

        if (refToCheck.getMultiplicity(currRefDepth) == TreeReference.INDEX_ATTRIBUTE) {
            TreeElement templateAttr = currTemplateNode.getAttribute(null, name);
            return followsTemplateSpec(refToCheck, templateAttr, currRefDepth + 1);
        } else {
            TreeElement nextTemplateNode = currTemplateNode.getChild(name, 0);
            if (nextTemplateNode == null) {
                // didn't find a node of the given name in the template, check
                // if a wild card exists at this level of the template.
                nextTemplateNode = currTemplateNode.getChild(CASEDB_WILD_CARD, 0);
            }
            return followsTemplateSpec(refToCheck, nextTemplateNode, currRefDepth + 1);
        }
    }
}
