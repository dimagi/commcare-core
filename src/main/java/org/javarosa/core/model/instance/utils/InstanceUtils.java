package org.javarosa.core.model.instance.utils;

import static org.javarosa.core.model.instance.utils.TreeUtilities.xmlToTreeElement;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.UnrecognisedInstanceRootException;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.IOException;

/**
 * Collection of static instance loading methods
 *
 * @author Phillip Mates
 */
public class InstanceUtils {

    public static FormInstance loadFormInstance(String formFilepath)
            throws InvalidStructureException, IOException {
        TreeElement root = xmlToTreeElement(formFilepath);
        return new FormInstance(root, null);
    }

    /**
     * Sets instance properties to the given instance root
     *
     * @param instanceRoot instance root
     * @param instanceId   instance id to set
     * @param instanceBase instance base to set
     */
    public static void setUpInstanceRoot(AbstractTreeElement instanceRoot, String instanceId,
            InstanceBase instanceBase) {
        if (instanceRoot instanceof TreeElement) {
            TreeElement rootAsTreeElement = ((TreeElement)instanceRoot);
            rootAsTreeElement.setInstanceName(instanceId);
            rootAsTreeElement.setParent(instanceBase);
        } else if (instanceRoot instanceof CaseInstanceTreeElement) {
            CaseInstanceTreeElement caseInstanceRoot = ((CaseInstanceTreeElement)instanceRoot);
            caseInstanceRoot.rebase(instanceBase);
        } else {
            String error = "Unrecognised Instance root of type " + instanceRoot.getClass().getName() +
                    " for instance " + instanceId;
            throw new UnrecognisedInstanceRootException(error);
        }
    }
}
