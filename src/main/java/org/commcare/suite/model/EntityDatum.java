package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.AbstractTreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.expr.XPathEqExpr;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathStringLiteral;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Represents entity selection requirement in the current session. The nodeset
 * defines entities up for selection and detail fields are keys to display
 * formatting definitions
 */
public class EntityDatum extends SessionDatum {
    private TreeReference nodeset;
    private String shortDetail;
    private String longDetail;
    private String inlineDetail;
    private String persistentDetail;
    private boolean autoSelectEnabled;

    @SuppressWarnings("unused")
    public EntityDatum() {
        // used in serialization
    }

    public EntityDatum(String id, String nodeset, String shortDetail, String longDetail,
                        String inlineDetail, String persistentDetail, String value, String autoselect) {
        super(id, value);
        this.nodeset = XPathReference.getPathExpr(nodeset).getReference();
        this.shortDetail = shortDetail;
        this.longDetail = longDetail;
        this.inlineDetail = inlineDetail;
        this.persistentDetail = persistentDetail;
        this.autoSelectEnabled = "true".equals(autoselect);
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

    public boolean isAutoSelectEnabled() {
        return autoSelectEnabled;
    }

    /**
     *
     * @param ec
     * @return The case that would be auto-selected for this EntityDatum in the given eval context
     * if there is one, or null if there is not
     */
    public TreeReference getCurrentAutoselectableCase(EvaluationContext ec) {
        if (isAutoSelectEnabled()) {
            Vector<TreeReference> entityListElements = ec.expandReference(this.getNodeset());
            if (entityListElements.size() == 1) {
                return entityListElements.elementAt(0);
            }
        }
        return null;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        super.readExternal(in, pf);

        if (ExtUtil.readBool(in)) {
            nodeset = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        } else {
            nodeset = null;
        }
        shortDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        longDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        inlineDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        persistentDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        autoSelectEnabled = ExtUtil.readBool(in);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);

        ExtUtil.writeBool(out, nodeset != null);
        if (nodeset != null) {
            ExtUtil.write(out, nodeset);
        }
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(shortDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(longDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(inlineDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(persistentDetail));
        ExtUtil.writeBool(out, autoSelectEnabled);
    }

    /**
     * Takes an ID and identifies a reference in the provided context which corresponds
     * to that element if one can be found.
     *
     * NOT GUARANTEED TO WORK! May return an entity if one exists
     */
    public TreeReference getEntityFromID(EvaluationContext ec, String elementId) {
        //The uniqueid here is the value selected, so we can in theory track down the value we're looking for.

        //Get root nodeset
        TreeReference nodesetRef = this.getNodeset().clone();
        Vector<XPathExpression> predicates = nodesetRef.getPredicate(nodesetRef.size() - 1);
        if (predicates == null) {
            predicates = new Vector<>();
        }
        // For speed reasons, add a case id selection as the first predicate
        // This has potential to change outcomes if other predicates utilize 'position'
        XPathEqExpr caseIdSelection =
                new XPathEqExpr(XPathEqExpr.EQ, XPathReference.getPathExpr(this.getValue()), new XPathStringLiteral(elementId));
        predicates.insertElementAt(caseIdSelection, 0);
        nodesetRef.addPredicate(nodesetRef.size() - 1, predicates);

        Vector<TreeReference> elements = ec.expandReference(nodesetRef);
        if (elements.size() == 1) {
            return elements.firstElement();
        } else if (elements.size() > 1) {
            //Lots of nodes. Can't really choose one yet.
            return null;
        } else {
            return null;
        }
    }

    public static String getCaseIdFromReference(TreeReference contextRef,
                                                EntityDatum selectDatum,
                                                EvaluationContext ec) {
        // Grab the session's (form) element reference, and load it.
        TreeReference elementRef =
                XPathReference.getPathExpr(selectDatum.getValue()).getReference();
        AbstractTreeElement element =
                ec.resolveReference(elementRef.contextualize(contextRef));

        if (element != null && element.getValue() != null) {
            return element.getValue().uncast().getString();
        }
        return "";
    }
}
