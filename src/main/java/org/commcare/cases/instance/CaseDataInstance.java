package org.commcare.cases.instance;

import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.IOException;

/**
 * An external data instance that respects CaseDB template specifications.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseDataInstance extends ExternalDataInstance {
    private static TreeElement caseDbSpecTemplate = null;
    private static final String CASEDB_WILD_CARD = "CASEDB_WILD_CARD";

    public CaseDataInstance() {
        // For serialization
    }

    /**
     * Copy constructor
     */
    public CaseDataInstance(ExternalDataInstance instance) {
        super(instance);
    }

    /**
     * Does the reference follow the statically defined CaseDB spec?
     */
    @Override
    public boolean hasTemplatePath(TreeReference ref) {
        loadTemplateSpecLazily();


        // enforce (artificial) constraint that the instance name matches the
        // root element name. For instance, instance('casedb')/casedb
        boolean instanceNameMatch = ref.size() > 0 && instanceid.equals(ref.getName(0));

        return instanceNameMatch && followsTemplateSpec(ref, caseDbSpecTemplate, 1);
    }

    private static synchronized void loadTemplateSpecLazily() {
        final String errorMsg = "Failed to load casedb template spec xml file " +
                "while checking if case related xpath follows the template structure.";
        if (caseDbSpecTemplate == null) {
            try {
                caseDbSpecTemplate =
                        FormLoadingUtils.stringToTreeElement("" +
                            "<wrapper>\n" +
                            "    <case case_id=\"\" case_type=\"\" owner_id=\"\" status=\"\" external_id=\"\">\n" +
                            "        <!-- case_id: The unique GUID of this case -->\n" +
                            "        <!-- case_type: The id of this case's type -->\n" +
                            "        <!-- owner_id: The GUID of the case or group which owns this case -->\n" +
                            "        <!-- status: 'open' if the case has not been closed. 'closed' if the case has -->\n" +
                            "        <case_name/>\n" +
                            "        <!-- The name of the case-->\n" +
                            "        <date_opened/>\n" +
                            "        <!-- The date this case was opened -->\n" +
                            "        <last_modified/>\n" +
                            "        <!-- The date of the case's last transaction -->\n" +
                            "        <CASEDB_WILD_CARD/>\n" +
                            "        <!-- An arbitrary data value set in this case -->\n" +
                            "        <index>\n" +
                            "            <CASEDB_WILD_CARD case_type=\"\" relationship=\"\"/>\n" +
                            "            <!-- An index to another case of the given type -->\n" +
                            "            <!-- @case_type: Exactly one - the type of the indexed case -->\n" +
                            "            <!-- @relationship: Exactly one - the relationship of this case to the indexed case. See the casexml spec for details -->\n" +
                            "        </index>\n" +
                            "        <attachment>\n" +
                            "            <CASEDB_WILD_CARD/>\n" +
                            "            <!-- A named element which provides a reference to an attachment in the local environment. This attachment may or may not be currently available (if it is being processed asynchronously, for instance, but should have a valid JR reference URI either way whose existence can be checked.-->\n" +
                            "        </attachment>\n" +
                            "    </case>\n" +
                            "</wrapper>");
            } catch (InvalidStructureException | IOException e) {
                throw new RuntimeException(errorMsg);
            }
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

    @Override
    public boolean useCaseTemplate() {
        return true;
    }
}
