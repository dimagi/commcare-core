package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathStringLiteral;

import java.util.Vector;

/**
 * @author ctsims
 */
public abstract class SessionDatum implements Externalizable {

    private String id;
    private TreeReference nodeset;
    private String shortDetail;
    private String longDetail;
    private String inlineDetail;
    private String persistentDetail;
    private String value;
    private boolean autoSelectEnabled;

    private int type;

    public static final int DATUM_TYPE_NORMAL = 0;
    public static final int DATUM_TYPE_FORM = 1;

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

    public String getInlineDetail() {
        return inlineDetail;
    }

    public String getPersistentDetail() {
        return persistentDetail;
    }

    public String getValue() {
        return value;
    }
}
