/**
 * 
 */
package org.commcare.suite.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.model.xform.XPathReference;

/**
 * @author ctsims
 *
 */
public class SessionDatum implements Externalizable {
    
    private String id;
    private TreeReference nodeset;
    private String shortDetail; 
    private String longDetail;
    private String inlineDetail;
    private String value;
    
    private int type;
    
    public static final int DATUM_TYPE_NORMAL = 0;
    public static final int DATUM_TYPE_FORM = 1;
    
    public SessionDatum() {
        
    }

    public SessionDatum(String id, String nodeset, String shortDetail, String longDetail, String inlineDetail, String value) {
        type = DATUM_TYPE_NORMAL;
        this.id = id;
        this.nodeset = XPathReference.getPathExpr(nodeset).getReference(true);
        this.shortDetail = shortDetail;
        this.longDetail = longDetail;
        this.inlineDetail = inlineDetail;
        this.value = value;
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

    /**
     * @return the shortDetail
     */
    public String getShortDetail() {
        return shortDetail;
    }

    /**
     * @return the longDetail
     */
    public String getLongDetail() {
        return longDetail;
    }
    
    /**
     * @return the inlineDetail
     */
    public String getInlineDetail() {
        return inlineDetail;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    public int getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        id = ExtUtil.readString(in);
        type = ExtUtil.readInt(in);
        
        if(ExtUtil.readBool(in)) {
            nodeset = (TreeReference)ExtUtil.read(in, TreeReference.class);
        } else {
            nodeset = null;
        }
        shortDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        longDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        inlineDetail = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        value = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, id);
        ExtUtil.writeNumeric(out, type);
        
        ExtUtil.writeBool(out, nodeset != null);
        if(nodeset != null) {
            ExtUtil.write(out, nodeset);
        }
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(shortDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(longDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(inlineDetail));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(value));
    }

}
