package org.commcare.suite.model;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
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
 * @author ctsims
 */
public class SessionDatum implements Externalizable {

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

    public SessionDatum() {

    }

    public SessionDatum(String id, String nodeset, String shortDetail, String longDetail,
                        String inlineDetail, String persistentDetail, String value, String autoselect) {
        type = DATUM_TYPE_NORMAL;
        this.id = id;
        this.nodeset = XPathReference.getPathExpr(nodeset).getReference(true);
        this.shortDetail = shortDetail;
        this.longDetail = longDetail;
        this.inlineDetail = inlineDetail;
        this.persistentDetail = persistentDetail;
        this.value = value;
        this.autoSelectEnabled = "true".equals(autoselect);
    }

    public SessionDatum(String id, String value) {
        type = DATUM_TYPE_NORMAL;
        this.id = id;
        this.value = value;
    }

    public static SessionDatum FormIdDatum(String calculate) {
        SessionDatum ret = new SessionDatum();
        ret.id = "";
        ret.type = DATUM_TYPE_FORM;
        ret.value = calculate;
        return ret;
    }

    public String getDataId() {
        return id;
    }

    public TreeReference getNodeset() {
        return nodeset;
    }

    public String getShortDetail() {
        return shortDetail;
    }

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

    public int getType() {
        return type;
    }

    public boolean isAutoSelectEnabled() {
        return autoSelectEnabled;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        type = ExtUtil.readInt(in);

        if (ExtUtil.readBool(in)) {
            nodeset = (TreeReference)ExtUtil.read(in, TreeReference.class, pf);
        } else {
            nodeset = null;
        }
        shortDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        longDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        inlineDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        persistentDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        autoSelectEnabled = ExtUtil.readBool(in);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.writeNumeric(out, type);

        ExtUtil.writeBool(out, nodeset != null);
        if (nodeset != null) {
            ExtUtil.write(out, nodeset);
        }
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(shortDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(longDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(inlineDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(persistentDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
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
        predicates.addElement(new XPathEqExpr(true, XPathReference.getPathExpr(this.getValue()), new XPathStringLiteral(elementId)));
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
}
