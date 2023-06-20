package org.javarosa.core.model.instance.utils;

import static org.javarosa.core.model.instance.utils.TreeUtilities.xmlToTreeElement;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.InstanceBase;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.UnrecognisedInstanceRootException;
import org.javarosa.xml.util.InvalidStructureException;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

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

    /**
     *
     * @param limitingList a list of instance names to restrict the returning set to; null
     *                     if no limiting is being used
     * @return a hashtable representing the data instances that are in scope for this Entry,
     * potentially limited by @limitingList
     */
    public static Hashtable<String, DataInstance> getLimitedInstances(Set<String> limitingList,
            Hashtable<String, DataInstance> instances) {
        Hashtable<String, DataInstance> copy = new Hashtable<>();
        for (Enumeration en = instances.keys(); en.hasMoreElements(); ) {
            String key = (String)en.nextElement();

            //This is silly, all of these are external data instances. TODO: save their
            //construction details instead.
            DataInstance cur = instances.get(key);
            if (limitingList == null || limitingList.contains(cur.getInstanceId())) {
                // Make sure we either aren't using a limiting list, or the instanceid is in the list
                if (cur instanceof ExternalDataInstance) {
                    //Copy the EDI so when it gets populated we don't keep it dependent on this object's lifecycle!!
                    copy.put(key, new ExternalDataInstance(((ExternalDataInstance)cur).getReference(), cur.getInstanceId()));
                } else {
                    copy.put(key, cur);
                }
            }
        }

        return copy;
    }
}
