/**
 * 
 */
package org.commcare.cases.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

/**
 * @author ctsims
 *
 */
public class CaseIndex implements Externalizable {
    
    private String name;
    private String targetId;
    private String targetCaseType;
    
    /*
     * serialization only!
     */
    public CaseIndex() {
        
    }
    
    public CaseIndex(String name, String targetCaseType, String targetId) {
        this.name = name;
        this.targetId = targetId;
        this.targetCaseType = targetCaseType;
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#readExternal(java.io.DataInputStream, org.javarosa.core.util.externalizable.PrototypeFactory)
     */
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        name = ExtUtil.readString(in);
        targetId = ExtUtil.readString(in);
        targetCaseType = ExtUtil.readString(in);
    }

    /* (non-Javadoc)
     * @see org.javarosa.core.util.externalizable.Externalizable#writeExternal(java.io.DataOutputStream)
     */
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, name);
        ExtUtil.writeString(out, targetId);
        ExtUtil.writeString(out, targetCaseType);
    }

    public String getName() {
        return name;
    }

    public String getTargetType() {
        return targetCaseType;
    }

    public String getTarget() {
        return targetId;
    }

}
