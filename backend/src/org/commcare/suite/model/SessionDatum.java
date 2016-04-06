package org.commcare.suite.model;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.Externalizable;

/**
 * @author ctsims
 */
public abstract class SessionDatum implements Externalizable{

    public String getDataId() {
        return id;
    }

    public TreeReference getNodeset() {
        return nodeset;
    }

    /**
     * the ID of a detail that structures the screen for selecting an item from the nodeset
     */
    public String getShortDetail() {
        return shortDetail;
    }

    /**
     * the ID of a detail that will show a selected item for confirmation. If not present,
     * no confirmation screen is shown after item selection
     */
    public String getLongDetail() {
        return longDetail;
    }

    public String getValue() {
        return value;
    }
}
